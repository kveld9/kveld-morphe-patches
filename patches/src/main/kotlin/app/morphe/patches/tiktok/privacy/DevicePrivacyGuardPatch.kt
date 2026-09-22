package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes background clipboard snooping routines, motion sensor profiling, and screenshot/recording detection and telemetry listeners to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
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
