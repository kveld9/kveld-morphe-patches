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

        // 1. Neutralize Search & Trending Bar Trigger Predicates (Kr -> return false)
        val triggerClasses = listOf(
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssemTriggerV2;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/TrendingBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/AdFeedSearchBottomBarAssemTrigger;",
            "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedEcSearchBottomBarAssemTrigger;",
        )

        for (triggerClass in triggerClasses) {
            try {
                Fingerprint(
                    definingClass = triggerClass,
                    name = "Kr",
                    returnType = "Z",
                    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;"),
                ).method.replaceWithReturnBoolean(false)
                patched++
            } catch (e: Exception) {
                println("[Hide Feed Search Bar] $triggerClass.Kr note: ${e.message}")
            }
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
            try {
                Fingerprint(
                    definingClass = assemClass,
                    name = "onViewCreated",
                    returnType = "V",
                    parameters = listOf("Landroid/view/View;"),
                ).method.replaceWithReturnVoid()
                patched++
            } catch (e: Exception) {
                println("[Hide Feed Search Bar] $assemClass.onViewCreated note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = assemClass,
                    name = "onBind",
                    returnType = "V",
                    parameters = listOf("Ljava/lang/Object;"),
                ).method.replaceWithReturnVoid()
                patched++
            } catch (e: Exception) {
                println("[Hide Feed Search Bar] $assemClass.onBind note: ${e.message}")
            }
        }

        // 3. Neutralize FeedSearchBottomBarAssem.Sr() binding subroutine
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/cell/interact/bottom/bar/FeedSearchBottomBarAssem;",
                name = "Sr",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] FeedSearchBottomBarAssem.Sr note: ${e.message}")
        }

        // 4. Override Aweme Data Model Trending & Search Bar Flags
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "isDisableSearchTrendingBar",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(true)
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.isDisableSearchTrendingBar note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "hasTrendingBar",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.hasTrendingBar note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "hasTrendingBarFYP",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.hasTrendingBarFYP note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getTrendingBar",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.getTrendingBar note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getTrendingBarFYP",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/AwemeTrendingBar;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.getTrendingBarFYP note: ${e.message}")
        }

        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getHotSearchInfo",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/HotSearchInfo;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Feed Search Bar] Aweme.getHotSearchInfo note: ${e.message}")
        }

        println("[Hide Feed Search Bar] Applied $patched feed search and trending bar hook(s) -> Bottom search bar neutralized.")
    }
}
