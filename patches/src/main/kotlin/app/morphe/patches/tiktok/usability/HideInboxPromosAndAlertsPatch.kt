package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid

val hideInboxPromosAndAlertsPatch = bytecodePatch(
    name = "Hide Inbox Promos & Alerts",
    description = "Hides promotional banners, streak mascot cards, contact sync suggestions, friend recommendations, and migration guide tooltips in the inbox and direct messages.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val hideTopPromosAndBanners by booleanOption(
        key = "hideTopPromosAndBanners",
        title = "Hide Top Promotional Banners",
        description = "Hides header banners including the streak mascot (Mascota de racha), event announcements, top notice banners, and phone link prompts.",
        default = true,
    )

    val hideContactRecommendations by booleanOption(
        key = "hideContactRecommendations",
        title = "Hide Contact & Friend Recommendations",
        description = "Hides user recommendation cards (Chat with contacts / Find and chat with them), suggested accounts, and mutual friends modules in the chatlist.",
        default = true,
    )

    val hideNavigationNoticesAndTooltips by booleanOption(
        key = "hideNavigationNoticesAndTooltips",
        title = "Hide Navigation Notices & Tooltips",
        description = "Hides UI migration tooltips (e.g. New followers has moved), bulletin board guide banners, and Shop migration notices.",
        default = true,
    )

    execute {
        var patched = 0

        // 1. Top promotional banners & header notices
        if (hideTopPromosAndBanners != false) {
            val topBannerClasses = listOf(
                "Lcom/ss/android/ugc/aweme/inbox/v2/container/TopBannerWidgetContainerInjector;",
                "Lcom/ss/android/ugc/aweme/im/chatlist/impl/feature/topnotice/topnotice/TopNoticeInboxWidgetV2Injector;",
                "Lcom/ss/android/ugc/aweme/notification/banner/InboxBannerWidgetInjector;",
                "Lcom/ss/android/ugc/aweme/notification/banner/InboxLegacyTopBannerWidgetInjector;",
                "Lcom/ss/android/ugc/aweme/im/chatlist/impl/feature/topnotice/agegraduation/AgeGraduationWidgetInjector;",
            )
            for (definingClass in topBannerClasses) {
                Fingerprint(
                    definingClass = definingClass,
                    name = "enable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                patched++
            }
            println("[Hide Inbox Promos & Alerts] Disabled ${topBannerClasses.size} top promotional banner injector(s).")
        }

        // 2. Contact recommendations & friend suggestions
        if (hideContactRecommendations != false) {
            val contactClasses = listOf(
                "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetContainerInjector;",
                "Lcom/ss/android/ugc/aweme/inbox/v2/container/UserCardWidgetVisibleContainerInjector;",
                "Lcom/ss/android/ugc/aweme/inbox/v2/container/InboxUserCardWidgetContainerInjector;",
                "Lcom/ss/android/ugc/aweme/relation/recuser/inbox/RecommendUserWidgetV2Injector;",
                "Lcom/ss/android/ugc/aweme/inbox/followerv2/FollowerUserCardWidgetContainerInjector;",
                "Lcom/ss/android/ugc/aweme/im/chatlist/impl/feature/maf/ui/MafChatListWidgetV2Injector;",
            )
            for (definingClass in contactClasses) {
                Fingerprint(
                    definingClass = definingClass,
                    name = "enable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                patched++
            }

            // Suppress contact recommendation cards inside multi-entrance inbox list
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/widget/multi/MultiViewModel;",
                name = "D83",
                returnType = "LX/0Nnc;",
                parameters = emptyList(),
            ).method.replaceWithReturnNull()
            patched++

            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/widget/multi/MultiViewModel;",
                name = "E83",
                returnType = "LX/0Nnc;",
                parameters = emptyList(),
            ).method.replaceWithReturnNull()
            patched++

            println("[Hide Inbox Promos & Alerts] Disabled ${contactClasses.size} contact injector(s) and 2 MultiViewModel contact pod hook(s).")
        }

        // 3. Navigation notices, tooltips & guide banners
        if (hideNavigationNoticesAndTooltips != false) {
            val noticeClasses = listOf(
                "Lcom/ss/android/ugc/aweme/notification/view/guidepush/BulletBoardGuideWidgetInjector;",
                "Lcom/ss/android/ugc/aweme/inbox/shop/ShopEntranceMigrationWidgetInjector;",
            )
            for (definingClass in noticeClasses) {
                Fingerprint(
                    definingClass = definingClass,
                    name = "enable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                patched++
            }

            // Suppress follower migration guide tooltip ("New followers has moved")
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/widget/multi/MultiViewModel;",
                name = "K83",
                returnType = "V",
                parameters = listOf("Ljava/util/List;"),
            ).method.replaceWithReturnVoid()
            patched++

            println("[Hide Inbox Promos & Alerts] Disabled ${noticeClasses.size} navigation notice injector(s) and 1 follower guide hook.")
        }

        println("[Hide Inbox Promos & Alerts] Applied $patched inbox promo and alert suppression hook(s).")
    }
}
