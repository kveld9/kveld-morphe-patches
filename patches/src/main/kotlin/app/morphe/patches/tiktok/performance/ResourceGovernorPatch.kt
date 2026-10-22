package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val resourceGovernorPatch = bytecodePatch(
    name = "Battery & Resource Saver",
    description = "Stops 3D ad sensors and aggressive video preloading to save battery and data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

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
            println("[Battery & Resource Saver] Neutralized ShakeEggService.LIZ() -> Shake ad sensor triggers disabled.")
            patched++
        } catch (e: Exception) {
            println("[Battery & Resource Saver] ShakeEggService.LIZ note: ${e.message}")
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
            println("[Battery & Resource Saver] Neutralized ShakeEggService.LIZIZ() -> Shake model suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Battery & Resource Saver] ShakeEggService.LIZIZ note: ${e.message}")
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
            println("[Battery & Resource Saver] Hooked PreloadStrategyConfig.isEnableBufferPreload() -> Aggressive buffer preloading throttled.")
            patched++
        } catch (e: Exception) {
            println("[Battery & Resource Saver] PreloadStrategyConfig note: ${e.message}")
        }

        if (patched == 0) {
            println("[Battery & Resource Saver] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Battery & Resource Saver] Applied $patched hardware and network governor hook(s).")
        }
    }
}
