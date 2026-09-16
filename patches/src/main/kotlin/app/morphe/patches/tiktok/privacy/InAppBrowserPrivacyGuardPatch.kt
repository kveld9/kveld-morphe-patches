package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val inAppBrowserPrivacyGuardPatch = bytecodePatch(
    name = "In-App Browser Privacy Guard",
    description = "Neutralizes inline JavaScript tracking injection, DOM monitoring, and AJAX hooking when opening external links inside the in-app WebView.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Force WebView inline JS injection predicate to return false
        try {
            val predicateFp = Fingerprint(
                returnType = "Z",
                parameters = listOf("Landroid/webkit/WebView;"),
                strings = listOf("webview_inline_inject_js"),
            )
            predicateFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[In-App Browser Privacy Guard] Injection predicate note: ${e.message}")
        }

        // 2. Neutralize onPageStarted script execution in WebViewAjaxHooker
        try {
            val onPageStartedFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/compliance/sandbox/webview/WebViewAjaxHooker;",
                name = "onPageStarted",
                returnType = "V",
                parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;", "Landroid/graphics/Bitmap;"),
            )
            onPageStartedFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[In-App Browser Privacy Guard] onPageStarted note: ${e.message}")
        }

        println("[In-App Browser Privacy Guard] Applied $patched in-app browser tracking mitigations.")
    }
}
