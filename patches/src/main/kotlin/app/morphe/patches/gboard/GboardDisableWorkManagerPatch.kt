package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableWorkManagerPatch = bytecodePatch(
    name = "Disable WorkManager",
    description = "Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Landroidx/work/WorkManagerInitializer;",
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
            definingClass = "Laabo;",
            name = "a",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Laabo;",
            name = "b",
            parameters = listOf("Ljava/lang/String;", "Lcie;", "Lcja;"),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Laabo;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "Lcie;", "Lcja;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Laabo;",
            name = "d",
            parameters = listOf("Ljava/lang/String;", "Lcjm;"),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Laabo;",
            name = "f",
            parameters = emptyList(),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Laabo;",
            name = "g",
            parameters = listOf("Lagjs;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemjob/SystemJobService;",
            name = "onCreate",
            parameters = emptyList(),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                invoke-super {p0}, Landroid/app/job/JobService;->onCreate()V
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemjob/SystemJobService;",
            name = "onStartJob",
            parameters = listOf("Landroid/app/job/JobParameters;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemalarm/RescheduleReceiver;",
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
            definingClass = "Landroidx/work/impl/diagnostics/DiagnosticsReceiver;",
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
