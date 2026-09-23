package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

import java.io.File
import java.util.Locale

private val EMPTY_BYTES = byteArrayOf()

@Suppress("unused")
val nokoPrintAssetDebloatPatch = rawResourcePatch(
    name = "NokoPrint Asset De-bloat",
    description = "Strips embedded secondary Meta Audience Network DEX, tracking scripts, ad-viewer HTML/JS templates, RuStore TLS certs, and third-party ad assets to save APK space.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        var savedBytes = 0L
        var count = 0

        fun stripFile(file: File) {
            if (!file.exists() || !file.isFile) return
            val orig = file.length()
            if (orig > 0) {
                file.writeBytes(EMPTY_BYTES)
                savedBytes += orig
                count++
            }
        }

        fun stripFile(path: String) = stripFile(get(path))

        fun stripDirectory(dirPath: String) {
            val dir = get(dirPath)
            if (!dir.exists() || !dir.isDirectory) return
            dir.walkTopDown().filter { it.isFile }.forEach { stripFile(it) }
        }

        fun isRuStoreCertificate(name: String): Boolean {
            val hasCertExtension = name.endsWith(".pem") || name.endsWith(".cer")
            if (!hasCertExtension) return false
            val prefixes = listOf("sdk_imaging_", "sdk_pay_", "ministry_of_digital_", "rootca_ssl_rsa2022")
            return prefixes.any { name.startsWith(it) }
        }

        // 1. Strip dynamic secondary DEX, ad scripts, and telemetry configs in assets/
        val unneededAssets = listOf(
            "assets/audience_network.dex",
            "assets/audience_network/classes.dex",
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
            "assets/dic",
            "assets/PubMatic_Logo.svg",
            "assets/pob_info_button.svg",
            "assets/pob_person_button.svg",
            "assets/pob_mraid.js",
            "assets/openwrapsdk.js",
            "assets/com.moloco.sdk.xenoss.sdkdevkit.mraid.js",
            "assets/mraid-bridge.js",
            "assets/dsa_page.html",
            "assets/mbridge_download_dialog_view.xml",
            "assets/base_hms_app_root.cer",
            "assets/grs_sdk_global_route_config_apptouchupdatesdk.json",
            "assets/grs_sdk_global_route_config_updatesdk.json",
        )
        unneededAssets.forEach { stripFile(it) }

        // 2. Strip web ad-viewer HTML/JS/CSS assets & secondary audience network directory
        stripDirectory("assets/ad-viewer")
        stripDirectory("assets/audience_network")
        stripDirectory("assets/iads")
        stripDirectory("assets/template")

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
            "res/raw/omsdk_v1_6_2.js",
            "res/raw/omsdk_v1_6_9.js",
            "res/raw/omid_session_client_v1_6_9.js",
        )
        unneededRawFiles.forEach { stripFile(it) }

        // 4. Strip RuStore certificates in res/raw/
        val rawDir = get("res/raw")
        if (rawDir.exists() && rawDir.isDirectory) {
            val preservedRawNames = setOf(
                "computer.png",
                "documents.png",
                "folder.png",
                "photos.png",
                "printer.png",
                "up.png",
                "web.png",
                "workgroup.png",
                "shape_corner.svg",
            )
            rawDir.walkTopDown()
                .filter { it.isFile && it.name !in preservedRawNames && isRuStoreCertificate(it.name) }
                .forEach { stripFile(it) }
        }

        // 5. Strip redundant third-party ad network & floating animation drawables (only binary images, never XML vector drawables)
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
                "bigo_",
                "tt_",
                "pob_",
                "tappx_",
                "ogury_",
                "moloco_",
            )
            val imageExtensions = listOf(".png", ".jpg", ".webp")
            resDir.walkTopDown()
                .filter { file ->
                    val name = file.name.lowercase()
                    file.isFile &&
                        imageExtensions.any { name.endsWith(it) } &&
                        adDrawableTokens.any { name.startsWith(it) }
                }
                .forEach { stripFile(it) }
        }

        val savedMb = String.format(Locale.US, "%.2f", savedBytes.toDouble() / (1024 * 1024))
        println("[NokoPrint Asset De-bloat] Cleaned $count files, saved $savedMb MB in assets.")
    }
}
