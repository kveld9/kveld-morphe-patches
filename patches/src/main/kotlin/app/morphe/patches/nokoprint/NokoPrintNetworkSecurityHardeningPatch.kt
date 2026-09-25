package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val nokoPrintNetworkSecurityHardeningPatch = resourcePatch(
    name = "Network Security Hardening",
    description = "Enforces HTTPS encryption for nokoprint.com driver downloads while preserving LAN cleartext printer traffic.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val configFile = get("res/xml/network_security_config.xml")
        if (!configFile.exists()) {
            println("[Network Security Hardening] network_security_config.xml not found - skipping.")
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
        println("[Network Security Hardening] Configured network security config: enforced HTTPS for nokoprint.com, preserved LAN printer cleartext traffic.")
    }
}
