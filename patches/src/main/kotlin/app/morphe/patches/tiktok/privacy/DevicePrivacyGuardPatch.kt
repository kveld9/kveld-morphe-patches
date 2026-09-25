package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes invasive runtime permissions (contacts sync, location tracking, nearby devices), advertising ID profiling, background clipboard snooping routines, motion sensor profiling, and screenshot/recording detection to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // ==========================================
        // 1. RUNTIME PERMISSION DISPATCH & DEFENSE
        // ==========================================


        // 1.2 PowerPermissions FakeFragment dispatcher (FakeFragment;->jT)
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/powerpermissions/FakeFragment;",
                name = "jT",
                parameters = listOf("Ljava/util/HashSet;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptPowerPermissions(Ljava/lang/Object;Ljava/util/Set;)Z
                    move-result v0
                    if-eqz v0, :cond_proceed
                    return-void
                    :cond_proceed
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized FakeFragment.jT() (PowerPermissions request dispatcher).")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] FakeFragment.jT note: ${e.message}")
        }

        // 1.3 Permission denial cache checker (LX/04DS;->LIZ)
        try {
            Fingerprint(
                definingClass = "LX/04DS;",
                name = "LIZ",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->isPermissionBlocked(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :cond_check
                    const/4 v0, 0x1
                    return v0
                    :cond_check
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Intercepted LX/04DS.LIZ() -> permanently denied for blocked permissions.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/04DS.LIZ note: ${e.message}")
        }

        // ==========================================
        // 2. LOCATION TRACKING & POPUP NEUTRALIZATION
        // ==========================================

        // 2.1 Disable all scene permission apply (LX/0BK7;->LJI -> false)
        try {
            Fingerprint(
                definingClass = "LX/0BK7;",
                name = "LJI",
                parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LX/0BK7.LJI() -> location scene permission application disabled.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/0BK7.LJI note: ${e.message}")
        }

        // 2.2 Disable pre-instruction location popups (LX/0BK7;->LJII -> false)
        try {
            Fingerprint(
                definingClass = "LX/0BK7;",
                name = "LJII",
                parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LX/0BK7.LJII() -> pre-instruction location popup disabled.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/0BK7.LJII note: ${e.message}")
        }

        // 2.3 Disable popup scenes (LX/0BK7;->LJIIIIZZ -> false)
        try {
            Fingerprint(
                definingClass = "LX/0BK7;",
                name = "LJIIIIZZ",
                parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LX/0BK7.LJIIIIZZ() -> location popup scenes disabled.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/0BK7.LJIIIIZZ note: ${e.message}")
        }

        // 2.4 Force location scenes empty (LX/0BK7;->LJIIL -> true)
        try {
            Fingerprint(
                definingClass = "LX/0BK7;",
                name = "LJIIL",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LX/0BK7.LJIIL() -> location scenes declared empty.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] LX/0BK7.LJIIL note: ${e.message}")
        }

        // 2.5 Neutralize LocationServiceImpl precise and coarse optimization flags
        listOf("LJIIZILJ", "LJIJ").forEach { methodName ->
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/tiktok/location/serviceimpl/LocationServiceImpl;",
                    name = methodName,
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
                println("[Device Privacy Guard] Neutralized LocationServiceImpl.$methodName() -> false.")
                patched++
            } catch (e: Exception) {
                println("[Device Privacy Guard] LocationServiceImpl.$methodName note: ${e.message}")
            }
        }

        // 2.6 Neutralize location startup Lego tasks
        val locationTasks = listOf(
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTask;",
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTaskHolder\$Background;",
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTaskHolder\$Main;",
        )
        locationTasks.forEach { taskClass ->
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
                println("[Device Privacy Guard] Location task $taskClass note: ${e.message}")
            }
        }

        // ==========================================
        // 3. CONTACTS SYNC & RELATION PROMPTS NEUTRALIZATION
        // ==========================================

        // 3.1 Neutralize RelationAuthDialogControl.LJIIIIZZ (in-app Contacts sync popup dialog)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/auth/pipeline/common/RelationAuthDialogControl;",
                name = "LJIIIIZZ",
                parameters = listOf("Landroid/content/Context;", "LX/0Heg;", "Ljava/lang/String;", "Landroid/os/Bundle;", "LX/03Vs;"),
                returnType = "Ljava/lang/Object;",
            ).method.addInstructions(
                0,
                """
                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized RelationAuthDialogControl.LJIIIIZZ() -> suppressed Contacts sync dialog.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] RelationAuthDialogControl.LJIIIIZZ note: ${e.message}")
        }

        // 3.2 Neutralize RelationAuthDialogControl.LJI (in-app Facebook relation auth dialog)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/auth/pipeline/common/RelationAuthDialogControl;",
                name = "LJI",
                parameters = listOf("LX/02HM;", "LX/0N7F;", "LX/0N7k;"),
                returnType = "Ljava/lang/Object;",
            ).method.addInstructions(
                0,
                """
                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized RelationAuthDialogControl.LJI() -> suppressed Facebook sync dialog.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] RelationAuthDialogControl.LJI note: ${e.message}")
        }

        // 3.3 Neutralize contacts upload and background sync Lego tasks
        val contactTasks = listOf(
            "Lcom/ss/android/ugc/aweme/friends/lego/ContactsUploadRequest;",
            "Lcom/ss/android/ugc/aweme/relation/auth/lego/PermissionRequestAndUploadLegoTask;",
            "Lcom/ss/android/ugc/aweme/im/contacts/impl/bytesync/IMContactInitTask;",
            "Lcom/ss/android/ugc/aweme/friends/lego/MafFollowBackBootRequest;",
        )
        contactTasks.forEach { taskClass ->
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
                println("[Device Privacy Guard] Contact task $taskClass.run note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = taskClass,
                    name = "meetTrigger",
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
                println("[Device Privacy Guard] Neutralized $taskClass.meetTrigger() -> false.")
                patched++
            } catch (e: Exception) {
                println("[Device Privacy Guard] Contact task $taskClass.meetTrigger note: ${e.message}")
            }
        }

        // ==========================================
        // 4. ADVERTISING ID (AD_ID) PROFILING BLOCK
        // ==========================================

        try {
            Fingerprint(
                definingClass = "LX/02z2;",
                name = "LLLLIIL",
                parameters = listOf("Landroid/content/Context;", "LX/02yq;"),
                returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient${'$'}Info;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized AdvertisingIdClient.getInfo() -> null.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] AdvertisingIdClient.getInfo note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "LX/02z2;",
                name = "LLLLIIIILLL",
                parameters = listOf("Lcom/google/android/gms/ads/identifier/AdvertisingIdClient${'$'}Info;", "LX/02yq;"),
                returnType = "Ljava/lang/String;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, "00000000-0000-0000-0000-000000000000"
                    return-object v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized AdvertisingIdClient.getId() -> zeroed UUID.")
            patched++
        } catch (e: Exception) {
            println("[Device Privacy Guard] AdvertisingIdClient.getId note: ${e.message}")
        }

        // ==========================================
        // 5. CLIPBOARD PRIVACY PROTECTION
        // ==========================================

        // 5.1 Hook IMMessageListClipboardServiceImpl (messenger clipboard integration)
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

        // 5.2 Intercept BPEA clipboard reading (LX/1KAa;->LIZIZ)
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
        // 6. SCREENSHOT DETECTION & TELEMETRY SUPPRESSION
        // ==========================================

        // 6.1 Neutralize Lego Startup Screenshot Tasks
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

        // 6.2 Neutralize ScreenShotFeedbackService triggers & telemetry
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
        // 7. SCREEN CAPTURE & RECORDING UNRESTRICT (FLAG_SECURE BYPASS)
        // ==========================================

        // 7.1 Neutralize Live Paid Courses Anti-Screenshot Setting (LivePcsCourseVideoAntiScreenshotSetting.getValue() -> false)
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

        // 7.2 Neutralize AntiScreenRecordController.applyFlag(boolean enabled)
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

        // 7.3 Neutralize makeScreenProtection(Window window, boolean enableScreenProtection)
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
        // 8. SENSOR HAR (HUMAN ACTIVITY RECOGNITION) ISOLATION
        // ==========================================

        // 8.1 Intercept SmartHARServiceImpl.enable() -> false
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

        // 8.2 Intercept SmartHARServiceImpl.checkAndInit() -> return-void
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
