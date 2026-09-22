package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

@Suppress("unused")
val nokoPrintNetworkSecurityHardeningPatch = resourcePatch(
    name = "NokoPrint Network Security Hardening",
    description = "Enforces HTTPS encryption for driver downloads and backend communications with nokoprint.com while preserving local cleartext traffic for LAN network printers.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val configFile = get("res/xml/network_security_config.xml")
        if (!configFile.exists()) {
            println("[NokoPrint Network Security Hardening] network_security_config.xml not found - skipping.")
            return@execute
        }

        val hardenedConfig = """<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">nokoprint.com</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>
</network-security-config>
""".trimIndent()

        configFile.writeText(hardenedConfig)
        println("[NokoPrint Network Security Hardening] Configured network security config: enforced HTTPS for nokoprint.com, preserved LAN printer cleartext traffic.")
    }
}
