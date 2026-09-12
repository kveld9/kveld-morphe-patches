package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

val localeSlimmerPatch = rawResourcePatch(
    name = "Language Pack Purger",
    description = "Strips unselected language string bundles from assets/strings#lang_* to save APK space.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val targetLocales by stringOption(
        key = "locales",
        title = "Languages to keep",
        description = "Comma-separated language codes to keep (e.g. 'en, es, pt, fr, de'). English ('en') is always preserved as safe fallback.",
        default = "en",
        required = false,
    )

    execute {
        val selectedLangs = (targetLocales ?: "en")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        // Always ensure English fallback is kept
        selectedLangs.add("en")

        val assetsDir = get("assets")
        if (assetsDir.exists() && assetsDir.isDirectory) {
            var savedBytes = 0L
            var strippedFolders = 0
            val strippedList = mutableListOf<String>()

            assetsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("strings#lang_") }?.forEach { langDir ->
                val langCode = langDir.name.removePrefix("strings#lang_").lowercase()
                if (langCode !in selectedLangs) {
                    langDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val origSize = file.length()
                        if (origSize > 0) {
                            file.writeBytes(EMPTY_BYTES)
                            savedBytes += origSize
                        }
                    }
                    strippedFolders++
                    strippedList.add(langCode)
                }
            }

            if (strippedFolders > 0) {
                val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
                println("[Language Pack Purger] Stripped $strippedFolders unused language packs (retained: ${selectedLangs.joinToString()}) -> Saved $savedMb MB uncompressed (~15 MB in APK)")
            } else {
                println("[Language Pack Purger] All language packs match retained selection (${selectedLangs.joinToString()}).")
            }
        }
    }
}
