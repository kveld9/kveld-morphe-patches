package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardEnableDismissSuggestionsButtonPatch = bytecodePatch(
    name = "Enable Dismiss Suggestions Button",
    description = "Adds a close button (X) to dismiss proactive suggestions on the suggestion bar.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardPhenotypeResiliencePatch)

    execute {
        val fingerprint = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("enable_close_proactive_suggestions_access_point")),
        )

        val matchIndex = fingerprint.instructionMatches.first().index
        val nextInsn = fingerprint.method.getInstruction<Instruction>(matchIndex + 1)
        val reg = when (nextInsn) {
            is OneRegisterInstruction -> nextInsn.registerA
            is FiveRegisterInstruction -> nextInsn.registerD
            else -> 1
        }
        val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
        fingerprint.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")

        val targetClass = LocaleUtils.cleanClassName(fingerprint.originalClassDef.type)
        println("[Dismiss Suggestions Button] Injected flag override into $targetClass.<clinit>() at opcode index $insertIndex")
    }
}
