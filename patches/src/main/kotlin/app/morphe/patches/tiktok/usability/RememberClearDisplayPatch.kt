package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

val rememberClearDisplayPatch = bytecodePatch(
    name = "Remember Clear Display",
    description = "Keeps clear-display mode active when scrolling to the next video.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        if (isLite) {
            // 1. Hook clear-mode state dispatcher (LX/7t3 in 47.6, LX/7iG in 47.5) to track state and suppress onClose when switching videos
            val switchFp = Fingerprint(
                name = "LC",
                returnType = "V",
                strings = listOf("change_clear_mode_status"),
            )
            val switchMethod = switchFp.method
            switchMethod.addInstructionsWithLabels(
                0,
                """
                    invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_CLEARDISPLAY_HOOK}->onClearModeCall(Ljava/lang/Object;Ljava/lang/String;)Z
                    move-result v0
                    if-nez v0, :continue_lc
                    return-void
                    :continue_lc
                    nop
                """.trimIndent(),
            )
            println("[Remember Clear Display] Hooked Lite ${switchMethod.definingClass}.LC() -> intention tracking and switch_video suppression active.")
            patched++

            // 2. Hook FeedFragment->r5(Z)V to keep clear mode UI and exit button active across scroll
            val feedFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/main/homepage/fragment/FeedFragment;",
                name = "r5",
                parameters = listOf("Z"),
                returnType = "V",
                strings = listOf("switch_video"),
            )
            feedFp.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, ${Constants.TIKTOK_EXTENSION_CLEARDISPLAY_HOOK}->getClearDisplayState()Z
                    move-result v0
                    if-eqz v0, :continue_r5
                    return-void
                    :continue_r5
                    nop
                """.trimIndent(),
            )
            println("[Remember Clear Display] Hooked Lite FeedFragment.r5() -> video switch dismissal suppressed.")
            patched++

            println("[Remember Clear Display] Applied $patched clear-display hook(s).")
            return@execute
        }

        var eventClass: String? = null

        // 1. Record the user's clear-display choice from clear-mode events.
        try {
            val handler = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/panel/clearmode/ClearModePanelComponent;",
                name = "onClearModeEvent",
                returnType = "V",
            ).method
            val params = handler.parameterTypes.map { it.toString() }
            if (params.size != 1) error("onClearModeEvent has ${params.size} parameters, expected 1")
            eventClass = params[0]
            val isStatic = AccessFlags.STATIC.isSet(handler.accessFlags)
            val eventReg = if (isStatic) "p0" else "p1"
            handler.addInstructions(
                0,
                "invoke-static/range {$eventReg .. $eventReg}, ${Constants.TIKTOK_EXTENSION_CLEARDISPLAY_HOOK}->rememberClearDisplayEvent(Ljava/lang/Object;)V",
            )
            println("[Remember Clear Display] Hooked onClearModeEvent (${params[0]}) -> state recording active.")
            patched++
        } catch (e: Exception) {
            println("[Remember Clear Display] onClearModeEvent note: ${e.message}")
        }

        // 2. Re-apply the remembered state when a new video renders its first frame.
        try {
            val eventType = eventClass ?: error("clear-display event class unknown, record hook failed")
            val eventDef = classDefByOrNull(eventType) ?: error("event class $eventType not found")
            val hasCtor = eventDef.methods.any {
                it.name == "<init>" &&
                    it.parameterTypes.map { p -> p.toString() } == listOf("Z", "I", "Ljava/lang/String;", "Ljava/lang/String;") &&
                    it.returnType == "V"
            }
            if (!hasCtor) error("event class $eventType lacks <init>(ZILjava/lang/String;Ljava/lang/String;)V")
            val post = eventDef.methods.firstOrNull {
                it.name == "post" && it.parameterTypes.isEmpty()
            } ?: error("event class $eventType lacks post()")
            val postReturn = post.returnType.toString()
            val consumeResult = if (postReturn == "V") "" else "\nmove-result-object v0"

            val firstFrame = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
                returnType = "V",
                strings = listOf("onRenderFirstFrame:  time:"),
                custom = { method, _ ->
                    method.parameterTypes.size == 2 &&
                        method.parameterTypes[0] == "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;"
                },
            ).method
            firstFrame.ensureRegisterCount(3)
            firstFrame.addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, ${Constants.TIKTOK_EXTENSION_CLEARDISPLAY_HOOK}->getClearDisplayState()Z
                    move-result v1
                    if-eqz v1, :clear_display_disabled
                    const/4 v2, 0x0
                    const-string v3, ""
                    const-string v4, "long_press"
                    new-instance v0, $eventType
                    invoke-direct {v0, v1, v2, v3, v4}, $eventType-><init>(ZILjava/lang/String;Ljava/lang/String;)V
                    invoke-virtual {v0}, $eventType->post()$postReturn$consumeResult
                    :clear_display_disabled
                    nop
                """.trimIndent(),
            )
            println("[Remember Clear Display] Hooked PlayerController first-frame (${firstFrame.name}) -> remembered state replay active.")
            patched++
        } catch (e: Exception) {
            println("[Remember Clear Display] First-frame replay note: ${e.message}")
        }

        if (patched == 0) {
            println("[Remember Clear Display] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Remember Clear Display] Applied $patched clear-display hook(s).")
        }
    }
}
