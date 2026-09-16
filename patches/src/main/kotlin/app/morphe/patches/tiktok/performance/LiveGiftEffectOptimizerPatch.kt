package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val liveGiftEffectOptimizerPatch = bytecodePatch(
    name = "Live Stream 3D Gift Optimizer",
    description = "Disables Live 3D gift particle effect engine and widget rendering lifecycle to eliminate frame drops during live streams.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

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
            println("[Live Stream 3D Gift Optimizer] Neutralized LiveGiftEffectWidget.initView() -> 3D gift particle overhead eliminated.")
            patched++
        } catch (e: Exception) {
            println("[Live Stream 3D Gift Optimizer] LiveGiftEffectWidget.initView note: ${e.message}")
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
            println("[Live Stream 3D Gift Optimizer] Neutralized LiveGiftEffectWidget.onCreate() -> Gift engine lifecycle suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Live Stream 3D Gift Optimizer] LiveGiftEffectWidget.onCreate note: ${e.message}")
        }

        println("[Live Stream 3D Gift Optimizer] Applied $patched live gift engine hook(s).")
    }
}
