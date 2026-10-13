package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants

val localeSlimmerPatch = rawResourcePatch(
    name = "Language Pack Purger",
    description = "Strips unselected language string bundles from assets/strings#lang_* to save APK space.",
    default = false,
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

            assetsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("strings#lang_") }?.forEach { langDir ->
                val langCode = langDir.name.removePrefix("strings#lang_").lowercase()
                if (langCode !in selectedLangs) {
                    // Delete the folder instead of emptying every file inside it.
                    // Zeroing rewrites hundreds of entries and leaves them behind as
                    // 0-byte files; deleting is one operation and keeps the rebuilt
                    // APK free of empty entries. Deletion is already the approach used
                    // by the universal locale slimmer for the same purpose.
                    val folderSize = langDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    if (folderSize > 0) {
                        savedBytes += folderSize
                    }
                    langDir.deleteRecursively()
                    strippedFolders++
                }
            }

            if (strippedFolders > 0) {
                val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
                println("[Language Pack Purger] Stripped $strippedFolders unused language packs (retained: ${selectedLangs.joinToString()}) -> Saved $savedMb MB uncompressed")
            } else {
                println("[Language Pack Purger] All language packs match retained selection (${selectedLangs.joinToString()}).")
            }
        }
    }
}
