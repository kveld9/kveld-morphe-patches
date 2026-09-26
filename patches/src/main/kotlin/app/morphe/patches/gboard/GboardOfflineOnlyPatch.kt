package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"

private val GBOARD_BLOCKED_PERMISSIONS = setOf(
    "android.permission.INTERNET",
    "android.permission.ACCESS_WIFI_STATE",
    "android.permission.GET_ACCOUNTS",
    "com.google.android.providers.gsf.permission.READ_GSERVICES",
    "android.permission.GET_PACKAGE_SIZE",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.WAKE_LOCK",
    "android.permission.RECEIVE_BOOT_COMPLETED",
)

private val GBOARD_SERVICES_TO_DISABLE = setOf(
    "com.google.android.apps.inputmethod.libs.dataservice.superpacks.SuperpacksForegroundTaskService",
    "androidx.work.impl.foreground.SystemForegroundService",
)

private fun removeManifestPermissions(doc: Document, blocked: Set<String>): Int {
    var removed = 0
    for (tagName in listOf("uses-permission", "uses-permission-sdk-23")) {
        val nodes = doc.getElementsByTagName(tagName)
        val toRemove = mutableListOf<Element>()
        for (i in 0 until nodes.length) {
            val elem = nodes.item(i) as? Element ?: continue
            val name = elem.getAttribute("android:name")
            if (name in blocked) {
                toRemove.add(elem)
            }
        }
        for (elem in toRemove) {
            elem.parentNode?.removeChild(elem)
            removed++
        }
    }
    return removed
}

private fun disableForegroundServices(doc: Document, services: Set<String>): Int {
    var disabled = 0
    val serviceNodes = doc.getElementsByTagName("service")
    for (i in 0 until serviceNodes.length) {
        val elem = serviceNodes.item(i) as? Element ?: continue
        val name = elem.getAttribute("android:name")
        if (name in services) {
            elem.setAttributeNS(ANDROID_XML_NAMESPACE, "android:enabled", "false")
            disabled++
        }
    }
    return disabled
}

private fun blockCleartextTraffic(doc: Document) {
    val appElements = doc.getElementsByTagName("application")
    if (appElements.length > 0) {
        val appElement = appElements.item(0) as? Element
        appElement?.setAttributeNS(ANDROID_XML_NAMESPACE, "android:usesCleartextTraffic", "false")
    }
}

private val gboardOfflineManifestResourcePatch = resourcePatch(
    name = "Gboard Offline Manifest Purge",
    description = "Removes network, account syncing, background scheduling, and diagnostic permissions from AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Offline Only] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        var removedCount = 0
        var disabledCount = 0

        document(manifestFile.absolutePath).use { doc ->
            removedCount = removeManifestPermissions(doc, GBOARD_BLOCKED_PERMISSIONS)
            blockCleartextTraffic(doc)
            disabledCount = disableForegroundServices(doc, GBOARD_SERVICES_TO_DISABLE)
        }

        println("[Offline Only] Stripped $removedCount unnecessary/network permissions and disabled $disabledCount foreground services in AndroidManifest.xml.")
    }
}

val gboardOfflineOnlyPatch: BytecodePatch = bytecodePatch(
    name = "Offline Only",
    description = "Completely isolates Gboard from the network by revoking network permissions, neutralizing HTTP clients (Cronet, OkHttp, Superpacks), and spoofing offline status.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(gboardOfflineManifestResourcePatch)

    execute {
        var hookedMethods = 0

        // 1. DeviceStatusMonitor (Lxeg): force NO_CONNECTION
        Fingerprint(
            definingClass = "Lxeg;",
            name = "a",
            parameters = listOf("Landroid/content/Intent;"),
            returnType = "Lxei;",
        ).method.apply {
            addInstructions(
                0,
                """
                    sget-object v0, Lxei;->b:Lxei;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lxeg;",
            name = "g",
            parameters = listOf("Landroid/net/Network;"),
            returnType = "Lxei;",
        ).method.apply {
            addInstructions(
                0,
                """
                    sget-object v0, Lxei;->b:Lxei;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        // 2. NetworkInfoNotification (Lxej): spoof offline
        Fingerprint(
            definingClass = "Lxej;",
            name = "a",
            parameters = emptyList(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lxej;",
            name = "c",
            parameters = listOf("Lxej;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        // 3. Central HTTP Clients: Cronet (Lwcf), OkHttp3 (Lwcz), TrafficStats Wrapper (Lwam), and Lwbe
        val httpMethodSnippet = """
            new-instance v0, Ljava/io/IOException;
            const-string v1, "Offline mode"
            invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V
            invoke-static {v0}, Lagsw;->h(Ljava/lang/Throwable;)Lagtn;
            move-result-object v0
            return-object v0
        """.trimIndent()

        listOf("Lwcf;", "Lwcz;", "Lwam;", "Lwbe;").forEach { clientClass ->
            Fingerprint(
                definingClass = clientClass,
                name = "c",
                parameters = listOf("Lwbq;"),
                returnType = "Lagtn;",
            ).method.apply {
                addInstructions(0, httpMethodSnippet)
                hookedMethods++
            }
        }

        // 4. Superpacks HTTP Downloader (Lacjw): force connectivity false and throw on socket open
        Fingerprint(
            definingClass = "Lacjw;",
            name = "k",
            parameters = listOf("Lacjk;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lacjw;",
            name = "b",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "Ljava/net/HttpURLConnection;",
        ).method.apply {
            addInstructions(
                0,
                """
                    new-instance v0, Ljava/io/IOException;
                    const-string v1, "Offline mode"
                    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V
                    throw v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        // 5. LanguageDownloadQueue (Lzie): neutralize download queueing
        Fingerprint(
            definingClass = "Lzie;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "Lzjj;", "Ljava/util/function/Consumer;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        // 6. Glide ConnectivityMonitor (Ldxd): neutralize callback registration
        Fingerprint(
            definingClass = "Ldxd;",
            name = "b",
            parameters = listOf("Ldwd;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Ldxd;",
            name = "c",
            parameters = listOf("Ldwd;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        // 7. WorkManager NetworkStateTracker (Lcps & Lcpu): return offline state and prevent callbacks
        Fingerprint(
            definingClass = "Lcps;",
            name = "a",
            parameters = listOf("Landroid/net/ConnectivityManager;", "Z"),
            returnType = "Lcoj;",
        ).method.apply {
            addInstructions(
                0,
                """
                    new-instance v0, Lcoj;
                    const/4 v1, 0x0
                    const/4 v2, 0x0
                    const/4 v3, 0x0
                    const/4 v4, 0x1
                    const/4 v5, 0x0
                    invoke-direct/range {v0 .. v5}, Lcoj;-><init>(ZZZZZ)V
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lcpu;",
            name = "d",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lcpu;",
            name = "e",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        println("[Offline Only] Neutralized $hookedMethods network & connectivity methods across 9 classes.")
    }
}
