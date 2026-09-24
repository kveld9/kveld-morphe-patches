package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideAiTaggedContentPatch = bytecodePatch(
    name = "Hide AI-Generated Content",
    description = "Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds.",
    default = false,
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFeedItemList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide AI-Generated Content] Hooked FeedApiService.fetchFeedList() (${returnIndices.size} return point(s)) -> FYP stream protected.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide AI-Generated Content] FeedApiService note: ${e.message}")
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide AI-Generated Content] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> AI-generated feed items filtered.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide AI-Generated Content] FeedItemList.getItems note: ${e.message}")
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFollowFeedList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide AI-Generated Content] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following feed protected.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide AI-Generated Content] FollowFeedList.getItems note: ${e.message}")
        }

        // 4. Hook FriendsV3FeedResponse.<init> (Friends tab V3 feed)
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
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFriendsV3Feeds(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Hide AI-Generated Content] Hooked FriendsV3FeedResponse.<init> -> Friends V3 feed protected.")
            patched++
        } catch (e: Exception) {
            println("[Hide AI-Generated Content] FriendsV3FeedResponse note: ${e.message}")
        }

        // 5. Hook FriendsFeedResponse.<init> (Friends tab V2 feed)
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
                    invoke-static {p3}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFriendsFeedData(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Hide AI-Generated Content] Hooked FriendsFeedResponse.<init> -> Friends V2 feed protected.")
            patched++
        } catch (e: Exception) {
            println("[Hide AI-Generated Content] FriendsFeedResponse note: ${e.message}")
        }

        println("[Hide AI-Generated Content] Applied $patched feed filter hook(s) -> AI-generated content neutralized.")
    }
}
