package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch

private val suggestedAccountInjectors = listOf(
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/InboxUserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetVisibleContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/followerv2/FollowerUserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/inbox/widget/multi/FollowerUserCardLoadingWidgetV2Injector;",
    "Lcom/ss/android/ugc/aweme/relation/recuser/inbox/FollowerUserCardWidgetV2Injector;",
    "Lcom/ss/android/ugc/aweme/notification/v2/widget/container/UserCardWidgetContainerInjector;",
    "Lcom/ss/android/ugc/aweme/relation/recuser/inbox/NotificationRecommendUserWidgetV2Injector;",
)

val hideSuggestedAccountsPatch = bytecodePatch(
    name = "Hide Suggested Accounts",
    description = "Removes suggested-account cards from profile headers and inbox surfaces.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/profile/platform/business/header/business/recommend/assemble/ProfileHeaderRecommendComponent;",
                returnType = "V",
                parameters = emptyList(),
                strings = listOf("recommend_user_card"),
            ).method.replaceWithReturnVoid()
            println("[Hide Suggested Accounts] Suppressed ProfileHeaderRecommendComponent -> Profile recommend cards blocked.")
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Accounts] ProfileHeaderRecommendComponent note: ${e.message}")
        }

        suggestedAccountInjectors.forEach { injector ->
            try {
                Fingerprint(
                    definingClass = injector,
                    name = "enable",
                    parameters = emptyList(),
                    returnType = "Z",
                ).method.replaceWithReturnBoolean(false)
                println("[Hide Suggested Accounts] Disabled ${injector.substringAfterLast("/").removeSuffix(";")} -> Suggested cards blocked.")
                patched++
            } catch (e: Exception) {
                println("[Hide Suggested Accounts] ${injector.substringAfterLast("/").removeSuffix(";")} note: ${e.message}")
            }
        }

        println("[Hide Suggested Accounts] Applied $patched suggested accounts hook(s).")
    }
}
