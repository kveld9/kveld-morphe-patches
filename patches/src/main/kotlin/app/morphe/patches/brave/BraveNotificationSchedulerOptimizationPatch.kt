package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val braveNotificationSchedulerOptimizationPatch = bytecodePatch(
    name = "Silence Promo Notifications",
    description = "Stops tips, reward promos and marketing notifications.",
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

        // ── 2. Brave Rewards Onboarding: Neutralize Alarm Enqueue ───────────────────────
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/dialogs/BraveAdsSignupDialog;",
            name = "enqueueOnboardingNotificationNative",
            returnType = "V",
            parameters = emptyList(),
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("BraveAdsSignupDialog.enqueueOnboardingNotificationNative")
        }

        // ── 3. Retention Marketing Campaigns: Neutralize Publisher & Receiver ──────────
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
        if (hookedMethods.isEmpty()) {
            println("[Silence Promo Notifications] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Silence Promo Notifications] Injected Smali hooks into ${hookedMethods.size} methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
        }
    }
}
