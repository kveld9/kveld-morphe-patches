package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnVoid

val disableDoubleTapToLikePatch = bytecodePatch(
    name = "Disable Double Tap to Like",
    description = "Disables the double tap gesture to like videos in the feed, preventing accidental likes while scrolling or pausing. Videos can still be liked using the like button.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook DiggPanelComponent.handleDoubleClick(MotionEvent) in main feed
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/panel/digg/DiggPanelComponent;",
                name = "handleDoubleClick",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            println("[Disable Double Tap to Like] Hooked DiggPanelComponent.handleDoubleClick -> Main feed double tap like neutralized.")
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] DiggPanelComponent note: ${e.message}")
        }

        // 2. Hook LandscapeFragmentPanel.handleDoubleClick(MotionEvent) in landscape feed
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/landscape/LandscapeFragmentPanel;",
                name = "handleDoubleClick",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            println("[Disable Double Tap to Like] Hooked LandscapeFragmentPanel.handleDoubleClick -> Landscape feed double tap like neutralized.")
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] LandscapeFragmentPanel note: ${e.message}")
        }

        // 3. Hook FriendsV3GestureDetectorAssem.onDoubleTap(MotionEvent) in friends tab feed
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/base/FriendsV3GestureDetectorAssem;",
                name = "onDoubleTap",
                parameters = listOf("Landroid/view/MotionEvent;"),
                returnType = "V",
            ).method.replaceWithReturnVoid()
            println("[Disable Double Tap to Like] Hooked FriendsV3GestureDetectorAssem.onDoubleTap -> Friends tab double tap like neutralized.")
            patched++
        } catch (e: Exception) {
            println("[Disable Double Tap to Like] FriendsV3GestureDetectorAssem note: ${e.message}")
        }

        println("[Disable Double Tap to Like] Applied $patched hook(s) -> Double tap to like disabled.")
    }
}
