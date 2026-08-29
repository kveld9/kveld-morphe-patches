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
        val fp = Fingerprint(
            definingClass = "Limz;",
            name = "K",
            parameters = listOf("Lafzm;", "Lidv;"),
            returnType = "V",
        )
        fp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        val targetClass = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp.originalClassDef.type)
        println("[Disable Tenor Share Tracking] Neutralized $targetClass.K() GIF share telemetry")
    }
}
