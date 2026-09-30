package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils

val gboardBlockTelemetryPatch = bytecodePatch(
    name = "Block Telemetry",
    description = "Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Clearcut & Event Telemetry (vze, olo, shc)
        listOf("n", "p", "s").forEach { methodName ->
            val fp = Fingerprint(
                definingClass = "Lvze;",
                name = methodName,
                parameters = if (methodName == "n") listOf("Lvyz;") else emptyList(),
                returnType = "V",
            )
            fp.method.addInstructions(0, "return-void")
            val c = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            hookedMethods.add("$c.$methodName")
        }

        val fpHcg = Fingerprint(
            definingClass = "Lolo;",
            name = "b",
            parameters = listOf("Looe;"),
            returnType = "V",
        )
        fpHcg.method.addInstructions(0, "return-void")
        val cHcg = LocaleUtils.cleanClassName(fpHcg.originalClassDef.type)
        hookedMethods.add("$cHcg.b")

        val fpJga = Fingerprint(
            definingClass = "Lshc;",
            name = "dD",
            parameters = listOf("Landroid/content/Context;", "Lwbw;"),
            returnType = "V",
        )
        fpJga.method.addInstructions(0, "return-void")
        val cJga = LocaleUtils.cleanClassName(fpJga.originalClassDef.type)
        hookedMethods.add("$cJga.dD")

        // 2. Daily Ping Worker (DailyPingWorker.c)
        val fpDailyPing = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
            name = "c",
            parameters = emptyList(),
        )
        fpDailyPing.method.addInstructions(
            0,
            """
                new-instance v0, Lcim;
                invoke-direct {v0}, Lcim;-><init>()V
                invoke-static {v0}, Lahce;->i(Ljava/lang/Object;)Lahcv;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("DailyPingWorker.c")

        // 3. Google Primes & Crash Diagnostics (LifeboatReceiver, xam, acud, NativeCrashHandlerImpl, aabu)
        val fpLifeboat = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpLifeboat.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val fpLxd = Fingerprint(
            definingClass = "Lxam;",
            name = "dD",
            parameters = listOf("Landroid/content/Context;", "Lwbw;"),
            returnType = "V",
        )
        fpLxd.method.addInstructions(0, "return-void")
        val cLxd = LocaleUtils.cleanClassName(fpLxd.originalClassDef.type)
        hookedMethods.add("$cLxd.dD")

        val fpOrc = Fingerprint(
            definingClass = "Lacud;",
            name = "b",
            parameters = listOf("Lacud;"),
            returnType = "V",
        )
        fpOrc.method.addInstructions(0, "return-void")
        val cOrc = LocaleUtils.cleanClassName(fpOrc.originalClassDef.type)
        hookedMethods.add("$cOrc.b")

        val fpCrash = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Lades;"),
            returnType = "V",
        )
        fpCrash.method.addInstructions(0, "return-void")
        hookedMethods.add("NativeCrashHandlerImpl.a")

        val fpNjv = Fingerprint(
            definingClass = "Laabu;",
            name = "get",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
        )
        fpNjv.method.addInstructions(
            0,
            """
                invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                move-result-object v0
                new-instance v1, Landroid/os/Handler;
                invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                return-object v1
            """.trimIndent(),
        )
        val cNjv = LocaleUtils.cleanClassName(fpNjv.originalClassDef.type)
        hookedMethods.add("$cNjv.get")

        // 4. AppDoctor Diagnostics (AppDoctorReceiver)
        val fpAppDoctorRecv = Fingerprint(
            definingClass = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpAppDoctorRecv.method.addInstructions(0, "return-void")
        hookedMethods.add("AppDoctorReceiver.onReceive")

        // 5. Tenor Share Tracking (ioe.F)
        val fpTenor = Fingerprint(
            definingClass = "Lioe;",
            name = "F",
            parameters = listOf("Laglm;"),
            returnType = "V",
        )
        fpTenor.method.addInstructions(0, "return-void")
        val cTenor = LocaleUtils.cleanClassName(fpTenor.originalClassDef.type)
        hookedMethods.add("$cTenor.F")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Injected Smali hooks into ${hookedMethods.size} telemetry & diagnostic methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
