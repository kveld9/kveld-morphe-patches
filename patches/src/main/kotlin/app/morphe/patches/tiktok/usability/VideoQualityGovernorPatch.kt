package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val videoQualityGovernorPatch = bytecodePatch(
    name = "Video Quality Governor",
    description = "Caps video playback and download resolutions (1080p, 720p, 540p, 480p, 360p) independently to conserve battery, GPU/MediaCodec load, and mobile data.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
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
        val chosenDownloadRes = parseResolution(maxDownloadQuality, 1080)

        var patched = 0

        // 1. Initialize default maxAllowedResolution & downloadAllowedResolution in TikTokVideoQualityHook.<clinit>
        val hookClinitFp = Fingerprint(
            definingClass = Constants.TIKTOK_EXTENSION_QUALITY_HOOK,
            name = "<clinit>",
        )
        val hookClinit = hookClinitFp.method
        hookClinit.ensureRegisterCount(2)
        val clinitInstructions = hookClinit.implementation!!.instructions
        val returnIdx = clinitInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        val insertIdx = if (returnIdx != -1) returnIdx else 0

        hookClinit.addInstructions(
            insertIdx,
            """
                const/4 v0, 1
                sput-boolean v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->isGovernorEnabled:Z
                const/16 v0, $chosenPlaybackRes
                sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->maxAllowedResolution:I
                const/16 v0, $chosenDownloadRes
                sput v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->downloadAllowedResolution:I
            """.trimIndent(),
        )
        println("[Video Quality Governor] Initialized default caps: playback=${chosenPlaybackRes}p, download=${chosenDownloadRes}p.")
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
            method.addInstructions(
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
            videoMethod.addInstructions(
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
            rawMethod.addInstructions(
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
            simMethod.addInstructions(
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

        // 6. Hook Video.getDownloadNoWatermarkAddr() to enforce download resolution ceiling
        val downloadNoWrmkFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getDownloadNoWatermarkAddr",
            returnType = "Lcom/ss/android/ugc/aweme/base/model/UrlModel;",
        )
        val downloadNoWrmkMethod = downloadNoWrmkFp.method
        val downloadNoWrmkIndices = downloadNoWrmkMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        downloadNoWrmkIndices.asReversed().forEach { (returnIndex, reg) ->
            downloadNoWrmkMethod.addInstructions(
                returnIndex,
                """
                    invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforceDownloadCap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$reg
                    check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                """.trimIndent(),
            )
        }
        if (downloadNoWrmkIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getDownloadNoWatermarkAddr() (${downloadNoWrmkIndices.size} return point(s)) -> Download cap active.")
            patched++
        }

        // 7. Hook Video.getDownloadAddr() to enforce download resolution ceiling
        val downloadAddrFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
            name = "getDownloadAddr",
            returnType = "Lcom/ss/android/ugc/aweme/base/model/UrlModel;",
        )
        val downloadAddrMethod = downloadAddrFp.method
        val downloadAddrIndices = downloadAddrMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        downloadAddrIndices.asReversed().forEach { (returnIndex, reg) ->
            downloadAddrMethod.addInstructions(
                returnIndex,
                """
                    invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforceDownloadCap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$reg
                    check-cast v$reg, Lcom/ss/android/ugc/aweme/base/model/UrlModel;
                """.trimIndent(),
            )
        }
        if (downloadAddrIndices.isNotEmpty()) {
            println("[Video Quality Governor] Hooked Video.getDownloadAddr() (${downloadAddrIndices.size} return point(s)) -> Download cap active.")
            patched++
        }

        println("[Video Quality Governor] Applied $patched video resolution capping hook(s) (playback: ${chosenPlaybackRes}p, download: ${chosenDownloadRes}p).")
    }
}
