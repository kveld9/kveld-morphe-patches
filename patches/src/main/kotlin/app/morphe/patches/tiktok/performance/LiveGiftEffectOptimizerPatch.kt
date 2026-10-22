package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val liveGiftEffectOptimizerPatch = bytecodePatch(
    name = "Disable Live Gift Effects",
    description = "Disables 3D gift animations in live streams to prevent frame drops.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. LiveGiftEffectWidget.initView()
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/android/livesdk/gift/base/platform/business/effect/LiveGiftEffectWidget;",
                name = "initView",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Disable Live Gift Effects] Neutralized LiveGiftEffectWidget.initView() -> 3D gift particle overhead eliminated.")
            patched++
        } catch (e: Exception) {
            println("[Disable Live Gift Effects] LiveGiftEffectWidget.initView note: ${e.message}")
        }

        // 2. LiveGiftEffectWidget.onCreate()
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/android/livesdk/gift/base/platform/business/effect/LiveGiftEffectWidget;",
                name = "onCreate",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[Disable Live Gift Effects] Neutralized LiveGiftEffectWidget.onCreate() -> Gift engine lifecycle suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Disable Live Gift Effects] LiveGiftEffectWidget.onCreate note: ${e.message}")
        }

        if (patched == 0) {
            println("[Disable Live Gift Effects] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Disable Live Gift Effects] Applied $patched live gift engine hook(s).")
        }
    }
}
