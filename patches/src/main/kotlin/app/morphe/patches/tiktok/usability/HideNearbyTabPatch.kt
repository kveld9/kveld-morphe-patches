package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

val hideNearbyTabPatch = bytecodePatch(
    name = "Hide Nearby Feed Tab",
    description = "Removes the Nearby (local city or region) feed tab from the top navigation feed strip.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Discover and hook NearbyTabProvider from NearbyServiceImpl.LJIIZILJ()
        val serviceClass = "Lcom/ss/android/ugc/nearby/service/NearbyServiceImpl;"
        val serviceFp = Fingerprint(
            definingClass = serviceClass,
            name = "LJIIZILJ",
            parameters = emptyList(),
        )
        var tabProviderClass = "LX/03ry;"
        val serviceInsns = serviceFp.method.implementation?.instructions
        if (serviceInsns != null) {
            for (insn in serviceInsns) {
                val ref = (insn as? ReferenceInstruction)?.reference
                if (ref is TypeReference) {
                    tabProviderClass = ref.type
                    break
                }
            }
        }

        // Hook tabProvider.LJ() -> return null (forces TopTabProtocol to null so TopTabOperator skips adding it)
        Fingerprint(
            definingClass = tabProviderClass,
            name = "LJ",
            parameters = emptyList(),
        ).method.replaceWithReturnNull()
        println("[Hide Nearby Feed Tab] Hooked $tabProviderClass.LJ() -> null (TopTabProtocol eliminated).")
        patched++

        // 2. Hook NearbyTabProtocol.enable() -> return false
        val nearbyTabClass = "Lcom/ss/android/ugc/nearby/tab/NearbyTabProtocol;"
        val nearbyTabFp = Fingerprint(
            definingClass = nearbyTabClass,
            name = "enable",
            returnType = "Z",
            parameters = emptyList(),
        )

        // Discover and hook the experiment boolean evaluator (e.g. LX/05tt.LIZIZ())
        val nearbyTabInsns = nearbyTabFp.method.implementation?.instructions
        if (nearbyTabInsns != null) {
            for (insn in nearbyTabInsns) {
                val ref = (insn as? ReferenceInstruction)?.reference
                if (ref is MethodReference && !ref.definingClass.startsWith("Ljava/")) {
                    val evalClass = ref.definingClass
                    val methodName = ref.name
                    Fingerprint(
                        definingClass = evalClass,
                        name = methodName,
                        returnType = "Z",
                        parameters = emptyList(),
                    ).method.replaceWithReturnBoolean(false)
                    println("[Hide Nearby Feed Tab] Hooked $evalClass.$methodName() -> false (Nearby experiment evaluator).")
                    patched++
                    break
                }
            }
        }

        nearbyTabFp.method.replaceWithReturnBoolean(false)
        println("[Hide Nearby Feed Tab] Hooked NearbyTabProtocol.enable() -> false.")
        patched++

        // 3. Hook NearbyServiceImpl boolean gates
        Fingerprint(
            definingClass = serviceClass,
            name = "LJIIIIZZ",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Nearby Feed Tab] Hooked NearbyServiceImpl.LJIIIIZZ() -> false.")
        patched++

        Fingerprint(
            definingClass = serviceClass,
            name = "LJIIL",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Nearby Feed Tab] Hooked NearbyServiceImpl.LJIIL() -> false.")
        patched++

        println("[Hide Nearby Feed Tab] Successfully applied $patched hook(s) -> Nearby feed tab eliminated.")
    }
}
