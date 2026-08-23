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
        Fingerprint(
            definingClass = "Lsew;",
            name = "H",
            parameters = listOf("Landroid/view/inputmethod/EditorInfo;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lfoh;",
            name = "F",
            parameters = emptyList(),
            returnType = "Z",
        ).method.addInstructions(
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

        onPrimaryClipChangedFingerprint.method.apply {
            val patternMatch = onPrimaryClipChangedFingerprint.instructionMatches
            val isIncognitoModeIndex = patternMatch.first().index
            val returnVoidIndex = patternMatch.last().index
            val count = (returnVoidIndex - isIncognitoModeIndex) + 1

            removeInstructions(
                index = isIncognitoModeIndex,
                count = count,
            )
        }
    }
}
