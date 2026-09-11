package app.morphe.patches.hevy

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_TRANSPARENT_PNG by lazy {
    byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06,
        0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(), 0x00, 0x00, 0x00, 0x0B,
        0x49, 0x44, 0x41, 0x54, 0x78, 0x9C.toByte(), 0x63, 0x60, 0x00, 0x02, 0x00, 0x00, 0x05, 0x00,
        0x01, 0xE9.toByte(), 0xFA.toByte(), 0xDC.toByte(), 0xD8.toByte(), 0x00, 0x00, 0x00,
        0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
    )
}

private val EMPTY_MP4_HEADER by lazy {
    byteArrayOf(
        0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D,
        0x00, 0x00, 0x02, 0x00, 0x69, 0x73, 0x6F, 0x6D, 0x69, 0x73, 0x6F, 0x32,
        0x61, 0x76, 0x63, 0x31, 0x6D, 0x70, 0x34, 0x31
    )
}

@Suppress("unused")
val hevyResourceSlimmerPatch = resourcePatch(
    name = "Hevy Resource Slimmer",
    description = "Strips embedded onboarding MP4 tutorial video, heavy IMG.LY photo editor stickers/textures, and compiler junk metadata.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)

    execute {
        var totalSavedBytes = 0L
        var stubbedMediaCount = 0
        var junkFilesCount = 0

        val apkRoot = get("AndroidManifest.xml").parentFile ?: get(".")
        val protectedPrefixes = listOf("assets", "res", "smali", "lib")

        // 1. Clean up build properties, compiler junk, and obsolete licenses in APK root
        apkRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(apkRoot).path.replace('\\', '/')
            val segments = relativePath.split('/')
            val topDir = segments.firstOrNull() ?: ""

            if (topDir in protectedPrefixes) return@forEach
            if (relativePath == "AndroidManifest.xml" || relativePath.startsWith("META-INF/services/")) return@forEach

            val name = file.name
            val isJunk = name.endsWith(".properties") ||
                name.endsWith(".proto") ||
                name.endsWith(".version") ||
                name.endsWith("_VERSION") ||
                name == "DebugProbesKt.bin" ||
                name == "stamp-cert-sha256" ||
                (topDir == "META-INF" && (name.startsWith("LICENSE") || name.startsWith("NOTICE") || name.startsWith("README")))

            if (isJunk) {
                val size = file.length()
                if (file.delete()) {
                    junkFilesCount++
                    totalSavedBytes += size
                }
            }
        }

        // 2. Stub heavy onboarding MP4 tutorial in res/raw/
        val resRawDir = get("res/raw")
        if (resRawDir.exists() && resRawDir.isDirectory) {
            resRawDir.walkTopDown().filter { it.isFile && it.name.endsWith(".mp4") }.forEach { file ->
                val originalSize = file.length()
                file.writeBytes(EMPTY_MP4_HEADER)
                val saved = originalSize - file.length()
                if (saved > 0) {
                    stubbedMediaCount++
                    totalSavedBytes += saved
                }
            }
        }

        // 3. Stub heavy IMG.LY PhotoEditor textures and stickers in res/
        val resDir = get("res")
        if (resDir.exists() && resDir.isDirectory) {
            resDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val name = file.name
                if (name.startsWith("imgly_text_design_") || name.startsWith("imgly_overlay_")) {
                    val originalSize = file.length()
                    file.writeBytes(EMPTY_TRANSPARENT_PNG)
                    val saved = originalSize - file.length()
                    if (saved > 0) {
                        stubbedMediaCount++
                        totalSavedBytes += saved
                    }
                }
            }
        }

        // 4. Prune unused IMG.LY fonts in assets/fonts/
        val assetsFontsDir = get("assets/fonts")
        if (assetsFontsDir.exists() && assetsFontsDir.isDirectory) {
            assetsFontsDir.walkTopDown().filter { it.isFile && it.name.startsWith("imgly_font_") }.forEach { file ->
                val size = file.length()
                if (file.delete()) {
                    stubbedMediaCount++
                    totalSavedBytes += size
                }
            }
        }

        val savedFormatted = app.morphe.patches.shared.LocaleUtils.formatBytes(totalSavedBytes)
        println("[Hevy Resource Slimmer] Pruned $junkFilesCount build junk files, stubbed $stubbedMediaCount media assets -> Saved $savedFormatted")
    }
}
