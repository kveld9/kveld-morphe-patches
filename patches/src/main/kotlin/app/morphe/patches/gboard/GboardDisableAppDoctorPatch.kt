package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableAppDoctorPatch = bytecodePatch(
    name = "Disable Diagnostics",
    description = "Disables Google's diagnostic and recovery telemetry.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/appdoctor/initializer/AppDoctorInitializer;",
            name = "a",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/Object;",
        ).method.addInstructions(
            0,
            """
                return-object p0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
