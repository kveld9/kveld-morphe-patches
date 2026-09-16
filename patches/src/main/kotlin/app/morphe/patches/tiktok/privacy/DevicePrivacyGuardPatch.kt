package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val devicePrivacyResourcePatch = resourcePatch(
    name = "Device Privacy Manifest Guard",
    description = "Strips local network discovery permissions (ACCESS_LOCAL_NETWORK) from AndroidManifest.xml.",
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
        // at Activity.registerScreenCaptureCallback call sites.
        val blockedPermissions = setOf(
            "android.permission.ACCESS_LOCAL_NETWORK",
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
        }

        println("[Device Privacy Guard] Stripped $removedPermissions permission(s) from AndroidManifest.xml.")
    }
}

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes background clipboard snooping routines, local network scanning permissions, and screenshot/recording detection and telemetry listeners to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    dependsOn(devicePrivacyResourcePatch)

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

        println("[Device Privacy Guard] Applied $patched device privacy protection hook(s).")
    }
}
