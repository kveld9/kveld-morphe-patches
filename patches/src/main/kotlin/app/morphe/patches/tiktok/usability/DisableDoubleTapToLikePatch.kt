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
    compatibleWith(Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0

        // 1. Hook Lite DiggPresenter (standard feed mode)
        // Resolves the presenter method returning "right_container_digg" and neutralizes its double tap and heart animation handlers.
        val diggPresenterFp = Fingerprint(
            returnType = "Ljava/lang/String;",
            parameters = emptyList(),
            strings = listOf("right_container_digg"),
        )
        val diggClass = diggPresenterFp.method.definingClass
        val diggDef = mutableClassDefBy(diggClass)
        var standardHooks = 0
        for (method in diggDef.methods) {
            if (method.returnType == "V" &&
                method.parameterTypes == listOf("Ljava/lang/Float;", "Ljava/lang/Float;")
            ) {
                method.replaceWithReturnVoid()
                println("[Disable Double Tap to Like] Hooked ${diggDef.type}.${method.name}(Float, Float) -> Standard feed double tap neutralized.")
                standardHooks++
                patched++
            }
        }
        if (standardHooks == 0) {
            error("[Disable Double Tap to Like] Failed to find (Float, Float)V methods in standard feed presenter ${diggDef.type}")
        }

        // 2. Hook Lite MiniLite feed mode double tap like
        val miniLiteDiggFp = Fingerprint(
            returnType = "V",
            parameters = listOf("Ljava/lang/Float;", "Ljava/lang/Float;"),
            strings = listOf("click_double_like"),
        )
        miniLiteDiggFp.method.replaceWithReturnVoid()
        println("[Disable Double Tap to Like] Hooked ${miniLiteDiggFp.method.definingClass}.${miniLiteDiggFp.method.name}(Float, Float) -> MiniLite double tap like neutralized.")
        patched++

        println("[Disable Double Tap to Like] Applied $patched hook(s) -> Double tap to like disabled.")
    }
}
