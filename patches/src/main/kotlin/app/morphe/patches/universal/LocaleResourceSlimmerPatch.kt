package app.morphe.patches.universal

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.LocaleUtils

@Suppress("unused")
val localeResourceSlimmerPatch = resourcePatch(
    name = "Locale Resource Slimmer",
    description = "Strips unselected language translation directories from res/ (e.g. values-*, raw-*, xml-*). Base fallback resources with no language qualifiers are always preserved.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI
    val targetLocales by stringOption(
        key = "locales",
        title = "Locales to keep",
        description = "Comma-separated language codes to preserve (e.g. 'en, es, pt, fr, de'). English fallback is always retained.",
        default = "en",
        required = false,
    )

    execute {
        val mainRes = get("res")
        val resDirs = LocaleUtils.resolveResourceDirectories(mainRes)
        if (resDirs.isEmpty()) {
            println("[Locale Resource Slimmer] res/ directory not found - skipping safely.")
            return@execute
        }

        val keepSet = LocaleUtils.parseTargetLocales(targetLocales, defaultLocales = setOf("en", "en-us"))
        var removedDirs = 0
        var savedBytes = 0L

        for (resDir in resDirs) {
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
        }

        if (removedDirs == 0) {
            val packageSuffix = if (resDirs.size > 1) " across ${resDirs.size} resource packages" else ""
            println("[Locale Resource Slimmer] No non-target localization directories found to strip$packageSuffix (kept: ${keepSet.sorted().joinToString(", ")}).")
            return@execute
        }

        val savedFormatted = LocaleUtils.formatBytes(savedBytes)
        val packageSuffix = if (resDirs.size > 1) " across ${resDirs.size} resource packages" else ""
        println("[Locale Resource Slimmer] Stripped $removedDirs localization dirs$packageSuffix (kept: ${keepSet.sorted().joinToString(", ")}) -> Saved $savedFormatted")
    }
}
