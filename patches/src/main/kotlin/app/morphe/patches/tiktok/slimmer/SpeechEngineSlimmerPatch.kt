package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val speechEngineSlimmerPatch = rawResourcePatch(
    name = "Voice & Speech Engine De-bloat",
    description = "Strips on-device voice recognition and speech synthesis engines (libspeechspg.so, libspeechsdk.so) and their loader stubs (libspeechengine.so, libspeechepg.so) to save APK space. Breaks voice search (microphone button), voice input, and editor text-to-speech/sing features.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var savedBytes = 0L
        var count = 0

        val abis = listOf("lib/arm64-v8a", "lib/armeabi-v7a")
        val libs = listOf(
            "libspeechspg.so",
            "libspeechsdk.so",
            "libspeechengine.so",
            "libspeechepg.so",
        )

        abis.forEach { abi ->
            libs.forEach { lib ->
                val file = get("$abi/$lib")
                if (file.exists() && file.isFile) {
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
            println("[Voice & Speech Engine De-bloat] Stripped $count voice/speech engine binaries -> Saved $savedMb MB uncompressed")
        } else {
            println("[Voice & Speech Engine De-bloat] Target speech engines not present.")
        }
    }
}
