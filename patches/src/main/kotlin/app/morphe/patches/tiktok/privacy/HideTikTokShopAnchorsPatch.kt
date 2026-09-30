package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideTikTokShopAnchorsPatch = bytecodePatch(
    name = "Hide TikTok Shop & Mall",
    description = "Removes product showcase badges, shopping cart tags, and the TikTok Shop / Mall tab from navigation bars and video posts.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    extendWith("extensions/extension.mpe")

    val hideShopTab by booleanOption(
        key = "hideShopTab",
        title = "Hide Shop Navigation Tab",
        description = "Removes the TikTok Shop / Mall navigation tab and indicator from navigation bars. Disable if you want to keep access to your purchases and orders while still hiding video product tags.",
        default = true,
    )

    val hideVideoAnchors by booleanOption(
        key = "hideVideoAnchors",
        title = "Hide Video Product Anchors",
        description = "Removes shopping cart tags, product showcase badges, and commercial anchors from FYP and Following feed videos.",
        default = true,
    )

    execute {
        var patched = 0

        if (hideVideoAnchors != false) {
            // 1. Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
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
                feedApiMethod.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInFeedItemList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide TikTok Shop & Mall] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP feed shop anchors stripped.")
                patched++
            }

            // 2. Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
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
                feedItemListMethod.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (feedItemListReturnIndices.isNotEmpty()) {
                println("[Hide TikTok Shop & Mall] Hooked FeedItemList.getItems() (${feedItemListReturnIndices.size} return point(s)) -> Feed video shop anchors stripped.")
                patched++
            }

            // 3. Hook FollowFeedList.getItems() (covers Following feed UI consumers)
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
                followFeedListMethod.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInFollowFeedList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (followFeedListReturnIndices.isNotEmpty()) {
                println("[Hide TikTok Shop & Mall] Hooked FollowFeedList.getItems() (${followFeedListReturnIndices.size} return point(s)) -> Following feed shop anchors stripped.")
                patched++
            }
        }

        if (hideShopTab != false) {
            // 4. Disable ShopBottomTabProtocol
            val bottomTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/ShopBottomTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            bottomTabFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++

            // 5. Disable ShopTopTabProtocol
            val topTabFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/ShopTopTabProtocol;",
                name = "enable",
                returnType = "Z",
            )
            topTabFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++

            // 6. Disable ShopIconServiceImpl indicator
            val iconServiceFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/vm/ShopIconServiceImpl;",
                parameters = emptyList(),
                returnType = "Z",
            )
            iconServiceFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        }

        println("[Hide TikTok Shop & Mall] Applied $patched TikTok Shop anchor and navigation tab suppression hook(s).")
    }
}
