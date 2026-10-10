package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid

private const val NEW_USER_JOURNEY_SERVICE = "Lcom/ss/android/ugc/aweme/NewUserJourneyService;"

val skipFirstLaunchOnboardingPatch = bytecodePatch(
    name = "Skip First-Launch Onboarding",
    description = "Bypasses the entire first-run introduction funnel (interest pickers, swipe tutorials, language prompts, and consent sheets) directly to the feed.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. NewUserJourneyService.LIZJ()Z -> return true (reports did_finish_nuj = true)
        Fingerprint(
            definingClass = NEW_USER_JOURNEY_SERVICE,
            returnType = "Z",
            strings = listOf("did_finish_nuj"),
        ).method.replaceWithReturnBoolean(true)
        println("[SkipFirstLaunchOnboarding] Forced NewUserJourneyService.LIZJ() -> Reported did_finish_nuj = true.")
        patched++

        // 2. NewUserJourneyService.LJJJI(Landroid/app/Activity;)Z -> return false (never show NUJ)
        Fingerprint(
            definingClass = NEW_USER_JOURNEY_SERVICE,
            returnType = "Z",
            parameters = listOf("Landroid/app/Activity;"),
            strings = listOf("new_user_journey"),
        ).method.replaceWithReturnBoolean(false)
        println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJJJI() -> Should show NUJ suppressed.")
        patched++

        // 3. NewUserJourneyService.LJIJI(...)V -> return-void (suppress launching NUJ Activity)
        Fingerprint(
            definingClass = NEW_USER_JOURNEY_SERVICE,
            returnType = "V",
            strings = listOf("deeplink_intent_about_welcome_screen"),
        ).method.replaceWithReturnVoid()
        println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJIJI() -> Launch activity intent suppressed.")
        patched++

        // 4. NewUserJourneyService.LJJJ(Landroid/app/Activity;, Landroid/content/Intent;)V -> return-void
        Fingerprint(
            definingClass = NEW_USER_JOURNEY_SERVICE,
            returnType = "V",
            parameters = listOf("Landroid/app/Activity;", "Landroid/content/Intent;"),
            strings = listOf("reorder_new_journey_front"),
        ).method.replaceWithReturnVoid()
        println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJJJ() -> Reorder NUJ suppressed.")
        patched++

        // 5. NewUserJourneyService.LJIJJLI(Lcom/bytedance/ies/foundation/activity/BaseActivity;)Z -> return false
        // Explicit overload selection scoped to the class: fail hard on
        // ambiguity instead of silently patching the first match.
        val candidates = Fingerprint(
            definingClass = NEW_USER_JOURNEY_SERVICE,
            returnType = "Z",
            parameters = listOf("Lcom/bytedance/ies/foundation/activity/BaseActivity;"),
        ).matchAll(classDefBy(NEW_USER_JOURNEY_SERVICE)).map { it.method }
        require(candidates.size == 1) {
            "Ambiguous LJIJJLI overloads: ${candidates.size}"
        }
        val method = candidates.first()
        method.replaceWithReturnBoolean(false)
        println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJIJJLI() -> Container activity launch suppressed.")
        patched++

        println("[SkipFirstLaunchOnboarding] Applied $patched hooks -> Direct feed launch achieved.")
    }
}
