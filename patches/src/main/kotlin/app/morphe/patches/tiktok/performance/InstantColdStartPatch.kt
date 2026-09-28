package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid

val instantColdStartPatch = bytecodePatch(
    name = "Instant Launch & Splash Blocker",
    description = "Eliminates cold startup delays, background resume splash advertisements, real-time splash requests, and TopView ad preloading.",
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
        ).method.replaceWithReturnVoid()
        println("[InstantColdStart] Neutralized SplashAdManagerPreloadTask.run() -> Splash preloading delay removed.")
        patched++

        // 2. Neutralize SplashAdManagerPreloadTaskEntry
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/SplashAdManagerPreloadTaskEntry;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[InstantColdStart] Neutralized SplashAdManagerPreloadTaskEntry.run().")
        patched++

        // 3. Neutralize TopViewPreloadTask.run()
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/topview/TopViewPreloadTask;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[InstantColdStart] Neutralized TopViewPreloadTask.run() -> Background TopView video caching disabled.")
        patched++

        // 4. Neutralize TopViewPreloadJsonTask.run()
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/topview/TopViewPreloadJsonTask;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[InstantColdStart] Neutralized TopViewPreloadJsonTask.run() -> TopView JSON preloading disabled.")
        patched++

        // 5. Neutralize RealTimeSplashTask.run()
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/topview/RealTimeSplashTask;",
            name = "run",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        println("[InstantColdStart] Neutralized RealTimeSplashTask.run() -> Real-time splash background tasks disabled.")
        patched++

        // 6. Force enable_force_skip_topview in SplashSettingServiceImpl.LIZ()
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashSettingServiceImpl;",
            name = "LIZ",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(true)
        println("[InstantColdStart] Forced SplashSettingServiceImpl.LIZ() -> Force skip TopView enabled.")
        patched++

        // 7. Disable RealTimeSplashManagerImpl real-time splash gate (LIZLLL in v47.1.3)
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/realtimesplash/RealTimeSplashManagerImpl;",
            name = "LIZLLL",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        println("[InstantColdStart] Disabled RealTimeSplashManagerImpl splash gate -> Real-time splash execution disabled.")
        patched++

        // 8. Disable SplashAdServiceImpl methods (LJ, LJIILIIL, LJJIFFI, LJJIJIIJIL in v47.1.3)
        for (mName in listOf("LJ", "LJIILIIL", "LJJIFFI", "LJJIJIIJIL")) {
            Fingerprint(
                definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
                name = mName,
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[InstantColdStart] Disabled SplashAdServiceImpl.$mName() -> Splash ad service disabled.")
            patched++
        }

        // 9. Neutralize SplashAdServiceImpl.LJIILJJIL() -> Returns null Aweme for TopView
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/core/SplashAdServiceImpl;",
            name = "LJIILJJIL",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        ).method.replaceWithReturnNull()
        println("[InstantColdStart] Neutralized SplashAdServiceImpl.LJIILJJIL() -> TopView Aweme retrieval returns null.")
        patched++

        // 10. Neutralize TopViewJsonManager.LIZIZ() -> Returns null Aweme
        Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/topview/TopViewJsonManager;",
            name = "LIZIZ",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        ).method.replaceWithReturnNull()
        println("[InstantColdStart] Neutralized TopViewJsonManager.LIZIZ() -> TopView cache parsed Aweme returns null.")
        patched++

        // 11. Disable SplashAdManager global splash check (LX/03fN.LJFF() in v47.1.3)
        Fingerprint(
            definingClass = "LX/03fN;",
            name = "LJFF",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        println("[InstantColdStart] Disabled LX/03fN.LJFF() -> Global splash enable flag neutralized.")
        patched++

        // 12. Disable SplashAdShowManager (LX/05W1.LJI() in v47.1.3) - blocks both cold (1) and warm (2) splash
        Fingerprint(
            definingClass = "LX/05W1;",
            name = "LJI",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        println("[InstantColdStart] Disabled LX/05W1.LJI() -> Cold and warm background resume splash show blocked.")
        patched++

        // 13. Disable CommercializeSplashManager dispatch (LX/03nI.LJFF() in v47.1.3)
        Fingerprint(
            definingClass = "LX/03nI;",
            name = "LJFF",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        println("[InstantColdStart] Disabled LX/03nI.LJFF() -> Resume activity splash trigger neutralized.")
        patched++

        // 14. Neutralize TopView Feed Inserter (LX/07nN.LJIJJ() in v47.1.3)
        Fingerprint(
            definingClass = "LX/07nN;",
            name = "LJIJJ",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        println("[InstantColdStart] Disabled LX/07nN.LJIJJ() -> FeedRecommendFragment TopView insertion blocked.")
        patched++

        // 15. Fail-safe instant finish for NormalSplashAdActivity
        val normalSplashOnCreate = Fingerprint(
            definingClass = "Lcom/bytedance/ies/ugc/aweme/commercialize/splash/show/NormalSplashAdActivity;",
            name = "onCreate",
            returnType = "V",
        ).method
        normalSplashOnCreate.ensureRegisterCount(2)
        normalSplashOnCreate.addInstructions(
            0,
            """
                invoke-super {p0, p1}, Lcom/bytedance/ies/foundation/activity/BaseActivity;->onCreate(Landroid/os/Bundle;)V
                invoke-virtual {p0}, Lcom/bytedance/ies/ugc/aweme/commercialize/splash/show/NormalSplashAdActivity;->finish()V
                return-void
            """.trimIndent(),
        )
        println("[InstantColdStart] Injected instant finish into NormalSplashAdActivity.onCreate().")
        patched++

        println("[InstantColdStart] Applied $patched startup optimizations -> Instant cold/warm start and zero splash/TopView ads achieved.")
    }
}
