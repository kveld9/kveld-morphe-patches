package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val enableLiveSearchPatch = bytecodePatch(
    name = "Enable Live Search",
    description = "Shows TikTok's search entry in the Live drawer where supported.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        try {
            val gate = Fingerprint(
                definingClass = "Lcom/bytedance/android/livesdk/livesetting/feed/LiveDrawerSearchEnableSetting;",
                name = "getValue",
            ).method
            val returnType = gate.returnType.toString()
            if (returnType != "Z" && returnType != "I") {
                println("[Enable Live Search] Note: getValue returns $returnType, skipped.")
            } else {
                val returnIndices = gate.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()
                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    gate.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        "const/4 v$reg, 0x1",
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Enable Live Search] Forced LiveDrawerSearchEnableSetting.getValue (${returnIndices.size} return(s)) -> enabled.")
                    patched++
                }
            }
        } catch (e: Exception) {
            println("[Enable Live Search] Live drawer search gate note: ${e.message}")
        }

        println("[Enable Live Search] Applied $patched live search hook(s).")
    }
}
