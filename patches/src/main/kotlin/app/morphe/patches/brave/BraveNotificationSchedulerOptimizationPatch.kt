package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveNotificationSchedulerOptimizationPatch = bytecodePatch(
    name = "Brave In-Product Notification Scheduler Optimization",
    description = "Eliminates periodic background wakeups and native library loading caused by Chromium in-product tips/promo scheduler (Job ID 105).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        // ── 1. Neutralize schedule(JJ)V ────────────────────────────────────────────────
        // Prevents C++ from scheduling or rescheduling Job ID 105 in Android JobScheduler.
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
            name = "schedule",
            returnType = "V",
            parameters = listOf("J", "J"),
        ).method.addInstructions(
            0,
            """
                return-void
            """,
        )

        // ── 2. Neutralize onStartTask c(Context, TaskParameters, Callback)I ─────────────
        // If a persisted Job ID 105 was already scheduled in Android OS before patching,
        // cancel it immediately and return 1 (STOP_TASK) to prevent loading libmonochrome_64.so.
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
            name = "c",
            returnType = "I",
            parameters = listOf(
                "Landroid/content/Context;",
                "Lfjj;",
                "Lu5c;",
            ),
        ).method.addInstructions(
            0,
            """
                invoke-static {}, Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;->cancel()V
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
