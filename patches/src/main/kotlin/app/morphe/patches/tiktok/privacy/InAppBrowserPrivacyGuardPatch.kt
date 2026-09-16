package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val inAppBrowserPrivacyGuardPatch = bytecodePatch(
    name = "In-App Browser Privacy Guard",
    description = "Redirects external and third-party web links to the default system browser and neutralizes inline JavaScript tracking, DOM monitoring, and AJAX hooking in residual in-app WebViews.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

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

        // 3. Redirect third-party and external web links to the default system browser (SparkThird dispatcher)
        val redirectSmali = """
            invoke-static {p0, p1}, ${Constants.TIKTOK_EXTENSION_BROWSER_HOOK}->redirectSparkThird(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :cond_skip
            return-void
            :cond_skip
            nop
        """.trimIndent()

        try {
            val sparkThirdFp = Fingerprint(
                returnType = "V",
                parameters = listOf("Landroid/content/Context;", "Lcom/bytedance/hybrid/spark/third/router/SparkThirdContext;"),
                strings = listOf("ContainerId", "Context_startActivity_1"),
            )
            sparkThirdFp.method.addInstructionsWithLabels(0, redirectSmali)
            println("[In-App Browser Privacy Guard] Redirected SparkThirdActivity -> external browser hook.")
            patched++

            // Also redirect SparkThird popup / dialog dispatcher in the same class
            sparkThirdFp.classDef.methods.firstOrNull {
                it.name == "LIZJ" && it.returnType == "V" && it.parameterTypes.size == 2
            }?.let { popupMethod ->
                popupMethod.addInstructionsWithLabels(0, redirectSmali)
                println("[In-App Browser Privacy Guard] Redirected SparkThirdPopUp -> external browser hook.")
                patched++
            }
        } catch (e: Exception) {
            println("[In-App Browser Privacy Guard] SparkThird dispatcher note: ${e.message}")
        }

        // 4. Force AnchorInfoStruct.getOpenSystemBrowser() -> true
        listOf(
            "Lcom/ss/android/ugc/aweme/commercialize/model/feed/anchor/AnchorInfoStruct;",
            "Lcom/bytedance/ies/ugc/aweme/rich/model/commercialize/feed/anchor/AnchorInfoStruct;",
        ).forEach { className ->
            try {
                Fingerprint(
                    definingClass = className,
                    name = "getOpenSystemBrowser",
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                    const/4 v0, 0x1
                    return v0
                    """.trimIndent()
                )
                println("[In-App Browser Privacy Guard] Forced $className.getOpenSystemBrowser() -> true.")
                patched++
            } catch (e: Exception) {
                println("[In-App Browser Privacy Guard] AnchorInfoStruct note ($className): ${e.message}")
            }
        }

        println("[In-App Browser Privacy Guard] Applied $patched in-app browser tracking mitigations & external redirection hooks.")
    }
}
