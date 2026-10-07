package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.LocaleUtils
import app.morphe.patches.shared.getAttributeValue
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private val STRIPPABLE_DENSITY_QUALIFIERS = setOf(
    "ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"
)

private val PROTECTED_DENSITY_QUALIFIERS = setOf(
    "nodpi", "anydpi"
)

private val DENSITY_RANK = mapOf(
    "ldpi" to 120,
    "mdpi" to 160,
    "tvdpi" to 213,
    "hdpi" to 240,
    "xhdpi" to 320,
    "xxhdpi" to 480,
    "xxxhdpi" to 640
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
    var preservedInSitu: Int = 0,
    var savedBytes: Long = 0L,
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

private fun getPrefixWithoutDensity(dirName: String, density: String): String {
    return dirName.replace("-$density", "")
}

private fun getDirectoryBucket(dirName: String): String? {
    val density = extractDensityQualifier(dirName) ?: return dirName
    if (density in PROTECTED_DENSITY_QUALIFIERS || density in STRIPPABLE_DENSITY_QUALIFIERS) {
        return getPrefixWithoutDensity(dirName, density)
    }
    return null
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

private fun extractResourceEntryName(fileName: String): String {
    if (fileName.endsWith(".9.png", ignoreCase = true)) {
        return fileName.substring(0, fileName.length - 6)
    }
    return fileName.substringBeforeLast('.')
}

private fun extractEntryNameFromResourceRef(ref: String): String? {
    val clean = ref.trim()
    if (!clean.startsWith("@")) return null
    val slashIndex = clean.lastIndexOf('/')
    if (slashIndex == -1 || slashIndex >= clean.length - 1) return null
    return extractResourceEntryName(clean.substring(slashIndex + 1))
}

private fun extractIconNamesFromElement(
    el: Element,
    iconNames: MutableSet<String>,
) {
    val iconAttrs = listOf("icon", "roundIcon")
    for (attr in iconAttrs) {
        val value = getAttributeValue(el, attr)
        if (value.isNotEmpty()) {
            extractEntryNameFromResourceRef(value)?.let { iconNames.add(it) }
        }
    }
}

private fun collectLauncherIconNames(doc: Document): Set<String> {
    val iconNames = mutableSetOf<String>()
    val tags = listOf("application", "activity", "activity-alias")

    for (tag in tags) {
        val elements = doc.getElementsByTagName(tag)
        for (i in 0 until elements.length) {
            val el = elements.item(i) as? Element ?: continue
            extractIconNamesFromElement(el, iconNames)
        }
    }
    return iconNames
}

private fun parseManifestReadOnly(manifestFile: File): Document? {
    if (!manifestFile.exists() || !manifestFile.isFile) return null
    return try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        manifestFile.inputStream().use { builder.parse(it) }
    } catch (_: Exception) {
        null
    }
}

private fun collectEntryNamesFromDir(dir: File, names: MutableSet<String>) {
    dir.listFiles { f -> f.isFile }?.forEach {
        names.add(extractResourceEntryName(it.name))
    }
}

private fun collectProtectedEntryNamesForBucket(
    resDir: File,
    bucket: String,
    keptDirs: List<File>,
): MutableSet<String> {
    val protectedNames = mutableSetOf<String>()

    keptDirs.forEach { dir ->
        if (getDirectoryBucket(dir.name) == bucket) {
            collectEntryNamesFromDir(dir, protectedNames)
        }
    }

    resDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
        if (isGraphicResourceDirectory(dir.name)) {
            val density = extractDensityQualifier(dir.name)
            val isDensityIndependent = density == null || density in PROTECTED_DENSITY_QUALIFIERS
            if (isDensityIndependent && getDirectoryBucket(dir.name) == bucket) {
                collectEntryNamesFromDir(dir, protectedNames)
            }
        }
    }

    return protectedNames
}

private fun trimDirectoryFiles(
    dir: File,
    protectedEntryNames: MutableSet<String>,
    launcherIconNames: Set<String>,
    stats: SlimmerStats,
) {
    val files = dir.listFiles { f -> f.isFile } ?: return

    for (file in files) {
        val entryName = extractResourceEntryName(file.name)
        if (entryName in launcherIconNames) {
            continue
        }

        if (entryName in protectedEntryNames) {
            val fileSize = file.length()
            if (file.delete()) {
                stats.removedFiles++
                stats.savedBytes += fileSize
            }
        } else {
            stats.preservedInSitu++
            protectedEntryNames.add(entryName)
        }
    }
}

private val UI_MODE_WATCH = setOf("watch")
private val UI_MODE_TV = setOf("television")
private val UI_MODE_OTHER = setOf("car", "desk", "appliance", "vrheadset")

private fun stripUiModeDirectories(resDir: File, stripModes: Set<String>, stats: SlimmerStats): Int {
    if (stripModes.isEmpty()) return 0
    var removedDirs = 0
    resDir.listFiles { f -> f.isDirectory }?.forEach { dir ->
        // Never touch values directories to avoid deleting string/style/dimen resource IDs
        if (dir.name.startsWith("values")) return@forEach

        val qualifiers = dir.name.split("-").drop(1).map { it.lowercase() }
        if (qualifiers.none { it in stripModes }) return@forEach

        val dirFiles = dir.walkTopDown().filter { it.isFile }.toList()
        val dirSize = dirFiles.sumOf { it.length() }
        if (dir.deleteRecursively()) {
            removedDirs++
            stats.removedFiles += dirFiles.size
            stats.savedBytes += dirSize
        }
    }
    return removedDirs
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

@Suppress("unused")
val dpiResourceSlimmerPatch = resourcePatch(
    name = "DPI Resource Slimmer",
    description = "Strips unselected screen density resource directories from res/ (e.g. drawable-mdpi, drawable-hdpi, mipmap-xhdpi). Density-independent resources (nodpi, anydpi) and orphan resources are safely preserved in-situ.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI
    val targetDpis by stringOption(
        key = "dpis",
        title = "DPI densities to keep",
        description = "Comma-separated screen densities to preserve (e.g. 'xxhdpi', 'xhdpi, xxhdpi', 'xxxhdpi'). Density-independent resources (nodpi, anydpi) and unquantified base directories are always preserved.",
        default = "xxhdpi",
        required = false,
    )

    val stripSmartwatch by booleanOption(
        key = "stripSmartwatch",
        title = "Remove smartwatch (Wear OS) resources",
        description = "Removes graphic and non-values layout resources qualified for Wear OS smartwatches (e.g. watch qualifiers).",
        default = true,
        required = false,
    )

    val stripTelevision by booleanOption(
        key = "stripTelevision",
        title = "Remove Android TV resources",
        description = "Removes graphic and non-values layout resources qualified for Android TV / Leanback (e.g. television qualifiers).",
        default = true,
        required = false,
    )

    val stripOtherFormFactors by booleanOption(
        key = "stripOtherFormFactors",
        title = "Remove automotive, dock, and VR resources",
        description = "Removes graphic and non-values layout resources qualified for car head units, desk docks, appliances, or VR headsets.",
        default = true,
        required = false,
    )

    execute {
        val mainRes = get("res")
        val resDirs = LocaleUtils.resolveResourceDirectories(mainRes)
        if (resDirs.isEmpty()) {
            println("[DPI Resource Slimmer] Skipped: res directory not found.")
            return@execute
        }

        val keepSet = parseTargetDpis(targetDpis)
        val uiModeStripSet = buildSet {
            if (stripSmartwatch == true) addAll(UI_MODE_WATCH)
            if (stripTelevision == true) addAll(UI_MODE_TV)
            if (stripOtherFormFactors == true) addAll(UI_MODE_OTHER)
        }

        val manifestFile = get("AndroidManifest.xml")
        val launcherIconNames = parseManifestReadOnly(manifestFile)?.let {
            collectLauncherIconNames(it)
        } ?: emptySet()

        val stats = SlimmerStats()
        var totalRemovedDirs = 0

        println("[DPI Resource Slimmer] Target densities: ${keepSet.sorted().joinToString(", ")}")
        if (uiModeStripSet.isNotEmpty()) {
            println("[DPI Resource Slimmer] Stripping non-phone UI mode qualifiers: ${uiModeStripSet.sorted().joinToString(", ")}")
        }

        for (resDir in resDirs) {
            if (uiModeStripSet.isNotEmpty()) {
                totalRemovedDirs += stripUiModeDirectories(resDir, uiModeStripSet, stats)
            }

            val allDirs = resDir.listFiles { f -> f.isDirectory }?.toList() ?: continue
            val keptDirs = allDirs.filter { isGraphicResourceDirectory(it.name) && extractDensityQualifier(it.name) in keepSet }
            if (keptDirs.isEmpty()) {
                val available = allDirs
                    .filter { isGraphicResourceDirectory(it.name) }
                    .mapNotNull { extractDensityQualifier(it.name) }
                    .distinct()
                    .sorted()
                val pkgName = resDir.parentFile?.name ?: "package"
                println("[DPI Resource Slimmer] $pkgName: No matching density directories found for target $keepSet (available: $available) - skipping density trimming safely.")
                continue
            }

            val candidateDirs = allDirs.filter { isDensityDirectoryCandidate(it) && extractDensityQualifier(it.name) !in keepSet }
            val candidateBuckets = candidateDirs.groupBy { getDirectoryBucket(it.name) ?: it.name }

            for ((bucket, dirsInBucket) in candidateBuckets) {
                val protectedEntryNames = collectProtectedEntryNamesForBucket(resDir, bucket, keptDirs)
                val sortedDirs = dirsInBucket.sortedByDescending { dir ->
                    val density = extractDensityQualifier(dir.name)
                    DENSITY_RANK[density] ?: 0
                }
                for (dir in sortedDirs) {
                    trimDirectoryFiles(dir, protectedEntryNames, launcherIconNames, stats)
                }
            }

            totalRemovedDirs += pruneEmptyDirectories(resDir)
        }

        val savedFormatted = LocaleUtils.formatBytes(stats.savedBytes)
        val packageSuffix = if (resDirs.size > 1) " across ${resDirs.size} resource packages" else ""

        if (stats.preservedInSitu > 0) {
            println("[DPI Resource Slimmer] Preserved ${stats.preservedInSitu} single-density orphan asset(s) in-situ.")
        }

        println("[DPI Resource Slimmer] Stripped ${stats.removedFiles} files across $totalRemovedDirs density/device dirs$packageSuffix (${stats.preservedInSitu} orphan resources preserved in-situ, kept: ${keepSet.sorted().joinToString(", ")}) -> Saved $savedFormatted")
    }
}

