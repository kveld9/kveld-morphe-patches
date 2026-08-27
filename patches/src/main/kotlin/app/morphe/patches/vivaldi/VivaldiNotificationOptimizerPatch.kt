package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val vivaldiNotificationOptimizerPatch = bytecodePatch(
    name = "Disable Chromium Tips & Notification Scheduler",
    description = "Eliminates periodic background wakeups, native library loading, and in-product promotional tips from Chromium tips scheduler.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val hookedMethods = mutableListOf<String>()

        // ── 1. Chromium Tips Scheduler: Neutralize schedule(JJ)V ────────────────────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
            name = "schedule",
            returnType = "V",
            parameters = listOf("J", "J"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("NotificationSchedulerTask.schedule")
        }

        // ── 2. Chromium Tips Scheduler: Neutralize onStartTask c(...)I ─────────────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;",
            name = "c",
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;->cancel()V
                    const/4 v0, 0x1
                    return v0
                """,
            )
            hookedMethods.add("NotificationSchedulerTask.c")
        }

        // ── 3. Chromium Tips Agent: Neutralize showTipsPromo(I)V ────────────────────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/scheduler/TipsAgent;",
            name = "showTipsPromo",
            returnType = "V",
            parameters = listOf("I"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("TipsAgent.showTipsPromo")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Disable Notification Scheduler] Neutralized ${hookedMethods.size} tips scheduler methods in ${targetClasses.joinToString(", ")}")
    }
}
