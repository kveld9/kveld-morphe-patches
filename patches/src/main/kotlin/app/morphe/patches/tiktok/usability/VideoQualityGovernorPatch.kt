package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val videoQualityGovernorPatch = bytecodePatch(
    name = "Video Quality Governor",
    description = "Caps video playback resolution (1080p, 720p, 540p, 480p, 360p) to conserve battery, GPU/MediaCodec load, and mobile data. Download quality is controlled separately by the Media Usability patch (downloadQuality).",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val maxQuality by stringOption(
        key = "maxQuality",
        title = "Maximum Playback Resolution",
        description = "Select maximum video playback resolution ceiling: 1080 (1080p ExtremelyHigh), 720 (720p SuperHigh), 540 (540p H_High), 480 (480p High), or 360 (360p Standard).",
        default = "480",
        required = false,
    )

    val avoidByteVC2 by booleanOption(
        key = "avoidByteVC2",
        default = true,
        title = "Avoid ByteVC2 Software Decoding",
        description = "Drops ByteVC2 renditions from playback bitrate lists when an H.264 or ByteVC1 alternative exists, so videos use the hardware decoder instead of TikTok's CPU-bound ByteVC2 software decoder.",
        required = false,
    )
    execute {
        fun parseResolution(raw: String?, defaultRes: Int): Int {
            val q = raw?.trim()?.lowercase() ?: return defaultRes
            return when {
                q.contains("none") || q.contains("unconstrained") || q == "0" -> 0
                q.contains("1080") -> 1080
                q.contains("720") -> 720
                q.contains("540") -> 540
                q.contains("360") -> 360
                q.contains("480") -> 480
                else -> defaultRes
            }
        }

        val chosenPlaybackRes = parseResolution(maxQuality, 480)

        var patched = 0

        // 1. Initialize default maxAllowedResolution in TikTokVideoQualityHook.<clinit> and
        // retire the legacy download ceiling (download quality lives in Media Usability now).
        val hookClinitFp = Fingerprint(
            definingClass = Constants.TIKTOK_EXTENSION_QUALITY_HOOK,
            name = "<clinit>",
        )
        val hookClinit = hookClinitFp.method
        hookClinit.ensureRegisterCount(2)
        val clinitInstructions = hookClinit.implementation!!.instructions
        val returnIdx = clinitInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        val insertIdx = if (returnIdx != -1) returnIdx else 0

        val avoidByteVC2Enabled = avoidByteVC2 ?: true
        val avoidByteVC2Const = if (avoidByteVC2Enabled) 1 else 0
        hookClinit.addInstructions(
            insertIdx,
            """
                const/4 v0, 1
                sput-boolean v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->isGovernorEnabled:Z
                const/16 v0, $chosenPlaybackRes
                sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->maxAllowedResolution:I
                const/4 v0, 0
                sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->downloadAllowedResolution:I
                const/4 v0, $avoidByteVC2Const
                sput-boolean v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->avoidByteVC2:Z
            """.trimIndent(),
        )
        println("[Video Quality Governor] Initialized playback cap: playback=${chosenPlaybackRes}p, avoidByteVC2=$avoidByteVC2Enabled (download ceiling retired).")
        patched++

        // 2. Hook Aweme.getVideo() return points to cap Video model and default play addresses
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
            method.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->capVideoObject(Ljava/lang/Object;Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (returnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Aweme.getVideo() (${returnIndices.size} return point(s)) -> Video capping active.")
            patched++
        }

        // 3. Hook Video.getBitRate() to filter bitrates
        val videoGetBitrateFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getBitRate",
            returnType = "Ljava/util/List;",
        )
        val videoMethod = videoGetBitrateFp.method
        val videoReturnIndices = videoMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        videoReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            videoMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$reg
                """.trimIndent(),
            )
        }
        if (videoReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getBitRate() (${videoReturnIndices.size} return point(s)) -> Bitrate list filter active.")
            patched++
        }

        // 4. Hook Video.getRawBitRate() to filter raw bitrates
        val videoGetRawBitrateFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getRawBitRate",
            returnType = "Ljava/util/List;",
        )
        val rawMethod = videoGetRawBitrateFp.method
        val rawReturnIndices = rawMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        rawReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            rawMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$reg
                """.trimIndent(),
            )
        }
        if (rawReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getRawBitRate() (${rawReturnIndices.size} return point(s)) -> Raw bitrate list filter active.")
            patched++
        }

        // 5. Hook SimVideoUrlModel.getBitRate() for PlayerKit playback engine
        val simGetBitrateFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimVideoUrlModel;",
            name = "getBitRate",
            returnType = "Ljava/util/List;",
        )
        val simMethod = simGetBitrateFp.method
        val simReturnIndices = simMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        simReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            simMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$reg
                """.trimIndent(),
            )
        }
        if (simReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked SimVideoUrlModel.getBitRate() (${simReturnIndices.size} return point(s)) -> PlayerKit bitrate filter active.")
            patched++
        }

        // 6. Hook SimVideoUrlModel.getRawBitRate() for detail-page PlayerKit playback engine
        val simGetRawBitrateFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimVideoUrlModel;",
            name = "getRawBitRate",
            returnType = "Ljava/util/List;",
        )
        val simRawMethod = simGetRawBitrateFp.method
        val simRawReturnIndices = simRawMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        simRawReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            simRawMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitrates(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$reg
                """.trimIndent(),
            )
        }
        if (simRawReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked SimVideoUrlModel.getRawBitRate() (${simRawReturnIndices.size} return point(s)) -> Detail playback bitrate filter active.")
            patched++
        }

        // 7. Hook Video.getProperPlayAddr() return points to enforce capped play address.
        // Covers direct URL reads on every path (feed, detail, search, profile) even when
        // Aweme.getVideo() is bypassed by the detail page.
        val videoGetProperPlayAddrFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getProperPlayAddr",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/VideoUrlModel;",
        )
        val properPlayAddrMethod = videoGetProperPlayAddrFp.method
        val properPlayAddrReturnIndices = properPlayAddrMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        properPlayAddrReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            properPlayAddrMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforcePlaybackCap(Ljava/lang/Object;Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (properPlayAddrReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getProperPlayAddr() (${properPlayAddrReturnIndices.size} return point(s)) -> Detail-path play address enforcement active.")
            patched++
        }

        // 8. Hook Video.getPlayAddr() return points with the same enforcement.
        val videoGetPlayAddrFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getPlayAddr",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/VideoUrlModel;",
        )
        val playAddrMethod = videoGetPlayAddrFp.method
        val playAddrReturnIndices = playAddrMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        playAddrReturnIndices.asReversed().forEach { (returnIndex, reg) ->
            playAddrMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforcePlaybackCap(Ljava/lang/Object;Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (playAddrReturnIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getPlayAddr() (${playAddrReturnIndices.size} return point(s)) -> Direct play address enforcement active.")
            patched++
        }

        println("[Video Quality Governor] Applied $patched video resolution capping hook(s) (playback: ${chosenPlaybackRes}p).")
    }
}
