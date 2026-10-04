package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

private fun BytecodePatchContext.hookStandardFontProvider(): Int {
    val standardProviderFp = Fingerprint(
        strings = listOf("TikTokSans-VF.otf"),
    )

    // LIZLLL([Landroid/graphics/fonts/FontVariationAxis; I Z)Landroid/graphics/Typeface;
    val axisMethod = standardProviderFp.classDef.methods.firstOrNull {
        it.parameterTypes == listOf("[Landroid/graphics/fonts/FontVariationAxis;", "I", "Z") &&
            it.returnType == "Landroid/graphics/Typeface;"
    } ?: error("Could not find standard font provider LIZLLL method")

    axisMethod.removeInstructions(0, axisMethod.implementation!!.instructions.count())
    axisMethod.addInstructions(
        0,
        """
            move/from16 v0, p2
            move/from16 v1, p3
            invoke-static {v0, v1}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->getSystemTypeface(IZ)Landroid/graphics/Typeface;
            move-result-object v0
            return-object v0
        """.trimIndent(),
    )

    // LIZ(Ljava/lang/String;)Landroid/graphics/Typeface;
    val pathMethods = standardProviderFp.classDef.methods.filter {
        it.implementation != null &&
            it.parameterTypes == listOf("Ljava/lang/String;") &&
            it.returnType == "Landroid/graphics/Typeface;"
    }
    pathMethods.forEach { method ->
        val pathReg = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
        method.removeInstructions(0, method.implementation!!.instructions.count())
        method.addInstructions(
            0,
            """
                move-object/from16 v0, $pathReg
                invoke-static {v0}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->getSystemTypefaceForPath(Ljava/lang/String;)Landroid/graphics/Typeface;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    }

    println("[System Font] Hooked standard font provider (${standardProviderFp.classDef.type}) -> System font redirection active.")
    return 1 + (if (pathMethods.isNotEmpty()) 1 else 0)
}

private fun BytecodePatchContext.hookDynamicFontProvider(): Int {
    val dynamicProviderFp = Fingerprint(
        strings = listOf("buildBaseTypefaceFromBuffer failed, fallback for: "),
    )

    val axisMethod = dynamicProviderFp.classDef.methods.firstOrNull {
        it.parameterTypes == listOf("[Landroid/graphics/fonts/FontVariationAxis;", "I", "Z") &&
            it.returnType == "Landroid/graphics/Typeface;"
    } ?: error("Could not find dynamic font provider LIZLLL method")

    axisMethod.removeInstructions(0, axisMethod.implementation!!.instructions.count())
    axisMethod.addInstructions(
        0,
        """
            move/from16 v0, p2
            move/from16 v1, p3
            invoke-static {v0, v1}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->getSystemTypeface(IZ)Landroid/graphics/Typeface;
            move-result-object v0
            return-object v0
        """.trimIndent(),
    )

    val pathMethods = dynamicProviderFp.classDef.methods.filter {
        it.implementation != null &&
            it.parameterTypes == listOf("Ljava/lang/String;") &&
            it.returnType == "Landroid/graphics/Typeface;"
    }
    pathMethods.forEach { method ->
        val pathReg = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
        method.removeInstructions(0, method.implementation!!.instructions.count())
        method.addInstructions(
            0,
            """
                move-object/from16 v0, $pathReg
                invoke-static {v0}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->getSystemTypefaceForPath(Ljava/lang/String;)Landroid/graphics/Typeface;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
    }

    println("[System Font] Hooked dynamic font provider (${dynamicProviderFp.classDef.type}) -> System font redirection active.")
    return 1 + (if (pathMethods.isNotEmpty()) 1 else 0)
}

private fun BytecodePatchContext.hookAssetFontLoader(): Int {
    val assetFontFp = Fingerprint(
        parameters = listOf("Landroid/content/res/AssetManager;", "Ljava/lang/String;"),
        returnType = "Landroid/graphics/Typeface;",
    )
    val assetMethod = assetFontFp.method
    assetMethod.addInstructions(
        0,
        """
            invoke-static {p0, p1}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->interceptAssetTypeface(Landroid/content/res/AssetManager;Ljava/lang/String;)Landroid/graphics/Typeface;
            move-result-object v0
            if-eqz v0, :cond_orig
            return-object v0
            :cond_orig
        """.trimIndent(),
    )
    println("[System Font] Hooked asset font loader (${assetFontFp.classDef.type}->${assetMethod.name}) -> Asset font redirection active.")
    return 1
}

private fun BytecodePatchContext.hookWebViewFontLoader(): Int {
    val webFontFp = Fingerprint(
        definingClass = "Lcom/ss/android/ugc/aweme/base/font/FontLoadManager\$init\$1;",
        name = "shouldInterceptRequest",
        parameters = listOf("Landroid/webkit/WebView;", "Landroid/webkit/WebResourceRequest;"),
    )
    webFontFp.method.addInstructions(
        0,
        """
            invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->shouldInterceptWebFont(Landroid/webkit/WebResourceRequest;)Z
            move-result v0
            if-eqz v0, :cond_web_orig
            const/4 v0, 0x0
            return-object v0
            :cond_web_orig
        """.trimIndent(),
    )
    println("[System Font] Hooked WebView font interceptor -> WebView fallback active.")
    return 1
}

val systemFontPatch = bytecodePatch(
    name = "System Font",
    description = "Forces TikTok to use the Android system font instead of bundled proprietary TikTokSans fonts.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
        patched += hookStandardFontProvider()
        patched += hookDynamicFontProvider()
        patched += hookAssetFontLoader()
        patched += hookWebViewFontLoader()
        println("[System Font] Applied $patched system font hook(s) -> System font active.")
    }
}
