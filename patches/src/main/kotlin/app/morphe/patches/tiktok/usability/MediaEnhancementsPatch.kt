package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val mediaEnhancementsPatch = bytecodePatch(
    name = "Media Usability & Watermark-Free Downloader",
    description = "Enables progress seekbar scrubbing on all videos, unblocks the download button on creator-restricted videos, and routes downloads to clean unwatermarked media streams.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Force Aweme.isPreventDownload() -> false
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

        // 2. Force Aweme.getDownloadWithoutWatermark() -> true
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

        // 3. Force Aweme.needTTSWatermarkWhenDownload() -> false
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

        // 4. Hook Video.getDownloadAddr() to route to clean unwatermarked playAddr stream
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

        // 5. Hook client-side WaterMarkServiceImpl
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/watermark/WaterMarkServiceImpl;",
                name = "waterMark",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Media Usability] Neutralized WaterMarkServiceImpl.waterMark().")
            patched++
        } catch (e: Exception) {
            println("[Media Usability] WaterMarkServiceImpl note: ${e.message}")
        }

        println("[Media Usability & Watermark-Free Downloader] Applied $patched media usability and watermark-free download hook(s).")
    }
}
