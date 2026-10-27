package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
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

    val isStatic = AccessFlags.STATIC.isSet(axisMethod.accessFlags)
    val weightReg = if (isStatic) "p1" else "p2"
    val italicReg = if (isStatic) "p2" else "p3"

    axisMethod.clearTryBlocks()
    axisMethod.removeInstructions(0, axisMethod.implementation!!.instructions.count())
    axisMethod.addInstructions(
        0,
        """
            move/from16 v0, $weightReg
            move/from16 v1, $italicReg
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
        method.clearTryBlocks()
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

    println("[Use System Font] Hooked standard font provider (${standardProviderFp.classDef.type}) -> System font redirection active.")
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

    val isStatic = AccessFlags.STATIC.isSet(axisMethod.accessFlags)
    val weightReg = if (isStatic) "p1" else "p2"
    val italicReg = if (isStatic) "p2" else "p3"

    axisMethod.clearTryBlocks()
    axisMethod.removeInstructions(0, axisMethod.implementation!!.instructions.count())
    axisMethod.addInstructions(
        0,
        """
            move/from16 v0, $weightReg
            move/from16 v1, $italicReg
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
        method.clearTryBlocks()
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

    println("[Use System Font] Hooked dynamic font provider (${dynamicProviderFp.classDef.type}) -> System font redirection active.")
    return 1 + (if (pathMethods.isNotEmpty()) 1 else 0)
}

private fun BytecodePatchContext.hookAssetFontLoader(): Int {
    val assetFontFp = Fingerprint(
        definingClass = "LX/00kd;",
        name = "t4", // in v47.5.3, was s4 in v47.4.3
        parameters = listOf("Landroid/content/res/AssetManager;", "Ljava/lang/String;"),
        returnType = "Landroid/graphics/Typeface;",
    )
    val assetMethod = assetFontFp.method
    val isStatic = AccessFlags.STATIC.isSet(assetMethod.accessFlags)
    val assetReg = if (isStatic) "p0" else "p1"
    val pathReg = if (isStatic) "p1" else "p2"
    assetMethod.addInstructions(
        0,
        """
            invoke-static {$assetReg, $pathReg}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->interceptAssetTypeface(Landroid/content/res/AssetManager;Ljava/lang/String;)Landroid/graphics/Typeface;
            move-result-object v0
            if-eqz v0, :cond_orig
            return-object v0
            :cond_orig
        """.trimIndent(),
    )
    println("[Use System Font] Hooked asset font loader (${assetFontFp.classDef.type}->${assetMethod.name}) -> Asset font redirection active.")
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
            invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_FONT_HOOK}->interceptWebFont(Landroid/webkit/WebResourceRequest;)Landroid/webkit/WebResourceResponse;
            move-result-object v0
            if-eqz v0, :cond_web_orig
            return-object v0
            :cond_web_orig
        """.trimIndent(),
    )
    println("[Use System Font] Hooked WebView font interceptor -> WebView fallback active.")
    return 1
}

val systemFontPatch = bytecodePatch(
    name = "Use System Font",
    description = "Uses the system font instead of bundled TikTok fonts.",
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
        if (patched == 0) {
            println("[Use System Font] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Use System Font] Applied $patched system font hook(s) -> System font active.")
        }
    }
}
