package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val resourceGovernorPatch = bytecodePatch(
    name = "Resource & Battery Governor",
    description = "Throttles background sensor polling (gyroscope/accelerometer 3D ads), prevents aggressive video buffer preloading, and caps Fresco animated image memory retention to conserve battery and RAM.",
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

        // ==========================================
        // 3. RUNTIME MEMORY & BITMAP CACHE
        // ==========================================

        // 3.1 FrescoFrameCache frame getter (LIZJ in v46.9.3) -> return null
        try {
            Fingerprint(
                definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                name = "LIZJ",
                custom = { method, _ -> method.returnType.startsWith("L") },
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Resource Governor] Capped FrescoFrameCache.LIZJ() -> Frame bitmap memory retention bounded.")
            patched++
        } catch (e: Exception) {
            println("[Resource Governor] FrescoFrameCache.LIZJ note: ${e.message}")
        }

        // 3.2 FrescoFrameCache parameterized frame getter (LJFF in v46.9.3) -> return null
        try {
            Fingerprint(
                definingClass = "Lcom/facebook/fresco/animation/bitmap/cache/FrescoFrameCache;",
                name = "LJFF",
                custom = { method, _ -> method.returnType.startsWith("L") },
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Resource Governor] Capped FrescoFrameCache.LJFF() -> Frame cache allocation suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Resource Governor] FrescoFrameCache.LJFF note: ${e.message}")
        }

        println("[Resource & Battery Governor] Applied $patched hardware, network, and memory governor hook(s).")
    }
}
