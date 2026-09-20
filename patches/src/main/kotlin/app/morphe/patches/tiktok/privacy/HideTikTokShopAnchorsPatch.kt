package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideTikTokShopAnchorsPatch = bytecodePatch(
    name = "Hide TikTok Shop & Mall",
    description = "Removes product showcase badges, shopping cart tags, and the TikTok Shop / Mall tab from navigation bars and video posts.",
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
                ?.map { it.index }
                ?.toList() ?: emptyList()

            returnIndices.asReversed().forEach { returnIndex ->
                val reg = (method.implementation!!.instructions[returnIndex] as OneRegisterInstruction).registerA
                method.addInstructions(
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
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] FeedApiService note: ${e.message}")
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide TikTok Shop & Mall] Hooked FeedItemList.getItems() (${returnIndices.size} return point(s)) -> Feed video shop anchors stripped.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] FeedItemList.getItems note: ${e.message}")
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
                        invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->stripShopAnchorsInFollowFeedList(Ljava/lang/Object;)V
                    """,
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Hide TikTok Shop & Mall] Hooked FollowFeedList.getItems() (${returnIndices.size} return point(s)) -> Following feed shop anchors stripped.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] FollowFeedList.getItems note: ${e.message}")
        }

        // 4. Disable ShopBottomTabProtocol
        try {
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
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] ShopBottomTabProtocol note: ${e.message}")
        }

        // 5. Disable ShopTopTabProtocol
        try {
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
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] ShopTopTabProtocol note: ${e.message}")
        }

        // 6. Disable ShopIconServiceImpl indicator
        try {
            val iconServiceFp = try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/vm/ShopIconServiceImpl;",
                    name = "vw",
                    returnType = "Z",
                )
            } catch (_: Exception) {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/ecommerce/mall/vm/ShopIconServiceImpl;",
                    name = "rw",
                    returnType = "Z",
                )
            }
            iconServiceFp.method.addInstructions(
                0,
                """
                const/4 v0, 0
                return v0
                """.trimIndent()
            )
            patched++
        } catch (e: Exception) {
            println("[Hide TikTok Shop & Mall] ShopIconServiceImpl note: ${e.message}")
        }

        println("[Hide TikTok Shop & Mall] Applied $patched TikTok Shop anchor and navigation tab suppression hook(s).")
    }
}
