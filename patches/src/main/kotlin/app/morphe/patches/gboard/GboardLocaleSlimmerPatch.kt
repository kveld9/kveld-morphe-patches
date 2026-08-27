package app.morphe.patches.gboard

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils

val gboardLocaleSlimmerPatch = resourcePatch(
    name = "Locale Resource Slimmer",
    description = "Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val targetLocales by stringOption(
        key = "locales",
        title = "Locales to keep",
        description = "Comma-separated language codes to preserve (e.g. 'en, es, pt, fr, de'). English fallback is always retained.",
        default = "en, es",
        required = false,
    )

    execute {
        val resDir = get("res")
        if (!resDir.exists() || !resDir.isDirectory) return@execute

        val keepSet = LocaleUtils.parseTargetLocales(targetLocales, defaultLocales = setOf("en", "en-us"))
        var removedDirs = 0
        var savedBytes = 0L

        resDir.listFiles { file -> file.isDirectory }?.forEach { dir ->
            val languages = LocaleUtils.extractResourceLanguages(dir.name)

            // Base resources with no language qualifiers are always kept as fallback
            if (languages.isEmpty()) return@forEach

            // Check if any extracted language matches the keep set
            val shouldKeep = languages.any { it in keepSet }

            if (!shouldKeep) {
                val dirSize = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (dir.deleteRecursively()) {
                    removedDirs++
                    savedBytes += dirSize
                }
            }
        }

        // Clean up any remaining empty directories
        resDir.walkBottomUp()
            .filter { it.isDirectory && it != resDir && it.listFiles()?.isEmpty() == true }
            .forEach { it.delete() }

        val savedMb = String.format(java.util.Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
        println("[Gboard Locale Resource Slimmer] Stripped $removedDirs localization dirs (kept: ${keepSet.sorted().joinToString(", ")}) -> Saved $savedMb MB")
    }
}
