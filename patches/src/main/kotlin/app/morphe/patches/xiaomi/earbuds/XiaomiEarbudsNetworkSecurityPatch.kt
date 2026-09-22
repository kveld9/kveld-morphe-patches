package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val xiaomiEarbudsNetworkSecurityConfigResourcePatch = resourcePatch(
    name = "Xiaomi Earbuds Network Security Config",
    description = "Configures network_security_config.xml to disable cleartext traffic and trust user-installed CA certificates.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val configFile = get("res/xml/network_security_config.xml")
        if (!configFile.exists()) {
            println("[Xiaomi Earbuds Network Security Config] res/xml/network_security_config.xml not found - skipping config patch.")
            return@execute
        }

        document(configFile.absolutePath).use { doc ->
            val baseConfigs = doc.getElementsByTagName("base-config")
            val baseConfig = if (baseConfigs.length > 0) {
                baseConfigs.item(0) as Element
            } else {
                val elem = doc.createElement("base-config")
                doc.documentElement.appendChild(elem)
                elem
            }
            baseConfig.setAttribute("cleartextTrafficPermitted", "false")

            val trustAnchorsList = baseConfig.getElementsByTagName("trust-anchors")
            val trustAnchors = if (trustAnchorsList.length > 0) {
                trustAnchorsList.item(0) as Element
            } else {
                val elem = doc.createElement("trust-anchors")
                baseConfig.appendChild(elem)
                elem
            }

            val certs = trustAnchors.getElementsByTagName("certificates")
            var hasUser = false
            for (i in 0 until certs.length) {
                val cert = certs.item(i) as? Element ?: continue
                if (cert.getAttribute("src") == "user") {
                    hasUser = true
                    break
                }
            }
            if (!hasUser) {
                val userCertElem = doc.createElement("certificates")
                userCertElem.setAttribute("src", "user")
                trustAnchors.appendChild(userCertElem)
            }
        }

        println("[Xiaomi Earbuds Network Security Config] Configured network_security_config.xml: cleartextTrafficPermitted=false, user CA certificates trusted.")
    }
}

@Suppress("unused")
val xiaomiEarbudsNetworkSecurityPatch = bytecodePatch(
    name = "Xiaomi Earbuds Network Security & TLS Inspection",
    description = "Disables cleartext traffic, trusts user-installed certificates, and bypasses OkHttp certificate pinning.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)
    dependsOn(xiaomiEarbudsNetworkSecurityConfigResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()
        val certificatePinnerClass = "Lokhttp3/CertificatePinner;"

        Fingerprint(
            definingClass = certificatePinnerClass,
            name = "check",
            parameters = listOf("Ljava/lang/String;", "Ljava/util/List;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CertificatePinner.check(String, List)")
        }

        Fingerprint(
            definingClass = certificatePinnerClass,
            name = "check",
            parameters = listOf("Ljava/lang/String;", "[Ljava/security/cert/Certificate;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CertificatePinner.check(String, Certificate[])")
        }

        Fingerprint(
            definingClass = certificatePinnerClass,
            name = "check\$okhttp",
            parameters = listOf("Ljava/lang/String;", "Lkotlin/jvm/functions/Function0;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CertificatePinner.check\$okhttp(String, Function0)")
        }

        println("[Xiaomi Earbuds Network Security] Neutralized OkHttp CertificatePinner across ${hookedMethods.size} targets.")
    }
}
