package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

// Entry points exposed through several overloads must be silenced with this helper because
// `.method` only resolves a single match, while fingerprints that resolve to exactly one
// method keep using `.method`.
private fun BytecodePatchContext.silenceEveryOverload(fingerprint: Fingerprint, label: String, effect: String): Int {
    val methods = fingerprint.matchAll().mapNotNull { match -> match.method.takeIf { it.implementation != null } }
    require(methods.isNotEmpty()) { "$label fingerprint matched no concrete method" }
    methods.forEach { method ->
        method.addInstructions(
            0,
            """
                return-void
            """,
        )
    }
    println("[Block Trackers] Neutralized ${methods.size} $label overload(s) -> $effect")
    return methods.size
}

val unifiedTelemetryTrackerSilencerPatch = bytecodePatch(
    name = "Block Trackers",
    description = "Neutralizes ByteDance AppLog tracking, crash monitors, and analytics.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0
        var multiOverloadTargets = 0
        var multiOverloadBodies = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        // 1. AppLog onEvent. The app funnels events through several overloads, so hook
        // every match: patching only the first one leaves the others sending telemetry.
        multiOverloadBodies += silenceEveryOverload(
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "onEvent",
                returnType = "V",
            ),
            label = "AppLog.onEvent",
            effect = "event logging disabled.",
        )
        patched++
        multiOverloadTargets++

        // 2. AppLog onEventV3 (modern bytedance package), every overload
        multiOverloadBodies += silenceEveryOverload(
            Fingerprint(
                definingClass = "Lcom/bytedance/applog/AppLog;",
                name = "onEventV3",
                returnType = "V",
            ),
            label = "AppLog.onEventV3",
            effect = "event logging disabled.",
        )
        patched++
        multiOverloadTargets++

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

            // 5. NpthTask secondary initialization path (LCCII)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/legoImpl/task/NpthTask;",
                name = "LCCII",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Successfully neutralized NpthTask.LCCII() secondary crash reporter init.")
            patched++

            // 6. Npth crash reporter core initialization
            Fingerprint(
                definingClass = "Lcom/bytedance/crash/Npth;",
                name = "init",
                parameters = listOf("Landroid/content/Context;", "Lcom/bytedance/crash/ICommonParams;", "Z", "Z", "Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Successfully neutralized Npth.init() -> Crash and monitor reporting suppressed.")
            patched++
        } else {
            // 3. Npth Core Init Task, every overload
            multiOverloadBodies += silenceEveryOverload(
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthCoreInitTask;",
                    name = "run",
                    returnType = "V",
                ),
                label = "NpthCoreInitTask.run",
                effect = "crash reporting disabled.",
            )
            patched++
            multiOverloadTargets++

            // 4. Npth Second Init Task, every overload
            multiOverloadBodies += silenceEveryOverload(
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthSecondInitTask;",
                    name = "run",
                    returnType = "V",
                ),
                label = "NpthSecondInitTask.run",
                effect = "telemetry monitor disabled.",
            )
            patched++
            multiOverloadTargets++

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

        // 8. FirebaseAnalytics.setCurrentScreen(), every overload
        multiOverloadBodies += silenceEveryOverload(
            Fingerprint(
                definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
                name = "setCurrentScreen",
                returnType = "V",
            ),
            label = "FirebaseAnalytics.setCurrentScreen",
            effect = "Firebase screen tracking suppressed.",
        )
        patched++
        multiOverloadTargets++

        val silencedBodies = (patched - multiOverloadTargets) + multiOverloadBodies
        if (patched == 0) {
            println("[Block Trackers] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Block Trackers] Applied $patched telemetry and tracking suppression targets ($silencedBodies method bodies silenced).")
        }
    }
}
