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

val gboardEnableCursorTrackpadPatch = bytecodePatch(
    name = "Enable Cursor Trackpad",
    description = "Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardPhenotypeResiliencePatch)

    execute {
        val fingerprint = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("free_cursor"), string("free_cursor_lock_mode")),
        )

        val targetClass = LocaleUtils.cleanClassName(fingerprint.originalClassDef.type)
        var patched = 0

        // Process instruction matches in descending index order so insertions never shift offsets of subsequent matches
        for (match in fingerprint.instructionMatches.sortedByDescending { it.index }) {
            val matchIndex = match.index
            val nextInsn = fingerprint.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is OneRegisterInstruction -> nextInsn.registerA
                is FiveRegisterInstruction -> nextInsn.registerD
                else -> 1
            }
            val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
            fingerprint.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
            patched++
        }

        println("[Cursor Trackpad] Injected $patched flag override(s) into $targetClass.<clinit>() -> 2D spacebar trackpad enabled.")
    }
}
