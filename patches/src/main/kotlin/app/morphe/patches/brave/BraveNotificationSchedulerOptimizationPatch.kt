package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveNotificationSchedulerOptimizationPatch = bytecodePatch(
    name = "Brave In-Product & Commercial Notification Optimizer",
    description = "Eliminates background wakeups and notifications from Chromium tips scheduler (Job ID 105), Brave Rewards onboarding promo, and retention marketing campaigns.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

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
            parameters = listOf(
                "Landroid/content/Context;",
                "Lwtj;",
                "Lpcc;",
            ),
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

        // ── 4. Brave Rewards Onboarding: Neutralize Alarm Enqueue ───────────────────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
            name = "enqueueOnboardingNotificationNative",
            returnType = "V",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveAdsSignupDialog.enqueueOnboardingNotificationNative")
        }

        // ── 5. Brave Rewards Onboarding: Neutralize Broadcast Receiver & Publisher ─────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/BraveOnboardingNotification;",
            name = "a",
            returnType = "V",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveOnboardingNotification.a")
        }

        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/BraveOnboardingNotification;",
            name = "onReceive",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveOnboardingNotification.onReceive")
        }

        // ── 6. Retention Marketing Campaigns: Neutralize Publisher & Receiver ──────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/retention/RetentionNotificationPublisher;",
            name = "a",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("RetentionNotificationPublisher.a")
        }

        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/notifications/retention/RetentionNotificationPublisher;",
            name = "onReceive",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("RetentionNotificationPublisher.onReceive")
        }

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()
        println("[Notification Optimizer] Injected Smali hooks into ${hookedMethods.size} methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
    }
}
