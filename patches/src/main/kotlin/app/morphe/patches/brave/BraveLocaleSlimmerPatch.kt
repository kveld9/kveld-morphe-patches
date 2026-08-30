package app.morphe.patches.brave

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils

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

val braveLocaleSlimmerPatch = rawResourcePatch(
    name = "Locale PAK Slimmer",
    description = "Strips unselected language resource PAKs from assets/locales/.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE, Constants.COMPATIBILITY_VIVALDI)

    val targetLocales by stringOption(
        key = "locales",
        title = "Locales to keep",
        description = "Comma-separated locale codes to keep (e.g. 'es, es-419, en-US, pt-BR, fr, de'). English (en-US) is always kept as a safe fallback.",
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
            localesDir.walkTopDown().filter { it.isFile && it.name.endsWith(".pak", ignoreCase = true) }.forEach { pakFile ->
                val nameWithoutExt = pakFile.nameWithoutExtension.lowercase()
                val baseName = nameWithoutExt.substringBefore("_").substringBefore("-")
                if (nameWithoutExt !in baseLocales && baseName !in baseLocales) {
                    val originalSize = pakFile.length()
                    pakFile.writeBytes(EMPTY_DATAPACK_V5)
                    totalSavedBytes += (originalSize - pakFile.length())
                    removedLocales.add(pakFile.nameWithoutExtension)
                }
            }
        }

        val totalSavedMb = String.format(java.util.Locale.US, "%.2f", totalSavedBytes.toDouble() / (1024 * 1024))
        println("[Locale PAK Slimmer] Stripped ${removedLocales.size} PAKs (kept: ${baseLocales.sorted().joinToString(", ")}) -> Saved $totalSavedMb MB")
    }
}
