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
    description = "Keeps clear-display mode active when scrolling to the next video.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        if (isLite) {
            // 1. Hook clear-mode state dispatcher (LC) to track user clear-display intent.
            val switchFp = Fingerprint(
                name = "LC",
                returnType = "V",
                strings = listOf("change_clear_mode_status"),
            )
            val switchMethod = switchFp.method
            val holderClass = switchMethod.definingClass
            val enumType = switchMethod.parameterTypes.firstOrNull()?.toString()
                ?: error("[Remember Clear Display] Lite dispatcher LC has no parameters")

            val holderDef = classDefByOrNull(holderClass)
                ?: error("[Remember Clear Display] Could not resolve class $holderClass")
            val stateFields = holderDef.fields.filter {
                AccessFlags.STATIC.isSet(it.accessFlags) && it.type.toString() == enumType
            }
            if (stateFields.size != 1) {
                error("[Remember Clear Display] Expected exactly 1 static field of type $enumType in $holderClass, found ${stateFields.size}")
            }
            val stateField = stateFields.first()

            val enumDef = classDefByOrNull(enumType)
                ?: error("[Remember Clear Display] Could not resolve enum class $enumType")
            val openField = enumDef.fields.firstOrNull {
                it.name == "OPEN" && AccessFlags.STATIC.isSet(it.accessFlags) && it.type.toString() == enumType
            } ?: error("[Remember Clear Display] Could not resolve static field OPEN in $enumType")
            val closeField = enumDef.fields.firstOrNull {
                it.name == "CLOSE" && AccessFlags.STATIC.isSet(it.accessFlags) && it.type.toString() == enumType
            } ?: error("[Remember Clear Display] Could not resolve static field CLOSE in $enumType")

            println("[Remember Clear Display] Resolved Lite dispatcher $holderClass (${stateField.name}, OPEN=${openField.name}, CLOSE=${closeField.name}).")

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
            println("[Remember Clear Display] Hooked Lite $holderClass.${switchMethod.name}() -> user choice recording active.")
            patched++

            // 2. Hook per-video anchors to re-apply clear mode on video switches.
            data class Anchor(
                val label: String,
                val definingClass: String,
                val name: String,
                val parameters: List<String>,
                val strings: List<String> = emptyList(),
                val hookMethod: String,
            )

            val anchors = listOf(
                Anchor(
                    label = "r5",
                    definingClass = "Lcom/ss/android/ugc/aweme/main/homepage/fragment/FeedFragment;",
                    name = "r5",
                    parameters = listOf("Z"),
                    strings = listOf("switch_video"),
                    hookMethod = "onVideoSwitchR5",
                ),
                Anchor(
                    label = "bind",
                    definingClass = "LX/Eqi;",
                    name = "LIILLZLL",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "I"),
                    hookMethod = "onVideoSwitchBind",
                ),
                Anchor(
                    label = "first_frame",
                    definingClass = "LX/Eqi;",
                    name = "onRenderFirstFrame",
                    parameters = listOf("Ljava/lang/String;", "LX/1gF;"),
                    hookMethod = "onVideoSwitchFirstFrame",
                ),
                Anchor(
                    label = "bind_all",
                    definingClass = "LX/Eqi;",
                    name = "LILZL",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                    hookMethod = "onVideoSwitchBindAll",
                ),
                Anchor(
                    label = "page_selected",
                    definingClass = "Landroidx/viewpager/widget/ViewPager;",
                    name = "dispatchOnPageSelected",
                    parameters = listOf("I"),
                    hookMethod = "onVideoSwitchPager",
                ),
                Anchor(
                    label = "page_internal",
                    definingClass = "Landroidx/viewpager/widget/ViewPager;",
                    name = "setCurrentItemInternal",
                    parameters = listOf("I", "Z", "Z"),
                    hookMethod = "onVideoSwitchPager",
                ),
                Anchor(
                    label = "state_get",
                    definingClass = "LX/7t3;",
                    name = "LB",
                    parameters = listOf(),
                    hookMethod = "onVideoSwitchStateGet",
                ),
                Anchor(
                    label = "state_query",
                    definingClass = "LX/7t3;",
                    name = "LBL",
                    parameters = listOf("LX/7t4;"),
                    hookMethod = "onVideoSwitchStateQuery",
                ),
                Anchor(
                    label = "pager_wrapper",
                    definingClass = "LX/67h;",
                    name = "setCurrentItem",
                    parameters = listOf("I"),
                    hookMethod = "onVideoSwitchPagerWrapper",
                ),
                Anchor(
                    label = "select_index",
                    definingClass = "LX/5ag;",
                    name = "setCurrentSelectIndex",
                    parameters = listOf("I"),
                    hookMethod = "onVideoSwitchSelectIndex",
                ),
                Anchor(
                    label = "sdk_first_frame",
                    definingClass = "LX/1Gi;",
                    name = "run",
                    parameters = listOf(),
                    hookMethod = "onVideoSwitchSdkFirstFrame",
                ),
                Anchor(
                    label = "sdk_chain_a",
                    definingClass = "LX/28R;",
                    name = "onRenderFirstFrame",
                    parameters = listOf("Ljava/lang/String;", "LX/1gF;"),
                    hookMethod = "onVideoSwitchSdkChainA",
                ),
                Anchor(
                    label = "sdk_chain_b",
                    definingClass = "LX/2L5;",
                    name = "onRenderFirstFrame",
                    parameters = listOf("Ljava/lang/String;", "LX/1gF;"),
                    hookMethod = "onVideoSwitchSdkChainB",
                ),
            )

            for (anchor in anchors) {
                try {
                    val returnType = if (anchor.name == "LB" || anchor.name == "LBL") "Z" else "V"
                    val fp = Fingerprint(
                        definingClass = anchor.definingClass,
                        name = anchor.name,
                        parameters = anchor.parameters,
                        returnType = returnType,
                        strings = anchor.strings,
                    )
                    val method = fp.method
                    method.addInstructions(
                        0,
                        "invoke-static {}, ${Constants.TIKTOK_EXTENSION_CLEARDISPLAY_HOOK}->${anchor.hookMethod}()V",
                    )
                    println("[Remember Clear Display] Hooked Lite ${anchor.definingClass}.${anchor.name}() -> ${anchor.label} trigger active.")
                    patched++
                } catch (e: Exception) {
                    println("[Remember Clear Display] ${anchor.label} note: ${e.message}")
                }
            }

            if (patched == 0) {
                println("[Remember Clear Display] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
            } else {
                println("[Remember Clear Display] Applied $patched clear-display hook(s).")
            }
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
