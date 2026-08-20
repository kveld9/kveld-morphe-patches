package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardForceIncognitoPatch = bytecodePatch(
    name = "Force Incognito Mode",
    description = "Forces Gboard to always operate in incognito mode, disabling personalized learning and persistent input logging across all sessions.",
    default = true,
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
    }
}
