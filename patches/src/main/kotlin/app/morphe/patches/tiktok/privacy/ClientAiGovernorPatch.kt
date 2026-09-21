package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val clientAiGovernorPatch = bytecodePatch(
    name = "Client-Side AI & Behavioral Profiling Governor",
    description = "Neutralizes on-device machine learning inference (Pitaya), Tako AI chatbot entry points and icons, and AI smart search suggestion clutter.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize PitayaBootLoader setup
        try {
            val setupFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
                name = "setup",
                returnType = "V",
            )
            setupFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] PitayaBootLoader.setup note: ${e.message}")
        }

        // 2. Neutralize PitayaBootLoader.commitBootTaskBySettings
        try {
            val commitFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
                name = "commitBootTaskBySettings",
                returnType = "V",
            )
            commitFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] commitBootTaskBySettings note: ${e.message}")
        }

        // 3. Neutralize PitayaBootLoader$BootTask.run
        try {
            val bootTaskRunFp = Fingerprint(
                definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader\$BootTask;",
                name = "run",
                returnType = "V",
            )
            bootTaskRunFp.method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] BootTask.run note: ${e.message}")
        }

        // 4. Neutralize TakoLaunchServiceImpl.LJ() -> globally disables Tako launch
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/TakoLaunchServiceImpl;",
                name = "LJ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoLaunchServiceImpl.LJ note: ${e.message}")
        }

        // 5. Neutralize TakoTrigger methods returning boolean (Kr, Mr, yr, k9) -> suppresses Tako button in feed interact area
        listOf("Kr", "Mr", "yr", "k9").forEach { methodName ->
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoTrigger;",
                    name = methodName,
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                    const/4 v0, 0
                    return v0
                    """.trimIndent()
                )
                patched++
            } catch (e: Exception) {
                println("[Client-Side AI & Behavioral Profiling Governor] TakoTrigger.$methodName note: ${e.message}")
            }
        }

        // 6. Neutralize TakoTriggerRoof methods returning boolean (Kr, Mr, yr) -> suppresses Tako roof trigger in feed
        listOf("Kr", "Mr", "yr").forEach { methodName ->
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoTriggerRoof;",
                    name = methodName,
                    returnType = "Z",
                ).method.addInstructions(
                    0,
                    """
                    const/4 v0, 0
                    return v0
                    """.trimIndent()
                )
                patched++
            } catch (e: Exception) {
                println("[Client-Side AI & Behavioral Profiling Governor] TakoTriggerRoof.$methodName note: ${e.message}")
            }
        }

        // 7. Neutralize TakoServiceImpl.LJLLJ() -> blocks feed right-bottom entrance assem creation
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/TakoServiceImpl;",
                name = "LJLLJ",
                returnType = "Lcom/bytedance/assem/arch/reused/ReusedUIAssem;",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoServiceImpl.LJLLJ note: ${e.message}")
        }

        // 8. Neutralize TakoRightBottomEntranceTrigger.Kr() -> blocks right-bottom entrance trigger
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/otherpage/feed/mainentrance/ui/TakoRightBottomEntranceTrigger;",
                name = "Kr",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoRightBottomEntranceTrigger.Kr note: ${e.message}")
        }

        // 11. Neutralize TakoRouterServiceImpl.LIZ() & LIZIZ() -> neutralizes any routing to Tako chat
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/common/router/TakoRouterServiceImpl;",
                name = "LIZ",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoRouterServiceImpl.LIZ note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/common/router/TakoRouterServiceImpl;",
                name = "LIZIZ",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                return-void
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoRouterServiceImpl.LIZIZ note: ${e.message}")
        }

        // 12. Neutralize TakoFeedIconServiceImpl.LIZIZ() -> suppresses Tako floating top icon in feed
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/feed/topicon/TakoFeedIconServiceImpl;",
                name = "LIZIZ",
                parameters = emptyList(),
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoFeedIconServiceImpl.LIZIZ note: ${e.message}")
        }

        // 13. Neutralize TakoCommentTopBarServiceImpl.canShow() -> suppresses Tako bar in comments
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/tako/detail/related/TakoCommentTopBarServiceImpl;",
                name = "canShow",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] TakoCommentTopBarServiceImpl.canShow note: ${e.message}")
        }

        // 14. Neutralize SearchMixFeed.isTako() -> suppresses Tako cards in search results
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeed;",
                name = "isTako",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchMixFeed.isTako note: ${e.message}")
        }

        // 15. Neutralize SearchMixFeed.getBot() -> prevents Tako bot model binding in search
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeed;",
                name = "getBot",
                returnType = "Lcom/ss/android/ugc/aweme/search/pages/result/bot/model/TakoInfo;",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchMixFeed.getBot note: ${e.message}")
        }

        // 16. Neutralize SearchMixFeed.getAiAdCard() -> suppresses AI commerce ad cards
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeed;",
                name = "getAiAdCard",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/search/AIAdCardStruct;",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return-object v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchMixFeed.getAiAdCard note: ${e.message}")
        }

        // 17. Neutralize SearchTakoSugListAssem.Nb() -> suppresses Tako suggestion list cards
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchTakoSugListAssem;",
                name = "Nb",
                parameters = listOf("Ljava/lang/Object;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchTakoSugListAssem.Nb note: ${e.message}")
        }

        // 18. Neutralize SearchTakoCardProtocol.Lr2() -> suppresses legacy Tako search card matching
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/bot/component/SearchTakoCardProtocol;",
                name = "Lr2",
                returnType = "Z",
                custom = { method, _ -> method.parameterTypes.size == 1 },
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchTakoCardProtocol.Lr2 note: ${e.message}")
        }

        // 19. Neutralize SearchTakoNewBotCardProtocol.Lr2() -> suppresses new Tako bot search card matching
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/tako/newbot/SearchTakoNewBotCardProtocol;",
                name = "Lr2",
                returnType = "Z",
                custom = { method, _ -> method.parameterTypes.size == 1 },
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchTakoNewBotCardProtocol.Lr2 note: ${e.message}")
        }

        // 20. Neutralize SearchAdAISummaryCardProtocol.Lr2() -> suppresses AI summary cards in search
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/commerce/aisummarycard/SearchAdAISummaryCardProtocol;",
                name = "Lr2",
                returnType = "Z",
                custom = { method, _ -> method.parameterTypes.size == 1 },
            ).method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Client-Side AI & Behavioral Profiling Governor] SearchAdAISummaryCardProtocol.Lr2 note: ${e.message}")
        }

        println("[Client-Side AI & Behavioral Profiling Governor] Applied $patched client-side AI, Tako, and search clutter governors.")
    }
}
