package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils

val gboardDisableBackgroundSyncPatch = bytecodePatch(
    name = "Disable Background Sync",
    description = "Neutralizes AndroidX WorkManager schedulers, MDD (Mobile Data Download) periodic sync, and Superpacks eager asset synchronization (opt-in to preserve initial dictionary downloads).",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. AndroidX WorkManager Schedulers & Sinks (ntk)
        Fingerprint(
            definingClass = "Lntk;",
            name = "a",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lrhq;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Lntk.a")
        }

        Fingerprint(
            definingClass = "Lntk;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "I", "Luou;"),
            returnType = "Lrhq;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Lntk.c")
        }

        Fingerprint(
            definingClass = "Lntk;",
            name = "d",
            parameters = listOf("Ljava/lang/String;", "I", "Luou;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Lntk.d")
        }

        Fingerprint(
            definingClass = "Lntk;",
            name = "f",
            parameters = listOf("Ljava/lang/String;", "Luou;"),
            returnType = "Lrhq;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Lntk.f")
        }

        Fingerprint(
            definingClass = "Lntk;",
            name = "h",
            parameters = emptyList(),
            returnType = "Lrhq;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("Lntk.h")
        }

        Fingerprint(
            definingClass = "Lntk;",
            name = "l",
            parameters = listOf("Lrhq;", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Lntk.l")
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
            definingClass = "Loeh;",
            name = "O",
            parameters = listOf("Llas;", "Lnzr;"),
            returnType = "V",
        )
        fpMdd1.method.addInstructions(0, "return-void")
        val cMdd1 = LocaleUtils.cleanClassName(fpMdd1.originalClassDef.type)
        hookedMethods.add("$cMdd1.O")

        val fpMdd2 = Fingerprint(
            definingClass = "Llai;",
            name = "t",
            parameters = listOf("Llah;"),
            returnType = "V",
        )
        fpMdd2.method.addInstructions(0, "return-void")
        val cMdd2 = LocaleUtils.cleanClassName(fpMdd2.originalClassDef.type)
        hookedMethods.add("$cMdd2.t")

        val fpMdd3 = Fingerprint(
            definingClass = "Llai;",
            name = "l",
            parameters = emptyList(),
            returnType = "V",
        )
        fpMdd3.method.addInstructions(0, "return-void")
        hookedMethods.add("$cMdd2.l")

        val fpMdd4 = Fingerprint(
            definingClass = "Llai;",
            name = "g",
            parameters = listOf("Llaj;"),
            returnType = "Lrhq;",
        )
        fpMdd4.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("$cMdd2.g")

        val fpMdd5 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/MDDTaskScheduler${'$'}Worker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lrhq;",
        )
        fpMdd5.method.addInstructions(
            0,
            """
                new-instance v0, Lbek;
                invoke-direct {v0}, Lbek;-><init>()V
                invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("MDDTaskSchedulerWorker.c")

        val fpMdd6 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/cleanup/MddMetadataCleanupWorker;",
            name = "k",
            parameters = emptyList(),
            returnType = "Labc;",
        )
        fpMdd6.method.addInstructions(
            0,
            """
                new-instance v0, Lbek;
                invoke-direct {v0}, Lbek;-><init>()V
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("MddMetadataCleanupWorker.k")

        val fpMdd7 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/ForegroundDownloadTaskWorker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lrhq;",
        )
        fpMdd7.method.addInstructions(
            0,
            """
                new-instance v0, Lbek;
                invoke-direct {v0}, Lbek;-><init>()V
                invoke-static {v0}, Luxc;->fH(Ljava/lang/Object;)Lrhq;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("ForegroundDownloadTaskWorker.c")

        // 3. Superpacks Eager Startup Synchronization
        val fpSp1 = Fingerprint(
            definingClass = "Ldli;",
            name = "n",
            parameters = emptyList(),
            returnType = "V",
        )
        fpSp1.method.addInstructions(0, "return-void")
        val cSp1 = LocaleUtils.cleanClassName(fpSp1.originalClassDef.type)
        hookedMethods.add("$cSp1.n")

        val fpSp2 = Fingerprint(
            definingClass = "Ldkt;",
            name = "n",
            parameters = emptyList(),
            returnType = "V",
        )
        fpSp2.method.addInstructions(0, "return-void")
        val cSp2 = LocaleUtils.cleanClassName(fpSp2.originalClassDef.type)
        hookedMethods.add("$cSp2.n")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Background Sync] Neutralized ${hookedMethods.size} background sync methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
