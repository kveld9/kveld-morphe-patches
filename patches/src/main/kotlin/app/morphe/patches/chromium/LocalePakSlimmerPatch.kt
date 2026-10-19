package app.morphe.patches.chromium

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import java.io.File

// DataPack v5 minimal valid empty header:
// version=5 (4B), tables=1 (1B), encoding=1 (1B), num_aliases=0 (2B), num_resources=0 (2B), alias_count=0 (2B)
// margin index entry: id=0 (2B), offset=18 (4B) -> total 18 bytes
private val EMPTY_DATAPACK_V5 by lazy {
    byteArrayOf(
        0x05, 0x00, 0x00, 0x00,
        0x01, 0x01,
        0x00, 0x00,
        0x00, 0x00,
        0x00, 0x00,
        0x00, 0x00,
        0x12, 0x00, 0x00, 0x00,
    )
}

@Suppress("unused")
val localePakSlimmerPatch = rawResourcePatch(
    name = "Remove Extra Languages",
    description = "Deletes unselected language packs.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    val targetLocales by stringOption(
        key = "locales",
        title = "Languages to keep",
        description = "Comma-separated language codes to keep (e.g. es, fr, de). English is always kept.",
        default = "en-US",
        required = false,
    )

    execute {
        val baseLocales = LocaleUtils.parseTargetLocales(targetLocales, defaultLocales = setOf("en-us", "en"))
        var totalSavedBytes = 0L

        // Strip Chromium PAK locale files in assets/locales/
        val localesDir = get("assets/locales")
        val removedLocales = mutableListOf<String>()

        if (localesDir.exists() && localesDir.isDirectory) {
            val fallbackPak = localesDir.walkTopDown().firstOrNull { it.isFile && it.name.equals("en-US.pak", ignoreCase = true) }
            val fallbackBytes = fallbackPak?.readBytes() ?: EMPTY_DATAPACK_V5

            localesDir.walkTopDown().filter { it.isFile && it.name.endsWith(".pak", ignoreCase = true) }.forEach { pakFile ->
                val nameWithoutExt = pakFile.nameWithoutExtension.lowercase()
                val baseName = nameWithoutExt.substringBefore("_").substringBefore("-")
                if (nameWithoutExt !in baseLocales && baseName !in baseLocales) {
                    val originalSize = pakFile.length()
                    if (originalSize <= EMPTY_DATAPACK_V5.size) {
                        return@forEach // Already a minimal empty stub (e.g. gender variants)
                    }
                    val isGenderVariant = nameWithoutExt.endsWith("_feminine") ||
                        nameWithoutExt.endsWith("_masculine") ||
                        nameWithoutExt.endsWith("_neuter")
                    val replacementBytes = if (isGenderVariant) EMPTY_DATAPACK_V5 else fallbackBytes
                    val savedBytes = replaceLocalePak(pakFile, replacementBytes)
                    if (savedBytes > 0L) {
                        totalSavedBytes += savedBytes
                        removedLocales.add(pakFile.nameWithoutExtension)
                    }
                }
            }
        }

        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", totalSavedBytes.toDouble() / (1024 * 1024))
        println("[Remove Extra Languages] Stripped ${removedLocales.size} PAKs (kept: ${baseLocales.sorted().joinToString(", ")}) -> Saved $totalSavedMb MB")
    }
}

private fun replaceLocalePak(pakFile: File, replacement: ByteArray): Long {
    val originalSize = pakFile.length()
    if (originalSize <= replacement.size) {
        return 0L
    }
    pakFile.writeBytes(replacement)
    return originalSize - pakFile.length()
}
