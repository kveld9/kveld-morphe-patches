package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardEnableKeyShapeSelectionPatch = bytecodePatch(
    name = "Enable Key Shape Selection",
    description = "Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Lxgy;",
            name = "i",
            parameters = listOf("Landroid/content/Context;"),
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
