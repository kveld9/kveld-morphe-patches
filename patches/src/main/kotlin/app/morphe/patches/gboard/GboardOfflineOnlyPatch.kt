package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.Constants
import org.w3c.dom.Document
import org.w3c.dom.Element

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
    name = "Go Offline (Setup)",
    description = "Removes network permissions.",
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
    description = "Fully disconnects Gboard. Download dictionaries first.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(gboardOfflineManifestResourcePatch)

    execute {
        var hookedMethods = 0

        // 1. DeviceStatusMonitor (mfb): force NO_CONNECTION
        Fingerprint(
            definingClass = "Lmfb;",
            name = "a",
            parameters = listOf("Landroid/content/Intent;"),
            returnType = "Lmff;",
        ).method.apply {
            addInstructions(
                0,
                """
                    sget-object v0, Lmff;->b:Lmff;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lmfb;",
            name = "h",
            parameters = listOf("Landroid/net/Network;"),
            returnType = "Lmff;",
        ).method.apply {
            addInstructions(
                0,
                """
                    sget-object v0, Lmff;->b:Lmff;
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        // 2. NetworkInfoNotification (mfg): spoof offline
        Fingerprint(
            definingClass = "Lmfg;",
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
            definingClass = "Lmfg;",
            name = "c",
            parameters = listOf("Lmfg;"),
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

        // 3. Central HTTP Clients (Cronet, OkHttp3, and TrafficStats Wrappers)
        // Note (18.5.1): the legacy HTTP wrapper layer (why/wis/wgf/wgx over the
        // Lwhj; request type) is gone upstream; the stack moved to
        // CronetWithOkHttpFallbackTransport. Offline coverage stays via the
        // connectivity spoofs above plus the downloader/queue hooks below.
        println("[Offline Only] Pruned obsolete HTTP wrapper hooks: targets removed upstream.")

        // 4. Superpacks HTTP Downloader (otm): force connectivity false and throw on socket open
        Fingerprint(
            definingClass = "Lotm;",
            name = "k",
            parameters = listOf("Lotd;"),
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
            definingClass = "Lotm;",
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

        // 5. LanguageDownloadQueue (nhq): neutralize download queueing
        Fingerprint(
            definingClass = "Lnhq;",
            name = "c",
            parameters = listOf("Ljava/lang/String;", "Lnii;", "Ljava/util/function/Consumer;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        // 6. Glide ConnectivityMonitor (chf): neutralize callback registration
        Fingerprint(
            definingClass = "Lchf;",
            name = "b",
            parameters = listOf("Lcgj;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lchf;",
            name = "c",
            parameters = listOf("Lcgj;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        // 7. WorkManager NetworkStateTracker (bja & bjc): return offline state and prevent callbacks
        Fingerprint(
            definingClass = "Lbja;",
            name = "a",
            parameters = listOf("Landroid/net/ConnectivityManager;", "Z"),
            returnType = "Lbhz;",
        ).method.apply {
            addInstructions(
                0,
                """
                    new-instance v0, Lbhz;
                    const/4 v1, 0x0
                    const/4 v2, 0x0
                    const/4 v3, 0x0
                    const/4 v4, 0x1
                    const/4 v5, 0x0
                    invoke-direct/range {v0 .. v5}, Lbhz;-><init>(ZZZZZ)V
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lbjc;",
            name = "d",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        Fingerprint(
            definingClass = "Lbjc;",
            name = "e",
            parameters = emptyList(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods++
        }

        if (hookedMethods == 0) {
            println("[Offline Only] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Offline Only] Neutralized $hookedMethods network & connectivity methods across 9 classes.")
        }
    }
}
