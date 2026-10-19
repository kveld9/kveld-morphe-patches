package app.morphe.patches.brave

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_STUB_BYTES = byteArrayOf()

// Unused companion native libraries in Brave (Android XR Impress renderer, Android XR, ARCore)
private val BLOAT_NATIVE_LIBS = listOf(
    "libimpress_api_jni.so",
    "libandroidx.xr.arcore.openxr.so",
    "libandroidx.xr.runtime.openxr.so",
    "libarcore_sdk_c.so",
    "libarcore_sdk_jni.so",
)

@Suppress("unused")
val braveNativeBloatSlimmerPatch = rawResourcePatch(
    name = "Remove Extra Files",
    description = "Deletes unused AR and XR files to shrink the app.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        var savedBytes = 0L
        val strippedLibs = mutableListOf<String>()

        val abis = listOf("lib/arm64-v8a", "lib/armeabi-v7a")

        abis.forEach { abi ->
            val libDir = get(abi)
            if (!libDir.exists() || !libDir.isDirectory) return@forEach

            BLOAT_NATIVE_LIBS.forEach { libName ->
                val libFile = get("$abi/$libName")
                if (libFile.exists() && libFile.isFile) {
                    val originalSize = libFile.length()
                    if (originalSize > 0) {
                        libFile.writeBytes(EMPTY_STUB_BYTES)
                        savedBytes += (originalSize - libFile.length())
                        strippedLibs.add("$abi/$libName")
                    }
                }
            }
        }

        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
        println("[Remove Extra Files] Stripped ${strippedLibs.size} bloat binaries (${strippedLibs.joinToString(", ")}) -> Saved $totalSavedMb MB")
    }
}
