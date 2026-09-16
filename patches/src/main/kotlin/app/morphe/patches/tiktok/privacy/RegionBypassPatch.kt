package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants

val regionBypassPatch = bytecodePatch(
    name = "SIM Region Selector",
    description = "Spoofs the detected SIM and network country ISO code to bypass regional feed restrictions and catalog blocks.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    val targetRegion by stringOption(
        key = "region",
        title = "Spoofed Region ISO Code",
        description = "Two-letter ISO country code for SIM and network country spoofing (e.g. 'CH', 'US', 'JP', 'GB').",
        default = "CH",
        required = false,
    )

    execute {
        val raw = targetRegion?.trim()?.lowercase() ?: "ch"
        val region = if (raw.matches(Regex("^[a-z]{2}$"))) raw else "ch"
        var patched = 0

        // 1. Hook getSimCountryIso with Cert token wrapper
        try {
            val simCertFp = Fingerprint(
                returnType = "Ljava/lang/String;",
                strings = listOf("TelephonyManager_getSimCountryIso"),
                custom = { method, _ ->
                    method.parameterTypes.size == 2 && method.parameterTypes[0] == "Landroid/telephony/TelephonyManager;"
                },
            )
            simCertFp.method.addInstructions(
                0,
                """
                const-string v0, "$region"
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[SIM Region Selector] simCert note: ${e.message}")
        }

        // 2. Hook getSimCountryIso direct
        try {
            val simDirectFp = Fingerprint(
                returnType = "Ljava/lang/String;",
                strings = listOf("TelephonyManager_getSimCountryIso"),
                custom = { method, _ ->
                    method.parameterTypes.size == 1 && method.parameterTypes[0] == "Landroid/telephony/TelephonyManager;"
                },
            )
            simDirectFp.method.addInstructions(
                0,
                """
                const-string v0, "$region"
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[SIM Region Selector] simDirect note: ${e.message}")
        }

        // 3. Hook getNetworkCountryIso with Cert token wrapper
        try {
            val netCertFp = Fingerprint(
                returnType = "Ljava/lang/String;",
                strings = listOf("TelephonyManager_getNetworkCountryIso"),
                custom = { method, _ ->
                    method.parameterTypes.size == 2 && method.parameterTypes[0] == "Landroid/telephony/TelephonyManager;"
                },
            )
            netCertFp.method.addInstructions(
                0,
                """
                const-string v0, "$region"
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[SIM Region Selector] netCert note: ${e.message}")
        }

        // 4. Hook getNetworkCountryIso direct
        try {
            val netDirectFp = Fingerprint(
                returnType = "Ljava/lang/String;",
                strings = listOf("TelephonyManager_getNetworkCountryIso"),
                custom = { method, _ ->
                    method.parameterTypes.size == 1 && method.parameterTypes[0] == "Landroid/telephony/TelephonyManager;"
                },
            )
            netDirectFp.method.addInstructions(
                0,
                """
                const-string v0, "$region"
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[SIM Region Selector] netDirect note: ${e.message}")
        }

        println("[SIM Region Selector] Applied $patched SIM/Network country spoofing hooks (region: $region).")
    }
}
