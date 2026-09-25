package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

val hideStemAndCommunityTabsPatch = bytecodePatch(
    name = "Hide STEM and Community Tabs",
    description = "Removes the STEM and Topics (Community / Explore) tabs from the top navigation feed strip.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        val tabProtocolTargets = listOf(
            "Lcom/ss/android/ugc/aweme/stemfeed/ui/tab/StemTabProtocol;" to "STEM Tab",
            "Lcom/ss/android/ugc/aweme/explore/entrance/ExploreXTabProtocol;" to "Explore / Comunidad Tab",
            "Lcom/ss/android/ugc/aweme/topicfeed/ui/tab/BaseTopicTabProtocol;" to "Base Topic Tab",
            "Lcom/ss/android/ugc/aweme/topicfeed/ui/tab/BaseTopicFCPTabProtocol;" to "Topic FCP Tab",
            "Lcom/ss/android/ugc/aweme/topicfeed/ui/tab/BasePersonalizedTabProtocol;" to "Personalized Topic Tab",
        )

        for ((targetClass, label) in tabProtocolTargets) {
            try {
                Fingerprint(
                    definingClass = targetClass,
                    name = "enable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                println("[Hide STEM and Community Tabs] Hooked $label ($targetClass.enable) -> false")
                patched++
            } catch (e: Exception) {
                println("[Hide STEM and Community Tabs] $label note: ${e.message}")
            }
        }

        println("[Hide STEM and Community Tabs] Successfully disabled $patched tab protocol(s).")
    }
}
