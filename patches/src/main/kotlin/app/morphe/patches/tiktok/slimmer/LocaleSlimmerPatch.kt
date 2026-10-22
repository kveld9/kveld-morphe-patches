package app.morphe.patches.tiktok.slimmer

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import java.util.Locale

private val EMPTY_BYTES = byteArrayOf()

private val LOCALE_ALIASES = mapOf(
    "in" to "id",
    "id" to "in",
    "iw" to "he",
    "he" to "iw",
    "ji" to "yi",
    "yi" to "ji",
    "no" to "nb",
    "nb" to "no",
    "tl" to "fil",
    "fil" to "tl",
)

private fun expandLocaleAliases(locales: Set<String>): Set<String> =
    locales + locales.mapNotNull { LOCALE_ALIASES[it] }

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
        val assetsDir = get("assets")
        if (!assetsDir.exists() || !assetsDir.isDirectory) {
            println("[Remove Extra Languages] Skipped: assets directory not found.")
            return@execute
        }

        val selectedLangs = expandLocaleAliases(
            LocaleUtils.parseTargetLocales(targetLocales, defaultLocales = setOf("en"))
        )

        var savedBytes = 0L
        var strippedFolders = 0

        assetsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("strings#lang_") }?.forEach { langDir ->
            val langCode = langDir.name.removePrefix("strings#lang_").lowercase()
            if (langCode !in selectedLangs) {
                var folderStripped = false
                langDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val origSize = file.length()
                    if (origSize > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += origSize
                        folderStripped = true
                    }
                }
                if (folderStripped) {
                    strippedFolders++
                }
            }
        }

        if (strippedFolders > 0) {
            val savedMb = String.format(Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
            println("[Remove Extra Languages] Stripped $strippedFolders unused language packs (retained: ${selectedLangs.joinToString()}) -> Saved $savedMb MB uncompressed")
        } else {
            println("[Remove Extra Languages] All language packs match retained selection (${selectedLangs.joinToString()}).")
        }
    }
}
