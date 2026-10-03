package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardVoiceTypingIncognitoPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

    dependsOn(gboardCoreIntegrityPatch)

    execute {
        val fp = Fingerprint(
            returnType = "Z",
            parameters = listOf(
                "Landroid/content/Context;",
                "Landroid/view/inputmethod/EditorInfo;",
                "Z",
            ),
            filters = OpcodesFilter.opcodesToFilters(
                Opcode.RETURN,
                Opcode.INVOKE_STATIC,
                Opcode.MOVE_RESULT,
                Opcode.IF_NEZ,
                Opcode.IF_EQZ,
            ),
        )

        fp.method.apply {
            val match = fp.instructionMatches.last()
            val insn = getInstruction<OneRegisterInstruction>(match.index)
            val reg = insn.registerA
            addInstructions(
                match.index,
                """
                    invoke-static {v$reg}, ${Constants.GBOARD_EXTENSION_CLASS}->overrideVoiceTypingIncognito(Z)Z
                    move-result v$reg
                """.trimIndent(),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Voice Typing in Incognito] Hooked incognito check in $targetClass.${fp.method.name}() -> voice dictation controlled by preference.")
        }
    }
}
