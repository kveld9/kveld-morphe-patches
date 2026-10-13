package app.morphe.patches.tiktok.performance

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

val playbackDisplayGovernorPatch = bytecodePatch(
    name = "Playback & Display Governor",
    description = "Caps playback resolution and dynamic range (SDR ceiling, HDR off) and locks the display refresh rate via per-feature toggles. Download quality is NOT here: it lives in the Media Usability patch (downloadQuality).",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val capPlaybackQuality by booleanOption(
        key = "capPlaybackQuality",
        default = true,
        title = "Cap Playback Resolution",
        description = "Caps video playback resolution to conserve battery, GPU/MediaCodec load, and mobile data.",
        required = false,
    )

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

    val dropUndecodableVideo by booleanOption(
        key = "dropUndecodableVideo",
        default = true,
        title = "Drop Undecodable Video Streams",
        description = "When the ladder's lowest video stream exceeds the device hardware decoder capability (queried via MediaCodecList), drops video streams and keeps audio-only instead of entering decoder-reject retry loops that freeze the device.",
        required = false,
    )

    val disableHdr by booleanOption(
        key = "disableHdr",
        default = true,
        title = "Disable HDR Video Playback",
        description = "Plays standard video instead of HDR. Keeps screen brightness normal.",
        required = false,
    )

    val governRefreshRate by booleanOption(
        key = "governRefreshRate",
        default = true,
        title = "Govern Display Refresh Rate",
        description = "Forces TikTok to run at peak display refresh rate (120Hz/90Hz/60Hz) and neutralizes video playback framerate downclocking routines.",
        required = false,
    )

    val targetRate by stringOption(
        key = "targetRate",
        title = "Target Refresh Rate",
        description = "Select target display refresh rate: max (display peak rate), 120 (120 Hz), 90 (90 Hz), or 60 (60 Hz). Clamped automatically to display maximum.",
        default = "max",
        required = false,
    )

    execute {
        val capQuality = capPlaybackQuality ?: true
        val hdrDisabled = disableHdr ?: true
        val governRate = governRefreshRate ?: true

        if (!capQuality && !hdrDisabled && !governRate) {
            println("[Playback & Display Governor] Skipped: All governor features disabled.")
            return@execute
        }

        var patched = 0

        if (capQuality) {
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
            val dropUndecodableEnabled = dropUndecodableVideo ?: true
            val dropUndecodableConst = if (dropUndecodableEnabled) 1 else 0

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
                    const/4 v0, $dropUndecodableConst
                    sput-boolean v0, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->dropUndecodableVideo:Z
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Initialized playback cap: playback=${chosenPlaybackRes}p, avoidByteVC2=$avoidByteVC2Enabled, dropUndecodable=$dropUndecodableEnabled (download ceiling retired).")
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
                println("[Playback & Display Governor] Hooked Aweme.getVideo() (${returnIndices.size} return point(s)) -> Video capping active.")
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
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitratesEx(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (videoReturnIndices.isNotEmpty()) {
                println("[Playback & Display Governor] Hooked Video.getBitRate() (${videoReturnIndices.size} return point(s)) -> Bitrate list filter active.")
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
                        invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->filterBitratesEx(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;
                        move-result-object v$reg
                    """.trimIndent(),
                )
            }
            if (rawReturnIndices.isNotEmpty()) {
                println("[Playback & Display Governor] Hooked Video.getRawBitRate() (${rawReturnIndices.size} return point(s)) -> Raw bitrate list filter active.")
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
                println("[Playback & Display Governor] Hooked SimVideoUrlModel.getBitRate() (${simReturnIndices.size} return point(s)) -> PlayerKit bitrate filter active.")
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
                println("[Playback & Display Governor] Hooked SimVideoUrlModel.getRawBitRate() (${simRawReturnIndices.size} return point(s)) -> Detail playback bitrate filter active.")
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
                println("[Playback & Display Governor] Hooked Video.getProperPlayAddr() (${properPlayAddrReturnIndices.size} return point(s)) -> Detail-path play address enforcement active.")
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
                println("[Playback & Display Governor] Hooked Video.getPlayAddr() (${playAddrReturnIndices.size} return point(s)) -> Direct play address enforcement active.")
                patched++
            }

            // 9-11. Hook codec-specific play address getters with the same enforcement.
            // Detail playback of HEVC/ ByteVC1 ladders may read these instead of getPlayAddr().
            val codecPlayAddrMethods = listOf("getPlayAddrBytevc1", "getPlayAddrH264", "getH264PlayAddr")
            for (getterName in codecPlayAddrMethods) {
                val getterFp = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Video;",
                    name = getterName,
                    returnType = "Lcom/ss/android/ugc/aweme/feed/model/VideoUrlModel;",
                )
                val getterMethod = getterFp.method
                val getterReturnIndices = getterMethod.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                getterReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                    getterMethod.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static {v$reg, p0}, ${Constants.TIKTOK_EXTENSION_QUALITY_HOOK}->enforcePlaybackCap(Ljava/lang/Object;Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (getterReturnIndices.isNotEmpty()) {
                    println("[Playback & Display Governor] Hooked Video.$getterName() (${getterReturnIndices.size} return point(s)) -> Codec play address enforcement active.")
                    patched++
                }
            }
        }

        if (hdrDisabled) {
            // Force isForceHdrOff() -> true across all ISimPlayerConfig implementations
            var forceHdrHooks = 0
            classDefForEach { classDef ->
                if (classDef.type == "Lcom/ss/android/ugc/aweme/video/simplayer/PlayerConfigImpl;" ||
                    classDef.interfaces.contains("Lcom/ss/android/ugc/aweme/video/config/ISimPlayerConfig;")
                ) {
                    val mutableClass = mutableClassDefBy(classDef)
                    val method = mutableClass.methods.firstOrNull { it.name == "isForceHdrOff" && it.returnType == "Z" }
                    if (method != null) {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x1
                                return v0
                            """.trimIndent(),
                        )
                        forceHdrHooks++
                    }
                }
            }
            if (forceHdrHooks > 0) {
                println("[Playback & Display Governor] Forced isForceHdrOff() -> true across $forceHdrHooks player config provider(s).")
                patched += forceHdrHooks
            }

            // Force SimVideoUrlModel.isHaveHdr() -> false
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimVideoUrlModel;",
                name = "isHaveHdr",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Forced SimVideoUrlModel.isHaveHdr() -> false.")
            patched++

            // Force SimBitRate.isHdr() -> false
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/playerkit/simapicommon/model/SimBitRate;",
                name = "isHdr",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Forced SimBitRate.isHdr() -> false.")
            patched++
        }

        if (governRate) {
            val rateFloat = when (val trimmed = targetRate?.trim()?.lowercase()) {
                null, "", "max" -> 0.0f
                else -> trimmed.toFloatOrNull() ?: 0.0f
            }
            val rateBits = java.lang.Float.floatToIntBits(rateFloat)

            // 1. Initialize default targetRefreshRate in TikTokRefreshRateHook.<clinit>
            val hookClinitFp = Fingerprint(
                definingClass = Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK,
                name = "<clinit>",
            )
            val method = hookClinitFp.method
            val instructions = method.implementation?.instructions?.toList() ?: emptyList()
            val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            val targetIndex = if (returnIndex >= 0) returnIndex else 0
            method.addInstructions(
                targetIndex,
                """
                    const v0, $rateBits
                    sput v0, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->targetRefreshRate:F
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Initialized targetRefreshRate=${if (rateFloat == 0f) "max" else "${rateFloat.toInt()}Hz"} (hardware bounded).")
            patched++

            // 2. Neutralize PlayerController video framerate downclocking by forcing ui_video_frame_rate_opt
            Fingerprint(
                name = "invoke",
                returnType = "Ljava/lang/Object;",
                strings = listOf("ui_video_frame_rate_opt"),
            ).method.addInstructions(
                0,
                """
                    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    return-object v0
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Forced ui_video_frame_rate_opt -> Boolean.TRUE (bypasses PlayerController downclocking safely without aborting onRenderFirstFrame).")
            patched++

            // 2.2 Neutralize setRefreshRateIfNeeded
            Fingerprint(
                name = "invoke",
                returnType = "Ljava/lang/Object;",
                strings = listOf("setRefreshRateIfNeeded preferredRefreshRate:"),
            ).method.addInstructions(
                0,
                """
                    sget-object v0, Lkotlin/Unit;->LIZ:Lkotlin/Unit;
                    return-object v0
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Neutralized setRefreshRateIfNeeded -> forced Unit.LIZ.")
            patched++

            // 3. Override window refresh rate reset when drag stops
            Fingerprint(
                name = "LIZ",
                returnType = "V",
                strings = listOf("set dragging rate "),
            ).method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                    return-void
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Overrode drag rate reset -> enforced target rate on drag release.")
            patched++

            // 4. Override RefreshFrequencyTutor
            val tutorLIZfp = Fingerprint(
                name = "LIZ",
                parameters = listOf("Landroid/app/Activity;"),
                returnType = "V",
                strings = listOf("restore to default"),
            )
            tutorLIZfp.method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                    return-void
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Overrode RefreshFrequencyTutor.restore -> enforced target rate.")
            patched++

            Fingerprint(
                definingClass = tutorLIZfp.method.definingClass,
                name = "LIZIZ",
                parameters = listOf("Landroid/app/Activity;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                    return-void
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Overrode RefreshFrequencyTutor.set -> enforced target rate.")
            patched++

            // 5. Lock refresh rate in MainActivity.onResume()
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
                name = "onResume",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Hooked MainActivity.onResume() -> enforced target rate.")
            patched++

            // 6. Lock refresh rate in MainActivity.onWindowFocusChanged(Z)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
                name = "onWindowFocusChanged",
                parameters = listOf("Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    if-eqz p1, :skip_refresh
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_REFRESH_RATE_HOOK}->applyToWindow(Landroid/app/Activity;)V
                    :skip_refresh
                """.trimIndent(),
            )
            println("[Playback & Display Governor] Hooked MainActivity.onWindowFocusChanged() -> enforced target rate on focus gain.")
            patched++
        }

        if (patched == 0) {
            println("[Playback & Display Governor] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Playback & Display Governor] Applied $patched hook(s) across active playback and display governor feature(s).")
        }
    }
}
