package app.morphe.patches.shared

import app.morphe.patcher.patch.rawResourcePatch
import java.io.File

private val PROTECTED_EXTENSIONS = setOf(
    "dex", "arsc", "xml", "so", "rsa", "sf", "dsa", "ec"
)

private val PROTECTED_ROOT_DIRS = setOf(
    "assets", "res", "lib", "smali"
)

private val EXACT_JUNK_NAMES = setOf(
    "debugprobeskt.bin",
    "stamp-cert-sha256",
    "version-control-info.textproto",
    "kotlin-tooling-metadata.json",
    "androidsupportmultidexversion.txt",
    "changes",
    "dependencies"
)

private fun isProtectedFile(relativePath: String, file: File): Boolean {
    val ext = file.extension.lowercase()
    if (ext in PROTECTED_EXTENSIONS) return true
    if (relativePath == "AndroidManifest.xml" || relativePath == "resources.arsc") return true
    if (relativePath.startsWith("META-INF/services/")) return true
    if (relativePath.equals("META-INF/MANIFEST.MF", ignoreCase = true)) return true

    val topDir = relativePath.substringBefore('/').lowercase()
    return topDir in PROTECTED_ROOT_DIRS
}

private fun isJunkFile(relativePath: String, file: File): Boolean {
    val lowerName = file.name.lowercase()
    if (lowerName in EXACT_JUNK_NAMES) return true
    if (lowerName.endsWith(".version") || lowerName.endsWith("_version")) return true
    if (lowerName.endsWith(".properties") || lowerName.endsWith(".proto")) return true

    if (relativePath.startsWith("META-INF/")) {
        if (lowerName.startsWith("license") || lowerName.startsWith("notice") || lowerName.startsWith("readme")) {
            return true
        }
    }

    return false
}

private fun pruneEmptyDirectories(root: File) {
    root.walkBottomUp()
        .filter { it.isDirectory && it != root && it.listFiles()?.isEmpty() == true }
        .forEach { it.delete() }
}

@Suppress("unused")
val apkJunkCleanerPatch = rawResourcePatch(
    name = "APK Junk Cleaner",
    description = "Strips non-functional build metadata, compiler properties, Kotlin coroutines debug tables, and duplicate license texts from META-INF and APK root.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI
    execute {
        val apkRoot = try {
            get("AndroidManifest.xml").parentFile ?: get(".")
        } catch (_: Throwable) {
            get(".")
        }

        if (!apkRoot.exists() || !apkRoot.isDirectory) {
            println("[APK Junk Cleaner] APK root directory not found - skipping safely.")
            return@execute
        }

        var junkFilesCount = 0
        var savedBytes = 0L

        apkRoot.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(apkRoot).path.replace('\\', '/')

                if (isProtectedFile(relativePath, file)) return@forEach

                if (isJunkFile(relativePath, file)) {
                    val fileSize = file.length()
                    if (file.delete()) {
                        junkFilesCount++
                        savedBytes += fileSize
                    }
                }
            }

        // Clean up orphan Kotlin build metadata directory if present at root
        try {
            val kotlinDir = get("kotlin")
            if (kotlinDir.exists() && kotlinDir.isDirectory) {
                val kFiles = kotlinDir.walkTopDown().filter { it.isFile }.toList()
                val kSize = kFiles.sumOf { it.length() }
                if (kotlinDir.deleteRecursively()) {
                    junkFilesCount += kFiles.size
                    savedBytes += kSize
                }
            }
        } catch (_: Throwable) {}

        // Prune empty subdirectories left in META-INF
        try {
            val metaInfDir = get("META-INF")
            if (metaInfDir.exists() && metaInfDir.isDirectory) {
                pruneEmptyDirectories(metaInfDir)
            }
        } catch (_: Throwable) {}

        if (junkFilesCount == 0) {
            println("[APK Junk Cleaner] APK root and META-INF are already clean (0 junk files found).")
            return@execute
        }

        val savedFormatted = LocaleUtils.formatBytes(savedBytes)
        println("[APK Junk Cleaner] Purged $junkFilesCount non-functional build/metadata files -> Saved $savedFormatted")
    }
}
