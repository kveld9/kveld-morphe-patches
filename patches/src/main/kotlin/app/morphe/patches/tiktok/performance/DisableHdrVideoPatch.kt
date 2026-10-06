package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disableHdrVideoPatch = bytecodePatch(
    name = "Disable HDR Video Playback",
    description = "Forces the video playback engine to select standard SDR bitrates (BT.709/sRGB) instead of HDR (HDR10/PQ/HLG), preventing blinding screen brightness spikes and display thermal throttling while preserving smooth playback.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Force isForceHdrOff() -> true across all ISimPlayerConfig implementations
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
            println("[Disable HDR Video Playback] Forced isForceHdrOff() -> true across $forceHdrHooks player config provider(s).")
            patched += forceHdrHooks
        }

        // 2. Force SimVideoUrlModel.isHaveHdr() -> false
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
        println("[Disable HDR Video Playback] Forced SimVideoUrlModel.isHaveHdr() -> false.")
        patched++

        // 3. Force SimBitRate.isHdr() -> false
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
        println("[Disable HDR Video Playback] Forced SimBitRate.isHdr() -> false.")
        patched++

        println("[Disable HDR Video Playback] Applied $patched HDR suppression hook(s) -> SDR playback enforced.")
    }
}
