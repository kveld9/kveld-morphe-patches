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
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Lwsd;",
            name = "dC",
            parameters = listOf("Landroid/content/Context;", "Lvul;"),
            returnType = "V",
        )
        fp1.method.addInstructions(0, "return-void")
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)
        hookedMethods.add("$c1.dC")

        val fp2 = Fingerprint(
            definingClass = "Laciz;",
            name = "b",
            parameters = listOf("Laciz;"),
            returnType = "V",
        )
        fp2.method.addInstructions(0, "return-void")
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)
        hookedMethods.add("$c2.b")

        val fp3 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Lacto;"),
            returnType = "V",
        )
        fp3.method.addInstructions(0, "return-void")
        hookedMethods.add("NativeCrashHandlerImpl.a")

        val fp4 = Fingerprint(
            definingClass = "Lacwf;",
            name = "gk",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
        )
        fp4.method.addInstructions(
            0,
            """
                invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                move-result-object v0
                new-instance v1, Landroid/os/Handler;
                invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                return-object v1
            """.trimIndent(),
        )
        val c4 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp4.originalClassDef.type)
        hookedMethods.add("$c4.gk")

        val fp5 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fp5.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Google Primes] Neutralized ${hookedMethods.size} Primes methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
