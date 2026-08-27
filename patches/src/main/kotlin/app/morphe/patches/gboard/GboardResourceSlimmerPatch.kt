package app.morphe.patches.gboard

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_TRANSPARENT_GIF by lazy {
    byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
        0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x21,
        0xf9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
        0x01, 0x00, 0x3b,
    )
}

private val EMPTY_LOTTIE_JSON by lazy {
    """{"v":"5.5.0","fr":60,"ip":0,"op":60,"w":100,"h":100,"layers":[]}""".toByteArray(Charsets.UTF_8)
}

private val DUMMY_LICENSE_METADATA by lazy {
    "0:28 Gboard Lite (Morphe)\n".toByteArray(Charsets.US_ASCII)
}

private val DUMMY_LICENSE_TEXT by lazy {
    "Gboard Lite (Morphe Patched)\n".toByteArray(Charsets.US_ASCII)
}

private val LICENSE_METADATA_REGEX = Regex("""\d+:\d+\s+[A-Za-z]""")

val gboardResourceSlimmerPatch = resourcePatch(
    name = "Resource Slimmer",
    description = "Strips embedded third-party license text, onboarding tutorial Lottie animations, promotional GIFs, and APK root metadata/junk files.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        var totalSavedBytes = 0L

        // 1. Clean up APK root metadata, build properties, and compiler junk
        val apkRoot = get("AndroidManifest.xml").parentFile ?: get(".")
        val protectedPrefixes = listOf("assets", "res", "smali", "lib")
        var junkFilesCount = 0

        apkRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(apkRoot).path.replace('\\', '/')
            val segments = relativePath.split('/')
            val topDir = segments.firstOrNull() ?: ""

            // Never delete from protected code/resource trees here
            if (topDir in protectedPrefixes) return@forEach
            if (relativePath == "AndroidManifest.xml" || relativePath.startsWith("META-INF/services/")) return@forEach

            val name = file.name
            val isJunk = name.endsWith(".properties") ||
                name.endsWith(".proto") ||
                name.endsWith(".version") ||
                name.endsWith("_VERSION") ||
                name == "DebugProbesKt.bin" ||
                name == "stamp-cert-sha256" ||
                name == "version-control-info.textproto" ||
                name == "kotlin-tooling-metadata.json" ||
                (topDir == "META-INF" && (name.startsWith("LICENSE") || name.startsWith("NOTICE") || name.startsWith("README") || name == "CHANGES"))

            if (isJunk) {
                val size = file.length()
                if (file.delete()) {
                    junkFilesCount++
                    totalSavedBytes += size
                }
            }
        }

        val kotlinDir = get("kotlin")
        if (kotlinDir.exists() && kotlinDir.isDirectory) {
            val kSize = kotlinDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            if (kotlinDir.deleteRecursively()) {
                junkFilesCount++
                totalSavedBytes += kSize
            }
        }

        // 2. Resource & Asset heuristic slimming
        val targetDirs = listOfNotNull(
            get("res").takeIf { it.exists() && it.isDirectory },
            get("assets").takeIf { it.exists() && it.isDirectory },
        )

        var gifCount = 0
        var lottieCount = 0
        var licenseCount = 0

        for (dir in targetDirs) {
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val name = file.name
                val size = file.length()

                // Unified GIF stubbing (res/ and assets/)
                if (name.endsWith(".gif")) {
                    val originalSize = file.length()
                    file.writeBytes(EMPTY_TRANSPARENT_GIF)
                    val saved = originalSize - file.length()
                    if (saved > 0) {
                        gifCount++
                        totalSavedBytes += saved
                    }
                    return@forEach
                }

                // Memory-efficient Lottie JSON detection for tutorials/onboarding
                if (name.endsWith(".json")) {
                    val lowerPath = file.path.replace('\\', '/').lowercase()
                    // Never stub interactive UI, access point panel, or motion handler animations
                    if (lowerPath.contains("access_point") || lowerPath.contains("motion") ||
                        lowerPath.contains("panel") || lowerPath.contains("drag") ||
                        lowerPath.contains("trackpad") || lowerPath.contains("cursor") ||
                        lowerPath.contains("res/raw")) {
                        return@forEach
                    }

                    try {
                        val buf = ByteArray(minOf(1024, size.toInt()))
                        val readBytes = file.inputStream().use { it.read(buf) }
                        if (readBytes > 0) {
                            val headStr = String(buf, 0, readBytes, Charsets.UTF_8)
                            if (headStr.contains("\"v\"") && headStr.contains("\"layers\"") && headStr.contains("\"fr\"")) {
                                val originalSize = file.length()
                                file.writeBytes(EMPTY_LOTTIE_JSON)
                                val saved = originalSize - file.length()
                                if (saved > 0) {
                                    lottieCount++
                                    totalSavedBytes += saved
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Skip unreadable files
                    }
                    return@forEach
                }

                // Simplified Open Source License text detection (> 100 KB and contains license headers)
                if (size > 100_000) {
                    try {
                        val buf = ByteArray(minOf(512, size.toInt()))
                        val readBytes = file.inputStream().use { it.read(buf) }
                        if (readBytes > 0) {
                            val headStr = String(buf, 0, readBytes, Charsets.US_ASCII)
                            if (headStr.contains("License") || headStr.contains("Copyright")) {
                                val originalSize = file.length()
                                file.writeBytes(DUMMY_LICENSE_TEXT)
                                val saved = originalSize - file.length()
                                if (saved > 0) {
                                    licenseCount++
                                    totalSavedBytes += saved
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore read errors
                    }
                    return@forEach
                }

                // Simplified Open Source License metadata detection (matches "<offset>:<length> <Library Name>")
                if (size in 1_000..50_000) {
                    try {
                        val buf = ByteArray(minOf(128, size.toInt()))
                        val readBytes = file.inputStream().use { it.read(buf) }
                        if (readBytes > 0) {
                            val headStr = String(buf, 0, readBytes, Charsets.US_ASCII)
                            if (headStr.contains(LICENSE_METADATA_REGEX)) {
                                val originalSize = file.length()
                                file.writeBytes(DUMMY_LICENSE_METADATA)
                                val saved = originalSize - file.length()
                                if (saved > 0) {
                                    licenseCount++
                                    totalSavedBytes += saved
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore read errors
                    }
                }
            }
        }

        val totalMb = String.format(java.util.Locale.US, "%.2f", totalSavedBytes.toDouble() / (1024 * 1024))
        println("[Gboard Resource Slimmer] Cleaned $junkFilesCount junk files, $lottieCount animations, $gifCount GIFs, $licenseCount licenses -> Saved $totalMb MB")
    }
}
