package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnEmptyList
import app.morphe.patches.shared.sharedExtensionPatch

val hideInboxStoryTrayPatch = bytecodePatch(
    name = "Hide Inbox Story Tray",
    description = "Hides the stories and status tray at the top of the inbox.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val hideSuggestedOnly by booleanOption(
        key = "hideSuggestedOnly",
        title = "Only suggested",
        description = "Hide only suggested stories, keep followed.",
        default = true,
    )

    execute {
        var patched = 0

        if (hideSuggestedOnly != true) {
            // 1. Hook InboxSkylightWidgetV2Injector.enable() -> return false
            // Prevents registering and injecting the Skylight widget into the multi-pod Inbox recycler.
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2Injector;",
                name = "enable",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Hide Inbox Story Tray] Hooked InboxSkylightWidgetV2Injector.enable() -> return false.")
            patched++

            // 2. Hook InboxSkylightWidgetV2 pod provider -> return emptyList()
            // Neutralizes the pod provider so no story or thought combine pods are created.
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2;",
                name = "Sq",
                returnType = "Ljava/util/List;",
                parameters = emptyList(),
            ).method.replaceWithReturnEmptyList()
            println("[Hide Inbox Story Tray] Hooked InboxSkylightWidgetV2 pod provider -> return emptyList().")
            patched++

            // 3. Hook InboxSkylightWidgetV2 display eligibility check -> return false
            // Forces the display eligibility check to report empty/ineligible.
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2;",
                returnType = "Z",
                parameters = listOf("Ljava/util/List;"),
            ).method.replaceWithReturnBoolean(false)
            println("[Hide Inbox Story Tray] Hooked InboxSkylightWidgetV2 display eligibility -> return false.")
            patched++

            if (patched == 0) {
                println("[Hide Inbox Story Tray] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
            } else {
                println("[Hide Inbox Story Tray] Applied $patched hook(s) -> Inbox story and status tray hidden.")
            }
            return@execute
        }

        // Granular filtering mode: Hook StoryGetFeedByPageResponse constructor (p1: List<Aweme>)
        // to prune suggested / non-followed story items from the tray feed while preserving followed stories.
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/story/inbox/StoryGetFeedByPageResponse;",
            name = "<init>",
            parameters = listOf(
                "Ljava/util/List;",
                "Z",
                "J",
                "J",
                "Ljava/lang/Long;",
                "Ljava/util/List;",
                "Ljava/lang/Boolean;",
                "Ljava/util/List;",
                "Ljava/lang/Boolean;",
                "I",
                "Ljava/util/List;",
                "Ljava/lang/String;",
                "Ljava/util/List;",
                "Ljava/lang/Boolean;",
            ),
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_FILTER_CLASS}->filterSuggestedStoriesInList(Ljava/lang/Object;)Ljava/util/List;
                move-result-object p1
            """.trimIndent(),
        )
        println("[Hide Inbox Story Tray] Hooked StoryGetFeedByPageResponse.<init> -> suggested stories filtered from tray.")
        patched++

        println("[Hide Inbox Story Tray] Applied $patched hook(s) -> non-followed stories filtered from inbox tray.")
    }
}
