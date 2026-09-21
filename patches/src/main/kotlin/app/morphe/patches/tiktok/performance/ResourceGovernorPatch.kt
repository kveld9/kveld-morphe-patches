package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val resourceGovernorPatch = bytecodePatch(
    name = "Resource & Battery Governor",
    description = "Throttles background sensor polling (gyroscope/accelerometer 3D ads) and prevents aggressive video buffer preloading to conserve battery and CPU resources.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // ==========================================
        // 1. HARDWARE SENSORS & 3D ADS
        // ==========================================

        // 1.1 ShakeEggService (interactive shake ad sensors in feed)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ad/feed/shakeEgg/ShakeEggService;",
                name = "LIZ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Resource Governor] Neutralized ShakeEggService.LIZ() -> Shake ad sensor triggers disabled.")
            patched++
        } catch (e: Exception) {
            println("[Resource Governor] ShakeEggService.LIZ note: ${e.message}")
        }

        // 1.2 ShakeEggService.LIZIZ (shake model provider)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ad/feed/shakeEgg/ShakeEggService;",
                name = "LIZIZ",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Resource Governor] Neutralized ShakeEggService.LIZIZ() -> Shake model suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Resource Governor] ShakeEggService.LIZIZ note: ${e.message}")
        }


        // ==========================================
        // 2. NETWORK & BACKGROUND PRELOADING
        // ==========================================

        // 2.1 PreloadStrategyConfig.isEnableBufferPreload() -> return false
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/video/preload/model/PreloadStrategyConfig;",
                name = "isEnableBufferPreload",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Resource Governor] Hooked PreloadStrategyConfig.isEnableBufferPreload() -> Aggressive buffer preloading throttled.")
            patched++
        } catch (e: Exception) {
            println("[Resource Governor] PreloadStrategyConfig note: ${e.message}")
        }

        println("[Resource & Battery Governor] Applied $patched hardware and network governor hook(s).")
    }
}
