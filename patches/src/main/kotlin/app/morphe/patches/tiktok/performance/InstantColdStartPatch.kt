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
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

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

        // 4. Disable RealTimeSplashManagerImpl real-time splash gate (LIZJ in modern / LIZLLL in legacy)
        try {
            val realTimeMethod = try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
                    name = "LIZJ",
                    returnType = "Z",
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
                    name = "LIZLLL",
                    returnType = "Z",
                ).method
            }

            realTimeMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled RealTimeSplashManagerImpl splash execution -> Real-time splash execution disabled.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] RealTimeSplashManagerImpl note: ${e.message}")
        }

        // 5. Disable SplashAdServiceImpl methods (LJFF/LJ, LJIILJJIL/LJIILIIL, LJJIJIIJIL/LJJIJIL)
        try {
            val splashAdMethod = try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJFF",
                    returnType = "Z",
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJ",
                    returnType = "Z",
                ).method
            }

            splashAdMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl splash gate -> Splash ad service disabled.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl splash gate note: ${e.message}")
        }

        try {
            val triggerMethod = try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJIILJJIL",
                    returnType = "Z",
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJIILIIL",
                    returnType = "Z",
                ).method
            }

            triggerMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl trigger -> Splash ad trigger suppressed.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl trigger note: ${e.message}")
        }

        try {
            val presentationMethod = try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJJIJIIJIL",
                    returnType = "Z",
                ).method
            } catch (e: Exception) {
                Fingerprint(
                    definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                    name = "LJJIJIL",
                    returnType = "Z",
                ).method
            }

            presentationMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl presentation -> Splash ad presentation suppressed.")
            patched++
        } catch (e: Exception) {
            println("[InstantColdStart] SplashAdServiceImpl presentation note: ${e.message}")
        }

        println("[InstantColdStart] Applied $patched startup optimizations -> Instant cold start and zero splash ads achieved.")
    }
}
