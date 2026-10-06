package app.morphe.patches.shared

import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Shared DOM ([org.w3c.dom.Element]) helpers for editing AndroidManifest.xml from
 * resource patches without boilerplate NodeList iteration.
 */

internal const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
internal val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

internal fun getAttributeValue(element: Element, attributeName: String): String {
    val attr = element.getAttribute("android:$attributeName")
    if (attr.isNotBlank()) return attr.trim()
    val attrNs = element.getAttributeNS(ANDROID_XML_NAMESPACE, attributeName)
    if (attrNs.isNotBlank()) return attrNs.trim()
    return element.getAttribute(attributeName).trim()
}

internal fun Element.childrenNamed(name: String): List<Element> {
    val nodes = childNodes
    return buildList {
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element && node.nodeName == name) add(node)
        }
    }
}

internal fun Element.childrenNamed(vararg names: String): List<Element> {
    val acceptedNames = names.toSet()
    val nodes = childNodes
    return buildList {
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element && node.nodeName in acceptedNames) add(node)
        }
    }
}

internal fun Element.removeChildren(nodes: List<Node>) {
    nodes.forEach(::removeChild)
}

/** Replaces an android: attribute whether or not the decoded manifest bound it to the android namespace. */
private fun Element.setAndroidAttribute(localName: String, value: String) {
    removeAttribute("android:$localName")
    setAttributeNS(ANDROID_XML_NAMESPACE, "android:$localName", value)
}

internal fun Element.getOrCreateApplicationMetaData(name: String): Element {
    childrenNamed("meta-data")
        .firstOrNull { it.getAttribute("android:name") == name || it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") == name }
        ?.let { return it }

    val metaData = ownerDocument.createElement("meta-data")
    metaData.setAttributeNS(ANDROID_XML_NAMESPACE, "android:name", name)
    appendChild(metaData)
    return metaData
}

internal fun Element.setApplicationMetaData(name: String, value: String) {
    getOrCreateApplicationMetaData(name).setAndroidAttribute("value", value)
}

internal fun Element.disableComponentsWhere(
    vararg tagNames: String = arrayOf("activity", "provider", "service", "receiver"),
    predicate: (String) -> Boolean,
): Int {
    var disabled = 0

    childrenNamed(*tagNames)
        .filter { component ->
            val name = component.getAttribute("android:name").ifBlank { component.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
            predicate(name)
        }
        .forEach { component ->
            component.setAndroidAttribute("enabled", "false")
            component.setAndroidAttribute("exported", "false")
            disabled++
        }

    return disabled
}

internal fun Element.disableComponentsByName(vararg names: String): Int {
    val namesSet = names.toSet()
    return disableComponentsWhere { it in namesSet }
}

internal fun Element.stripPermissionsWhere(predicate: (String) -> Boolean): List<String> {
    val matches = childrenNamed("uses-permission", "uses-permission-sdk-23")
        .filter {
            val name = it.getAttribute("android:name").ifBlank { it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
            predicate(name)
        }
    val names = matches.map { it.getAttribute("android:name").ifBlank { it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") } }
    removeChildren(matches)
    return names
}

internal fun Element.removeComponentDiscoveryRegistrarsWhere(predicate: (String) -> Boolean): Int {
    var removed = 0

    childrenNamed("service")
        .filter {
            val name = it.getAttribute("android:name").ifBlank { it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
            name == "com.google.firebase.components.ComponentDiscoveryService"
        }
        .forEach { discoveryService ->
            val matches = discoveryService.childrenNamed("meta-data")
                .filter { metaData ->
                    val name = metaData.getAttribute("android:name").ifBlank { metaData.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
                    name.startsWith("com.google.firebase.components:") && predicate(name)
                }
            discoveryService.removeChildren(matches)
            removed += matches.size
        }

    return removed
}

