package app.morphe.patches.shared

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.io.File

private val STRIPPABLE_DENSITY_QUALIFIERS = setOf(
    "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"
)

private val PROTECTED_DENSITY_QUALIFIERS = setOf(
    "nodpi", "anydpi"
)

private val DPI_ALIASES = mapOf(
    "120" to "ldpi",
    "160" to "mdpi",
    "213" to "tvdpi",
    "240" to "hdpi",
    "320" to "xhdpi",
    "720p" to "xhdpi",
    "480" to "xxhdpi",
    "1080p" to "xxhdpi",
    "640" to "xxxhdpi",
    "1440p" to "xxxhdpi",
    "2k" to "xxxhdpi"
)

private class SlimmerStats(
    var removedFiles: Int = 0,
    var preservedOrphans: Int = 0,
    var savedBytes: Long = 0L,
    val rescuedNames: MutableList<String> = mutableListOf(),
)

private fun parseTargetDpis(rawInput: String?): Set<String> {
    if (rawInput.isNullOrBlank()) return setOf("xxhdpi")

    val parsed = rawInput.split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .map { DPI_ALIASES[it] ?: it }
        .filter { it in STRIPPABLE_DENSITY_QUALIFIERS }
        .toSet()

    return if (parsed.isEmpty()) setOf("xxhdpi") else parsed
}

private fun extractDensityQualifier(dirName: String): String? {
    val segments = dirName.split("-")
    if (segments.size < 2) return null

    return segments.drop(1)
        .map { it.lowercase() }
        .firstOrNull { it in STRIPPABLE_DENSITY_QUALIFIERS || it in PROTECTED_DENSITY_QUALIFIERS }
}

private fun isGraphicResourceDirectory(dirName: String): Boolean {
    return dirName.startsWith("drawable") || dirName.startsWith("mipmap")
}

private fun isDensityDirectoryCandidate(dir: File): Boolean {
    if (!dir.isDirectory) return false
    if (!isGraphicResourceDirectory(dir.name)) return false
    val density = extractDensityQualifier(dir.name) ?: return false
    return density !in PROTECTED_DENSITY_QUALIFIERS
}

private fun getPrefixWithoutDensity(dirName: String, density: String): String {
    return dirName.replace("-$density", "")
}

private fun resolveTargetDir(sourceDir: File, keptDirs: List<File>): File? {
    val sourceDensity = extractDensityQualifier(sourceDir.name) ?: return keptDirs.firstOrNull()
    val basePrefix = getPrefixWithoutDensity(sourceDir.name, sourceDensity)

    return keptDirs.firstOrNull {
        val keptDensity = extractDensityQualifier(it.name) ?: ""
        getPrefixWithoutDensity(it.name, keptDensity) == basePrefix
    } ?: keptDirs.firstOrNull()
}

private fun preserveOrphanFile(file: File, targetDir: File): Boolean {
    val destination = File(targetDir, file.name)
    if (destination.exists()) return false

    file.copyTo(destination, overwrite = false)
    return true
}

private fun collectProtectedFileNames(resDir: File, keptDirs: List<File>): MutableSet<String> {
    val protectedNames = mutableSetOf<String>()

    keptDirs.forEach { dir ->
        dir.listFiles { f -> f.isFile }?.forEach { protectedNames.add(it.name) }
    }

    resDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
        if (isGraphicResourceDirectory(dir.name)) {
            val density = extractDensityQualifier(dir.name)
            if (density == null || density in PROTECTED_DENSITY_QUALIFIERS) {
                dir.listFiles { f -> f.isFile }?.forEach { protectedNames.add(it.name) }
            }
        }
    }

    return protectedNames
}

private fun trimDirectoryFiles(
    dir: File,
    targetDir: File?,
    protectedFileNames: MutableSet<String>,
    stats: SlimmerStats,
) {
    val files = dir.listFiles { f -> f.isFile } ?: return

    for (file in files) {
        val fileSize = file.length()
        if (file.name !in protectedFileNames && targetDir != null) {
            if (preserveOrphanFile(file, targetDir)) {
                stats.preservedOrphans++
                stats.rescuedNames.add(file.name)
                protectedFileNames.add(file.name)
            }
        }

        if (file.delete()) {
            stats.removedFiles++
            stats.savedBytes += fileSize
        }
    }
}

private fun pruneEmptyDirectories(resDir: File): Int {
    var pruned = 0
    resDir.walkBottomUp()
        .filter { it.isDirectory && it != resDir && isGraphicResourceDirectory(it.name) && it.listFiles()?.isEmpty() == true }
        .forEach {
            if (it.delete()) pruned++
        }
    return pruned
}

private fun formatRescuedSample(rescued: List<String>): String {
    if (rescued.size <= 6) return rescued.joinToString(", ")
    return "${rescued.take(6).joinToString(", ")} ... (+${rescued.size - 6} more)"
}

@Suppress("unused")
val dpiResourceSlimmerPatch = resourcePatch(
    name = "DPI Resource Slimmer",
    description = "Strips unselected screen density resource directories from res/ (e.g. drawable-mdpi, drawable-hdpi, mipmap-xhdpi). Density-independent resources (nodpi, anydpi) and orphan resources are safely preserved.",
    default = false,
) {
    compatibleWith(
        Constants.COMPATIBILITY_BRAVE,
        Constants.COMPATIBILITY_GBOARD,
        Constants.COMPATIBILITY_VIVALDI,
    )

    val targetDpis by stringOption(
        key = "dpis",
        title = "DPI densities to keep",
        description = "Comma-separated screen densities to preserve (e.g. 'xxhdpi', 'xhdpi, xxhdpi', 'xxxhdpi'). Density-independent resources (nodpi, anydpi) and unquantified base directories are always preserved.",
        default = "xxhdpi",
        required = false,
    )

    execute {
        val resDir = get("res")
        if (!resDir.exists() || !resDir.isDirectory) return@execute

        val keepSet = parseTargetDpis(targetDpis)
        val allDirs = resDir.listFiles { f -> f.isDirectory }?.toList() ?: return@execute

        val keptDirs = allDirs.filter { isGraphicResourceDirectory(it.name) && extractDensityQualifier(it.name) in keepSet }
        if (keptDirs.isEmpty()) {
            val available = allDirs
                .filter { isGraphicResourceDirectory(it.name) }
                .mapNotNull { extractDensityQualifier(it.name) }
                .distinct()
                .sorted()
            println("[DPI Resource Slimmer] No matching density directories found for target $keepSet across APK (available: $available) - skipping safely to prevent resource loss.")
            return@execute
        }

        val protectedFileNames = collectProtectedFileNames(resDir, keptDirs)
        val candidateDirs = allDirs.filter { isDensityDirectoryCandidate(it) && extractDensityQualifier(it.name) !in keepSet }

        println("[DPI Resource Slimmer] Keeping densities ${keepSet.sorted().joinToString(", ")} across ${keptDirs.size} directories: ${keptDirs.map { it.name }.sorted().joinToString(", ")}")
        if (candidateDirs.isNotEmpty()) {
            println("[DPI Resource Slimmer] Trimming ${candidateDirs.size} unselected directories: ${candidateDirs.map { it.name }.sorted().joinToString(", ")}")
        }

        val stats = SlimmerStats()
        for (dir in candidateDirs) {
            val targetDir = resolveTargetDir(dir, keptDirs)
            trimDirectoryFiles(dir, targetDir, protectedFileNames, stats)
        }

        val removedDirs = pruneEmptyDirectories(resDir)
        val savedFormatted = LocaleUtils.formatBytes(stats.savedBytes)

        if (stats.preservedOrphans > 0) {
            println("[DPI Resource Slimmer] Rescued ${stats.preservedOrphans} single-density orphan assets into kept targets: ${formatRescuedSample(stats.rescuedNames)}")
        }

        println("[DPI Resource Slimmer] Stripped ${stats.removedFiles} files across $removedDirs density dirs (${stats.preservedOrphans} orphan resources preserved, kept: ${keepSet.sorted().joinToString(", ")}) -> Saved $savedFormatted")
    }
}
