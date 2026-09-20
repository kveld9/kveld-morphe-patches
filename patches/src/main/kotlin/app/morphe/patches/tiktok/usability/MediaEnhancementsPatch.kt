package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

val mediaEnhancementsPatch = bytecodePatch(
    name = "Media Usability & Watermark-Free Downloader",
    description = "Unblocks the download button on creator-restricted videos inside the Share panel, and routes downloads to clean unwatermarked media streams.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Force Aweme download permission flags
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "isPreventDownload",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Media Usability] Forced Aweme.isPreventDownload() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] Aweme.isPreventDownload note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getDownloadWithoutWatermark",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            println("[Media Usability] Forced Aweme.getDownloadWithoutWatermark() -> true.")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] Aweme.getDownloadWithoutWatermark note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "needTTSWatermarkWhenDownload",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Media Usability] Forced Aweme.needTTSWatermarkWhenDownload() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] Aweme.needTTSWatermarkWhenDownload note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getIsCommentPostVideo",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Media Usability] Forced Aweme.getIsCommentPostVideo() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] Aweme.getIsCommentPostVideo note: ${e.message}")
        }

        // 2. ACLCommonShare invariants (Unblocks restricted downloads & activates watermark-free mode)
        val aclHooks = listOf(
            "getCode" to ("I" to "const/4 v0, 0x0\nreturn v0"), // 0 = Allowed
            "getShowType" to ("I" to "const/4 v0, 0x2\nreturn v0"), // 2 = Visible in share panel
            "getTranscode" to ("I" to "const/4 v0, 0x1\nreturn v0"), // 1 = Direct watermark-free mode
            "getMute" to ("Z" to "const/4 v0, 0x0\nreturn v0"), // false = Audio retained
        )

        for ((methodName, sig) in aclHooks) {
            val (retType, smaliCode) = sig
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/model/ACLCommonShare;",
                    name = methodName,
                    returnType = retType,
                ).method.addInstructions(0, smaliCode.trimIndent())
                println("[Media Usability] Forced ACLCommonShare.$methodName() -> unblocked/watermark-free.")
                patched++
            } catch (e: Exception) {
                println("[Media Usability] ACLCommonShare.$methodName note: ${e.message}")
            }
        }

        // 3. Hook Aweme.getVideo() to pre-populate video.downloadNoWatermarkAddr with clean stream
        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getVideo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            )
            val method = fp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->patchVideoObject(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Media Usability] Hooked Aweme.getVideo() (${returnIndices.size} return point(s)) -> Video object unwatermarked stream injector active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Media Usability] Aweme.getVideo note: ${e.message}")
        }

        // 4. Hook Video.getDownloadNoWatermarkAddr() to guarantee non-null clean stream
        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
                name = "getDownloadNoWatermarkAddr",
                returnType = "Lcom/ss/android/ugc/aweme/base/model/UrlModel;",
            )
            val method = fp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->getWatermarkFreeDownloadUrl(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$reg
                        check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Media Usability] Hooked Video.getDownloadNoWatermarkAddr() (${returnIndices.size} return point(s)) -> Clean stream redirection active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Media Usability] Video.getDownloadNoWatermarkAddr note: ${e.message}")
        }

        // 5. Hook Video.getDownloadAddr() to route legacy download requests to clean unwatermarked playAddr stream
        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
                name = "getDownloadAddr",
                returnType = "Lcom/ss/android/ugc/aweme/base/model/UrlModel;",
            )
            val method = fp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->getWatermarkFreeDownloadUrl(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$reg
                        check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Media Usability] Hooked Video.getDownloadAddr() (${returnIndices.size} return point(s)) -> Clean stream redirection active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Media Usability] Video.getDownloadAddr note: ${e.message}")
        }

        // 6. Neutralize AwemeVideoWaterMarkAbilityProtocol watermark triggers
        val protocolClasses = listOf(
            "Lcom/ss/android/ugc/trill/download/protocol/watermark/AwemeVideoWaterMarkAbilityProtocol;",
            "Lcom/ss/android/ugc/aweme/share/base/download/configuration/protocol/WaterMarkAbilityProtocol;",
        )
        val protocolMethods = listOf("LJIIIZ", "LJIIJ", "LJIIIIZZ", "LJII")

        for (clsName in protocolClasses) {
            for (mName in protocolMethods) {
                try {
                    Fingerprint(
                        definingClass = clsName,
                        name = mName,
                        returnType = "Z",
                    ).method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """.trimIndent(),
                    )
                    patched++
                } catch (e: Exception) {
                    // Protocol methods may be partially implemented or inherited
                }
            }
        }
        println("[Media Usability] Neutralized WaterMarkAbilityProtocol & AwemeVideoWaterMarkAbilityProtocol watermark flags.")

        // 7. Strip client-side drawn watermark on saved image posts & comment pictures
        try {
            val fp = Fingerprint(
                strings = listOf("[tiktok_logo]"),
                parameters = listOf("Landroid/graphics/Bitmap;"),
                returnType = "V",
            )
            val method = fp.method
            val drawIndices = method.implementation?.instructions?.withIndex()
                ?.filter {
                    it.value.opcode == Opcode.INVOKE_VIRTUAL &&
                        (it.value as? ReferenceInstruction)?.reference?.toString()
                            ?.contains("->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V") == true
                }
                ?.map { it.index }
                ?.toList() ?: emptyList()

            drawIndices.asReversed().forEach { idx ->
                method.replaceInstruction(idx, "nop")
            }
            if (drawIndices.isNotEmpty()) {
                println("[Media Usability] Neutralized client-side image watermark overlay (${drawIndices.size} draw point(s)).")
                patched++
            }
        } catch (e: Exception) {
            println("[Media Usability] CommentImageWatermark note: ${e.message}")
        }

        // 8. Safe client-side WaterMarkServiceImpl bypass
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/watermark/WaterMarkServiceImpl;",
                name = "waterMark",
            ).method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->handleWatermark(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :cond_skip
                    return-void
                    :cond_skip
                    nop
                """.trimIndent(),
            )
            println("[Media Usability] Injected smart lossless bypass into WaterMarkServiceImpl.waterMark().")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] WaterMarkServiceImpl note: ${e.message}")
        }

        // 9. Redirect watermarked downloader spec (LIZIZ) to unwatermarked downloader spec (LIZ)
        try {
            val fp = Fingerprint(
                strings = listOf("suffix_scene"),
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                returnType = "V",
            )
            val method = fp.method
            val classDef = fp.classDef
            val targetMethod = classDef.methods.firstOrNull {
                it.parameterTypes == listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Z")
            }?.name ?: "LIZ"

            method.ensureRegisterCount(3)
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    invoke-virtual {p0, p1, v0}, ${classDef.type}->$targetMethod(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Z)V
                    return-void
                """.trimIndent(),
            )
            println("[Media Usability] Redirected downloader spec ($targetMethod) -> unwatermarked mode permanently.")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] DownloaderSpec redirect note: ${e.message}")
        }

        println("[Media Usability & Watermark-Free Downloader] Applied $patched media usability and watermark-free download hook(s).")
    }
}

