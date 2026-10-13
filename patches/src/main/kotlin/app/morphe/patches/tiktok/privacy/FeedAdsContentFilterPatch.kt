package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val feedAdsContentFilterPatch = bytecodePatch(
    name = "Feed Ads & Content Filter",
    description = "Filters ads, bloat cards, shop anchors, live cards, AI-tagged and seen videos from feeds, plus view/like-range and stories/photos filters, via per-content toggles.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val filterAds by booleanOption(
        key = "filterAds",
        title = "Filter Feed Ads",
        description = "Removes sponsored advertisements, brand promotions, and promotional audio from the For You, Following, and Search feeds.",
        default = true,
        required = false,
    )

    val filterBloat by booleanOption(
        key = "filterBloat",
        title = "Filter Feed Bloat",
        description = "Removes non-video clutter and floating ad widgets from the For You and Following feeds, including Touchpoint Rewards pendants, floating ad stickers, mini-games, CapCut/template creation prompts, memories ('On This Day'), community/topic cards, post-video surveys and evaluation questionnaires, mini-drama paywalls, and in-feed search recommendations.",
        default = true,
        required = false,
    )

    val stripShopAnchors by booleanOption(
        key = "stripShopAnchors",
        title = "Hide Video Product Anchors",
        description = "Removes shopping cart tags, product showcase badges, and commercial anchors from FYP and Following feed videos.",
        default = true,
        required = false,
    )

    val blockLiveStreams by booleanOption(
        key = "blockLiveStreams",
        title = "Block Live Streams",
        description = "Removes live stream broadcast cards and live recommendations from the For You and Following feeds.",
        default = false,
        required = false,
    )

    val hideAiContent by booleanOption(
        key = "hideAiContent",
        title = "Hide AI-Generated Content",
        description = "Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds.",
        default = false,
        required = false,
    )

    val hideSeenVideos by booleanOption(
        key = "hideSeenVideos",
        title = "Hide Seen Videos",
        description = "Filters previously watched videos from incoming For You feed batches.",
        default = true,
        required = false,
    )

    val minViews by intOption(
        key = "minViews",
        title = "Minimum Views",
        description = "Hide videos with fewer views than this. 0 disables the minimum.",
        default = 0,
        required = false,
    )

    val maxViews by intOption(
        key = "maxViews",
        title = "Maximum Views",
        description = "Hide videos with more views than this. 0 disables the maximum.",
        default = 0,
        required = false,
    )

    val minLikes by intOption(
        key = "minLikes",
        title = "Minimum Likes",
        description = "Hide videos with fewer likes than this. 0 disables the minimum.",
        default = 0,
        required = false,
    )

    val maxLikes by intOption(
        key = "maxLikes",
        title = "Maximum Likes",
        description = "Hide videos with more likes than this. 0 disables the maximum.",
        default = 0,
        required = false,
    )

    val hideStories by booleanOption(
        key = "hideStories",
        title = "Hide Stories",
        description = "Removes story posts from the For You feed.",
        default = true,
        required = false,
    )

    val hidePhotoPosts by booleanOption(
        key = "hidePhotoPosts",
        title = "Hide Photo Posts",
        description = "Removes photo-mode posts from the For You feed.",
        default = false,
        required = false,
    )

    execute {
        val viewsLikesConfigured = (minViews ?: 0) > 0 || (maxViews ?: 0) > 0 || (minLikes ?: 0) > 0 || (maxLikes ?: 0) > 0
        val contentFilterActive = viewsLikesConfigured || hideStories == true || hidePhotoPosts == true

        if (filterAds != true &&
            filterBloat != true &&
            stripShopAnchors != true &&
            blockLiveStreams != true &&
            hideAiContent != true &&
            hideSeenVideos != true &&
            !contentFilterActive
        ) {
            println("[Feed Ads & Content Filter] Skipped: All content filter options are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Branch: filterAds
        if (filterAds != false) {
            fun hookReturnList(definingClass: String, name: String, returnType: String, logLabel: String) {
                val fingerprint = Fingerprint(
                    definingClass = definingClass,
                    name = name,
                    returnType = returnType,
                )
                val method = fingerprint.method
                val returnIndices = method.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    method.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInList(Ljava/lang/Object;)V
                        """,
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Feed Ads & Content Filter] Hooked $definingClass->$name() (${returnIndices.size} return point(s)) -> $logLabel protected.")
                    patched++
                }
            }

            // Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
            val feedApiFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            )
            val feedApiMethod = feedApiFingerprint.method
            val feedApiReturns = feedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            feedApiReturns.asReversed().forEach { (returnIndex, reg) ->
                feedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInFeedItemList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (feedApiReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP stream protected.")
                patched++
            }

            // Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
            hookReturnList(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
                logLabel = "All feed model consumers",
            )

            // Hook FollowFeedList.getItems() (covers Following feed UI consumers)
            val followFeedListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val followMethod = followFeedListFingerprint.method
            val followReturns = followMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            followReturns.asReversed().forEach { (returnIndex, reg) ->
                followMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInFollowFeedList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (followReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FollowFeedList.getItems() (${followReturns.size} return point(s)) -> Following feed protected.")
                patched++
            }

            // Hook ContinuousLoadingAwemeList.LIZLLL() (Search video continuous loading feed)
            hookReturnList(
                definingClass = "Lcom/ss/android/ugc/aweme/search/common/model/ContinuousLoadingAwemeList;",
                name = "LIZLLL",
                returnType = "Ljava/util/List;",
                logLabel = "Search continuous video feed",
            )

            // Hook DynamicPatch.getAwemeList() (Search video detail launcher & Lynx cards)
            hookReturnList(
                definingClass = "Lcom/ss/android/ugc/aweme/discover/mixfeed/DynamicPatch;",
                name = "getAwemeList",
                returnType = "Ljava/util/List;",
                logLabel = "Search video detail launcher",
            )

            // Hook SearchMix.getAwemeList() (Search mix and grid aweme streams)
            hookReturnList(
                definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMix;",
                name = "getAwemeList",
                returnType = "Ljava/util/List;",
                logLabel = "Search mix feed",
            )

            // Hook BaseDetailShareVM.getAwemeList() (Search and detail video viewer shared VM)
            hookReturnList(
                definingClass = "Lcom/ss/android/ugc/aweme/detail/vm/BaseDetailShareVM;",
                name = "getAwemeList",
                returnType = "Ljava/util/List;",
                logLabel = "Detail video player shared VM",
            )
        }

        // 2. Branch: filterBloat (excluding Section 7 Friends-feed blocks)
        if (filterBloat != false) {
            // 2.1 Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
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
                    println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP stream protected from bloat cards.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedApiService note: ${e.message}")
            }

            // 2.2 Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
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
                    println("[Feed Ads & Content Filter] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> Feed bloat filtered.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedItemList.getItems note: ${e.message}")
            }

            // 2.3 Hook FollowFeedList.getItems() (covers Following feed UI consumers)
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
                    println("[Feed Ads & Content Filter] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following bloat filtered.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FollowFeedList.getItems note: ${e.message}")
            }

            // 2.4 Neutralize Lemon8 cross-promotion feed task
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
                println("[Feed Ads & Content Filter] Neutralized Lemon8ServiceInitTask.run(Context).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Lemon8ServiceInitTask note: ${e.message}")
            }

            // 2.5 Suppress Touchpoint Rewards & Ad Pendants (floating activity widgets / Issue #33)
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
                println("[Feed Ads & Content Filter] Suppressed SpecActWidget.bind(ViewGroup) -> Floating pendant inflation blocked.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] SpecActWidget.bind note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Suppressed SpecActWidget.showOrHidePendant(ZZ).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] SpecActWidget.showOrHidePendant note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Suppressed SpecActWidget.showNormalPendant().")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] SpecActWidget.showNormalPendant note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized FeedPendantService.LIZ(String).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedPendantService.LIZ note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized AdPendantService.LIZ(String).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] AdPendantService.LIZ note: ${e.message}")
            }

            // 2.6 Suppress In-Video Floating Bloat, Commercial Stickers & Activity Pendants on Aweme
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getActivityPendant() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getActivityPendant note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getCommerceStickerInfo() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getCommerceStickerInfo note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getSpecialSticker() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getSpecialSticker note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getFloatingCardInfo() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getFloatingCardInfo note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getBannerTip() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getBannerTip note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getStandardComponentInfo() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getStandardComponentInfo note: ${e.message}")
            }

            // 2.8 Neutralize In-Feed Search Recommendations & Trending Search Cards
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
                println("[Feed Ads & Content Filter] Neutralized AbsSearchService.u() -> In-feed search recommendation and trending card insertions disabled.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] AbsSearchService.u note: ${e.message}")
            }

            // 2.9 Neutralize In-Feed Safety Surveys, Evaluation Cards & Questionnaires
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
                println("[Feed Ads & Content Filter] Suppressed CellSurveyComponent.onViewCreated(View) -> Survey inflation blocked.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] CellSurveyComponent.onViewCreated note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized CellSurveyComponent.ur(Aweme).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] CellSurveyComponent.ur note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized CellSurveyComponent.wr(Aweme, Z).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] CellSurveyComponent.wr note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized AwemeExtKt.isWithSurvey(Aweme) -> False returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] AwemeExtKt.isWithSurvey note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized PNSSurveyService.LIZIZ() -> Safety surveys disabled.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] PNSSurveyService.LIZIZ note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized PushSurveyAssemTrigger.yr(VideoItemParams) -> Push survey triggers disabled.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] PushSurveyAssemTrigger.yr note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Suppressed FeedBottomSurveyButtonComponent.onParentViewCreated().")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedBottomSurveyButtonComponent.onParentViewCreated note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getWithSurvey() -> False returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getWithSurvey note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getSurveyInfo() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getSurveyInfo note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getSurveyInfos() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getSurveyInfos note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getPersonalizedSurveyUI() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getPersonalizedSurveyUI note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getOnboardingSurvey() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getOnboardingSurvey note: ${e.message}")
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getPersonalizedOnboardingSurvey() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getPersonalizedOnboardingSurvey note: ${e.message}")
            }

            // 2.10 Neutralize In-Feed Explore Community / Topic Cards on Aweme
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
                println("[Feed Ads & Content Filter] Neutralized Aweme.getExploreCommunityCommentShowType() -> Null returned.")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Aweme.getExploreCommunityCommentShowType note: ${e.message}")
            }
        }

        // 3. Branch: stripShopAnchors (video anchors feed hooks only)
        if (stripShopAnchors != false) {
            // Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
            val feedApiFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            )
            val feedApiMethod = feedApiFingerprint.method
            val returnIndices = feedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { returnIndex ->
                val reg = (feedApiMethod.implementation!!.instructions[returnIndex] as OneRegisterInstruction).registerA
                feedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInFeedItemList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP feed shop anchors stripped.")
                patched++
            }

            // Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
            val feedItemListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val feedItemListMethod = feedItemListFingerprint.method
            val feedItemListReturnIndices = feedItemListMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            feedItemListReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                feedItemListMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (feedItemListReturnIndices.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedItemList.getItems() (${feedItemListReturnIndices.size} return point(s)) -> Feed video shop anchors stripped.")
                patched++
            }

            // Hook FollowFeedList.getItems() (covers Following feed UI consumers)
            val followFeedListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val followFeedListMethod = followFeedListFingerprint.method
            val followFeedListReturnIndices = followFeedListMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            followFeedListReturnIndices.asReversed().forEach { (returnIndex, reg) ->
                followFeedListMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInFollowFeedList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (followFeedListReturnIndices.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FollowFeedList.getItems() (${followFeedListReturnIndices.size} return point(s)) -> Following feed shop anchors stripped.")
                patched++
            }
        }

        // 4. Branch: blockLiveStreams
        if (blockLiveStreams != false) {
            // Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
            try {
                val feedApiFingerprint = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                    name = "fetchFeedList",
                    returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                )
                val method = feedApiFingerprint.method
                val returnIndices = method.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { returnIndex ->
                    val reg = (method.implementation!!.instructions[returnIndex] as OneRegisterInstruction).registerA
                    method.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterLiveStreamsInFeedItemList(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP stream protected from live cards.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedApiService note: ${e.message}")
            }

            // Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
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
                            invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterLiveStreamsInList(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Feed Ads & Content Filter] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> Feed live streams filtered.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedItemList.getItems note: ${e.message}")
            }

            // Hook FollowFeedList.getItems() (covers Following feed UI consumers)
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
                            invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterLiveStreamsInFollowFeedList(Ljava/lang/Object;)V
                        """.trimIndent(),
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Feed Ads & Content Filter] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following live streams filtered.")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FollowFeedList.getItems note: ${e.message}")
            }
        }

        // 5. Branch: hideAiContent
        if (hideAiContent != false) {
            // Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
            val feedApiFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            )
            val feedApiMethod = feedApiFingerprint.method
            val feedApiReturns = feedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            feedApiReturns.asReversed().forEach { (returnIndex, reg) ->
                feedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFeedItemList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (feedApiReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP stream protected.")
                patched++
            }

            // Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
            val feedItemListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val feedItemListMethod = feedItemListFingerprint.method
            val feedItemListReturns = feedItemListMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            feedItemListReturns.asReversed().forEach { (returnIndex, reg) ->
                feedItemListMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (feedItemListReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedItemList.getItems() (${feedItemListReturns.size} return point(s)) -> AI-generated feed items filtered.")
                patched++
            }

            // Hook FollowFeedList.getItems() (covers Following feed UI consumers)
            val followFeedListFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
            )
            val followFeedListMethod = followFeedListFingerprint.method
            val followFeedListReturns = followFeedListMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            followFeedListReturns.asReversed().forEach { (returnIndex, reg) ->
                followFeedListMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFollowFeedList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (followFeedListReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FollowFeedList.getItems() (${followFeedListReturns.size} return point(s)) -> Following feed protected.")
                patched++
            }

            // Hook FriendsV3FeedNetworkSource.LJ (Friends tab V3 network response)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedNetworkSource;",
                name = "LJ",
                parameters = listOf("Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedResponse;"),
                returnType = "Ljava/lang/Object;",
            ).method.addInstructions(
                0,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFriendsV3Response(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Feed Ads & Content Filter] Hooked FriendsV3FeedNetworkSource.LJ -> Friends V3 network responses protected.")
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
                        invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFriendsFeedResponse(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (friendsFeedApiReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses protected.")
                patched++
            }
        }

        // 6. Branch: hideSeenVideos
        if (hideSeenVideos != false) {
            val playerControllerFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
            )

            val onProgressMethod = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "onPlayProgressChange" &&
                    it.parameterTypes == listOf("Ljava/lang/String;", "J", "J") &&
                    it.returnType == "V"
            }
            if (onProgressMethod != null) {
                onProgressMethod.addInstructions(
                    0,
                    """
                        invoke-static/range {p1 .. p5}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->onPlayProgressChange(Ljava/lang/String;JJ)V
                    """.trimIndent(),
                )
                println("[Feed Ads & Content Filter] Hooked PlayerController.onPlayProgressChange -> Playback threshold tracker active.")
                patched++
            } else {
                println("[Feed Ads & Content Filter] Skipped: PlayerController.onPlayProgressChange(String, long, long) not found.")
            }

            val onCompletedMethod = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "onPlayCompleted" &&
                    it.parameterTypes == listOf("Ljava/lang/String;") &&
                    it.returnType == "V"
            }
            if (onCompletedMethod != null) {
                onCompletedMethod.addInstructions(
                    0,
                    """
                        invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->onPlayCompleted(Ljava/lang/String;)V
                    """.trimIndent(),
                )
                println("[Feed Ads & Content Filter] Hooked PlayerController.onPlayCompleted -> Completion tracker active.")
                patched++
            } else {
                println("[Feed Ads & Content Filter] Skipped: PlayerController.onPlayCompleted(String) not found.")
            }

            // Hook FeedApiService.fetchFeedList return points (live FYP network responses)
            val feedApiFingerprint = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            )
            val feedApiMethod = feedApiFingerprint.method
            val feedApiReturns = feedApiMethod.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            feedApiReturns.asReversed().forEach { (returnIndex, reg) ->
                feedApiMethod.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    """
                        invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->filterSeenVideosInFeedItemList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (feedApiReturns.isNotEmpty()) {
                println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP incoming batch filter active.")
                patched++
            }
        }

        // 7. Branch: views/likes/stories/photos content filter
        if (contentFilterActive) {
            val hook = Constants.TIKTOK_EXTENSION_CONTENT_FILTER_HOOK

            // Push patch-time configuration into the runtime hook.
            try {
                val minV = (minViews ?: 0).coerceAtLeast(0).toLong()
                val maxV = (maxViews ?: 0).coerceAtLeast(0).toLong()
                val minL = (minLikes ?: 0).coerceAtLeast(0).toLong()
                val maxL = (maxLikes ?: 0).coerceAtLeast(0).toLong()
                val clinit = Fingerprint(
                    definingClass = hook,
                    name = "<clinit>",
                ).method
                clinit.ensureRegisterCount(2)
                val instructions = clinit.implementation!!.instructions
                val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                val insertIdx = if (returnIdx != -1) returnIdx else 0
                clinit.addInstructions(
                    insertIdx,
                    """
                        const-wide v0, 0x${minV.toString(16)}
                        sput-wide v0, $hook->minViews:J
                        const-wide v0, 0x${maxV.toString(16)}
                        sput-wide v0, $hook->maxViews:J
                        const-wide v0, 0x${minL.toString(16)}
                        sput-wide v0, $hook->minLikes:J
                        const-wide v0, 0x${maxL.toString(16)}
                        sput-wide v0, $hook->maxLikes:J
                        const/4 v0, ${if (hideStories != false) "0x1" else "0x0"}
                        sput-boolean v0, $hook->hideStories:Z
                        const/4 v0, ${if (hidePhotoPosts == true) "0x1" else "0x0"}
                        sput-boolean v0, $hook->hidePhotoPosts:Z
                    """.trimIndent(),
                )
                println("[Feed Ads & Content Filter] Pushed configuration (views ${minViews ?: 0}..${maxViews ?: 0}, likes ${minLikes ?: 0}..${maxLikes ?: 0}, stories=$hideStories, photos=$hidePhotoPosts).")
                patched++
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] Configuration note: ${e.message}")
            }

            // Hook FeedApiService.fetchFeedList return points (live FYP responses).
            try {
                val method = Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                    name = "fetchFeedList",
                    returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
                ).method
                val returnIndices = method.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()
                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    method.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        "invoke-static {v$reg}, $hook->filterContentInFeedItemList(Ljava/lang/Object;)V",
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Feed Ads & Content Filter] Hooked FeedApiService.fetchFeedList (${returnIndices.size} return(s)).")
                    patched++
                }
            } catch (e: Exception) {
                println("[Feed Ads & Content Filter] FeedApiService note: ${e.message}")
            }
        }

        if (patched == 0) {
            println("[Feed Ads & Content Filter] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Feed Ads & Content Filter] Applied $patched feed ads and content filter hook(s).")
        }
    }
}
