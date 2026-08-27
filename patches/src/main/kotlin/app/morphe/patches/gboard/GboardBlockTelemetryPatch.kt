package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val gboardBlockTelemetryPatch = bytecodePatch(
    name = "Block Telemetry",
    description = "Disables background metrics dispatch, event logging, daily pings, and crash reporting.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        val fp1 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fp1.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val fp2 = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
            name = "c",
            parameters = emptyList(),
        )
        fp2.method.addInstructions(
            0,
            """
                invoke-static {}, Landroidx/work/ListenableWorker${'$'}Result;->success()Landroidx/work/ListenableWorker${'$'}Result;
                move-result-object v0
                invoke-static {v0}, Lagnf;->g(Ljava/lang/Object;)Lagjs;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("DailyPingWorker.c")

        val fp3 = Fingerprint(
            definingClass = "Lvpy;",
            name = "n",
            parameters = listOf("Lvpt;"),
            returnType = "V",
        )
        fp3.method.addInstructions(0, "return-void")
        val c3 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp3.originalClassDef.type)
        hookedMethods.add("$c3.n")

        val fp4 = Fingerprint(
            definingClass = "Lvpy;",
            name = "p",
            parameters = emptyList(),
            returnType = "V",
        )
        fp4.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.p")

        val fp5 = Fingerprint(
            definingClass = "Lvpy;",
            name = "s",
            parameters = emptyList(),
            returnType = "V",
        )
        fp5.method.addInstructions(0, "return-void")
        hookedMethods.add("$c3.s")

        val fp6 = Fingerprint(
            definingClass = "Loge;",
            name = "b",
            parameters = listOf("Loii;"),
            returnType = "V",
        )
        fp6.method.addInstructions(0, "return-void")
        val c6 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp6.originalClassDef.type)
        hookedMethods.add("$c6.b")

        val fp7 = Fingerprint(
            definingClass = "Lrzl;",
            name = "dB",
            parameters = listOf("Landroid/content/Context;", "Lvsp;"),
            returnType = "V",
        )
        fp7.method.addInstructions(0, "return-void")
        val c7 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp7.originalClassDef.type)
        hookedMethods.add("$c7.dB")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Injected Smali hooks into ${hookedMethods.size} telemetry methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
