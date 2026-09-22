package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val EMPTY_BYTES = byteArrayOf()

@Suppress("unused")
val nokoPrintAssetDebloatPatch = rawResourcePatch(
    name = "NokoPrint Asset De-bloat",
    description = "Strips embedded secondary Meta Audience Network DEX and redundant web ad-viewer HTML/JS assets to save APK space.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        var savedBytes = 0L
        var count = 0

        fun stripFile(path: String) {
            val file = get(path)
            if (file.exists() && file.isFile) {
                val orig = file.length()
                if (orig > 0) {
                    file.writeBytes(EMPTY_BYTES)
                    savedBytes += orig
                    count++
                }
            }
        }

        fun stripDirectory(dirPath: String) {
            val dir = get(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val orig = file.length()
                    if (orig > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += orig
                        count++
                    }
                }
            }
        }

        // 1. Strip dynamic secondary DEX, ad scripts, and telemetry configs in assets/
        val unneededAssets = listOf(
            "assets/audience_network.dex",
            "assets/ad.html",
            "assets/android_bridge.js",
            "assets/aps-mraid.js",
            "assets/aps_mobile_client_config.json",
            "assets/dtb-m.js",
            "assets/fyb_iframe_endcard_tmpl.html",
            "assets/fyb_static_endcard_tmpl.html",
            "assets/grs_sdk_global_route_config_opensdkService.json",
            "assets/grs_sdk_server_config.json",
            "assets/grs_sp.bks",
            "assets/hmsincas.bks",
            "assets/hmsrootcas.bks",
            "assets/ia_js_load_monitor.txt",
            "assets/ia_mraid_bridge.txt",
            "assets/mraid.js",
            "assets/mraid3.js",
            "assets/omsdk-v1.js",
            "assets/rv_binddatas.xml",
            "assets/ui.js",
            "assets/AppstoreAuthenticationKey.pem",
        )
        unneededAssets.forEach { stripFile(it) }

        // 2. Strip web ad-viewer HTML/JS/CSS assets
        stripDirectory("assets/ad-viewer")

        // 3. Strip ad configs, consent flows, and trackers in res/raw/
        val unneededRawFiles = listOf(
            "res/raw/applovin_consent_flow_privacy_policy.json",
            "res/raw/applovin_consent_flow_terms_of_service_and_privacy_policy.json",
            "res/raw/applovin_consent_flow_unified_cmp.json",
            "res/raw/com_android_billingclient_heterodyne_info",
            "res/raw/com_android_billingclient_registration_info.binarypb",
            "res/raw/firebase_common_keep.xml",
            "res/raw/firebase_crashlytics_keep.xml",
            "res/raw/html_player_vast.js",
            "res/raw/omsdk_v1.js",
            "res/raw/omsdk_v1_4_12.js",
        )
        unneededRawFiles.forEach { stripFile(it) }

        // 4. Strip redundant third-party ad network & floating animation drawables (only binary images, never XML vector drawables)
        val resDir = get("res")
        if (resDir.exists() && resDir.isDirectory) {
            val adDrawableTokens = listOf(
                "applovin_",
                "mbridge_",
                "com_facebook_",
                "smaato_",
                "ironsource_",
                "fyber_",
                "iap_float_",
                "ia_",
            )
            resDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val name = file.name.lowercase()
                val isImage = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".webp")
                if (isImage && adDrawableTokens.any { name.startsWith(it) }) {
                    val orig = file.length()
                    if (orig > 0) {
                        file.writeBytes(EMPTY_BYTES)
                        savedBytes += orig
                        count++
                    }
                }
            }
        }

        val savedMb = savedBytes / (1024.0 * 1024.0)
        println("[NokoPrint Asset De-bloat] Cleaned $count files, saved ${"%.2f".format(savedMb)} MB in assets.")
    }
}
