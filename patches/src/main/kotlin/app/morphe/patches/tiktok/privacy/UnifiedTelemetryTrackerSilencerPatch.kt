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

            // 7. AppLogNetworkClient (Lite root package) - upload POST data
            Fingerprint(
                definingClass = "LAppLogNetworkClient;",
                name = "LCC",
                parameters = listOf("Ljava/lang/String;", "[B", "Ljava/util/Map;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"magic_tag\":\"ss_app_log\",\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized AppLogNetworkClient.LCC() -> Event uploads suppressed with synthetic success.")
            patched++

            // 8. AppLogNetworkClient (Lite root package) - GET
            Fingerprint(
                definingClass = "LAppLogNetworkClient;",
                name = "LB",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized AppLogNetworkClient.LB() -> GET requests suppressed.")
            patched++

            // 9. AppLogNetworkClient (Lite root package) - POST params
            Fingerprint(
                definingClass = "LAppLogNetworkClient;",
                name = "LBL",
                parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized AppLogNetworkClient.LBL() -> POST params suppressed.")
            patched++

            // 10. AppLogNetworkClient (Lite root package) - POST data stream
            Fingerprint(
                definingClass = "LAppLogNetworkClient;",
                name = "LCCII",
                parameters = listOf("Ljava/lang/String;", "[B", "Ljava/util/Map;"),
                returnType = "[B",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized AppLogNetworkClient.LCCII() -> Stream posts suppressed.")
            patched++

            // 11. com.ss.android.ugc.aweme.statistic.AppLogNetworkClient - upload POST data
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/statistic/AppLogNetworkClient;",
                name = "LCC",
                parameters = listOf("Ljava/lang/String;", "[B", "Ljava/util/Map;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"magic_tag\":\"ss_app_log\",\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized statistic.AppLogNetworkClient.LCC() -> Event uploads suppressed.")
            patched++

            // 12. com.ss.android.ugc.aweme.statistic.AppLogNetworkClient - GET
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/statistic/AppLogNetworkClient;",
                name = "LB",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized statistic.AppLogNetworkClient.LB() -> GET requests suppressed.")
            patched++

            // 13. com.ss.android.ugc.aweme.statistic.AppLogNetworkClient - POST params
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/statistic/AppLogNetworkClient;",
                name = "LBL",
                parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "{\"message\":\"success\"}"
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized statistic.AppLogNetworkClient.LBL() -> POST params suppressed.")
            patched++

            // 14. com.ss.android.ugc.aweme.statistic.AppLogNetworkClient - POST data stream
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/statistic/AppLogNetworkClient;",
                name = "LCCII",
                parameters = listOf("Ljava/lang/String;", "[B", "Ljava/util/Map;"),
                returnType = "[B",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized statistic.AppLogNetworkClient.LCCII() -> Stream posts suppressed.")
            patched++

            // 15. AppLog.flush()
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "flush",
                parameters = emptyList(),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Neutralized AppLog.flush().")
            patched++

            // 16. AppLog.sendHeartbeat()
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "sendHeartbeat",
                parameters = emptyList(),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Neutralized AppLog.sendHeartbeat().")
            patched++

            // 17. AppLog.tryUpdateConfig()
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "tryUpdateConfig",
                parameters = listOf("Z", "Z", "Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Block Trackers] Neutralized AppLog.tryUpdateConfig().")
            patched++

            // 18. AppLog.setupLogReaper()
            Fingerprint(
                definingClass = "Lcom/ss/android/common/applog/AppLog;",
                name = "setupLogReaper",
                parameters = emptyList(),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
            println("[Block Trackers] Neutralized AppLog.setupLogReaper().")
            patched++

            // 19. APM / SDKMonitor HTTP transport (DefaultTTNetImpl.doPost)
            Fingerprint(
                definingClass = "Lcom/bytedance/apm/net/DefaultTTNetImpl;",
                name = "doPost",
                parameters = listOf("Ljava/lang/String;", "[B", "Ljava/util/Map;"),
                returnType = "LX/1RQ;",
            ).method.addInstructions(
                0,
                """
                    new-instance v0, LX/1RQ;
                    const/16 v1, 0xc8
                    const/4 v2, 0x0
                    invoke-direct {v0, v1, v2}, LX/1RQ;-><init>(I[B)V
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized DefaultTTNetImpl.doPost() -> APM monitor upload suppressed.")
            patched++

            // 20. APM / SDKMonitor HTTP transport (DefaultTTNetImpl.doGet)
            Fingerprint(
                definingClass = "Lcom/bytedance/apm/net/DefaultTTNetImpl;",
                name = "doGet",
                parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
                returnType = "LX/1RQ;",
            ).method.addInstructions(
                0,
                """
                    new-instance v0, LX/1RQ;
                    const/16 v1, 0xc8
                    const/4 v2, 0x0
                    invoke-direct {v0, v1, v2}, LX/1RQ;-><init>(I[B)V
                    return-object v0
                """,
            )
            println("[Block Trackers] Neutralized DefaultTTNetImpl.doGet() -> APM monitor settings fetch suppressed.")
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
