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

        // 1. Clearcut & Event Telemetry (vtq, oib, sco)
        listOf("n", "p", "s").forEach { methodName ->
            val fp = Fingerprint(
                definingClass = "Lvtq;",
                name = methodName,
                parameters = if (methodName == "n") listOf("Lvtl;") else emptyList(),
                returnType = "V",
            )
            fp.method.addInstructions(0, "return-void")
            val c = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            hookedMethods.add("$c.$methodName")
        }

        val fpOib = Fingerprint(
            definingClass = "Loib;",
            name = "b",
            parameters = listOf("Lokr;"),
            returnType = "V",
        )
        fpOib.method.addInstructions(0, "return-void")
        val cOib = LocaleUtils.cleanClassName(fpOib.originalClassDef.type)
        hookedMethods.add("$cOib.b")

        val fpSco = Fingerprint(
            definingClass = "Lsco;",
            name = "dC",
            parameters = listOf("Landroid/content/Context;", "Lvwh;"),
            returnType = "V",
        )
        fpSco.method.addInstructions(0, "return-void")
        val cSco = LocaleUtils.cleanClassName(fpSco.originalClassDef.type)
        hookedMethods.add("$cSco.dC")

        // 2. Daily Ping Worker (DailyPingWorker.c)
        val fpDailyPing = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
            name = "c",
            parameters = emptyList(),
        )
        fpDailyPing.method.addInstructions(
            0,
            """
                invoke-static {}, Landroidx/work/ListenableWorker${'$'}Result;->success()Landroidx/work/ListenableWorker${'$'}Result;
                move-result-object v0
                invoke-static {v0}, Lagsw;->i(Ljava/lang/Object;)Lagtn;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("DailyPingWorker.c")

        // 3. Google Primes & Crash Diagnostics (LifeboatReceiver, wty, aclm, NativeCrashHandlerImpl, acys)
        val fpLifeboat = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpLifeboat.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val fpWty = Fingerprint(
            definingClass = "Lwty;",
            name = "dC",
            parameters = listOf("Landroid/content/Context;", "Lvwh;"),
            returnType = "V",
        )
        fpWty.method.addInstructions(0, "return-void")
        val cWty = LocaleUtils.cleanClassName(fpWty.originalClassDef.type)
        hookedMethods.add("$cWty.dC")

        val fpAclm = Fingerprint(
            definingClass = "Laclm;",
            name = "b",
            parameters = listOf("Laclm;"),
            returnType = "V",
        )
        fpAclm.method.addInstructions(0, "return-void")
        val cAclm = LocaleUtils.cleanClassName(fpAclm.originalClassDef.type)
        hookedMethods.add("$cAclm.b")

        val fpCrash = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Lacwb;"),
            returnType = "V",
        )
        fpCrash.method.addInstructions(0, "return-void")
        hookedMethods.add("NativeCrashHandlerImpl.a")

        val fpAcys = Fingerprint(
            definingClass = "Lacys;",
            name = "gm",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
        )
        fpAcys.method.addInstructions(
            0,
            """
                invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                move-result-object v0
                new-instance v1, Landroid/os/Handler;
                invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                return-object v1
            """.trimIndent(),
        )
        val cAcys = LocaleUtils.cleanClassName(fpAcys.originalClassDef.type)
        hookedMethods.add("$cAcys.gm")

        // 4. AppDoctor Diagnostics (AppDoctorInitializer, AppDoctorReceiver)
        val fpAppDoctorInit = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/appdoctor/initializer/AppDoctorInitializer;",
            name = "a",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/Object;",
        )
        fpAppDoctorInit.method.addInstructions(0, "return-object p0")
        hookedMethods.add("AppDoctorInitializer.a")

        val fpAppDoctorRecv = Fingerprint(
            definingClass = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpAppDoctorRecv.method.addInstructions(0, "return-void")
        hookedMethods.add("AppDoctorReceiver.onReceive")

        // 5. Tenor Share Tracking (inr.K)
        val fpTenor = Fingerprint(
            definingClass = "Linr;",
            name = "K",
            parameters = listOf("Lagca;", "Lien;"),
            returnType = "V",
        )
        fpTenor.method.addInstructions(0, "return-void")
        val cTenor = LocaleUtils.cleanClassName(fpTenor.originalClassDef.type)
        hookedMethods.add("$cTenor.K")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Block Telemetry] Injected Smali hooks into ${hookedMethods.size} telemetry & diagnostic methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
