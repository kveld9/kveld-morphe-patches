package app.morphe.patches.gboard

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element
import java.io.File

internal fun ByteArray.containsSequence(sequence: ByteArray): Boolean {
    if (sequence.isEmpty() || size < sequence.size) return false
    val max = size - sequence.size
    for (i in 0..max) {
        var matches = true
        for (j in sequence.indices) {
            if (this[i + j] != sequence[j]) {
                matches = false
                break
            }
        }
        if (matches) return true
    }
    return false
}

val gboardAaptWorkaroundPatch = resourcePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val resDir = get("res")
        if (!resDir.exists() || !resDir.isDirectory) {
            return@execute
        }

        val targetFiles = mutableListOf<File>()
        val defaultMethodFile = get("res/xml/method.xml")
        if (defaultMethodFile.exists()) {
            targetFiles.add(defaultMethodFile)
        }

        val attrName = "android:supportsConnectionlessStylusHandwriting"
        val attrBytes = attrName.toByteArray()

        resDir.walkTopDown().filter { it.isFile && it.extension == "xml" && it != defaultMethodFile }.forEach { file ->
            if (file.readBytes().containsSequence(attrBytes)) {
                targetFiles.add(file)
            }
        }

        var stripped = 0
        for (file in targetFiles) {
            document(file.absolutePath).use { doc ->
                val root = doc.documentElement ?: return@use
                if (root.hasAttribute(attrName)) {
                    root.removeAttribute(attrName)
                    stripped++
                }
                val nodes = root.getElementsByTagName("input-method")
                for (i in 0 until nodes.length) {
                    val elem = nodes.item(i) as? Element ?: continue
                    if (elem.hasAttribute(attrName)) {
                        elem.removeAttribute(attrName)
                        stripped++
                    }
                }
            }
        }

        if (stripped > 0) {
            println("[AAPT Workaround] Stripped $stripped unsupported stylus handwriting attribute(s) from ${targetFiles.size} input method resource(s).")
        }
    }
}
