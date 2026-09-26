package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val instantColdStartPatch = bytecodePatch(
    name = "Instant Launch & Splash Blocker",
    description = "Eliminates cold startup delays, real-time splash advertisements, and background TopView ad preloading.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize SplashAdManagerPreloadTask.run()
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

        // 2. Neutralize SplashAdManagerPreloadTaskEntry
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

        // 3. Disable SplashSettingServiceImpl.LIZ() & LIZIZ()
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

        // 4. Disable RealTimeSplashManagerImpl real-time splash gate (LIZLLL in v47.1.3)
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
            name = "LIZLLL",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[InstantColdStart] Disabled RealTimeSplashManagerImpl splash gate -> Real-time splash execution disabled.")
        patched++

        // 5. Disable SplashAdServiceImpl methods (LJ, LJIILIIL, LJJIJIIJIL in v47.1.3)
        for (mName in listOf("LJ", "LJIILIIL", "LJJIJIIJIL")) {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                name = mName,
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[InstantColdStart] Disabled SplashAdServiceImpl.$mName() -> Splash ad service disabled.")
            patched++
        }

        println("[InstantColdStart] Applied $patched startup optimizations -> Instant cold start and zero splash ads achieved.")
    }
}
