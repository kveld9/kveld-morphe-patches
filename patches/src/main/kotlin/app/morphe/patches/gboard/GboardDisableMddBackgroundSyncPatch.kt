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
        Fingerprint(
            definingClass = "Laane;",
            name = "a",
            parameters = listOf("Lvfi;", "Laani;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lvem;",
            name = "t",
            parameters = listOf("Lvel;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lvem;",
            name = "l",
            parameters = emptyList(),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lvem;",
            name = "g",
            parameters = listOf("Lveo;"),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/MDDTaskScheduler${'$'}Worker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                invoke-static {}, Lciu;->a()Lciu;
                move-result-object v0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/cleanup/MddMetadataCleanupWorker;",
            name = "k",
            parameters = emptyList(),
            returnType = "Lciu;",
        ).method.addInstructions(
            0,
            """
                invoke-static {}, Lciu;->a()Lciu;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/mdd/ForegroundDownloadTaskWorker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lagjs;",
        ).method.addInstructions(
            0,
            """
                invoke-static {}, Lciu;->a()Lciu;
                move-result-object v0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    }
}
