package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_FILTER_CLASS = "Lcom/kveld9/morphe/extension/tiktok/TikTokFeedAdFilter;"

val tikTokFeedAdBlockerPatch = bytecodePatch(
    name = "Feed Ad Blocker",
    description = "Removes sponsored advertisements, brand promotions, soft commercial videos, and promotional audio from the For You and Following feeds.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
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
                ?.map { it.index }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { returnIndex ->
                val reg = (method.implementation!!.instructions[returnIndex] as OneRegisterInstruction).registerA
                method.addInstructions(
                    returnIndex,
                    """
                        invoke-static {v$reg}, $EXTENSION_FILTER_CLASS->filterFeedItemList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Ad Blocker] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP stream protected.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Ad Blocker] FeedApiService note: ${e.message}")
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
                        invoke-static {v$reg}, $EXTENSION_FILTER_CLASS->filterAwemeList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Ad Blocker] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> All feed model consumers protected.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Ad Blocker] FeedItemList.getItems note: ${e.message}")
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
                        invoke-static {v$reg}, $EXTENSION_FILTER_CLASS->filterFollowFeedList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Ad Blocker] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following feed protected.")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Ad Blocker] FollowFeedList.getItems note: ${e.message}")
        }

        println("[Feed Ad Blocker] Applied $patched feed filter hooks -> Universal ad-free feed active.")
    }
}
