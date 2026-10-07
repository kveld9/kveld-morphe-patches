package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnIntegerObject
import app.morphe.patches.shared.sharedExtensionPatch

val disableFeedLongPressActionsPatch = bytecodePatch(
    name = "Disable Feed Long-Press Actions",
    description = "Disables long-press action gestures on feed buttons, including Like to repost, Share to quick DMs, and Comment to quick emojis, with optional long-press video body redirection.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val disableLikeRepost by booleanOption(
        key = "disableLikeRepost",
        title = "Disable Long-Press Like (Repost)",
        description = "Prevents holding the Like button on feed videos from opening the Repost action panel.",
        default = true,
        required = false,
    )

    val disableShareQuickDms by booleanOption(
        key = "disableShareQuickDms",
        title = "Disable Long-Press Share (Quick DMs)",
        description = "Prevents holding the Share button on feed videos from opening the quick share recent contacts tray.",
        default = true,
        required = false,
    )

    val disableCommentReactions by booleanOption(
        key = "disableCommentReactions",
        title = "Disable Long-Press Comment (Quick Emojis)",
        description = "Prevents holding the Comment button on feed videos from opening the quick reaction emojis picker.",
        default = true,
        required = false,
    )

    val longPressVideo by stringOption(
        key = "longPressVideo",
        title = "Long Press Video Action",
        description = "Action when long pressing feed video body: 'nothing' (default, suppresses menu/repost), 'comments' (opens comments), 'copyLink' (copies link to clipboard), or 'saveSound' (favorites/saves audio).",
        default = "nothing",
        values = mapOf("Do nothing" to "nothing", "Open comments" to "comments", "Copy link" to "copyLink", "Save sound" to "saveSound"),
        required = false,
    )

    execute {
        var patched = 0
        val lpMode = longPressVideo ?: "nothing"

        // 1. Disable or redirect long-press Like (Repost)
        // Returning true signals the touch event was consumed, preventing ACTION_UP from falling through to performClick().
        if (disableLikeRepost != false || lpMode != "nothing") {
            val likeRepostFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
                returnType = "Z",
                parameters = listOf("Landroid/view/View;"),
                strings = listOf(
                    "Long press detected on digg button for aweme: ",
                    "long_press_like_panel",
                ),
            )
            if (lpMode != "nothing") {
                likeRepostFingerprint.method.addInstructions(
                    0,
                    """
                        const-string v0, "$lpMode"
                        invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->handleLongPressVideoAction(Ljava/lang/Object;Ljava/lang/String;)Z
                        move-result v0
                        return v0
                    """.trimIndent(),
                )
                println("[Disable Feed Long-Press Actions] Hooked VideoDiggAssem.Sr() -> redirected long-press to $lpMode.")
            } else {
                likeRepostFingerprint.method.replaceWithReturnBoolean(true)
                println("[Disable Feed Long-Press Actions] Hooked VideoDiggAssem.Sr() -> consumed long-press, like repost disabled.")
            }
            patched++
        }

        // 2. Disable or redirect long-press Share (Quick DMs)
        if (disableShareQuickDms != false || lpMode != "nothing") {
            val shareConfigMethod = Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("im_long_press_share_button_to_quick_share"),
            ).method
            shareConfigMethod.replaceWithReturnIntegerObject(0)
            println("[Disable Feed Long-Press Actions] Hooked long-press quick share config lambda -> returns 0.")
            patched++

            val shareTriggerFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/unreadshare/ShareUnreadVideoQuickDMTrigger;",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            )
            if (lpMode != "nothing") {
                shareTriggerFingerprint.method.addInstructions(
                    0,
                    """
                        const-string v0, "$lpMode"
                        invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->handleLongPressVideoParams(Ljava/lang/Object;Ljava/lang/String;)V
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
                println("[Disable Feed Long-Press Actions] Hooked ShareUnreadVideoQuickDMTrigger trigger check -> redirected to $lpMode.")
            } else {
                shareTriggerFingerprint.method.replaceWithReturnBoolean(false)
                println("[Disable Feed Long-Press Actions] Hooked ShareUnreadVideoQuickDMTrigger trigger check -> return false.")
            }
            patched++
        }

        // 3. Disable long-press Comment (Quick Emojis)
        if (disableCommentReactions != false || lpMode != "nothing") {
            val commentConfigMethod = Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("long_press_quick_comment"),
            ).method
            commentConfigMethod.replaceWithReturnIntegerObject(0)
            println("[Disable Feed Long-Press Actions] Hooked long-press quick comment config lambda -> returns 0.")
            patched++
        }

        println("[Disable Feed Long-Press Actions] Applied $patched feed long-press action suppression hook(s).")
    }
}
