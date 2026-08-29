package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode

val gboardForceIncognitoPatch = bytecodePatch(
    name = "Force Incognito Mode",
    description = "Forces Gboard to always operate in incognito mode (disabling personalized learning and persistent input logging) while keeping clipboard functionality enabled.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val fp1 = Fingerprint(
            definingClass = "Lsgl;",
            name = "H",
            parameters = listOf("Landroid/view/inputmethod/EditorInfo;"),
            returnType = "Z",
        )
        fp1.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        val fp2 = Fingerprint(
            definingClass = "Lfon;",
            name = "F",
            parameters = emptyList(),
            returnType = "Z",
        )
        fp2.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        val onPrimaryClipChangedFingerprint = Fingerprint(
            name = "onPrimaryClipChanged",
            returnType = "V",
            parameters = emptyList(),
            filters = OpcodesFilter.opcodesToFilters(
                Opcode.INVOKE_STATIC,
                Opcode.MOVE_RESULT,
                Opcode.IF_EQZ,
                Opcode.RETURN_VOID,
            ),
            strings = listOf("clipboard_primary_uri", ""),
        )

        var removedOpcodes = 0
        onPrimaryClipChangedFingerprint.method.apply {
            val patternMatch = onPrimaryClipChangedFingerprint.instructionMatches
            val isIncognitoModeIndex = patternMatch.first().index
            val returnVoidIndex = patternMatch.last().index
            val count = (returnVoidIndex - isIncognitoModeIndex) + 1
            removedOpcodes = count

            removeInstructions(
                index = isIncognitoModeIndex,
                count = count,
            )
        }

        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        println("[Force Incognito Mode] Hooked incognito predicates in $c1, $c2 & unblocked clipboard ($removedOpcodes opcodes stripped)")
    }
}
