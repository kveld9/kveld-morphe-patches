package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val friendsFeedStrictMutualsPatch = bytecodePatch(
    name = "Friends Feed Strict Mutuals",
    description = "Filters out suggested accounts, recommended videos, and non-mutual profiles (such as 'People you may know') from the Friends feed so it only plays videos from accounts you mutually follow.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // 1. Hook FriendsV3FeedResponse constructor (p1: List<FriendsV3FeedModel>)
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
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsV3Feeds(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Friends Feed Strict Mutuals] Hooked FriendsV3FeedResponse.<init> -> Friends V3 feed filtered for mutuals only.")
            patched++
        } catch (e: Exception) {
            println("[Friends Feed Strict Mutuals] FriendsV3FeedResponse note: ${e.message}")
        }

        // 2. Hook FriendsFeedResponse constructor (p3: List<FriendsFeed>)
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
                    invoke-static {p3}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsFeedData(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Friends Feed Strict Mutuals] Hooked FriendsFeedResponse.<init> -> Friends V2 feed filtered for mutuals only.")
            patched++
        } catch (e: Exception) {
            println("[Friends Feed Strict Mutuals] FriendsFeedResponse note: ${e.message}")
        }

        // 3. Hook FriendsV3FeedNetworkSource.LJ (Friends V3 network responses)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedNetworkSource;",
            name = "LJ",
            parameters = listOf("Lcom/ss/android/ugc/aweme/friendstab/repo/FriendsV3FeedResponse;"),
            returnType = "Ljava/lang/Object;",
        ).method.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsV3Response(Ljava/lang/Object;)V
            """.trimIndent(),
        )
        println("[Friends Feed Strict Mutuals] Hooked FriendsV3FeedNetworkSource.LJ -> Friends V3 network responses protected.")
        patched++

        // 4. Hook FriendsFeedApi network fetch return points (LX/06CX;->LIZLLL in v47.1.4)
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
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedVideosInFriendsFeedResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (friendsFeedApiReturns.isNotEmpty()) {
            println("[Friends Feed Strict Mutuals] Hooked FriendsFeedApi.LIZLLL (${friendsFeedApiReturns.size} return point(s)) -> Friends V2 network responses protected.")
            patched++
        }

        println("[Friends Feed Strict Mutuals] Applied $patched feed filter hook(s) -> Non-mutual and suggested videos neutralized.")
    }
}
