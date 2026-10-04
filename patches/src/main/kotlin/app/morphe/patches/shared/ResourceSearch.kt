package app.morphe.patches.shared

import java.io.File

/**
 * Finds `.xml` files containing any of [keys].
 *
 * Preference XMLs live under `res/xml`, so that directory is scanned first. Only if nothing is
 * found there does it fall back to the whole `res/` tree, preserving the previous behavior
 * while avoiding reading every XML in the APK in the common case.
 */
internal fun File.findXmlContaining(keys: Collection<String>): List<File> {
    fun scan(root: File): List<File> = root.walkTopDown()
        .filter { it.isFile && it.extension == "xml" }
        .filter { file ->
            val content = file.readText()
            keys.any { key -> content.contains(key) }
        }
        .toList()

    val xmlDir = File(this, "xml")
    if (xmlDir.isDirectory) {
        val hits = scan(xmlDir)
        if (hits.isNotEmpty()) return hits
    }
    return scan(this)
}
