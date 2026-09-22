package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val xiaomiEarbudsOfflineManifestResourcePatch = resourcePatch(
    name = "Xiaomi Earbuds Offline Manifest Purge",
    description = "Removes INTERNET and ACCESS_NETWORK_STATE permissions from AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Xiaomi Earbuds Offline Manifest Purge] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        val blockedPermissions = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
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

        println("[Xiaomi Earbuds Offline Manifest Purge] Stripped $removedPermissions network permissions from AndroidManifest.xml.")
    }
}

@Suppress("unused")
val xiaomiEarbudsOfflineOnlyPatch = bytecodePatch(
    name = "Xiaomi Earbuds Offline Only",
    description = "Completely isolates the app from the network by revoking internet permissions and spoofing offline status to prevent background socket failures.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)
    dependsOn(xiaomiEarbudsOfflineManifestResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()
        val networkExtClass = "Lcom/xiaomi/fitness/common/extensions/NetworkExtKt;"

        // 1. isNetworkAvailable()Z -> false
        Fingerprint(
            definingClass = networkExtClass,
            name = "isNetworkAvailable",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.isNetworkAvailable")
        }

        // 2. isMobileAvailable()Z -> false
        Fingerprint(
            definingClass = networkExtClass,
            name = "isMobileAvailable",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.isMobileAvailable")
        }

        // 3. isWifiConnected()Z -> false
        Fingerprint(
            definingClass = networkExtClass,
            name = "isWifiConnected",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.isWifiConnected")
        }

        // 4. getNetworkType()I -> 0
        Fingerprint(
            definingClass = networkExtClass,
            name = "getNetworkType",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.getNetworkType")
        }

        // 5. getNetWorkState()I -> 0
        Fingerprint(
            definingClass = networkExtClass,
            name = "getNetWorkState",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.getNetWorkState")
        }

        // 6. getNetWorkTypeName()Ljava/lang/String; -> "NONE"
        Fingerprint(
            definingClass = networkExtClass,
            name = "getNetWorkTypeName",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-string v0, "NONE"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkExtKt.getNetWorkTypeName")
        }

        // 7. NetUtil direct callers (bypasses direct ConnectivityManager.getActiveNetworkInfo calls)
        val netUtilClass = "Lcom/xiaomi/fitness/common/utils/NetUtil;"

        Fingerprint(
            definingClass = netUtilClass,
            name = "getNetWorkAvailable",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.getNetWorkAvailable")
        }

        Fingerprint(
            definingClass = netUtilClass,
            name = "isMobileAva",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.isMobileAva")
        }

        Fingerprint(
            definingClass = netUtilClass,
            name = "isWifiCon",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.isWifiCon")
        }

        Fingerprint(
            definingClass = netUtilClass,
            name = "getNetworkType",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.getNetworkType")
        }

        Fingerprint(
            definingClass = netUtilClass,
            name = "getNetWorkState",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.getNetWorkState")
        }

        Fingerprint(
            definingClass = netUtilClass,
            name = "getNetWorkTypeName",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-string v0, "NONE"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("NetUtil.getNetWorkTypeName")
        }

        // 8. Prevent SecurityException in NetworkStateUtil background task & broadcast receiver
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/common/utils/NetworkStateUtil;",
            name = "updateNetworkState",
            parameters = listOf("Landroid/content/Context;", "Z"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("NetworkStateUtil.updateNetworkState")
        }

        // 9. Prevent SecurityException in Xiaomi account SDK network checker
        Fingerprint(
            definingClass = "Lcom/xiaomi/accountsdk/request/NetworkUtils;",
            name = "isNetworkConnected",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("NetworkUtils.isNetworkConnected")
        }

        // 10. Prevent SecurityException in OkHttp Dispatcher by failing DNS lookups with UnknownHostException
        Fingerprint(
            definingClass = "Lokhttp3/Dns\$Companion\$DnsSystem;",
            name = "lookup",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Ljava/util/List;",
        ).method.apply {
            addInstructions(
                0,
                """
                    new-instance v0, Ljava/net/UnknownHostException;
                    const-string v1, "Offline mode active"
                    invoke-direct {v0, v1}, Ljava/net/UnknownHostException;-><init>(Ljava/lang/String;)V
                    throw v0
                """.trimIndent(),
            )
            hookedMethods.add("DnsSystem.lookup")
        }

        println("[Xiaomi Earbuds Offline Only] Spoofed offline network state across ${hookedMethods.size} targets.")
    }
}
