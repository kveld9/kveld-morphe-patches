package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val tikTokFeedAdBlockerPatch = bytecodePatch(
    name = "Feed Ad Blocker",
    description = "Removes sponsored advertisements, brand promotions, and promotional audio from the For You, Following, and Search feeds.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

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
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Ad Blocker] Hooked $definingClass->$name() (${returnIndices.size} return point(s)) -> $logLabel protected.")
                patched++
            }
        }

        // 1. Hook FeedApiService.fetchFeedList return points (live FYP/Home responses)
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
            feedApiMethod.addInstructions(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInFeedItemList(Ljava/lang/Object;)V
                """,
            )
        }
        if (feedApiReturns.isNotEmpty()) {
            println("[Feed Ad Blocker] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP stream protected.")
            patched++
        }

        // 2. Hook FeedItemList.getItems() (covers cached, offline, and UI adapter consumers)
        hookReturnList(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            name = "getItems",
            returnType = "Ljava/util/List;",
            logLabel = "All feed model consumers",
        )

        // 3. Hook FollowFeedList.getItems() (covers Following feed UI consumers)
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
            followMethod.addInstructions(
                returnIndex,
                """
                    invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAdsInFollowFeedList(Ljava/lang/Object;)V
                """,
            )
        }
        if (followReturns.isNotEmpty()) {
            println("[Feed Ad Blocker] Hooked FollowFeedList.getItems() (${followReturns.size} return point(s)) -> Following feed protected.")
            patched++
        }

        // 4. Hook ContinuousLoadingAwemeList.LIZLLL() (Search video continuous loading feed)
        hookReturnList(
            definingClass = "Lcom/ss/android/ugc/aweme/search/common/model/ContinuousLoadingAwemeList;",
            name = "LIZLLL",
            returnType = "Ljava/util/List;",
            logLabel = "Search continuous video feed",
        )

        // 5. Hook DynamicPatch.getAwemeList() (Search video detail launcher & Lynx cards)
        hookReturnList(
            definingClass = "Lcom/ss/android/ugc/aweme/discover/mixfeed/DynamicPatch;",
            name = "getAwemeList",
            returnType = "Ljava/util/List;",
            logLabel = "Search video detail launcher",
        )

        // 6. Hook SearchMix.getAwemeList() (Search mix and grid aweme streams)
        hookReturnList(
            definingClass = "Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMix;",
            name = "getAwemeList",
            returnType = "Ljava/util/List;",
            logLabel = "Search mix feed",
        )

        // 7. Hook BaseDetailShareVM.getAwemeList() (Search and detail video viewer shared VM)
        hookReturnList(
            definingClass = "Lcom/ss/android/ugc/aweme/detail/vm/BaseDetailShareVM;",
            name = "getAwemeList",
            returnType = "Ljava/util/List;",
            logLabel = "Detail video player shared VM",
        )

        println("[Feed Ad Blocker] Applied $patched feed filter hooks -> Universal ad-free feed active.")
    }
}
