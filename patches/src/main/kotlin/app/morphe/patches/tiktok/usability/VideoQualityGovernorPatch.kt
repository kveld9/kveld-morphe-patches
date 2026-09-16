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
    description = "Caps maximum video playback resolution (1080p, 720p, 540p, 480p, 360p) to reduce GPU/MediaCodec load, lower memory retention, and prevent playback buffering.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    val maxQuality by stringOption(
        key = "maxQuality",
        title = "Maximum Video Resolution",
        description = "Select maximum resolution ceiling: 1080 (1080p ExtremelyHigh), 720 (720p SuperHigh), 540 (540p H_High), 480 (480p High), or 360 (360p Standard).",
        default = "480",
        required = false,
    )

    execute {
        val rawQuality = maxQuality?.trim()?.lowercase() ?: "480"
        val chosenRes = when {
            rawQuality.contains("1080") -> 1080
            rawQuality.contains("720") -> 720
            rawQuality.contains("540") -> 540
            rawQuality.contains("360") -> 360
            else -> 480
        }

        var patched = 0

        // 1. Initialize default maxAllowedResolution in TikTokVideoQualityHook.<clinit>
        try {
            val hookClinitFp = Fingerprint(
                definingClass = Constants.TIKTOK_EXTENSION_QUALITY_HOOK,
                name = "<clinit>",
            )
            hookClinitFp.method.addInstructions(
                0,
                """
                    const/16 v0, $chosenRes
                    sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->maxAllowedResolution:I
                """.trimIndent(),
            )
            println("[Video Quality Governor] Initialized default resolution cap to ${chosenRes}p.")
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

        println("[Video Quality Governor] Applied $patched video resolution capping hook(s) (cap: ${chosenRes}p).")
    }
}
