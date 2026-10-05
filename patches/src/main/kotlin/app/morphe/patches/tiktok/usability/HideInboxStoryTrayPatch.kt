package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnEmptyList

val hideInboxStoryTrayPatch = bytecodePatch(
    name = "Hide Inbox Story & Status Tray",
    description = "Hides the horizontal story, notes, and status tray (Skylight) displayed at the top of direct messages and the inbox.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Hook InboxSkylightWidgetV2Injector.enable() -> return false
        // Prevents registering and injecting the Skylight widget into the multi-pod Inbox recycler.
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2Injector;",
            name = "enable",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Inbox Story & Status Tray] Hooked InboxSkylightWidgetV2Injector.enable() -> return false.")
        patched++

        // 2. Hook InboxSkylightWidgetV2 pod provider -> return emptyList()
        // Neutralizes the pod provider so no story or thought combine pods are created.
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2;",
            name = "Sq",
            returnType = "Ljava/util/List;",
            parameters = emptyList(),
        ).method.replaceWithReturnEmptyList()
        println("[Hide Inbox Story & Status Tray] Hooked InboxSkylightWidgetV2 pod provider -> return emptyList().")
        patched++

        // 3. Hook InboxSkylightWidgetV2 display eligibility check -> return false
        // Forces the display eligibility check to report empty/ineligible.
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/inbox/skylight/InboxSkylightWidgetV2;",
            returnType = "Z",
            parameters = listOf("Ljava/util/List;"),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Inbox Story & Status Tray] Hooked InboxSkylightWidgetV2 display eligibility -> return false.")
        patched++

        println("[Hide Inbox Story & Status Tray] Applied $patched hook(s) -> Inbox story and status tray hidden.")
    }
}
