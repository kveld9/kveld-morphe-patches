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
        Fingerprint(
            definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
            name = "setup",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
            return-void
            """.trimIndent()
        )
        patched++

        // 2. Neutralize PitayaBootLoader.commitBootTaskBySettings
        Fingerprint(
            definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader;",
            name = "commitBootTaskBySettings",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
            return-void
            """.trimIndent()
        )
        patched++

        // 3. Neutralize PitayaBootLoader$BootTask.run
        Fingerprint(
            definingClass = "Lcom/bytedance/pitaya/api/PitayaBootLoader\$BootTask;",
            name = "run",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
            return-void
            """.trimIndent()
        )
        patched++

        // 4. Neutralize TakoLaunchServiceImpl.LJ() -> globally disables Tako launch
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

        // 5. Neutralize TakoTrigger methods returning boolean (yr, Fr) -> suppresses Tako button in feed interact area
        listOf("yr", "Fr").forEach { methodName ->
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
        }

        // 6. Neutralize TakoTriggerRoof methods returning boolean (yr, Fr) -> suppresses Tako roof trigger in feed
        listOf("yr", "Fr").forEach { methodName ->
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
        }

        // 7. Neutralize TakoServiceImpl -> blocks feed right-bottom entrance assem creation
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/tako/TakoServiceImpl;",
            returnType = "Lcom/bytedance/assem/arch/reused/ReusedUIAssem;",
        ).method.addInstructions(
            0,
            """
            const/4 v0, 0
            return-object v0
            """.trimIndent()
        )
        patched++

        // 8. Neutralize TakoRightBottomEntranceTrigger -> blocks right-bottom entrance trigger
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/tako/otherpage/feed/mainentrance/ui/TakoRightBottomEntranceTrigger;",
            parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
            const/4 v0, 0
            return v0
            """.trimIndent()
        )
        patched++

        // 11. Neutralize TakoRouterServiceImpl.LIZ() & LIZIZ() -> neutralizes any routing to Tako chat
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

        // 12. Neutralize TakoFeedIconServiceImpl.LIZIZ() -> suppresses Tako floating top icon in feed
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

        // 13. Neutralize TakoCommentTopBarServiceImpl.canShow() -> suppresses Tako bar in comments
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

        // 14. Neutralize SearchMixFeed.isTako() -> suppresses Tako cards in search results
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

        // 15. Neutralize SearchMixFeed.getBot() -> prevents Tako bot model binding in search
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

        // 16. Neutralize SearchMixFeed.getAiAdCard() -> suppresses AI commerce ad cards
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

        // 17. Neutralize SearchTakoSugListAssem -> suppresses Tako suggestion list cards
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/SearchTakoSugListAssem;",
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

        // 18. Neutralize SearchTakoCardProtocol -> suppresses legacy Tako search card matching
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/bot/component/SearchTakoCardProtocol;",
            returnType = "Z",
            custom = { method, _ -> method.parameterTypes.size == 1 && method.returnType == "Z" },
        ).method.addInstructions(
            0,
            """
            const/4 v0, 0
            return v0
            """.trimIndent()
        )
        patched++

        // 19. Neutralize SearchTakoNewBotCardProtocol -> suppresses new Tako bot search card matching
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/tako/newbot/SearchTakoNewBotCardProtocol;",
            returnType = "Z",
            custom = { method, _ -> method.parameterTypes.size == 1 && method.returnType == "Z" },
        ).method.addInstructions(
            0,
            """
            const/4 v0, 0
            return v0
            """.trimIndent()
        )
        patched++

        // 20. Neutralize SearchAdAISummaryCardProtocol -> suppresses AI summary cards in search
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/commerce/aisummarycard/SearchAdAISummaryCardProtocol;",
            returnType = "Z",
            custom = { method, _ -> method.parameterTypes.size == 1 && method.returnType == "Z" },
        ).method.addInstructions(
            0,
            """
            const/4 v0, 0
            return v0
            """.trimIndent()
        )
        patched++

        println("[Client-Side AI & Behavioral Profiling Governor] Applied $patched client-side AI, Tako, and search clutter governors.")
    }
}
