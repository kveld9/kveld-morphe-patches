package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisablePhenotypeSyncPatch = bytecodePatch(
    name = "Disable Remote Configuration",
    description = "Disables periodic remote experiment flag synchronization and background updates.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Lcom/google/android/libraries/phenotype/client/stable/PhenotypeUpdateBackgroundBroadcastReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/phenotype/client/stable/AccountRemovedBroadcastReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lwhh;",
            name = "dB",
            parameters = listOf("Landroid/content/Context;", "Lvsp;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lwhh;",
            name = "e",
            parameters = emptyList(),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lwhh;",
            name = "g",
            parameters = emptyList(),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
