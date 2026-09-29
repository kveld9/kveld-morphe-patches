package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disableSearchVideoAutoplayPatch = bytecodePatch(
    name = "Disable Search Video Autoplay",
    description = "Disables automatic video playback in search results. Videos only play when tapped to view in detail.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook SearchListAutoplayHelper check logic loop to suppress background evaluation of autoplay candidates
        val searchListAutoplayFp = Fingerprint(
            returnType = "V",
            strings = listOf("checkLogic() is not called on main thread"),
        )
        searchListAutoplayFp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        patched++

        // 2. Hook SearchCardVideoPlayerAssem$autoPlayAbility$2$1 to disable card autoplay ability and playback triggers
        val videoCardAbilityClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchCardVideoPlayerAssem${'$'}autoPlayAbility${'$'}2${'$'}1;"

        val videoCardL2Fp = Fingerprint(
            definingClass = videoCardAbilityClass,
            name = "l2",
            returnType = "Z",
        )
        videoCardL2Fp.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        patched++

        val videoCardRFp = Fingerprint(
            definingClass = videoCardAbilityClass,
            name = "r",
            returnType = "V",
        )
        videoCardRFp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        patched++

        // 3. Hook SearchVideoForLynx$ability$1 to disable Lynx search video autoplay ability and playback triggers
        val lynxVideoAbilityClass = "Lcom/ss/android/ugc/aweme/search/lynx/xsearch/searchvideo/core/ui/SearchVideoForLynx${'$'}ability${'$'}1;"

        val lynxVideoL2Fp = Fingerprint(
            definingClass = lynxVideoAbilityClass,
            name = "l2",
            returnType = "Z",
        )
        lynxVideoL2Fp.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        patched++

        val lynxVideoRFp = Fingerprint(
            definingClass = lynxVideoAbilityClass,
            name = "r",
            returnType = "V",
        )
        lynxVideoRFp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        patched++

        // 4. Hook SearchCardPhotoPlayerAssem$autoPlayAbility$2$1 to disable photo/slideshow search card autoplay
        val photoCardAbilityClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchCardPhotoPlayerAssem${'$'}autoPlayAbility${'$'}2${'$'}1;"

        val photoCardL2Fp = Fingerprint(
            definingClass = photoCardAbilityClass,
            name = "l2",
            returnType = "Z",
        )
        photoCardL2Fp.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        patched++

        val photoCardRFp = Fingerprint(
            definingClass = photoCardAbilityClass,
            name = "r",
            returnType = "V",
        )
        photoCardRFp.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        patched++

        println("[Disable Search Video Autoplay] Applied $patched search autoplay hook(s) -> Search video autoplay disabled.")
    }
}
