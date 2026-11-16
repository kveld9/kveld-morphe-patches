package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName

val gboardDisableBackgroundSyncPatch = bytecodePatch(
    name = "Stop Background Sync",
    description = "Stops background downloads and sync to save battery.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        // Immediate-success future snippet (18.6.3 future framework: Lbeq; value + Lrjs; future).
        val immediateFutureSnippet = """
            new-instance v0, Lbeq;
            invoke-direct {v0}, Lbeq;-><init>()V
            new-instance v1, Lrjs;
            invoke-direct {v1, v0}, Lrjs;-><init>(Ljava/lang/Object;)V
            return-object v1
        """.trimIndent()

        // 1. AndroidX WorkManager Schedulers & Sinks (nwe)
        Fingerprint(
            definingClass = "Lnwe;",
            name = "a",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lrjw;",
        ).method.apply {
            addInstructions(0, immediateFutureSnippet)
            hookedMethods.add("Lnwe.a")
        }

        Fingerprint(
            definingClass = "Lnwe;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "I", "Lrmr;"),
            returnType = "Lrjw;",
        ).method.apply {
            addInstructions(0, immediateFutureSnippet)
            hookedMethods.add("Lnwe.c")
        }

        Fingerprint(
            definingClass = "Lnwe;",
            name = "d",
            parameters = listOf("Ljava/lang/String;", "I", "Lrmr;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Lnwe.d")
        }

        Fingerprint(
            definingClass = "Lnwe;",
            name = "f",
            parameters = listOf("Ljava/lang/String;", "Lrmr;"),
            returnType = "Lrjw;",
        ).method.apply {
            addInstructions(0, immediateFutureSnippet)
            hookedMethods.add("Lnwe.f")
        }

        Fingerprint(
            definingClass = "Lnwe;",
            name = "h",
            parameters = emptyList(),
            returnType = "Lrjw;",
        ).method.apply {
            addInstructions(0, immediateFutureSnippet)
            hookedMethods.add("Lnwe.h")
        }

        Fingerprint(
            definingClass = "Lnwe;",
            name = "l",
            parameters = listOf("Lrjw;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Lnwe.l")
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

        // 2. MDD (Mobile Data Download) Workers & Listeners
        val fpMdd1 = Fingerprint(
            definingClass = "Lleo;",
            name = "af",
            parameters = listOf("Llci;", "Locp;"),
            returnType = "V",
        )
        fpMdd1.method.addInstructions(0, "return-void")
        val cMdd1 = cleanClassName(fpMdd1.originalClassDef.type)
        hookedMethods.add("$cMdd1.af")

        val fpMdd2 = Fingerprint(
            definingClass = "Llca;",
            name = "s",
            parameters = listOf("Llbz;"),
            returnType = "V",
        )
        fpMdd2.method.addInstructions(0, "return-void")
        val cMdd2 = cleanClassName(fpMdd2.originalClassDef.type)
        hookedMethods.add("$cMdd2.s")

        val fpMdd3 = Fingerprint(
            definingClass = "Llca;",
            name = "l",
            parameters = emptyList(),
            returnType = "V",
        )
        fpMdd3.method.addInstructions(0, "return-void")
        hookedMethods.add("$cMdd2.l")

        val fpMdd4 = Fingerprint(
            definingClass = "Llca;",
            name = "g",
            parameters = listOf("Llcb;"),
            returnType = "Lrjw;",
        )
        fpMdd4.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                new-instance v1, Lrjs;
                invoke-direct {v1, v0}, Lrjs;-><init>(Ljava/lang/Object;)V
                return-object v1
            """.trimIndent(),
        )
        hookedMethods.add("$cMdd2.g")

        val fpMdd5 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/MDDTaskScheduler${'$'}Worker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lrjw;",
        )
        fpMdd5.method.addInstructions(0, immediateFutureSnippet)
        hookedMethods.add("MDDTaskSchedulerWorker.c")

        val fpMdd6 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/cleanup/MddMetadataCleanupWorker;",
            name = "k",
            parameters = emptyList(),
            returnType = "Labf;",
        )
        fpMdd6.method.addInstructions(
            0,
            """
                new-instance v0, Lbeq;
                invoke-direct {v0}, Lbeq;-><init>()V
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("MddMetadataCleanupWorker.k")

        val fpMdd7 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/ForegroundDownloadTaskWorker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lrjw;",
        )
        fpMdd7.method.addInstructions(0, immediateFutureSnippet)
        hookedMethods.add("ForegroundDownloadTaskWorker.c")

        // 3. Superpacks Eager Startup Synchronization
        // Note (18.5.1): the legacy eager-sync entry points (gvp.n/gsp.n) are gone
        // upstream (classes repurposed, timestamp/config flow removed). Eager sync
        // stays covered via the MDD worker and scheduler hooks above.
        println("[Stop Background Sync] Pruned obsolete Superpacks eager-sync hooks: targets removed upstream.")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        if (hookedMethods.isEmpty()) {
            println("[Stop Background Sync] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Stop Background Sync] Neutralized ${hookedMethods.size} background sync methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
        }
    }
}
