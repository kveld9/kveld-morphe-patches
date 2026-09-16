package app.morphe.patches.tiktok.performance

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val p2pVideoRelayBlockerPatch = rawResourcePatch(
    name = "P2P Video Relay & Mesh CDN Blocker",
    description = "Strips background Peer-to-Peer CDN distribution binaries (libavmdlp2pv2.so and libp2plivevdp.so) to prevent battery drain, background data upload, and mesh relay.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        val targets = listOf(
            "lib/arm64-v8a/libavmdlp2pv2.so",
            "lib/arm64-v8a/libp2plivevdp.so",
            "lib/armeabi-v7a/libavmdlp2pv2.so",
            "lib/armeabi-v7a/libp2plivevdp.so",
        )

        var savedBytes = 0L
        var count = 0

        targets.forEach { path ->
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

        if (count > 0) {
            val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[P2P Video Relay & Mesh CDN Blocker] Neutralized $count P2P streaming binaries -> Saved $savedMb MB uncompressed")
        } else {
            println("[P2P Video Relay & Mesh CDN Blocker] P2P binaries not present.")
        }
    }
}
