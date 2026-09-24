package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val videoQualityGovernorPatch = bytecodePatch(
    name = "Video Quality Governor",
    description = "Caps video playback and download resolutions (1080p, 720p, 540p, 480p, 360p) independently to conserve battery, GPU/MediaCodec load, and mobile data.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    val maxQuality by stringOption(
        key = "maxQuality",
        title = "Maximum Playback Resolution",
        description = "Select maximum video playback resolution ceiling: 1080 (1080p ExtremelyHigh), 720 (720p SuperHigh), 540 (540p H_High), 480 (480p High), or 360 (360p Standard).",
        default = "480",
        required = false,
    )

    val maxDownloadQuality by stringOption(
        key = "maxDownloadQuality",
        title = "Maximum Download Resolution",
        description = "Select maximum video download resolution ceiling: 1080 (1080p Maximum Quality), 720 (720p SuperHigh), 540 (540p H_High), 480 (480p High), or 360 (360p Standard).",
        default = "1080",
        required = false,
    )

    execute {
        fun parseResolution(raw: String?, defaultRes: Int): Int {
            val q = raw?.trim()?.lowercase() ?: return defaultRes
            return when {
                q.contains("1080") -> 1080
                q.contains("720") -> 720
                q.contains("540") -> 540
                q.contains("360") -> 360
                q.contains("480") -> 480
                else -> defaultRes
            }
        }

        val chosenPlaybackRes = parseResolution(maxQuality, 480)
        val chosenDownloadRes = parseResolution(maxDownloadQuality, 1080)

        var patched = 0

        // 1. Initialize default maxAllowedResolution & downloadAllowedResolution in TikTokVideoQualityHook.<clinit>
        try {
            val hookClinitFp = Fingerprint(
                definingClass = Constants.TIKTOK_EXTENSION_QUALITY_HOOK,
                name = "<clinit>",
            )
            hookClinitFp.method.addInstructions(
                0,
                """
                    const/16 v0, $chosenPlaybackRes
                    sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->maxAllowedResolution:I
                    const/16 v0, $chosenDownloadRes
                    sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->downloadAllowedResolution:I
                """.trimIndent(),
            )
            println("[Video Quality Governor] Initialized default caps: playback=${chosenPlaybackRes}p, download=${chosenDownloadRes}p.")
            patched++
        } catch (e: Exception) {
            println("[Video Quality Governor] TikTokVideoQualityHook.<clinit> note: ${e.message}")
        }

        // 2. Hook Aweme.getVideo() return points to cap Video model and default play addresses
        try {
            val awemeGetVideoFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getVideo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            )
            val method = awemeGetVideoFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->capVideoObject(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked Aweme.getVideo() (${returnIndices.size} return point(s)) -> Video capping active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] Aweme.getVideo note: ${e.message}")
        }

        // 3. Hook Video.getBitRate() to filter bitrates
        try {
            val videoGetBitrateFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
                name = "getBitRate",
                returnType = "Ljava/util/List;",
            )
            val method = videoGetBitrateFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked Video.getBitRate() (${returnIndices.size} return point(s)) -> Bitrate list filter active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] Video.getBitRate note: ${e.message}")
        }

        // 4. Hook Video.getRawBitRate() to filter raw bitrates
        try {
            val videoGetRawBitrateFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
                name = "getRawBitRate",
                returnType = "Ljava/util/List;",
            )
            val method = videoGetRawBitrateFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked Video.getRawBitRate() (${returnIndices.size} return point(s)) -> Raw bitrate list filter active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] Video.getRawBitRate note: ${e.message}")
        }

        // 5. Hook SimVideoUrlModel.getBitRate() for PlayerKit playback engine
        try {
            val simGetBitrateFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimVideoUrlModel;",
                name = "getBitRate",
                returnType = "Ljava/util/List;",
            )
            val method = simGetBitrateFp.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked SimVideoUrlModel.getBitRate() (${returnIndices.size} return point(s)) -> PlayerKit bitrate filter active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] SimVideoUrlModel.getBitRate note: ${e.message}")
        }

        // 6. Hook Video.getDownloadNoWatermarkAddr() to enforce download resolution ceiling
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
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforceDownloadCap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$reg
                        check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked Video.getDownloadNoWatermarkAddr() (${returnIndices.size} return point(s)) -> Download cap active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] Video.getDownloadNoWatermarkAddr note: ${e.message}")
        }

        // 7. Hook Video.getDownloadAddr() to enforce download resolution ceiling
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
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforceDownloadCap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$reg
                        check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Video Quality Governor] Hooked Video.getDownloadAddr() (${returnIndices.size} return point(s)) -> Download cap active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Video Quality Governor] Video.getDownloadAddr note: ${e.message}")
        }

        println("[Video Quality Governor] Applied $patched video resolution capping hook(s) (playback: ${chosenPlaybackRes}p, download: ${chosenDownloadRes}p).")
    }
}
