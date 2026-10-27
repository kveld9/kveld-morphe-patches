package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val unifiedTelemetryTrackerSilencerPatch = bytecodePatch(
    name = "Block Trackers",
    description = "Neutralizes ByteDance AppLog tracking, crash monitors, and analytics.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

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
        println("[Block Trackers] Successfully neutralized AppLog.onEvent() entrypoint.")
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
        println("[Block Trackers] Successfully neutralized AppLog.onEventV3() modern analytics tracker.")
        patched++

        if (isLite) {
            // 3. Npth Task (Lite crash reporter)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/legoImpl/task/NpthTask;",
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Successfully neutralized NpthTask (primary crash & stack trace reporter).")
            patched++

            // 4. UltraliteInitAppsFlyer.run()
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/launcher/task/UltraliteInitAppsFlyer;",
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Neutralized UltraliteInitAppsFlyer.run() -> AppsFlyer attribution disabled.")
            patched++
        } else {
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
            println("[Block Trackers] Successfully neutralized NpthCoreInitTask (primary crash & stack trace reporter).")
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
            println("[Block Trackers] Successfully neutralized NpthSecondInitTask (secondary telemetry monitor).")
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
            println("[Block Trackers] Neutralized InitAppsFlyer.run() -> AppsFlyer attribution disabled.")
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
            println("[Block Trackers] Neutralized InitAppsFlyerHolder\$Background.run().")
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
            println("[Block Trackers] Neutralized InitAppsFlyerHolder\$Main.run().")
            patched++
        }

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
        println("[Block Trackers] Neutralized FirebaseAnalytics.setCurrentScreen() -> Firebase screen tracking suppressed.")
        patched++

        if (patched == 0) {
            println("[Block Trackers] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Block Trackers] Applied $patched telemetry and tracking suppression hooks.")
        }
    }
}
