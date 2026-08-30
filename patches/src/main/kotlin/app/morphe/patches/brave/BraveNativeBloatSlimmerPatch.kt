package app.morphe.patches.brave

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_STUB_BYTES = byteArrayOf()

// Unused companion native libraries in Brave (Impress Vision AI, WireGuard VPN, Android XR)
private val BLOAT_NATIVE_LIBS = listOf(
    "libimpress_api_jni.so",
    "libwg-go.so",
    "libandroidx.xr.arcore.openxr.so",
    "libandroidx.xr.runtime.openxr.so",
    "libarcore_sdk_c.so",
    "libarcore_sdk_jni.so",
)

@Suppress("unused")
val braveNativeBloatSlimmerPatch = rawResourcePatch(
    name = "Native Bloat Slimmer",
    description = "Strips unused native companion binaries (Impress Vision AI, WireGuard VPN, and Android XR) to significantly reduce APK size.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        var savedBytes = 0L
        val strippedLibs = mutableListOf<String>()

        val libDir = get("lib/arm64-v8a")
        if (libDir.exists() && libDir.isDirectory) {
            BLOAT_NATIVE_LIBS.forEach { libName ->
                val libFile = get("lib/arm64-v8a/$libName")
                if (libFile.exists() && libFile.isFile) {
                    val originalSize = libFile.length()
                    if (originalSize > 0) {
                        libFile.writeBytes(EMPTY_STUB_BYTES)
                        savedBytes += (originalSize - libFile.length())
                        strippedLibs.add(libName)
                    }
                }
            }
        }

        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
        println("[Native Bloat Slimmer] Stripped ${strippedLibs.size} bloat binaries (${strippedLibs.joinToString(", ")}) -> Saved $totalSavedMb MB")
    }
}
