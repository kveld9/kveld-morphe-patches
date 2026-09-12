package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

private val liveGiftEffectOptimizerPatch = bytecodePatch(
    name = "Live Stream 3D Gift Optimizer",
    description = "Disables Live 3D gift particle effect engine and widget rendering lifecycle.",
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
            println("[LiveStreamSuiteOptimizer] Neutralized LiveGiftEffectWidget.initView() -> 3D gift particle overhead eliminated.")
            patched++
        } catch (e: Exception) {
            println("[LiveStreamSuiteOptimizer] LiveGiftEffectWidget.initView note: ${e.message}")
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
            println("[LiveStreamSuiteOptimizer] Neutralized LiveGiftEffectWidget.onCreate() -> Gift engine lifecycle suppressed.")
            patched++
        } catch (e: Exception) {
            println("[LiveStreamSuiteOptimizer] LiveGiftEffectWidget.onCreate note: ${e.message}")
        }

        println("[LiveStreamSuiteOptimizer] Applied $patched live gift engine hooks.")
    }
}

val liveStreamSuiteOptimizerPatch = rawResourcePatch(
    name = "Live Stream Suite Optimizer",
    description = "Strips Live link mic SDK (liblink_mic_sdk.so) and live stream interactive minigames, and disables 3D gift particle animation preloading.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    dependsOn(liveGiftEffectOptimizerPatch)

    execute {
        val fileTargets = listOf(
            "lib/arm64-v8a/liblink_mic_sdk.so",
        )

        var savedBytes = 0L
        var count = 0

        fileTargets.forEach { path ->
            val file = get(path)
            if (file.exists() && file.isFile) {
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        val dirTargets = listOf(
            "assets/native_runtime_server/game",
            "assets/offline/tiktok_live_tt_live_lynx_match_component_container",
        )

        dirTargets.forEach { dirPath ->
            val dir = get(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val orig = file.length()
                    if (orig > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += orig
                        count++
                    }
                }
            }
        }

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[LiveStreamSuiteOptimizer] Stripped $count live stream SDK binaries & minigame assets -> Saved $savedMb MB uncompressed (~3.5 MB in APK)")
        } else {
            println("[LiveStreamSuiteOptimizer] Target live stream bloat not present.")
        }
    }
}
