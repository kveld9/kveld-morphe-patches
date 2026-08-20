package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardDisablePrimesPatch = bytecodePatch(
    name = "Disable Google Primes",
    description = "Neutralizes Google Primes performance profiling, jank monitoring, native crash sidecars, and background telemetry threads.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        Fingerprint(
            definingClass = "Lwpr;",
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
            definingClass = "Lacbi;",
            name = "b",
            parameters = listOf("Lacbi;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Lacma;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lacor;",
            name = "gh",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
        ).method.addInstructions(
            0,
            """
                invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                move-result-object v0
                new-instance v1, Landroid/os/Handler;
                invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                return-object v1
            """.trimIndent(),
        )

        Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
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
