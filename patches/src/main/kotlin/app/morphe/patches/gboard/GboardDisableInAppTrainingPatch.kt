package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableInAppTrainingPatch = bytecodePatch(
    name = "Disable In-App Training",
    description = "Neutralizes periodic in-app training cache generation and federated-learning background work while preserving normal keyboard functionality and explicit user-requested operations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Lwdl;",
            name = "b",
            parameters = listOf("Lvnx;", "[Ljava/lang/Object;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lmxs;",
            name = "a",
            parameters = emptyList(),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/latin5/PeriodicTaskWorker;",
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
            definingClass = "Lcom/google/android/apps/inputmethod/libs/latin5/PeriodicTaskWorker;",
            name = "k",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
    }
}
