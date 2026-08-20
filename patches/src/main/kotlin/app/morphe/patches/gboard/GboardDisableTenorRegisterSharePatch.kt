package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableTenorRegisterSharePatch = bytecodePatch(
    name = "Disable Tenor Share Tracking",
    description = "Disables Tenor GIF selection and share tracking telemetry.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Limg;",
            name = "K",
            parameters = listOf("Lafsc;", "Lidb;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
