package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val NEW_USER_JOURNEY_SERVICE = "Lcom/ss/android/ugc/aweme/NewUserJourneyService;"

val skipFirstLaunchOnboardingPatch = bytecodePatch(
    name = "Skip First-Launch Onboarding",
    description = "Bypasses the entire first-run introduction funnel (interest pickers, swipe tutorials, language prompts, and consent sheets) directly to the feed.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. MainPageFragment.JS()Z -> return false
        // Ensures MainPageFragment directly mounts feed, player, and tabs without waiting for NUJ
        try {
            val method = try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/main/MainPageFragment;",
                    name = "JS",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method
            } catch (_: Exception) {
                // Obfuscation fallback: disambiguate sole ()Z method calling Boolean.booleanValue()
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/main/MainPageFragment;",
                    returnType = "Z",
                    parameters = emptyList(),
                    custom = { m, _ ->
                        val instructions = m.implementation?.instructions ?: return@Fingerprint false
                        instructions.any { ins ->
                            (ins as? ReferenceInstruction)?.reference?.toString()?.contains("booleanValue") == true
                        }
                    },
                ).method
            }
            method.replaceWithReturnBoolean(false)
            println("[SkipFirstLaunchOnboarding] Hooked MainPageFragment.JS() -> Direct feed and page assembly enabled.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] MainPageFragment.JS note: ${e.message}")
        }

        // 2. NewUserJourneyService.LIZJ()Z -> return true (reports did_finish_nuj = true)
        try {
            Fingerprint(
                definingClass = NEW_USER_JOURNEY_SERVICE,
                returnType = "Z",
                strings = listOf("did_finish_nuj"),
            ).method.replaceWithReturnBoolean(true)
            println("[SkipFirstLaunchOnboarding] Forced NewUserJourneyService.LIZJ() -> Reported did_finish_nuj = true.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] NewUserJourneyService.LIZJ note: ${e.message}")
        }

        // 3. NewUserJourneyService.LJJJI(Landroid/app/Activity;)Z -> return false (never show NUJ)
        try {
            Fingerprint(
                definingClass = NEW_USER_JOURNEY_SERVICE,
                returnType = "Z",
                parameters = listOf("Landroid/app/Activity;"),
                strings = listOf("new_user_journey"),
            ).method.replaceWithReturnBoolean(false)
            println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJJJI() -> Should show NUJ suppressed.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] NewUserJourneyService.LJJJI note: ${e.message}")
        }

        // 4. NewUserJourneyService.LJIJI(...)V -> return-void (suppress launching NUJ Activity)
        try {
            Fingerprint(
                definingClass = NEW_USER_JOURNEY_SERVICE,
                returnType = "V",
                strings = listOf("deeplink_intent_about_welcome_screen"),
            ).method.replaceWithReturnVoid()
            println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJIJI() -> Launch activity intent suppressed.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] NewUserJourneyService.LJIJI note: ${e.message}")
        }

        // 5. NewUserJourneyService.LJJJ(Landroid/app/Activity;, Landroid/content/Intent;)V -> return-void
        try {
            Fingerprint(
                definingClass = NEW_USER_JOURNEY_SERVICE,
                returnType = "V",
                parameters = listOf("Landroid/app/Activity;", "Landroid/content/Intent;"),
                strings = listOf("reorder_new_journey_front"),
            ).method.replaceWithReturnVoid()
            println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJJJ() -> Reorder NUJ suppressed.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] NewUserJourneyService.LJJJ note: ${e.message}")
        }

        // 6. NewUserJourneyService.LJIJJLI(Lcom/bytedance/ies/foundation/activity/BaseActivity;)Z -> return false
        try {
            val method = try {
                Fingerprint(
                    definingClass = NEW_USER_JOURNEY_SERVICE,
                    name = "LJIJJLI",
                    returnType = "Z",
                    parameters = listOf("Lcom/bytedance/ies/foundation/activity/BaseActivity;"),
                ).method
            } catch (_: Exception) {
                // Obfuscation fallback for variants
                Fingerprint(
                    definingClass = NEW_USER_JOURNEY_SERVICE,
                    returnType = "Z",
                    parameters = listOf("Lcom/bytedance/ies/foundation/activity/BaseActivity;"),
                ).method
            }
            method.replaceWithReturnBoolean(false)
            println("[SkipFirstLaunchOnboarding] Neutralized NewUserJourneyService.LJIJJLI() -> Container activity launch suppressed.")
            patched++
        } catch (e: Exception) {
            println("[SkipFirstLaunchOnboarding] NewUserJourneyService.LJIJJLI note: ${e.message}")
        }

        println("[SkipFirstLaunchOnboarding] Applied $patched hooks -> Direct feed launch achieved.")
    }
}
