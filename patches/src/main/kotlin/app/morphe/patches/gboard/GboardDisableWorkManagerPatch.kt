package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableWorkManagerPatch = bytecodePatch(
    name = "Disable WorkManager",
    description = "Neutralizes background WorkManager initialization, database creation, and periodic maintenance workers.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        Fingerprint(
            definingClass = "Landroidx/work/WorkManagerInitializer;",
            name = "a",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/Object;",
        ).method.apply {
            addInstructions(0, "return-object p0")
            hookedMethods.add("WorkManagerInitializer.a")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "a",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lagtn;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Laalt.a")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "I", "Lcjh;"),
            returnType = "Lagtn;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Laalt.c")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "d",
            parameters = listOf("Ljava/lang/String;", "I", "Lcjh;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Laalt.d")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "f",
            parameters = listOf("Ljava/lang/String;", "Lcjt;"),
            returnType = "Lagtn;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Laalt.f")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "h",
            parameters = emptyList(),
            returnType = "Lagtn;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Laalt.h")
        }

        Fingerprint(
            definingClass = "Laalt;",
            name = "l",
            parameters = listOf("Lagtn;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Laalt.l")
        }

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemjob/SystemJobService;",
            name = "onCreate",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-super {p0}, Landroid/app/job/JobService;->onCreate()V
                    return-void
                """.trimIndent(),
            )
            hookedMethods.add("SystemJobService.onCreate")
        }

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemjob/SystemJobService;",
            name = "onStartJob",
            parameters = listOf("Landroid/app/job/JobParameters;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SystemJobService.onStartJob")
        }

        Fingerprint(
            definingClass = "Landroidx/work/impl/background/systemalarm/RescheduleReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("RescheduleReceiver.onReceive")
        }

        Fingerprint(
            definingClass = "Landroidx/work/impl/diagnostics/DiagnosticsReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("DiagnosticsReceiver.onReceive")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable WorkManager] Neutralized ${hookedMethods.size} WorkManager methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
