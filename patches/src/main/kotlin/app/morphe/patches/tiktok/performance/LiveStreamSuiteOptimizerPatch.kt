package app.morphe.patches.tiktok.performance

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val liveStreamSuiteOptimizerPatch = rawResourcePatch(
    name = "Live Stream SDK & Minigame De-bloat",
    description = "Strips Live link mic SDK (liblink_mic_sdk.so) and live stream interactive minigames to save APK storage.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

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
