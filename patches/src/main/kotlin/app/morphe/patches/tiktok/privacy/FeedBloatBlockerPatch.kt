package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.replaceWithReturnEmptyList
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val feedBloatBlockerPatch = bytecodePatch(
    name = "Feed Bloat & Distraction Blocker",
    description = "Removes non-video clutter and floating ad widgets from the For You, Following, and Friends feeds, including Touchpoint Rewards pendants, floating ad stickers, suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), community/topic cards, post-video surveys and evaluation questionnaires, mini-drama paywalls, and in-feed search recommendations/interest cards.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // 1. Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
        try {
            val feedApiFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            )
            val method = feedApiFingerprint.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFeedItemList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Bloat Blocker] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP stream protected from bloat cards.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FeedApiService note: ${e.message}")
        }

        // 2. Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
        try {
            val feedItemListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val method = feedItemListFingerprint.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Bloat Blocker] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> Feed bloat filtered.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FeedItemList.getItems note: ${e.message}")
        }

        // 3. Hook FollowFeedList.getItems() (covers Following feed UI consumers)
        try {
            val followFeedListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val method = followFeedListFingerprint.method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFollowFeedList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Bloat Blocker] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following bloat filtered.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FollowFeedList.getItems note: ${e.message}")
        }

        // 4. Neutralize Lemon8 cross-promotion feed task
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/lemon/card/Lemon8ServiceInitTask;",
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Lemon8ServiceInitTask.run(Context).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Lemon8ServiceInitTask note: ${e.message}")
        }

        // 5. Suppress Touchpoint Rewards & Ad Pendants (floating activity widgets / Issue #33)
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/touchpoint/ui/pendant/SpecActWidget;",
                name = "bind",
                parameters = listOf("Landroid/view/ViewGroup;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Suppressed SpecActWidget.bind(ViewGroup) -> Floating pendant inflation blocked.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] SpecActWidget.bind note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/touchpoint/ui/pendant/SpecActWidget;",
                name = "showOrHidePendant",
                parameters = listOf("Z", "Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Suppressed SpecActWidget.showOrHidePendant(ZZ).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] SpecActWidget.showOrHidePendant note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/touchpoint/ui/pendant/SpecActWidget;",
                name = "showNormalPendant",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Suppressed SpecActWidget.showNormalPendant().")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] SpecActWidget.showNormalPendant note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/touchpoint/serviceimp/FeedPendantService;",
                name = "LIZ",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized FeedPendantService.LIZ(String).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FeedPendantService.LIZ note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/touchpoint/serviceimp/AdPendantService;",
                name = "LIZ",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized AdPendantService.LIZ(String).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] AdPendantService.LIZ note: ${e.message}")
        }

        // 6. Suppress In-Video Floating Bloat, Commercial Stickers & Activity Pendants on Aweme
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getActivityPendant",
                returnType = "Lcom/ss/android/ugc/aweme/commerce/model/CommerceActivityStruct;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getActivityPendant() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getActivityPendant note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getCommerceStickerInfo",
                returnType = "Lcom/ss/android/ugc/aweme/commercialize/model/CommerceStickerInfo;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getCommerceStickerInfo() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getCommerceStickerInfo note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getSpecialSticker",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/SpecialSticker;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getSpecialSticker() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getSpecialSticker note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getFloatingCardInfo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FloatingCardInfo;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getFloatingCardInfo() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getFloatingCardInfo note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getBannerTip",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/BannerTip;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getBannerTip() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getBannerTip note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getStandardComponentInfo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/banner/StandardComponentInfo;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getStandardComponentInfo() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getStandardComponentInfo note: ${e.message}")
        }

        // 7. Neutralize Friends Feed Bloat, Suggested Friend Cards & Recommended Users
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/experiment/FriendsV3RecUserConfig;",
                name = "<init>",
                parameters = listOf("Z", "Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    const/4 p1, 0x0
                    const/4 p2, 0x0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized FriendsV3RecUserConfig.<init>(ZZ) -> Suggested friend card insertions disabled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FriendsV3RecUserConfig note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedResponse;",
                name = "<init>",
                parameters = listOf(
                    "Ljava/util/List;",
                    "Ljava/lang/Boolean;",
                    "Ljava/lang/String;",
                    "Ljava/lang/Boolean;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Lcom/ss/android/ugc/aweme/friendstab/repo/LandingInfo;",
                    "Ljava/util/List;",
                    "I",
                    "I",
                    "Lcom/ss/android/ugc/aweme/feed/model/LogPbBean;",
                ),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsV3Feeds(Ljava/lang/Object;)V
                    const/4 p8, 0x0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Hooked FriendsV3FeedResponse.<init> -> Friends V3 feed filtered and suggested friends nulled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FriendsV3FeedResponse note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
                name = "<init>",
                parameters = listOf(
                    "I",
                    "Z",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Lcom/ss/android/ugc/aweme/feed/model/LogPbBean;",
                    "I",
                    "Ljava/util/List;",
                    "Ljava/util/List;",
                    "Ljava/lang/String;",
                ),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p3}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsFeedData(Ljava/lang/Object;)V
                    const/4 p8, 0x0
                    const/4 p9, 0x0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Hooked FriendsFeedResponse.<init> -> Friends V2 feed filtered and inserted cards nulled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FriendsFeedResponse note: ${e.message}")
        }

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedNetworkSource;",
            name = "LJ",
            parameters = listOf("Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedResponse;"),
            returnType = "Ljava/lang/Object;",
        ).method.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsV3Response(Ljava/lang/Object;)V
            """.trimIndent(),
        )
        println("[Feed Bloat Blocker] Hooked FriendsV3FeedNetworkSource.LJ -> Friends V3 network responses filtered and suggested friends nulled.")
        patched++

        // Hook FriendsFeedApi network fetch return points (LX/06CX;->LIZLLL in v47.1.4)
        val friendsFeedApiFingerprint = Fingerprint(
            definingClass = "LX/06CX;",
            name = "LIZLLL",
            parameters = listOf(
                "I",
                "I",
                "Ljava/lang/String;",
                "Ljava/util/List;",
                "Ljava/util/List;",
                "Ljava/util/List;",
                "Ljava/util/List;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Z",
                "Z",
            ),
            returnType = "Lcom/ss/android/ugc/aweme/friendstab/api/FriendsFeedResponse;",
        )
        val friendsFeedApiMethod = friendsFeedApiFingerprint.method
        val friendsFeedApiReturns = friendsFeedApiMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        friendsFeedApiReturns.asReversed().forEach { (returnIndex, reg) ->
            friendsFeedApiMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterFeedBloatInFriendsFeedResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (friendsFeedApiReturns.isNotEmpty()) {
            println("[Feed Bloat Blocker] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses filtered and inserted cards nulled.")
            patched++
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/recuser/FriendsV3HorizontalRecUserCardCell;",
                name = "onItemViewCreated",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Collapsed FriendsV3HorizontalRecUserCardCell.onItemViewCreated().")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FriendsV3HorizontalRecUserCardCell note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/ui/feed/cell/component/recuser/FriendsV3BottomRecUserListCell;",
                name = "onItemViewCreated",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Collapsed FriendsV3BottomRecUserListCell.onItemViewCreated().")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FriendsV3BottomRecUserListCell note: ${e.message}")
        }

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/v4/RecSwipeCardListAssem;",
            name = "or",
            parameters = listOf("Ljava/util/List;"),
            returnType = "Ljava/util/List;",
        ).method.replaceWithReturnEmptyList()
        println("[Feed Bloat Blocker] Emptied RecSwipeCardListAssem.or() -> Friend suggestion swipe cards suppressed.")
        patched++

        try {
            val bigCardConfigFull = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/RecUserBigCardConfig;",
                name = "<init>",
                parameters = listOf(
                    "I",
                    "I",
                    "J",
                    "J",
                    "J",
                    "I",
                    "I",
                    "I",
                    "Z",
                    "Z",
                    "Lcom/ss/android/ugc/aweme/relation/feed/FYPConfig;",
                    "Lcom/ss/android/ugc/aweme/relation/feed/FriendsTabConfig;",
                    "I",
                    "Lcom/ss/android/ugc/aweme/relation/feed/StackCardConfig;",
                    "Z",
                ),
                returnType = "V",
            ).method
            val fullReturns = bigCardConfigFull.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                ?.map { it.index }
                ?.toList() ?: emptyList()
            fullReturns.asReversed().forEach { returnIndex ->
                bigCardConfigFull.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->disableRecUserBigCardInFriendsTab(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (fullReturns.isNotEmpty()) {
                println("[Feed Bloat Blocker] Disabled RecUserBigCardConfig.<init>(full) -> Friends tab swipe BigCards disabled.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] RecUserBigCardConfig(full) note: ${e.message}")
        }

        try {
            val methodLJIIIIZZ = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/TTRecUserBigCardViewHolder;",
                name = "LJIIIIZZ",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                returnType = "V",
            ).method
            val returnsLJIIIIZZ = methodLJIIIIZZ.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                ?.map { it.index }
                ?.toList() ?: emptyList()
            returnsLJIIIIZZ.asReversed().forEach { returnIndex ->
                methodLJIIIIZZ.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnsLJIIIIZZ.isNotEmpty()) {
                println("[Feed Bloat Blocker] Collapsed TTRecUserBigCardViewHolder.LJIIIIZZ(Aweme).")
                patched++
            } else {
                println("[Feed Bloat Blocker] TTRecUserBigCardViewHolder.LJIIIIZZ note: No RETURN_VOID found")
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] TTRecUserBigCardViewHolder.LJIIIIZZ note: ${e.message}")
        }

        try {
            val methodB1 = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/relation/feed/TTRecUserBigCardViewHolder;",
                name = "B1",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            ).method
            val returnsB1 = methodB1.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_VOID }
                ?.map { it.index }
                ?.toList() ?: emptyList()
            returnsB1.asReversed().forEach { returnIndex ->
                methodB1.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->collapseRecUserCardCell(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnsB1.isNotEmpty()) {
                println("[Feed Bloat Blocker] Collapsed TTRecUserBigCardViewHolder.B1(View).")
                patched++
            } else {
                println("[Feed Bloat Blocker] TTRecUserBigCardViewHolder.B1 note: No RETURN_VOID found")
            }
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] TTRecUserBigCardViewHolder.B1 note: ${e.message}")
        }

        // 8. Neutralize In-Feed Search Recommendations & Trending Search Cards
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/common/communicate/AbsSearchService;",
                name = "u",
                returnType = "Ljava/util/List;",
            ).method.addInstructions(
                0,
                """
                    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized AbsSearchService.u() -> In-feed search recommendation and trending card insertions disabled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] AbsSearchService.u note: ${e.message}")
        }

        // 9. Neutralize In-Feed Safety Surveys, Evaluation Cards & Questionnaires
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/component/survey/CellSurveyComponent;",
                name = "onViewCreated",
                parameters = listOf("Landroid/view/View;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Suppressed CellSurveyComponent.onViewCreated(View) -> Survey inflation blocked.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] CellSurveyComponent.onViewCreated note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/component/survey/CellSurveyComponent;",
                name = "ur",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized CellSurveyComponent.ur(Aweme).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] CellSurveyComponent.ur note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/component/survey/CellSurveyComponent;",
                name = "wr",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Z"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized CellSurveyComponent.wr(Aweme, Z).")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] CellSurveyComponent.wr note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;",
                name = "isWithSurvey",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized AwemeExtKt.isWithSurvey(Aweme) -> False returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] AwemeExtKt.isWithSurvey note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/tiktok/pns/feedsafety/survey/PNSSurveyService;",
                name = "LIZIZ",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized PNSSurveyService.LIZIZ() -> Safety surveys disabled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] PNSSurveyService.LIZIZ note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/pushsurvey/PushSurveyAssemTrigger;",
                name = "yr",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized PushSurveyAssemTrigger.yr(VideoItemParams) -> Push survey triggers disabled.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] PushSurveyAssemTrigger.yr note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/component/surveybutton/FeedBottomSurveyButtonComponent;",
                name = "onParentViewCreated",
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Suppressed FeedBottomSurveyButtonComponent.onParentViewCreated().")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] FeedBottomSurveyButtonComponent.onParentViewCreated note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getWithSurvey",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getWithSurvey() -> False returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getWithSurvey note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getSurveyInfo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/survey/SurveyInfo;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getSurveyInfo() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getSurveyInfo note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getSurveyInfos",
                returnType = "Ljava/util/List;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getSurveyInfos() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getSurveyInfos note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getPersonalizedSurveyUI",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/survey/PersonalizedSurveyUI;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getPersonalizedSurveyUI() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getPersonalizedSurveyUI note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getOnboardingSurvey",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/survey/OnboardingSurvey;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getOnboardingSurvey() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getOnboardingSurvey note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getPersonalizedOnboardingSurvey",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/survey/PersonalizedOnboardingSurvey;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getPersonalizedOnboardingSurvey() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getPersonalizedOnboardingSurvey note: ${e.message}")
        }

        // 10. Neutralize In-Feed Explore Community / Topic Cards on Aweme
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getExploreCommunityCommentShowType",
                returnType = "Ljava/lang/Integer;",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """.trimIndent(),
            )
            println("[Feed Bloat Blocker] Neutralized Aweme.getExploreCommunityCommentShowType() -> Null returned.")
            patched++
        } catch (e: Exception) {
            println("[Feed Bloat Blocker] Aweme.getExploreCommunityCommentShowType note: ${e.message}")
        }

        println("[Feed Bloat Blocker] Applied $patched feed bloat blocker hook(s) -> Non-video distractions neutralized.")
    }
}
