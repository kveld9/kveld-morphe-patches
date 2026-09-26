package app.morphe.patches.gboard

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

private fun getAttributeValue(element: Element, attributeName: String): String {
    val attr = element.getAttribute("android:$attributeName")
    if (attr.isNotBlank()) return attr.trim()
    val attrNs = element.getAttributeNS(ANDROID_XML_NAMESPACE, attributeName)
    if (attrNs.isNotBlank()) return attrNs.trim()
    return element.getAttribute(attributeName).trim()
}

private fun removeManifestPermissions(doc: Document, blocked: Set<String>): List<String> {
    val removed = mutableListOf<String>()
    for (tagName in PERMISSION_TAGS) {
        val nodes = doc.getElementsByTagName(tagName)
        val toRemove = mutableListOf<Element>()
        for (i in 0 until nodes.length) {
            val elem = nodes.item(i) as? Element ?: continue
            val name = getAttributeValue(elem, "name")
            if (name in blocked) {
                toRemove.add(elem)
                removed.add(name)
            }
        }
        for (elem in toRemove) {
            elem.parentNode?.removeChild(elem)
        }
    }
    return removed
}

@Suppress("unused")
val gboardStripPermissionsPatch = resourcePatch(
    name = "Strip Permissions",
    description = "Selectively revokes sensitive hardware, privacy, and system permissions from AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val stripContacts by booleanOption(
        key = "stripContacts",
        title = "Strip Contacts Permission",
        description = "Revokes READ_CONTACTS permission from AndroidManifest.xml (disables contact name suggestions).",
        default = false,
        required = false,
    )

    val stripAudio by booleanOption(
        key = "stripAudio",
        title = "Strip Microphone Permission",
        description = "Revokes RECORD_AUDIO permission from AndroidManifest.xml (disables voice dictation).",
        default = false,
        required = false,
    )

    val stripMedia by booleanOption(
        key = "stripMedia",
        title = "Strip Media & Storage Permissions",
        description = "Revokes photo and media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, READ_EXTERNAL_STORAGE) from AndroidManifest.xml (disables custom image background themes).",
        default = false,
        required = false,
    )

    val stripUserDictionary by booleanOption(
        key = "stripUserDictionary",
        title = "Strip System Dictionary Permissions",
        description = "Revokes system personal dictionary sync permissions (READ_USER_DICTIONARY, WRITE_USER_DICTIONARY) from AndroidManifest.xml.",
        default = false,
        required = false,
    )

    val stripCrossProfile by booleanOption(
        key = "stripCrossProfile",
        title = "Strip Cross-Profile Permission",
        description = "Revokes INTERACT_ACROSS_PROFILES permission from AndroidManifest.xml to isolate work and personal profiles.",
        default = false,
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Strip Permissions] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val blocked = mutableSetOf<String>()
        if (stripContacts == true) {
            blocked.add("android.permission.READ_CONTACTS")
        }
        if (stripAudio == true) {
            blocked.add("android.permission.RECORD_AUDIO")
        }
        if (stripMedia == true) {
            blocked.add("android.permission.READ_MEDIA_IMAGES")
            blocked.add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
            blocked.add("android.permission.READ_EXTERNAL_STORAGE")
        }
        if (stripUserDictionary == true) {
            blocked.add("android.permission.READ_USER_DICTIONARY")
            blocked.add("android.permission.WRITE_USER_DICTIONARY")
        }
        if (stripCrossProfile == true) {
            blocked.add("android.permission.INTERACT_ACROSS_PROFILES")
        }

        if (blocked.isEmpty()) {
            println("[Strip Permissions] Skipped: No permissions selected to strip.")
            return@execute
        }

        var removedList: List<String> = emptyList()
        document(manifestFile.absolutePath).use { doc ->
            removedList = removeManifestPermissions(doc, blocked)
        }

        val removedShortNames = removedList.map { it.substringAfterLast('.') }.distinct()
        println("[Strip Permissions] Revoked ${removedList.size} permission(s) from AndroidManifest.xml: ${removedShortNames.joinToString(", ")}")
    }
}
