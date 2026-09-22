package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val xiaomiEarbudsDevicePrivacyGuardResourcePatch = resourcePatch(
    name = "Xiaomi Earbuds Device Privacy Guard Manifest Purge",
    description = "Removes location permissions from AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Xiaomi Earbuds Device Privacy Guard Manifest Purge] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val blockedPermissions = setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        )

        var removedPermissions = 0
        document(manifestFile.absolutePath).use { doc ->
            val usesPermissions = doc.getElementsByTagName("uses-permission")
            val permsToRemove = mutableListOf<Element>()
            for (i in 0 until usesPermissions.length) {
                val elem = usesPermissions.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name in blockedPermissions) {
                    permsToRemove.add(elem)
                }
            }
            permsToRemove.forEach {
                it.parentNode?.removeChild(it)
                removedPermissions++
            }
        }

        println("[Xiaomi Earbuds Device Privacy Guard Manifest Purge] Stripped $removedPermissions location permissions from AndroidManifest.xml.")
    }
}

@Suppress("unused")
val xiaomiEarbudsDevicePrivacyGuardPatch = bytecodePatch(
    name = "Xiaomi Earbuds Device Privacy Guard",
    description = "Blinds hardware device IDs, anonymizes device identifiers, neutralizes environment info leakage, and bypasses location checks for Bluetooth scanning.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)
    dependsOn(xiaomiEarbudsDevicePrivacyGuardResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Blind PrivacyDataMaster hardware getters
        val privacyDataClass = "Lcom/xiaomi/account/privacy_data/master/PrivacyDataMaster;"

        Fingerprint(
            definingClass = privacyDataClass,
            name = "get",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "[Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyDataMaster.get(Context, String, String[])")
        }

        Fingerprint(
            definingClass = privacyDataClass,
            name = "get",
            parameters = listOf("Landroid/content/Context;", "Lcom/xiaomi/account/privacy_data/master/PrivacyDataType;", "[Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyDataMaster.get(Context, PrivacyDataType, String[])")
        }

        Fingerprint(
            definingClass = privacyDataClass,
            name = "forceGet",
            parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "[Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyDataMaster.forceGet(Context, String, String[])")
        }

        Fingerprint(
            definingClass = privacyDataClass,
            name = "forceGet",
            parameters = listOf("Landroid/content/Context;", "Lcom/xiaomi/account/privacy_data/master/PrivacyDataType;", "[Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyDataMaster.forceGet(Context, PrivacyDataType, String[])")
        }

        // 2. Anonymize AccountDeviceIdManager device IDs
        val accountDeviceIdClass = "Lcom/xiaomi/accountsdk/hasheddeviceidlib/AccountDeviceIdManager;"

        Fingerprint(
            definingClass = accountDeviceIdClass,
            name = "getDeviceId",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-string v0, "0000000000000000"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("AccountDeviceIdManager.getDeviceId")
        }

        Fingerprint(
            definingClass = accountDeviceIdClass,
            name = "getHashedDeviceId",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-string v0, "0000000000000000"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("AccountDeviceIdManager.getHashedDeviceId")
        }

        Fingerprint(
            definingClass = accountDeviceIdClass,
            name = "syncGetDeviceId",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-string v0, "0000000000000000"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("AccountDeviceIdManager.syncGetDeviceId")
        }

        // 3. Neutralize PassportUserEnvironment info array (Wi-Fi SSID/BSSID, SIM serials)
        Fingerprint(
            definingClass = "Lcom/xiaomi/passport/PassportUserEnvironment;",
            name = "getEnvInfoArray",
            parameters = listOf("Landroid/app/Application;"),
            returnType = "[Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    new-array v0, v0, [Ljava/lang/String;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("PassportUserEnvironment.getEnvInfoArray")
        }

        // 4. Bypass location requirement for Bluetooth BLE scanning
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/common/extensions/LocationExtKt;",
            name = "isLocationEnable",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("LocationExtKt.isLocationEnable")
        }

        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/common/extensions/LocationExtKt;",
            name = "isGpsEnable",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("LocationExtKt.isGpsEnable")
        }

        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/common/utils/e;",
            name = "A",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("e.A(locationProviderCheck)")
        }

        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/ui/add/AddDeviceViewModel;",
            name = "isLocationEnable",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("AddDeviceViewModel.isLocationEnable")
        }

        // 13. CheckPermissionUtilKt.getLocationPermissions()[String -> return empty array so permission checks pass without location
        Fingerprint(
            definingClass = "Lcom/mi/earphone/bluetoothsdk/util/CheckPermissionUtilKt;",
            name = "getLocationPermissions",
            parameters = listOf(),
            returnType = "[Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    new-array v0, v0, [Ljava/lang/String;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("CheckPermissionUtilKt.getLocationPermissions")
        }

        // 14. AddDeviceFragment.hasScanCondition()Z -> return true to allow immediate BLE scan without location
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/ui/add/AddDeviceFragment;",
            name = "hasScanCondition",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("AddDeviceFragment.hasScanCondition")
        }

        println("[Xiaomi Earbuds Device Privacy Guard] Anonymized device identifiers and blinded hardware privacy getters across ${hookedMethods.size} targets.")
    }
}
