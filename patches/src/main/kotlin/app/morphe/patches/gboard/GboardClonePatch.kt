package app.morphe.patches.gboard

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

val gboardClonePatch = resourcePatch(
    name = "Clone Gboard",
    description = "Changes the package name by appending a dot and custom suffix (defaults to 'kveld') to allow installing Gboard alongside the original application.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardSignatureBypassPatch)

    val packageSuffix by stringOption(
        key = "packageSuffix",
        title = "Package name suffix",
        description = "Suffix appended after the original package name and a dot (e.g. 'kveld' -> 'com.google.android.inputmethod.latin.kveld').",
        default = "kveld",
        required = false,
    )

    execute {
        val originalPackage = Constants.GBOARD_PACKAGE_NAME
        val cleanSuffix = packageSuffix?.trim()?.trimStart('.')?.trimEnd('.')?.takeIf { it.isNotEmpty() } ?: "kveld"
        val newPackage = "$originalPackage.$cleanSuffix"

        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) return@execute

        var modifiedProviders = 0
        var modifiedPermissions = 0
        var modifiedComponents = 0
        var modifiedXmlFiles = 0

        document(manifestFile.absolutePath).use { doc ->
            // 1. Rewrite root package attribute
            val manifestElement = doc.documentElement
            manifestElement.setAttribute("package", newPackage)

            // 2. Rewrite provider authorities to prevent install-time authority collisions
            val providers = doc.getElementsByTagName("provider")
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as? Element ?: continue
                val authorities = provider.getAttribute("android:authorities")
                if (authorities.isNotEmpty()) {
                    val updatedAuthorities = authorities.split(";").joinToString(";") { auth ->
                        if (auth.startsWith(originalPackage)) {
                            modifiedProviders++
                            auth.replaceFirst(originalPackage, newPackage)
                        } else {
                            auth
                        }
                    }
                    provider.setAttribute("android:authorities", updatedAuthorities)
                }
            }

            // 3. Rewrite custom permissions to prevent duplicate permission declaration errors
            val permissionTags = listOf("permission", "permission-group", "permission-tree", "uses-permission")
            for (tagName in permissionTags) {
                val elements = doc.getElementsByTagName(tagName)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name.startsWith(originalPackage)) {
                        elem.setAttribute("android:name", name.replaceFirst(originalPackage, newPackage))
                        modifiedPermissions++
                    }
                }
            }

            // 4. Qualify relative component names, activity-alias targetActivity, and update process tags
            val componentTags = listOf("activity", "activity-alias", "service", "receiver", "provider")
            for (tagName in componentTags) {
                val elements = doc.getElementsByTagName(tagName)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name.startsWith(".")) {
                        elem.setAttribute("android:name", originalPackage + name)
                        modifiedComponents++
                    }
                    if (elem.hasAttribute("android:targetActivity")) {
                        val target = elem.getAttribute("android:targetActivity")
                        if (target.startsWith(".")) {
                            elem.setAttribute("android:targetActivity", originalPackage + target)
                            modifiedComponents++
                        }
                    }
                    val process = elem.getAttribute("android:process")
                    if (process.startsWith(originalPackage)) {
                        elem.setAttribute("android:process", process.replaceFirst(originalPackage, newPackage))
                        modifiedComponents++
                    }
                }
            }
        }

        // 5. Update authority references strictly in res/xml configuration files (e.g. sync adapters, searchables)
        val resXmlDir = get("res/xml")
        if (resXmlDir.exists() && resXmlDir.isDirectory) {
            val authorityAttributes = setOf("android:authorities", "authorities", "android:contentAuthority", "contentAuthority")
            resXmlDir.walkTopDown().filter { it.isFile && it.extension == "xml" }.forEach { file ->
                val content = file.readText()
                if (content.contains(originalPackage)) {
                    var fileTouched = false
                    document(file.absolutePath).use { doc ->
                        val elements = doc.getElementsByTagName("*")
                        for (i in 0 until elements.length) {
                            val node = elements.item(i) as? Element ?: continue
                            for (attrName in authorityAttributes) {
                                if (node.hasAttribute(attrName)) {
                                    val value = node.getAttribute(attrName)
                                    if (value.contains(originalPackage)) {
                                        node.setAttribute(attrName, value.replace(originalPackage, newPackage))
                                        fileTouched = true
                                    }
                                }
                            }
                        }
                    }
                    if (fileTouched) modifiedXmlFiles++
                }
            }
        }

        println("[Clone Gboard] Renamed package to '$newPackage' ($modifiedProviders providers, $modifiedPermissions permissions, $modifiedComponents components, $modifiedXmlFiles XML files)")
    }
}
