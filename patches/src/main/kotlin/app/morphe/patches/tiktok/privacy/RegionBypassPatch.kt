package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val regionBypassPatch = bytecodePatch(
    name = "SIM Region Selector",
    description = "Spoofs the detected SIM and network country ISO code, operator numeric codes, operator names, and cell identity MCC/MNC to bypass regional feed restrictions and catalog blocks.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val targetRegion by stringOption(
        key = "region",
        title = "Spoofed Region ISO Code",
        description = "Two-letter ISO country code for SIM and network country spoofing (e.g. 'CH', 'US', 'JP', 'GB').",
        default = "CH",
        required = false,
    )

    val operatorNumeric by stringOption(
        key = "operatorNumeric",
        title = "Spoofed Operator Numeric Code (MCC+MNC)",
        description = "5-6 digit MCC+MNC code (e.g. '22801' for Switzerland/Swisscom) used to spoof TelephonyManager operator queries and CellIdentity MCC/MNC reads. Empty keeps stock operator values.",
        default = "",
        required = false,
    )

    val operatorName by stringOption(
        key = "operatorName",
        title = "Spoofed Operator Name",
        description = "Display name reported for SIM/network operator queries (e.g. 'Swisscom'). Empty keeps the stock operator name.",
        default = "",
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

        // 5. Operator and CellIdentity spoofing at every TelephonyManager/CellIdentity call site.
        // Mirrors the upstream tiktok-patches-for-morphe region spoof: instead of hooking the
        // framework methods (shared system class), every INVOKE_VIRTUAL call site inside TikTok
        // code is followed by an override of the returned stock value.
        try {
            val digits = (operatorNumeric?.trim() ?: "").filter { it.isDigit() }.take(6)
            val mcc = digits.take(3)
            val mnc = digits.drop(3)
            val numericValid = mcc.length == 3 && mnc.length in 2..3
            val cleanOperatorName = (operatorName?.trim() ?: "")
                .filter { it.isLetterOrDigit() || it == ' ' || it == '.' || it == '-' || it == '_' }
                .take(64)

            if (numericValid || cleanOperatorName.isNotEmpty()) {
                data class CallSite(
                    val definingClass: String,
                    val methodName: String,
                    val parameterTypes: List<String>,
                    val returnType: String,
                    val callIndex: Int,
                    val replacement: String,
                )

                val sites = mutableListOf<CallSite>()
                classDefForEach { classDef ->
                    for (method in classDef.methods) {
                        val instructions = method.implementation?.instructions ?: continue
                        for ((index, instruction) in instructions.withIndex()) {
                            if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                                instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE
                            ) continue
                            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                            if (ref.returnType != "Ljava/lang/String;") continue
                            val replacement = when {
                                ref.definingClass == "Landroid/telephony/TelephonyManager;" &&
                                    (ref.name == "getSimOperator" || ref.name == "getNetworkOperator") &&
                                    ref.parameterTypes.isEmpty() && numericValid -> digits
                                ref.definingClass == "Landroid/telephony/TelephonyManager;" &&
                                    (ref.name == "getSimOperatorName" || ref.name == "getNetworkOperatorName") &&
                                    ref.parameterTypes.isEmpty() && cleanOperatorName.isNotEmpty() -> cleanOperatorName
                                ref.definingClass.startsWith("Landroid/telephony/CellIdentity") &&
                                    ref.name == "getMccString" &&
                                    ref.parameterTypes.isEmpty() && numericValid -> mcc
                                ref.definingClass.startsWith("Landroid/telephony/CellIdentity") &&
                                    ref.name == "getMncString" &&
                                    ref.parameterTypes.isEmpty() && numericValid -> mnc
                                else -> continue
                            }
                            sites.add(
                                CallSite(
                                    classDef.type,
                                    method.name,
                                    method.parameterTypes.map { it.toString() },
                                    method.returnType.toString(),
                                    index,
                                    replacement,
                                )
                            )
                        }
                    }
                }

                var rewritten = 0
                sites.groupBy { Triple(it.definingClass, it.methodName, it.parameterTypes to it.returnType) }
                    .forEach { (_, group) ->
                        val head = group.first()
                        val mutableMethod = mutableClassDefBy(head.definingClass).methods.firstOrNull {
                            it.name == head.methodName &&
                                it.parameterTypes.map { p -> p.toString() } == head.parameterTypes &&
                                it.returnType.toString() == head.returnType
                        } ?: return@forEach
                        // Apply in reverse index order so earlier call sites keep valid indices.
                        for (site in group.sortedByDescending { it.callIndex }) {
                            val moveResult = mutableMethod.implementation?.instructions
                                ?.getOrNull(site.callIndex + 1) as? OneRegisterInstruction
                            if (moveResult == null) {
                                println("[SIM Region Selector] Skipped call site without move-result in ${head.first}->${head.second}.")
                                continue
                            }
                            mutableMethod.addInstructions(
                                site.callIndex + 2,
                                """
                                    const-string v${moveResult.registerA}, "${site.replacement}"
                                """.trimIndent(),
                            )
                            rewritten++
                        }
                    }
                println("[SIM Region Selector] Rewrote $rewritten operator/CellIdentity call site(s) (numeric valid: $numericValid, name set: ${cleanOperatorName.isNotEmpty()}).")
                patched++
            } else {
                println("[SIM Region Selector] Operator numeric/name options empty -> stock operator values kept.")
            }
        } catch (e: Exception) {
            println("[SIM Region Selector] Operator spoofing note: ${e.message}")
        }

        println("[SIM Region Selector] Applied $patched SIM/Network country spoofing hooks (region: $region).")
    }
}
