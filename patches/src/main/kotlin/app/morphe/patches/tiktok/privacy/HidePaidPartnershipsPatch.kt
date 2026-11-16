package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hidePaidPartnershipsPatch = bytecodePatch(
    name = "Hide Sponsored & Paid Partnership",
    description = "Removes creator commercial disclosures, branded content, and posts labeled 'Sponsored' or 'Paid partnership' from the feed.",
    default = true,
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
                        "invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterPaidPartnershipInList(Ljava/lang/Object;)V",
                    )
                }
                if (returnIndices.isNotEmpty()) {
                    println("[Hide Paid Partnership] Hooked $definingClass->$name() (${returnIndices.size} return point(s)) -> $logLabel protected.")
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
                "invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterPaidPartnershipInList(Ljava/lang/Object;)V",
            )
            println("[Hide Paid Partnership] Hooked FeedFragment.C2(List, ...) -> Ingested feed stream protected at UI entry point.")
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

            println("[Hide Paid Partnership] Applied $patched hooks -> TikTok Lite sponsored & paid partnership posts hidden.")
            return@execute
        }

        // =========================================================================
        // FULL TIKTOK (com.zhiliaoapp.musically)
        // =========================================================================
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
                        invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterPaidPartnershipInList(Ljava/lang/Object;)V
                    """.trimIndent(),
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide Paid Partnership] Hooked $definingClass->$name() (${returnIndices.size} return point(s)) -> $logLabel protected.")
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterPaidPartnershipInFeedItemList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (feedApiReturns.isNotEmpty()) {
            println("[Hide Paid Partnership] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP stream protected.")
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterPaidPartnershipInFollowFeedList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (followReturns.isNotEmpty()) {
            println("[Hide Paid Partnership] Hooked FollowFeedList.getItems() (${followReturns.size} return point(s)) -> Following feed protected.")
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

        println("[Hide Paid Partnership] Applied $patched hooks -> Sponsored & paid partnership posts hidden.")
    }
}
