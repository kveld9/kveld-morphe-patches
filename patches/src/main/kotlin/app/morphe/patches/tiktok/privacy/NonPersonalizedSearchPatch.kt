package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Forces every boolean return point of [method] to true.
 */
private fun forceBooleanGateTrue(method: MutableMethod, tag: String): Int {
    val instructions = method.implementation?.instructions ?: return 0
    val returnIndices = instructions.withIndex()
        .filter { it.value.opcode == Opcode.RETURN }
        .map { it.index to (it.value as OneRegisterInstruction).registerA }
        .toList()
    returnIndices.asReversed().forEach { (returnIndex, reg) ->
        method.addInstructionsAtControlFlowLabel(
            returnIndex,
            "const/4 v$reg, 0x1",
        )
    }
    if (returnIndices.isNotEmpty()) {
        println("[$tag] Forced ${returnIndices.size} gate return(s) -> true.")
    }
    return if (returnIndices.isNotEmpty()) 1 else 0
}

val nonPersonalizedSearchPatch = bytecodePatch(
    name = "Non-Personalized Search",
    description = "Forces TikTok's non-personalized search mode instead of the saved account choice.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        try {
            val gate = Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("enable_non_personalized_search"),
            ).method
            patched += forceBooleanGateTrue(gate, "Non-Personalized Search")
        } catch (e: Exception) {
            println("[Non-Personalized Search] Search gate note: ${e.message}")
        }

        try {
            val state = Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("non_personalized_search_state_"),
            ).method
            patched += forceBooleanGateTrue(state, "Non-Personalized Search")
        } catch (e: Exception) {
            println("[Non-Personalized Search] Search state note: ${e.message}")
        }

        println("[Non-Personalized Search] Applied $patched non-personalized search hook(s).")
    }
}
