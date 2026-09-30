package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

val hideCommunityTabPatch = bytecodePatch(
    name = "Hide Community Tab",
    description = "Removes the Community (Explore) tab from the top navigation feed strip.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Hook ExploreFeedServiceImpl.LIZ() -> return false (disables explore feed service)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;",
            name = "LIZ",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Community Tab] Hooked ExploreFeedServiceImpl.LIZ() -> false.")
        patched++

        // 2. Discover and hook ExploreTabProvider from ExploreFeedServiceImpl.LJJIII()
        val serviceFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/explore/service/ExploreFeedServiceImpl;",
            name = "LJJIII",
            parameters = emptyList(),
        )
        var tabProviderClass = "LX/03rx;"
        val insns = serviceFp.method.implementation?.instructions
        if (insns != null) {
            for (insn in insns) {
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
        println("[Hide Community Tab] Hooked $tabProviderClass.LJ() -> null (TopTabProtocol eliminated).")
        patched++

        // Hook tabProvider.LIZ() -> return null (neutralizes bottom tab protocol if present)
        Fingerprint(
            definingClass = tabProviderClass,
            name = "LIZ",
            parameters = emptyList(),
        ).method.replaceWithReturnNull()
        println("[Hide Community Tab] Hooked $tabProviderClass.LIZ() -> null (BottomTabProtocol eliminated).")
        patched++

        // 3. Hook ExploreXTabProtocol.enable() -> return false (legacy tab fallback)
        val xTabClass = "Lcom/ss/android/ugc/aweme/explore/entrance/ExploreXTabProtocol;"
        val xTabFp = Fingerprint(
            definingClass = xTabClass,
            name = "enable",
            returnType = "Z",
            parameters = emptyList(),
        )

        // Discover and hook the experiment boolean evaluator (e.g. LX/04J8)
        val xTabInsns = xTabFp.method.implementation?.instructions
        if (xTabInsns != null) {
            for (insn in xTabInsns) {
                val ref = (insn as? ReferenceInstruction)?.reference
                if (ref is MethodReference) {
                    val evalClass = ref.definingClass
                    Fingerprint(
                        definingClass = evalClass,
                        name = "LIZIZ",
                        returnType = "Z",
                        parameters = emptyList(),
                    ).method.replaceWithReturnBoolean(false)
                    println("[Hide Community Tab] Hooked $evalClass.LIZIZ() -> false (top tab evaluator).")
                    patched++

                    Fingerprint(
                        definingClass = evalClass,
                        name = "LIZ",
                        returnType = "Z",
                        parameters = emptyList(),
                    ).method.replaceWithReturnBoolean(false)
                    println("[Hide Community Tab] Hooked $evalClass.LIZ() -> false (bottom tab evaluator).")
                    patched++
                    break
                }
            }
        }

        xTabFp.method.replaceWithReturnBoolean(false)
        println("[Hide Community Tab] Hooked ExploreXTabProtocol.enable() -> false.")
        patched++

        // 4. Hook ExploreBottomTabProtocol.enable() -> return false
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/explore/entrance/ExploreBottomTabProtocol;",
            name = "enable",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Community Tab] Hooked ExploreBottomTabProtocol.enable() -> false.")
        patched++

        println("[Hide Community Tab] Successfully applied $patched hook(s) -> Comunidad tab neutralized.")
    }
}
