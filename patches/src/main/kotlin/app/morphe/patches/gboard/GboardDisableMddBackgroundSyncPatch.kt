package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableMddBackgroundSyncPatch = bytecodePatch(
    name = "Disable MDD Background Sync",
    description = "Neutralizes Mobile Data Download (MDD) periodic background synchronization, automated polling, and prefetch worker tasks while preserving on-demand and user-requested downloads.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Laaxi;",
            name = "a",
            parameters = listOf("Lvja;", "Laaxm;"),
            returnType = "V",
        )
        fp1.method.addInstructions(0, "return-void")
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.a")

        val fp2 = Fingerprint(
            definingClass = "Lvie;",
            name = "t",
            parameters = listOf("Lvid;"),
            returnType = "V",
        )
        fp2.method.addInstructions(0, "return-void")
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.t")

        val fp3 = Fingerprint(
            definingClass = "Lvie;",
            name = "l",
            parameters = emptyList(),
            returnType = "V",
        )
        fp3.method.addInstructions(0, "return-void")
        hookedMethods.add("$c2.l")

        val fp4 = Fingerprint(
            definingClass = "Lvie;",
            name = "g",
            parameters = listOf("Lvig;"),
            returnType = "Lagtn;",
        )
        fp4.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("$c2.g")

        val fp5 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/MDDTaskScheduler${'$'}Worker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lagtn;",
        )
        fp5.method.addInstructions(
            0,
            """
                new-instance v0, Lcja;
                invoke-direct {v0}, Lcja;-><init>()V
                invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("MDDTaskSchedulerWorker.c")

        val fp6 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/cleanup/MddMetadataCleanupWorker;",
            name = "k",
            parameters = emptyList(),
            returnType = "Lcjb;",
        )
        fp6.method.addInstructions(
            0,
            """
                new-instance v0, Lcja;
                invoke-direct {v0}, Lcja;-><init>()V
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("MddMetadataCleanupWorker.k")

        val fp7 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/ForegroundDownloadTaskWorker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lagtn;",
        )
        fp7.method.addInstructions(
            0,
            """
                new-instance v0, Lcja;
                invoke-direct {v0}, Lcja;-><init>()V
                invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("ForegroundDownloadTaskWorker.c")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable MDD Sync] Neutralized ${hookedMethods.size} MDD sync methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
