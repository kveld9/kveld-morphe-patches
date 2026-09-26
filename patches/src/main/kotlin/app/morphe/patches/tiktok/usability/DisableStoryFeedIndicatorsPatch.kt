package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnVoid

val disableStoryFeedIndicatorsPatch = bytecodePatch(
    name = "Disable Story Feed Indicators",
    description = "Removes creator profile photo story rings from feed videos, ensuring avatar photos remain clean without blue story rings.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook User.getStoryStatus() -> return 0 (forces all users to report no active stories)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/profile/model/User;",
            name = "getStoryStatus",
            returnType = "I",
        ).method.replaceWithReturnInt(0)
        println("[Disable Story Feed Indicators] Hooked User.getStoryStatus() -> 0 (all users report no active stories).")
        patched++

        // 2. Neutralize FeedAvatarSocialPublishAssem lifecycle and click interception
        val socialPublishClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarSocialPublishAssem;"
        Fingerprint(
            definingClass = socialPublishClass,
            name = "onViewCreated",
            returnType = "V",
            parameters = listOf("Landroid/view/View;"),
        ).method.replaceWithReturnVoid()
        println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.onViewCreated() -> return-void.")
        patched++

        Fingerprint(
            definingClass = socialPublishClass,
            returnType = "V",
            parameters = listOf("Ljava/lang/Object;"),
        ).method.replaceWithReturnVoid()
        println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.(Object)V -> return-void.")
        patched++

        Fingerprint(
            definingClass = socialPublishClass,
            returnType = "V",
            parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
        ).method.replaceWithReturnVoid()
        println("[Disable Story Feed Indicators] Hooked FeedAvatarSocialPublishAssem.(VideoItemParams)V -> return-void.")
        patched++

        // 3. Hook SocPubDistributeServiceImpl(User) -> return false (social publish distributor)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/service/SocPubDistributeServiceImpl;",
            returnType = "Z",
            parameters = listOf("Lcom/ss/android/ugc/aweme/profile/model/User;"),
        ).method.replaceWithReturnBoolean(false)
        println("[Disable Story Feed Indicators] Hooked SocPubDistributeServiceImpl.(User)Z -> false.")
        patched++

        println("[Disable Story Feed Indicators] Successfully applied $patched hook(s).")
    }
}
