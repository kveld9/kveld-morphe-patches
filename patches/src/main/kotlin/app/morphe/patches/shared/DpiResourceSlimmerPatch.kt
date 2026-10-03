package app.morphe.patches.shared

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"

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

private fun getAttributeValue(element: Element, attributeName: String): String {
    val attrNs = element.getAttributeNS(ANDROID_XML_NAMESPACE, attributeName)
    if (attrNs.isNotBlank()) return attrNs.trim()
    val attrPrefixed = element.getAttribute("android:$attributeName")
    if (attrPrefixed.isNotBlank()) return attrPrefixed.trim()
    return element.getAttribute(attributeName).trim()
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

    execute {
        val resDir = get("res")
        if (!resDir.exists() || !resDir.isDirectory) {
            println("[DPI Resource Slimmer] Skipped: res directory not found.")
            return@execute
        }

        val keepSet = parseTargetDpis(targetDpis)
        val allDirs = resDir.listFiles { f -> f.isDirectory }?.toList() ?: run {
            println("[DPI Resource Slimmer] Skipped: res directory has no subdirectories.")
            return@execute
        }

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

        val manifestFile = get("AndroidManifest.xml")
        val launcherIconNames = parseManifestReadOnly(manifestFile)?.let {
            collectLauncherIconNames(it)
        } ?: emptySet()

        val candidateDirs = allDirs.filter { isDensityDirectoryCandidate(it) && extractDensityQualifier(it.name) !in keepSet }

        println("[DPI Resource Slimmer] Keeping densities ${keepSet.sorted().joinToString(", ")} across ${keptDirs.size} directories: ${keptDirs.map { it.name }.sorted().joinToString(", ")}")
        if (candidateDirs.isNotEmpty()) {
            println("[DPI Resource Slimmer] Trimming ${candidateDirs.size} unselected directories: ${candidateDirs.map { it.name }.sorted().joinToString(", ")}")
        }

        val candidateBuckets = candidateDirs.groupBy { getDirectoryBucket(it.name) ?: it.name }
        val stats = SlimmerStats()

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

        val removedDirs = pruneEmptyDirectories(resDir)
        val savedFormatted = LocaleUtils.formatBytes(stats.savedBytes)

        if (stats.preservedInSitu > 0) {
            println("[DPI Resource Slimmer] Preserved ${stats.preservedInSitu} single-density orphan asset(s) in-situ.")
        }

        println("[DPI Resource Slimmer] Stripped ${stats.removedFiles} duplicate files across $removedDirs density dirs (${stats.preservedInSitu} orphan resources preserved in-situ, kept: ${keepSet.sorted().joinToString(", ")}) -> Saved $savedFormatted")
    }
}

