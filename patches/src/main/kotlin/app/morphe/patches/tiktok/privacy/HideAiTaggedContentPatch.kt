package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideAiTaggedContentPatch = bytecodePatch(
    name = "Hide AI-Generated Content",
    description = "Filters and skips videos tagged with native AI-generated metadata, C2PA content credentials, or creator AI disclosure tags across the For You, Following, and Friends feeds.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // =========================================================================
        // TIKTOK LITE (com.tiktok.lite.go)
        // =========================================================================
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME
        if (isLite) {
            fun hookReturnListLite(definingClass: String, name: String, returnType: String, logLabel: String) {
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
                        "invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInList(Ljava/lang/Object;)V",
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Hide AI-Generated Content] Hooked $definingClass->$name() (${returnIndices.size} return point(s)) -> $logLabel protected.")
                    patched++
                }
            }

            // Hook FeedFragment.C2(List, ...) (primary ingestion point for all feed items before display)
            val c2Fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/minilite/feed/FeedFragment;",
                name = "C2",
                returnType = "V",
            )
            val c2Method = c2Fp.method
            c2Method.addInstructions(
                0,
                "invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInList(Ljava/lang/Object;)V",
            )
            println("[Hide AI-Generated Content] Hooked FeedFragment.C2(List, ...) -> Ingested feed stream protected at UI entry point.")
            patched++

            // Hook FeedItemList.getItems() and getAwemeList() (covers feed, cached, and home consumers on Lite)
            hookReturnListLite(
                definingClass = "Lcom/ss/android/ugc/aweme/minilite/feed/data/bean/FeedItemList;",
                name = "getItems",
                returnType = "Ljava/util/List;",
                logLabel = "All Lite feed model consumers (getItems)",
            )
            hookReturnListLite(
                definingClass = "Lcom/ss/android/ugc/aweme/minilite/feed/data/bean/FeedItemList;",
                name = "getAwemeList",
                returnType = "Ljava/util/List;",
                logLabel = "All Lite feed model consumers (getAwemeList)",
            )

            println("[Hide AI-Generated Content] Applied $patched hooks -> TikTok Lite AI-generated content hidden.")
            return@execute
        }

        // =========================================================================
        // FULL TIKTOK (com.zhiliaoapp.musically)
        // =========================================================================
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFeedItemList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (feedApiReturns.isNotEmpty()) {
            println("[Hide AI-Generated Content] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP stream protected.")
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (feedItemListReturns.isNotEmpty()) {
            println("[Hide AI-Generated Content] Hooked FeedItemList.getItems() (${feedItemListReturns.size} return point(s)) -> AI-generated feed items filtered.")
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterAiContentInFollowFeedList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (followFeedListReturns.isNotEmpty()) {
            println("[Hide AI-Generated Content] Hooked FollowFeedList.getItems() (${followFeedListReturns.size} return point(s)) -> Following feed protected.")
            patched++
        }

        // Hook FriendsFeedApi network fetch return points (LX/07KR;->LIZLLL in v47.7.3, was LX/07KN; in v47.7.2, LX/065q; in v47.6.3)
        val friendsFeedApiFingerprint = Fingerprint(
            definingClass = "LX/07KR;",
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
            println("[Hide AI-Generated Content] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses protected.")
            patched++
        }

        println("[Hide AI-Generated Content] Applied $patched hooks -> AI-generated content hidden.")
    }
}
