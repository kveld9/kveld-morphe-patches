package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference


val bypassScreenCapturePatch = bytecodePatch(
    name = "Bypass Screen Capture Detection",
    description = "Clears FLAG_SECURE on protected windows, restores Circle to Search / screen translate and recent apps snapshots, and neutralizes screenshot detection listeners and feedback prompts.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // ==========================================
        // 1. SCREENSHOT DETECTION & TELEMETRY SUPPRESSION
        // ==========================================

        // 1.1 Neutralize Lego Startup Screenshot Tasks
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
                println("[Bypass Screen Capture Detection] Neutralized $taskClass.run(Context).")
                patched++
            } catch (e: Exception) {
                println("[Bypass Screen Capture Detection] Task $taskClass note: ${e.message}")
            }
        }

        // 1.2 Neutralize ScreenShotFeedbackService triggers & telemetry
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
            println("[Bypass Screen Capture Detection] Neutralized ScreenShotFeedbackService.onShot() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ScreenShotFeedbackService.onShot note: ${e.message}")
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
            println("[Bypass Screen Capture Detection] Neutralized ScreenShotFeedbackService.safelyShowDialog().")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ScreenShotFeedbackService.safelyShowDialog note: ${e.message}")
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
            println("[Bypass Screen Capture Detection] Neutralized ScreenShotFeedbackService.sendShareFeedbackEvent().")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ScreenShotFeedbackService.sendShareFeedbackEvent note: ${e.message}")
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
            println("[Bypass Screen Capture Detection] Neutralized ScreenShotFeedbackService.isFeedbackEnable() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ScreenShotFeedbackService.isFeedbackEnable note: ${e.message}")
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
            println("[Bypass Screen Capture Detection] Neutralized ScreenShotFeedbackService.tryShowScreenShotFloatingView() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ScreenShotFeedbackService.tryShowScreenShotFloatingView note: ${e.message}")
        }

        // ==========================================
        // 2. SCREEN CAPTURE & RECORDING UNRESTRICT (FLAG_SECURE BYPASS)
        // ==========================================

        // 2.1 Neutralize Live Paid Courses Anti-Screenshot Setting (LivePcsCourseVideoAntiScreenshotSetting.getValue() -> false)
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
            println("[Bypass Screen Capture Detection] Neutralized LivePcsCourseVideoAntiScreenshotSetting.getValue() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] LivePcsCourseVideoAntiScreenshotSetting note: ${e.message}")
        }

        // 2.2 Neutralize AntiScreenRecordController.applyFlag(boolean enabled)
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
            println("[Bypass Screen Capture Detection] Neutralized AntiScreenRecordController.applyFlag() -> forced enabled=false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] AntiScreenRecordController note: ${e.message}")
        }

        // 2.3 Neutralize makeScreenProtection(Window window, boolean enableScreenProtection)
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
            println("[Bypass Screen Capture Detection] Neutralized makeScreenProtection() -> forced enable=false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] makeScreenProtection note: ${e.message}")
        }

        // 2.4 Neutralize Circle to Search Block Setting (circle_search_block -> 0)
        // Suppresses TikTok ABMock experiment that dynamically blocks Google Circle to Search and Recent Apps.
        try {
            Fingerprint(
                returnType = "Ljava/lang/Object;",
                strings = listOf("circle_search_block"),
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            println("[Bypass Screen Capture Detection] Neutralized circle_search_block setting -> 0.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] circle_search_block note: ${e.message}")
        }

        // 2.5 Neutralize MainContentSecurityAssem (Circle to Search & Recent Apps dynamic FLAG_SECURE injection)
        // Prevents touch interception at bottom navigation bar from adding FLAG_SECURE (0x2000) to the window.
        try {
            val assemFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/main/assems/MainContentSecurityAssem;",
                name = "dispatchTouchEvent",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            )
            assemFp.method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Bypass Screen Capture Detection] Neutralized MainContentSecurityAssem.dispatchTouchEvent().")
            patched++

            val rqMethod = assemFp.classDef.methods.firstOrNull { method ->
                method.name != "<clinit>" && method.name != "<init>" && !method.name.startsWith("on") &&
                    method.parameterTypes.isEmpty() && method.returnType == "V" &&
                    method.implementation?.instructions?.any { inst ->
                        val refStr = (inst as? ReferenceInstruction)?.reference?.toString() ?: ""
                        refStr.contains("D5l") || refStr.contains("D5n")
                    } == true
            }
            if (rqMethod != null) {
                Fingerprint(
                    definingClass = assemFp.classDef.type,
                    name = rqMethod.name,
                    parameters = emptyList(),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        return-void
                    """.trimIndent(),
                )
                println("[Bypass Screen Capture Detection] Neutralized MainContentSecurityAssem.${rqMethod.name}().")
                patched++
            }
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] MainContentSecurityAssem note: ${e.message}")
        }

        // 2.6 Neutralize ContentSecurityHelper (Circle to Search & Recent Apps gesture bar FLAG_SECURE trigger)
        // Directly neutralizes the touch handler that adds FLAG_SECURE (0x2000) when the user touches
        // the bottom navigation / home gesture bar across all activities (MainActivity, DetailActivity, etc.).
        try {
            val contentSecurityFp = Fingerprint(
                returnType = "Z",
                strings = listOf("config_navBarInteractionMode", "integer", "android"),
            )
            val helperClass = contentSecurityFp.classDef
            val touchMethod = helperClass.methods.firstOrNull {
                it.parameterTypes == listOf("Landroid/view/MotionEvent;") && it.returnType == "V"
            }
            if (touchMethod != null) {
                Fingerprint(
                    definingClass = helperClass.type,
                    name = touchMethod.name,
                    parameters = listOf("Landroid/view/MotionEvent;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        return-void
                    """.trimIndent(),
                )
                println("[Bypass Screen Capture Detection] Neutralized ${helperClass.type}->${touchMethod.name}(MotionEvent) -> return-void.")
                patched++
            }

            contentSecurityFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Bypass Screen Capture Detection] Neutralized ${helperClass.type}->${contentSecurityFp.method.name}() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] ContentSecurityHelper note: ${e.message}")
        }

        // 2.7 Neutralize Window FLAG_SECURE Activity Setter (setFlags(8192, 8192))
        // Strips unconditional FLAG_SECURE enforcement across feeds, paid content, landscape mode, and stories.
        try {
            val paidFp = Fingerprint(
                returnType = "V",
                strings = listOf("disable screen capture", "PaidContent"),
            )
            val instructions = paidFp.method.implementation?.instructions?.toList() ?: emptyList()
            var setFlagsMethodRef: MethodReference? = null
            for (i in instructions.indices) {
                val ref = (instructions[i] as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.returnType == "V" && ref.parameterTypes == listOf("Landroid/app/Activity;")) {
                    val nearbyStrings = (i..minOf(i + 3, instructions.lastIndex)).mapNotNull { idx ->
                        ((instructions[idx] as? ReferenceInstruction)?.reference as? StringReference)?.string
                    }
                    if (nearbyStrings.any { it.contains("disable screen capture") }) {
                        setFlagsMethodRef = ref
                        break
                    }
                }
            }
            if (setFlagsMethodRef != null) {
                Fingerprint(
                    definingClass = setFlagsMethodRef.definingClass,
                    name = setFlagsMethodRef.name,
                    parameters = listOf("Landroid/app/Activity;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        return-void
                    """.trimIndent(),
                )
                println("[Bypass Screen Capture Detection] Neutralized ${setFlagsMethodRef.definingClass}->${setFlagsMethodRef.name}(Activity) -> return-void.")
                patched++
            }
        } catch (e: Exception) {
            println("[Bypass Screen Capture Detection] setFlagsSecure(Activity) note: ${e.message}")
        }

        println("[Bypass Screen Capture Detection] Applied $patched screen capture unrestrict and screenshot detection suppression hook(s).")
    }
}

