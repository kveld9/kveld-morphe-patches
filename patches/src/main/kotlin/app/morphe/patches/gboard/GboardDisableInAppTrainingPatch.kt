package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisableInAppTrainingPatch = bytecodePatch(
    name = "Disable In-App Training",
    description = "Neutralizes periodic in-app training cache generation and federated-learning background work while preserving normal keyboard functionality and explicit user-requested operations.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Lwdl;",
            name = "b",
            parameters = listOf("Lvnx;", "[Ljava/lang/Object;"),
            returnType = "Z",
        )
        fp1.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.b")

        val fp2 = Fingerprint(
            definingClass = "Lmxs;",
            name = "a",
            parameters = emptyList(),
            returnType = "V",
        )
        fp2.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.a")

        val fp3 = Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/latin5/PeriodicTaskWorker;",
            name = "c",
            parameters = emptyList(),
            returnType = "Lagjs;",
        )
        fp3.method.addInstructions(
            0,
            """
                invoke-static {}, Lciu;->a()Lciu;
                move-result-object v0
                invoke-static {v0}, Lagjb;->i(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("PeriodicTaskWorker.c")

        val fp4 = Fingerprint(
            definingClass = "Lcom/google/android/apps/inputmethod/libs/latin5/PeriodicTaskWorker;",
            name = "k",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        )
        fp4.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        hookedMethods.add("PeriodicTaskWorker.k")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable In-App Training] Neutralized ${hookedMethods.size} training methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
