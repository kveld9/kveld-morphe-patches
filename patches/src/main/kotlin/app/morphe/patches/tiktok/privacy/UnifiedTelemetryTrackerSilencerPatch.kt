package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val unifiedTelemetryTrackerSilencerPatch = bytecodePatch(
    name = "Unified Telemetry & Tracker Silencer",
    description = "Neutralizes ByteDance AppLog user tracking, APM/Npth/Heimdallr crash monitors, AppsFlyer attribution, and Firebase analytics.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. AppLog onEvent
        Fingerprint(
            definingClass = "Lcom/ss/android/common/applog/AppLog;",
            name = "onEvent",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Successfully neutralized AppLog.onEvent() entrypoint.")
        patched++

        // 2. AppLog onEventV3 (modern bytedance package)
        Fingerprint(
            definingClass = "Lcom/bytedance/applog/AppLog;",
            name = "onEventV3",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Successfully neutralized AppLog.onEventV3() modern analytics tracker.")
        patched++

        // 3. Npth Core Init Task
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthCoreInitTask;",
            name = "run",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Successfully neutralized NpthCoreInitTask (primary crash & stack trace reporter).")
        patched++

        // 4. Npth Second Init Task
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthSecondInitTask;",
            name = "run",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Successfully neutralized NpthSecondInitTask (secondary telemetry monitor).")
        patched++

        // 5. InitAppsFlyer.run()
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/InitAppsFlyer;",
            name = "run",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Neutralized InitAppsFlyer.run() -> AppsFlyer attribution disabled.")
        patched++

        // 6. InitAppsFlyerHolder$Background.run()
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/InitAppsFlyerHolder\$Background;",
            name = "run",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Neutralized InitAppsFlyerHolder\$Background.run().")
        patched++

        // 7. InitAppsFlyerHolder$Main.run()
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/InitAppsFlyerHolder\$Main;",
            name = "run",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Neutralized InitAppsFlyerHolder\$Main.run().")
        patched++

        // 8. FirebaseAnalytics.setCurrentScreen()
        Fingerprint(
            definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
            name = "setCurrentScreen",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )
        println("[Unified Telemetry & Tracker Silencer] Neutralized FirebaseAnalytics.setCurrentScreen() -> Firebase screen tracking suppressed.")
        patched++

        if (patched == 0) {
            println("[Unified Telemetry & Tracker Silencer] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Unified Telemetry & Tracker Silencer] Applied $patched telemetry and tracking suppression hooks.")
        }
    }
}
