package app.morphe.patches.shared

import app.morphe.patcher.patch.ResourcePatchContext
import java.io.File

/**
 * Finds `.xml` files containing any of [keys].
 *
 * Preference XMLs live under `res/xml`, so that directory is decoded and scanned first.
 * Only if nothing is found there does it decode and scan the whole `res/` tree, preserving
 * the previous behavior while avoiding reading every XML in the APK in the common case.
 */
internal fun ResourcePatchContext.findXmlContaining(keys: Collection<String>): List<File> {
    decode("res/xml*")
    val res = get("res")
    val xmlDir = File(res, "xml")
    if (xmlDir.isDirectory) {
        val hits = scan(xmlDir, keys)
        if (hits.isNotEmpty()) return hits
    }
    decode("res")
    return scan(res, keys)
}

private fun scan(root: File, keys: Collection<String>): List<File> = root.walkTopDown()
    .filter { it.isFile && it.extension == "xml" }
    .filter { file ->
        val content = file.readText()
        keys.any { key -> content.contains(key) }
    }
    .toList()
