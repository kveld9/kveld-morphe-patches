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
    description = "Strips embedded third-party license text, onboarding tutorial Lottie animations, and promotional GIFs using dynamic content heuristics.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val targetDirs = listOfNotNull(
            get("res").takeIf { it.exists() && it.isDirectory },
            get("assets").takeIf { it.exists() && it.isDirectory },
        )

        for (dir in targetDirs) {
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val name = file.name
                val size = file.length()

                // 1. Unified GIF stubbing (res/ and assets/)
                if (name.endsWith(".gif")) {
                    file.writeBytes(EMPTY_TRANSPARENT_GIF)
                    return@forEach
                }

                // 2. Memory-efficient Lottie JSON detection for tutorials/onboarding
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
                                file.writeBytes(EMPTY_LOTTIE_JSON)
                            }
                        }
                    } catch (_: Exception) {
                        // Skip unreadable files
                    }
                    return@forEach
                }

                // 3. Simplified Open Source License text detection (> 100 KB and contains license headers)
                if (size > 100_000) {
                    try {
                        val buf = ByteArray(minOf(512, size.toInt()))
                        val readBytes = file.inputStream().use { it.read(buf) }
                        if (readBytes > 0) {
                            val headStr = String(buf, 0, readBytes, Charsets.US_ASCII)
                            if (headStr.contains("License") || headStr.contains("Copyright")) {
                                file.writeBytes(DUMMY_LICENSE_TEXT)
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore read errors
                    }
                    return@forEach
                }

                // 4. Simplified Open Source License metadata detection (matches "<offset>:<length> <Library Name>")
                if (size in 1_000..50_000) {
                    try {
                        val buf = ByteArray(minOf(128, size.toInt()))
                        val readBytes = file.inputStream().use { it.read(buf) }
                        if (readBytes > 0) {
                            val headStr = String(buf, 0, readBytes, Charsets.US_ASCII)
                            if (headStr.contains(LICENSE_METADATA_REGEX)) {
                                file.writeBytes(DUMMY_LICENSE_METADATA)
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore read errors
                    }
                }
            }
        }
    }
}
