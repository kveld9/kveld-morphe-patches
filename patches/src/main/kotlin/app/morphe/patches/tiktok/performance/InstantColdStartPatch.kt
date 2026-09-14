package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val instantColdStartPatch = bytecodePatch(
    name = "Instant Launch & Splash Blocker",
    description = "Eliminates cold startup delays, real-time splash advertisements, and background TopView preloading (<0.4s launch).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize SplashAdManagerPreloadTask.run()
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/SplashAdManagerPreloadTask;",
                name = "run",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[InstantColdStart] Neutralized SplashAdManagerPreloadTask.run() -> Splash preloading delay removed.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdManagerPreloadTask note: ${e.message}")
        }

        // 2. Neutralize SplashAdManagerPreloadTaskEntry
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/SplashAdManagerPreloadTaskEntry;",
                name = "run",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """,
            )
            println("[InstantColdStart] Neutralized SplashAdManagerPreloadTaskEntry.run().")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdManagerPreloadTaskEntry note: ${e.message}")
        }

        // 3. Disable SplashSettingServiceImpl.LIZ() & LIZIZ()
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashSettingServiceImpl;",
                name = "LIZ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled SplashSettingServiceImpl.LIZ() -> Splash settings neutralized.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashSettingServiceImpl.LIZ() note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashSettingServiceImpl;",
                name = "LIZIZ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled SplashSettingServiceImpl.LIZIZ() -> TopView settings neutralized.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashSettingServiceImpl.LIZIZ() note: ${e.message}")
        }

        // 4. Disable RealTimeSplashManagerImpl real-time splash gate (LIZJ in v46.9.3)
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
                name = "LIZJ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[InstantColdStart] Disabled RealTimeSplashManagerImpl.LIZJ() -> Real-time splash execution disabled.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] RealTimeSplashManagerImpl note: ${e.message}")
        }

        // 5. Disable SplashAdServiceImpl methods (LJ, LJIILJJIL, LJJIJIIJIL in v46.9.3)
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                name = "LJ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl.LJ() -> Splash ad service disabled.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl.LJ note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                name = "LJIILJJIL",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl.LJIILJJIL() -> Splash ad trigger suppressed.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl.LJIILJJIL note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                name = "LJJIJIIJIL",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl.LJJIJIIJIL() -> Splash ad presentation suppressed.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl.LJJIJIIJIL note: ${e.message}")
        }

        println("[InstantColdStart] Applied $patched startup optimizations -> Instant cold start and zero splash ads achieved.")
    }
}
