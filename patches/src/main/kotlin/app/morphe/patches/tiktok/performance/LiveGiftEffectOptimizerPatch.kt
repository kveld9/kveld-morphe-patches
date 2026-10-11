package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val liveGiftEffectOptimizerPatch = bytecodePatch(
    name = "Lighter Live Gifts",
    description = "Turns off heavy 3D gift effects in lives.",
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
