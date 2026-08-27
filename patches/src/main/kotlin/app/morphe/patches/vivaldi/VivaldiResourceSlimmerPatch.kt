package app.morphe.patches.vivaldi

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_STUB_BYTES = byteArrayOf()

@Suppress("unused")
val vivaldiResourceSlimmerPatch = rawResourcePatch(
    name = "Resource Slimmer",
    description = "Strips bundled stock wallpapers and sponsored Speed Dial thumbnails to reduce APK size.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        var totalSavedBytes = 0L

        // 1. Strip default wallpapers in assets/wallpapers/
        val wallpapersDir = get("assets/wallpapers")
        var wallpaperCount = 0

        if (wallpapersDir.exists() && wallpapersDir.isDirectory) {
            wallpapersDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val originalSize = file.length()
                if (originalSize > 0) {
                    file.writeBytes(EMPTY_STUB_BYTES)
                    totalSavedBytes += (originalSize - file.length())
                    wallpaperCount++
                }
            }
        }

        // 2. Strip default Speed Dial thumbnails in assets/sd_thumbnails/
        val thumbnailsDir = get("assets/sd_thumbnails")
        var thumbnailCount = 0

        if (thumbnailsDir.exists() && thumbnailsDir.isDirectory) {
            thumbnailsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val originalSize = file.length()
                if (originalSize > 0) {
                    file.writeBytes(EMPTY_STUB_BYTES)
                    totalSavedBytes += (originalSize - file.length())
                    thumbnailCount++
                }
            }
        }

        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", totalSavedBytes.toDouble() / (1024 * 1024))
        println("[Vivaldi Resource Slimmer] Cleaned $wallpaperCount wallpapers and $thumbnailCount thumbnails -> Saved $totalSavedMb MB")
    }
}
