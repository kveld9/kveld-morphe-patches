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
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized AppLog.onEvent() entrypoint.")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] AppLog.onEvent() note: ${e.message}")
        }

        // 2. AppLog onEventV3 (legacy & modern bytedance packages)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "onEventV3",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized AppLog.onEventV3() legacy analytics tracker.")
            patched++
        } catch (e: Exception) {
            // Handled via bytedance package in modern builds
        }

        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized AppLog.onEventV3() modern analytics tracker.")
            patched++
        } catch (e: Exception) {
            // Optional depending on build variant
        }

        // 3. AppLog sendEvent
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "sendEvent",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized AppLog.sendEvent() dispatcher.")
            patched++
        } catch (e: Exception) {
            // Optional/obfuscated in newer builds
        }

        // 4. Npth Core Init Task
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized NpthCoreInitTask (primary crash & stack trace reporter).")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] NpthCoreInitTask note: ${e.message}")
        }

        // 5. Npth Second Init Task
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized NpthSecondInitTask (secondary telemetry monitor).")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] NpthSecondInitTask note: ${e.message}")
        }

        // 6. ApmDelegate init
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/apm/internal/ApmDelegate;",
                name = "init",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[UnifiedTelemetryTrackerSilencer] Successfully neutralized ApmDelegate.init() (Heimdallr APM CPU/battery profiler).")
            patched++
        } catch (e: Exception) {
            // Optional if already omitted in current build
        }

        // 7. InitAppsFlyer.run()
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Neutralized InitAppsFlyer.run() -> AppsFlyer attribution disabled.")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] InitAppsFlyer note: ${e.message}")
        }

        // 8. InitAppsFlyerHolder$Background.run()
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Neutralized InitAppsFlyerHolder\$Background.run().")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] InitAppsFlyerHolder\$Background note: ${e.message}")
        }

        // 9. InitAppsFlyerHolder$Main.run()
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Neutralized InitAppsFlyerHolder\$Main.run().")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] InitAppsFlyerHolder\$Main note: ${e.message}")
        }

        // 10. FirebaseAnalytics.setCurrentScreen()
        try {
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
            println("[UnifiedTelemetryTrackerSilencer] Neutralized FirebaseAnalytics.setCurrentScreen() -> Firebase screen tracking suppressed.")
            patched++
        } catch (e: Exception) {
            println("[UnifiedTelemetryTrackerSilencer] FirebaseAnalytics note: ${e.message}")
        }

        println("[UnifiedTelemetryTrackerSilencer] Applied $patched telemetry and tracking suppression hooks.")
    }
}
