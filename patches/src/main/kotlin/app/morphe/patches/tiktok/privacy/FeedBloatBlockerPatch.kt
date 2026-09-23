package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val feedBloatBlockerPatch = bytecodePatch(
    name = "Feed Bloat & Distraction Blocker",
    description = "Removes non-video clutter and floating ad widgets from the For You, Following, and Friends feeds, including Touchpoint Rewards pendants, floating ad stickers, suggested friend cards, mini-games, CapCut/template creation prompts, memories ('On This Day'), surveys, and mini-drama paywalls.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

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
                method.addInstructions(
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
                method.addInstructions(
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
                method.addInstructions(
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

        println("[Feed Bloat Blocker] Applied $patched feed bloat blocker hook(s) -> Non-video distractions neutralized.")
    }
}
