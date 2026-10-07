package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch

val disableDoubleTapToLikePatch = bytecodePatch(
    name = "Disable Double Tap to Like",
    description = "Disables double tap like in the feed or redirects it to open comments.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val doubleTapMode by stringOption(
        key = "doubleTapMode",
        title = "Double Tap Gesture Action",
        description = "Action when double tapping on feed videos: 'disabled' (neutralizes double tap like) or 'comments' (opens comments).",
        default = "disabled",
        values = mapOf("Disabled" to "disabled", "Open comments" to "comments"),
        required = false,
    )

    execute {
        var patched = 0

        val dtMode = doubleTapMode ?: "disabled"

        // 1. Hook DiggPanelComponent.handleDoubleClick(MotionEvent) in main feed
        try {
            val method = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/panel/digg/DiggPanelComponent;",
                name = "handleDoubleClick",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method
            if (dtMode == "comments") {
                method.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->onDoubleTapComments(Ljava/lang/Object;Landroid/view/MotionEvent;)V
                        return-void
                    """.trimIndent(),
                )
                println("[Disable Double Tap to Like] Hooked DiggPanelComponent.handleDoubleClick -> Main feed double tap redirected to comments.")
            } else {
                method.replaceWithReturnVoid()
                println("[Disable Double Tap to Like] Hooked DiggPanelComponent.handleDoubleClick -> Main feed double tap like neutralized.")
            }
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] DiggPanelComponent note: ${e.message}")
        }

        // 2. Hook LandscapeFragmentPanel.handleDoubleClick(MotionEvent) in landscape feed
        try {
            val method = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeFragmentPanel;",
                name = "handleDoubleClick",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method
            if (dtMode == "comments") {
                method.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->onDoubleTapComments(Ljava/lang/Object;Landroid/view/MotionEvent;)V
                        return-void
                    """.trimIndent(),
                )
                println("[Disable Double Tap to Like] Hooked LandscapeFragmentPanel.handleDoubleClick -> Landscape feed double tap redirected to comments.")
            } else {
                method.replaceWithReturnVoid()
                println("[Disable Double Tap to Like] Hooked LandscapeFragmentPanel.handleDoubleClick -> Landscape feed double tap like neutralized.")
            }
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] LandscapeFragmentPanel note: ${e.message}")
        }

        // 3. Hook FriendsV3GestureDetectorAssem.onDoubleTap(MotionEvent) in friends tab feed
        try {
            val method = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/base/FriendsV3GestureDetectorAssem;",
                name = "onDoubleTap",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method
            if (dtMode == "comments") {
                method.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_GESTURE_HOOK}->onDoubleTapComments(Ljava/lang/Object;Landroid/view/MotionEvent;)V
                        return-void
                    """.trimIndent(),
                )
                println("[Disable Double Tap to Like] Hooked FriendsV3GestureDetectorAssem.onDoubleTap -> Friends tab double tap redirected to comments.")
            } else {
                method.replaceWithReturnVoid()
                println("[Disable Double Tap to Like] Hooked FriendsV3GestureDetectorAssem.onDoubleTap -> Friends tab double tap like neutralized.")
            }
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] FriendsV3GestureDetectorAssem note: ${e.message}")
        }

        println("[Disable Double Tap to Like] Applied $patched gesture customization hook(s).")
    }
}
