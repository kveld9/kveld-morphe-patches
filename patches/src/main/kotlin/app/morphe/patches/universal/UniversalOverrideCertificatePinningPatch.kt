package app.morphe.patches.universal

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.getAttributeValue
import java.io.File
import org.w3c.dom.Document
import org.w3c.dom.Element

private val XML_RESOURCE_REF_REGEX = """^@?[+]?(?:[a-zA-Z0-9_.]+:)?xml/([a-zA-Z0-9_]+)$""".toRegex()

val universalOverrideCertificatePinningPatch = resourcePatch(
    name = "Override certificate pinning",
    description = "Lets a user-installed certificate authority intercept the app's HTTPS traffic for debugging.",
    default = false,
) {
    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Override certificate pinning] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val nscAttribute = readNetworkSecurityConfigAttribute(manifestFile)
        if (nscAttribute.isNullOrEmpty() || nscAttribute == "@null") {
            println("[Override certificate pinning] Skipped: app declares no network security config; code-level pinning is not covered by this patch.")
            return@execute
        }

        val match = XML_RESOURCE_REF_REGEX.matchEntire(nscAttribute)
        if (match == null) {
            println("[Override certificate pinning] Skipped: unable to parse networkSecurityConfig resource reference '$nscAttribute'.")
            return@execute
        }
        val resourceName = match.groupValues[1]

        decode("res/xml*")
        val targetFiles = locateNetworkSecurityConfigFiles(resourceName)
        if (targetFiles.isEmpty()) {
            println("[Override certificate pinning] Skipped: network security config '$resourceName.xml' not found in res/xml*.")
            return@execute
        }

        var totalModifiedSections = 0
        for (file in targetFiles) {
            document(file.absolutePath).use { doc ->
                totalModifiedSections += patchNetworkSecurityConfig(doc)
            }
        }

        if (totalModifiedSections == 0) {
            println("[Override certificate pinning] User CA trust with overridePins already enabled (0 network-security-config section(s) modified).")
        } else {
            println("[Override certificate pinning] Enabled user CA trust in $totalModifiedSections network-security-config section(s).")
        }
    }
}

private fun ResourcePatchContext.readNetworkSecurityConfigAttribute(manifestFile: File): String? {
    var result: String? = null
    document(manifestFile.absolutePath).use { doc ->
        val appNodes = doc.getElementsByTagName("application")
        if (appNodes.length > 0) {
            val appElem = appNodes.item(0) as? Element
            if (appElem != null) {
                val attr = getAttributeValue(appElem, "networkSecurityConfig")
                if (attr.isNotEmpty()) {
                    result = attr
                }
            }
        }
    }
    return result
}

private fun ResourcePatchContext.locateNetworkSecurityConfigFiles(resourceName: String): List<File> {
    val targetFileName = "$resourceName.xml"
    val defaultFile = get("res/xml/$targetFileName")
    if (defaultFile.exists()) {
        return listOf(defaultFile)
    }

    val resDir = get("res")
    if (!resDir.exists() || !resDir.isDirectory) {
        return emptyList()
    }

    return resDir.walkTopDown()
        .filter { file ->
            file.isFile && file.name == targetFileName && file.parentFile?.name?.startsWith("xml") == true
        }
        .toList()
}

private fun patchNetworkSecurityConfig(doc: Document): Int {
    val root = doc.documentElement ?: return 0
    if (root.nodeName != "network-security-config") {
        return 0
    }

    val configElements = collectConfigElements(doc, root)
    var count = 0
    for (config in configElements) {
        if (configureTrustAnchors(doc, config)) {
            count++
        }
    }
    return count
}

private fun collectConfigElements(doc: Document, root: Element): List<Element> = buildList {
    add(getOrCreateBaseConfig(doc, root))
    val domainConfigs = doc.getElementsByTagName("domain-config")
    for (i in 0 until domainConfigs.length) {
        val elem = domainConfigs.item(i) as? Element ?: continue
        add(elem)
    }
}

private fun getOrCreateBaseConfig(doc: Document, root: Element): Element {
    val existing = root.childrenNamed("base-config").firstOrNull()
    if (existing != null) return existing
    val created = doc.createElement("base-config")
    root.appendChild(created)
    return created
}

private fun configureTrustAnchors(doc: Document, configElement: Element): Boolean {
    val trustAnchors = getOrCreateTrustAnchors(doc, configElement)
    ensureSystemCertificates(doc, trustAnchors)
    return ensureUserCertificates(doc, trustAnchors)
}

private fun getOrCreateTrustAnchors(doc: Document, configElement: Element): Element {
    val existing = configElement.childrenNamed("trust-anchors").firstOrNull()
    if (existing != null) return existing
    val created = doc.createElement("trust-anchors")
    configElement.appendChild(created)
    return created
}

private fun ensureSystemCertificates(doc: Document, trustAnchors: Element) {
    val hasSystem = trustAnchors.childrenNamed("certificates")
        .any { it.getAttribute("src") == "system" }
    if (!hasSystem) {
        val sysCert = doc.createElement("certificates")
        sysCert.setAttribute("src", "system")
        trustAnchors.appendChild(sysCert)
    }
}

private fun ensureUserCertificates(doc: Document, trustAnchors: Element): Boolean {
    val userCert = trustAnchors.childrenNamed("certificates")
        .firstOrNull { it.getAttribute("src") == "user" }
    if (userCert == null) {
        val created = doc.createElement("certificates")
        created.setAttribute("src", "user")
        created.setAttribute("overridePins", "true")
        trustAnchors.appendChild(created)
        return true
    }
    if (userCert.getAttribute("overridePins") != "true") {
        userCert.setAttribute("overridePins", "true")
        return true
    }
    return false
}
