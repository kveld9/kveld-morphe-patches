package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

val disableStoryFeedIndicatorsPatch = bytecodePatch(
    name = "Disable Story Feed Indicators",
    description = "Removes the top-center story drop-down indicator pill (e.g. '1 Story') and creator profile photo story rings from feed videos, ensuring avatar photos remain clean.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize Aweme story models at root data level
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getUserStory",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/story/UserStory;",
            ).method.replaceWithReturnNull()
            println("[Disable Story Feed Indicators] Hooked Aweme.getUserStory() -> null.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] Aweme.getUserStory note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getIsTikTokStory",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[Disable Story Feed Indicators] Hooked Aweme.getIsTikTokStory() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] Aweme.getIsTikTokStory note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getStory",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/story/Story;",
            ).method.replaceWithReturnNull()
            println("[Disable Story Feed Indicators] Hooked Aweme.getStory() -> null.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] Aweme.getStory note: ${e.message}")
        }

        // 2. Hook User.getStoryStatus() -> return 0 (forces all users to report no active stories)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/profile/model/User;",
                name = "getStoryStatus",
                returnType = "I",
            ).method.replaceWithReturnInt(0)
            println("[Disable Story Feed Indicators] Hooked User.getStoryStatus() -> 0 (all users report no active stories).")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] User.getStoryStatus note: ${e.message}")
        }

        // 2. Neutralize FeedAvatarSocialPublishAssem lifecycle and click interception
        val socialPublishClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarSocialPublishAssem;"
        try {
            Fingerprint(
                definingClass = socialPublishClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method.replaceWithReturnVoid()
            println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.onViewCreated() -> return-void.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] FeedAvatarSocialPublishAssem.onViewCreated note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = socialPublishClass,
                name = "onBind",
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.onBind() -> return-void.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] FeedAvatarSocialPublishAssem.onBind note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = socialPublishClass,
                name = "tr",
                returnType = "V",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnVoid()
            println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.tr() -> return-void.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] FeedAvatarSocialPublishAssem.tr note: ${e.message}")
        }

        // 3. Hook SocPubDistributeServiceImpl.LJII(User) -> return false (social publish distributor)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/service/SocPubDistributeServiceImpl;",
                name = "LJII",
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/profile/model/User;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Disable Story Feed Indicators] Hooked SocPubDistributeServiceImpl.LJII() -> false.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] SocPubDistributeServiceImpl note: ${e.message}")
        }

        // 4. Discover and hook the feed story tag visibility evaluator (e.g. LX/0AZy;->LIZ(Context, Aweme, String)Z)
        try {
            var storyTagCheckClass = "LX/0AZy;"
            var storyTagCheckMethod = "LIZ"

            try {
                val triggerFp = Fingerprint(definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/story/FeedStoryTagTrigger;")
                val krMethod = triggerFp.classDef.methods.firstOrNull { it.name == "Kr" }
                val insns = krMethod?.implementation?.instructions
                if (insns != null) {
                    for (insn in insns) {
                        val ref = (insn as? ReferenceInstruction)?.reference?.toString() ?: continue
                        if (ref.contains("(Landroid/content/Context;Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;)Z")) {
                            val parts = ref.split("->")
                            if (parts.size == 2) {
                                storyTagCheckClass = parts[0]
                                storyTagCheckMethod = parts[1].substringBefore("(")
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Disable Story Feed Indicators] Discovery note: ${e.message}")
            }

            Fingerprint(
                definingClass = storyTagCheckClass,
                name = storyTagCheckMethod,
                returnType = "Z",
                parameters = listOf(
                    "Landroid/content/Context;",
                    "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                    "Ljava/lang/String;",
                ),
            ).method.replaceWithReturnBoolean(false)
            println("[Disable Story Feed Indicators] Hooked $storyTagCheckClass->$storyTagCheckMethod() -> false (story tag trigger predicate disabled).")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] Story tag visibility evaluator note: ${e.message}")
        }

        // 5. Hook FeedStoryTagTrigger.Kr() and FeedStoryTagTriggerV2.Kr() -> return false
        for (triggerClass in listOf(
            "Lcom/ss/android/ugc/aweme/feed/assem/story/FeedStoryTagTrigger;",
            "Lcom/ss/android/ugc/aweme/feed/assem/story/FeedStoryTagTriggerV2;",
        )) {
            try {
                Fingerprint(
                    definingClass = triggerClass,
                    name = "Kr",
                    returnType = "Z",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
                ).method.replaceWithReturnBoolean(false)
                println("[Disable Story Feed Indicators] Hooked $triggerClass.Kr() -> false.")
                patched++
            } catch (e: Exception) {
                println("[Disable Story Feed Indicators] $triggerClass note: ${e.message}")
            }
        }

        // 6. Neutralize FeedStoryTagAssem and FeedStoryTagAssemV2 binding and visibility routines
        for (assemClass in listOf(
            "Lcom/ss/android/ugc/aweme/feed/assem/story/FeedStoryTagAssem;",
            "Lcom/ss/android/ugc/aweme/feed/assem/story/FeedStoryTagAssemV2;",
        )) {
            try {
                Fingerprint(
                    definingClass = assemClass,
                    name = "Sr",
                    returnType = "V",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
                ).method.replaceWithReturnVoid()
                println("[Disable Story Feed Indicators] Hooked $assemClass.Sr() -> return-void.")
                patched++
            } catch (e: Exception) {
                println("[Disable Story Feed Indicators] $assemClass.Sr note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = assemClass,
                    name = "onBind",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                println("[Disable Story Feed Indicators] Hooked $assemClass.onBind() -> return-void.")
                patched++
            } catch (e: Exception) {
                println("[Disable Story Feed Indicators] $assemClass.onBind note: ${e.message}")
            }
        }

        // 7. Hook StoryTag.onDraw(Canvas) -> return-void (prevents drawing story pill canvas shader)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/ui/StoryTag;",
                name = "onDraw",
                returnType = "V",
                parameters = listOf("Landroid/graphics/Canvas;"),
            ).method.replaceWithReturnVoid()
            println("[Disable Story Feed Indicators] Hooked StoryTag.onDraw() -> return-void.")
            patched++
        } catch (e: Exception) {
            println("[Disable Story Feed Indicators] StoryTag.onDraw note: ${e.message}")
        }

        println("[Disable Story Feed Indicators] Successfully applied $patched hook(s).")
    }
}
