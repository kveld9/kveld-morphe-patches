package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid

val hideFeedSearchBarPatch = bytecodePatch(
    name = "Hide Feed Search Bar",
    description = "Removes the search suggestion pill and trending bar ('Search · <keyword>') from the bottom of feed videos, providing a clean viewing area without search distractions.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize Search & Trending Bar Trigger Predicates (return false)
        val triggerClasses = listOf(
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTriggerV2;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/TrendingBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/AdFeedSearchBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedEcSearchBottomBarAssemTrigger;",
        )

        for (triggerClass in triggerClasses) {
            Fingerprint(
                definingClass = triggerClass,
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
            ).method.replaceWithReturnBoolean(false)
            patched++
        }

        // 2. Neutralize Search & Trending Bar Assem Lifecycle Routines
        val assemClasses = listOf(
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssem;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemV2;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/TrendingBottomBarAssem;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/AdFeedSearchBottomBarAssem;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedEcSearchBottomBarAssem;",
        )

        for (assemClass in assemClasses) {
            Fingerprint(
                definingClass = assemClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method.replaceWithReturnVoid()
            patched++

            Fingerprint(
                definingClass = assemClass,
                returnType = "V",
                parameters = listOf("Ljava/lang/Object;"),
            ).method.replaceWithReturnVoid()
            patched++
        }

        // 3. Override Aweme Data Model Trending & Search Bar Flags
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "isDisableSearchTrendingBar",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(true)
        patched++

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "hasTrendingBar",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        patched++

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "hasTrendingBarFYP",
            returnType = "Z",
        ).method.replaceWithReturnBoolean(false)
        patched++

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "getTrendingBar",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
        ).method.replaceWithReturnNull()
        patched++

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "getTrendingBarFYP",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
        ).method.replaceWithReturnNull()
        patched++

        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            name = "getHotSearchInfo",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/HotSearchInfo;",
        ).method.replaceWithReturnNull()
        patched++

        println("[Hide Feed Search Bar] Applied $patched feed search and trending bar hook(s) -> Bottom search bar neutralized.")
    }
}
