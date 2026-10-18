package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants

val localeSlimmerPatch = rawResourcePatch(
    name = "Remove Extra Languages",
    description = "Deletes unselected language packs.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val targetLocales by stringOption(
        key = "locales",
        title = "Languages to keep",
        description = "Comma-separated language codes to keep (e.g. en, es, pt). English is always kept.",
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

            assetsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("strings#lang_") }?.forEach { langDir ->
                val langCode = langDir.name.removePrefix("strings#lang_").lowercase()
                if (langCode !in selectedLangs) {
                    val dirSize = langDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    if (langDir.deleteRecursively()) {
                        savedBytes += dirSize
                        strippedFolders++
                    }
                }
            }

            if (strippedFolders > 0) {
                val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
                println("[Remove Extra Languages] Stripped $strippedFolders unused language packs (retained: ${selectedLangs.joinToString()}) -> Saved $savedMb MB uncompressed")
            } else {
                println("[Remove Extra Languages] All language packs match retained selection (${selectedLangs.joinToString()}).")
            }
        }
    }
}
