package app.morphe.patches.tiktok.performance

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val liveStreamSuiteOptimizerPatch = rawResourcePatch(
    name = "Remove Live Stream Files",
    description = "Deletes live broadcast and minigame files. Breaks lives and video calls.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        val fileTargets = listOf(
            "lib/arm64-v8a/liblink_mic_sdk.so",
            "lib/arm64-v8a/liblyrax.so",
            "lib/arm64-v8a/liblyrax_plugin.so",
            "lib/armeabi-v7a/liblink_mic_sdk.so",
            "lib/armeabi-v7a/liblyrax.so",
            "lib/armeabi-v7a/liblyrax_plugin.so",
            // DM voice/video calls (im/callroom, /tiktok/v1/im/voip/*) and live RTM/base runtimes
            "lib/arm64-v8a/libvoip.so",
            "lib/arm64-v8a/librtmglobal.so",
            "lib/arm64-v8a/libbase_live.so",
            "lib/armeabi-v7a/libvoip.so",
            "lib/armeabi-v7a/librtmglobal.so",
            "lib/armeabi-v7a/libbase_live.so",
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
            println("[Remove Live Stream Files] Stripped $count live stream SDK binaries, Lyrax RTC engines & minigame assets -> Saved $savedMb MB uncompressed")
        } else {
            println("[Remove Live Stream Files] Target live stream bloat not present.")
        }
    }
}
