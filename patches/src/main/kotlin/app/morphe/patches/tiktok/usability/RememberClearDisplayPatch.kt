package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

val rememberClearDisplayPatch = bytecodePatch(
    name = "Remember Clear Display",
    description = "Remembers TikTok's clear-display state between videos and re-applies it when new videos start.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
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

        println("[Remember Clear Display] Applied $patched clear-display hook(s).")
    }
}
