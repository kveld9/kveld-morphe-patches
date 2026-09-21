package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val devicePrivacyResourcePatch = resourcePatch(
    name = "Device Privacy Manifest Guard",
    description = "Strips privacy-invasive permissions (local network, location, ad-ID, OEM bloatware, in-app billing, and system trackers) from AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Device Privacy Guard] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        // IMPORTANT: DO NOT include android.permission.DETECT_SCREEN_CAPTURE here.
        // Stripping DETECT_SCREEN_CAPTURE causes fatal SecurityException crashes on Android 14+
        // at Activity.registerScreenCaptureCallback call sites (e.g. SeaSkuPanelFragment.onCreate).
        val blockedPermissions = setOf(
            // 1. Local Network & Hardware
            "android.permission.ACCESS_LOCAL_NETWORK",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.NFC",

            // 2. Location & GPS
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_MEDIA_LOCATION",

            // 3. Advertising, Ad-ID & Privacy Sandbox
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",

            // 4. OEM & Vendor Telemetry / Preload Bloat
            "com.orange.update.permission.READ_ATTRIBUTION",
            "com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA",
            "com.oplus.ocs.permission.third",
            "com.samsung.android.mapsagent.permission.READ_APP_INFO",
            "com.sec.android.provider.badge.permission.READ",
            "com.sec.android.provider.badge.permission.WRITE",
            "com.android.launcher.permission.READ_SETTINGS",
            "com.tiktok.preload.permission.IDENTIFY",
            "com.tiktok.manager.SYS_START_PERMISSION",
            "com.google.android.apps.aicore.service.BIND_SERVICE",

            // 5. Screen Recording Monitor (Safe on Android 15 due to checkSelfPermission guard)
            "android.permission.DETECT_SCREEN_RECORDING",

            // 6. Contacts, Task Control & Overlays
            "android.permission.READ_CONTACTS",
            "android.permission.REORDER_TASKS",
            "android.permission.SYSTEM_ALERT_WINDOW",

            // 7. In-App Billing & Live Wallpapers
            "com.android.vending.BILLING",
            "android.permission.SET_WALLPAPER",
            "com.zhiliao.musically.livewallpaper.permission.wallpaperplugin",

            // 8. Third-party Push & Biometrics
            "com.amazon.device.messaging.permission.RECEIVE",
            "com.zhiliaoapp.musically.permission.RECEIVE_ADM_MESSAGE",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT",

            // 9. Bluetooth Scanning & Advertising
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.BLUETOOTH_SCAN",
        )

        var removedPermissions = 0

        document(manifestFile.absolutePath).use { doc ->
            val usesPermissions = doc.getElementsByTagName("uses-permission")
            val toRemove = mutableListOf<Element>()
            for (i in 0 until usesPermissions.length) {
                val elem = usesPermissions.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name in blockedPermissions) {
                    toRemove.add(elem)
                }
            }
            toRemove.forEach {
                it.parentNode?.removeChild(it)
                removedPermissions++
            }

            // Purge external <package> tags inside <queries>
            val allowedPackages = setOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.zhiliao.musically.livewallpaper",
            )
            val queriesElements = doc.getElementsByTagName("queries")
            var removedQueriesPackages = 0
            for (i in 0 until queriesElements.length) {
                val qElem = queriesElements.item(i) as? Element ?: continue
                val packages = qElem.getElementsByTagName("package")
                val packagesToRemove = mutableListOf<Element>()
                for (j in 0 until packages.length) {
                    val pkgElem = packages.item(j) as? Element ?: continue
                    val pkgName = pkgElem.getAttribute("android:name").ifEmpty {
                        pkgElem.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
                    }
                    if (pkgName.isNotEmpty() && pkgName !in allowedPackages) {
                        packagesToRemove.add(pkgElem)
                    }
                }
                packagesToRemove.forEach {
                    it.parentNode?.removeChild(it)
                    removedQueriesPackages++
                }
            }

            println("[Device Privacy Guard] Stripped $removedPermissions permission(s) and $removedQueriesPackages external package query declaration(s) from AndroidManifest.xml.")
        }
    }
}

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes background clipboard snooping routines, local network scanning permissions, and screenshot/recording detection and telemetry listeners to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    dependsOn(devicePrivacyResourcePatch)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // ==========================================
        // 1. CLIPBOARD PRIVACY PROTECTION
        // ==========================================

        // 1.1 Hook IMMessageListClipboardServiceImpl (messenger clipboard integration)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/im/messagelist/impl/IMMessageListClipboardServiceImpl;",
                name = "LIZ",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized IMMessageListClipboardServiceImpl.LIZ().")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] IMMessageListClipboardServiceImpl note: ${e.message}")
        }

        // 1.2 Intercept BPEA clipboard reading (LX/1KAa;->LIZIZ)
        try {
            Fingerprint(
                definingClass = "LX/1KAa;",
                name = "LIZIZ",
                parameters = listOf("Landroid/content/ClipboardManager;", "Lcom/bytedance/bpea/basics/Cert;"),
                returnType = "Landroid/content/ClipData;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LX/1KAa.LIZIZ() (BPEA clipboard read) -> forced null.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/1KAa.LIZIZ note: ${e.message}")
        }

        // ==========================================
        // 2. SCREENSHOT DETECTION & TELEMETRY SUPPRESSION
        // ==========================================

        // 2.1 Neutralize Lego Startup Screenshot Tasks
        val contextTasks = listOf(
            "Lcom/ss/android/ugc/aweme/legoImp/task/ScreenShotTaskHolder\$BootFinish;",
            "Lcom/ss/android/ugc/aweme/legoImp/task/ScreenShotFeedbackTaskHolder\$BootFinish;",
            "Lcom/ss/android/ugc/aweme/legoImpl/task/ScreenShotTask;",
            "Lcom/ss/android/ugc/aweme/legoImpl/task/ScreenShotFeedbackTask;",
            "Lcom/ss/android/ugc/aweme/legoImp/task/ScreenRecordingMonitorInitTask;",
            "Lcom/ss/android/ugc/aweme/im/sharepanel/impl/screenshotshare/InternalShareScreenshotTask;",
            "Lcom/ss/android/ugc/aweme/im/sharepanel/impl/screenshotshare/InternalShareScreenshotTaskHolder\$BootFinish;",
        )
        contextTasks.forEach { taskClass ->
            try {
                Fingerprint(
                    definingClass = taskClass,
                    name = "run",
                    parameters = listOf("Landroid/content/Context;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        return-void
                    """.trimIndent(),
                )
                println("[Device Privacy Guard] Neutralized $taskClass.run(Context).")
                patched++
            } catch (e: Exception) {
                println("[Device Privacy Guard] Task $taskClass note: ${e.message}")
            }
        }

        // 2.2 Neutralize ScreenShotFeedbackService triggers & telemetry
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;",
                name = "onShot",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized ScreenShotFeedbackService.onShot() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] ScreenShotFeedbackService.onShot note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;",
                name = "safelyShowDialog",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized ScreenShotFeedbackService.safelyShowDialog().")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] ScreenShotFeedbackService.safelyShowDialog note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;",
                name = "sendShareFeedbackEvent",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized ScreenShotFeedbackService.sendShareFeedbackEvent().")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] ScreenShotFeedbackService.sendShareFeedbackEvent note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;",
                name = "isFeedbackEnable",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized ScreenShotFeedbackService.isFeedbackEnable() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] ScreenShotFeedbackService.isFeedbackEnable note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feedback/screenshot/ScreenShotFeedbackService;",
                name = "tryShowScreenShotFloatingView",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized ScreenShotFeedbackService.tryShowScreenShotFloatingView() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] ScreenShotFeedbackService.tryShowScreenShotFloatingView note: ${e.message}")
        }

        // ==========================================
        // 3. SCREEN CAPTURE & RECORDING UNRESTRICT (FLAG_SECURE BYPASS)
        // ==========================================

        // 3.1 Neutralize Live Paid Courses Anti-Screenshot Setting (LivePcsCourseVideoAntiScreenshotSetting.getValue() -> false)
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/android/livesdk/comp/api/pcs/data/setting/LivePcsCourseVideoAntiScreenshotSetting;",
                name = "getValue",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LivePcsCourseVideoAntiScreenshotSetting.getValue() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LivePcsCourseVideoAntiScreenshotSetting note: ${e.message}")
        }

        // 3.2 Neutralize AntiScreenRecordController.applyFlag(boolean enabled)
        // Forces parameter p1 (enabled) -> false so it permanently invokes Window.clearFlags(0x2000).
        try {
            Fingerprint(
                returnType = "V",
                parameters = listOf("Z"),
                strings = listOf("AntiScreenRecordController", "applyFlag window is null"),
            ).method.addInstructions(
                0,
                """
                    const/4 p1, 0x0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized AntiScreenRecordController.applyFlag() -> forced enabled=false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] AntiScreenRecordController note: ${e.message}")
        }

        // 3.3 Neutralize makeScreenProtection(Window window, boolean enableScreenProtection)
        // Forces parameter p2 (enableScreenProtection) -> false so canRecordScreen is true & Window.clearFlags(0x2000) is called.
        try {
            Fingerprint(
                returnType = "V",
                parameters = listOf("Landroid/view/Window;", "Z"),
                strings = listOf("makeScreenProtection, canRecordScreen:"),
            ).method.addInstructions(
                0,
                """
                    const/4 p2, 0x0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized makeScreenProtection() -> forced enable=false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] makeScreenProtection note: ${e.message}")
        }

        // ==========================================
        // 4. SENSOR HAR (HUMAN ACTIVITY RECOGNITION) ISOLATION
        // ==========================================

        // 6.3 Intercept SmartHARServiceImpl.enable() -> false
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ml/impl/har/SmartHARServiceImpl;",
                name = "enable",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized SmartHARServiceImpl.enable() -> forced false.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] SmartHARServiceImpl.enable note: ${e.message}")
        }

        // 6.4 Intercept SmartHARServiceImpl.checkAndInit() -> return-void
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ml/impl/har/SmartHARServiceImpl;",
                name = "checkAndInit",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized SmartHARServiceImpl.checkAndInit() -> return-void.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] SmartHARServiceImpl.checkAndInit note: ${e.message}")
        }

        println("[Device Privacy Guard] Applied $patched device privacy protection hook(s).")
    }
}
