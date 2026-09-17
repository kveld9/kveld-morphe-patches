package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid

private const val MANDATORY_LOGIN_SERVICE = "Lcom/ss/android/ugc/aweme/services/MandatoryLoginService;"

val mandatoryLoginBypassPatch = bytecodePatch(
    name = "Bypass Mandatory Login",
    description = "Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. MandatoryLoginService.shouldShowForcedLogin(Z)Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "shouldShowForcedLogin",
                returnType = "Z",
                parameters = listOf("Z"),
            ).method.replaceWithReturnBoolean(false)
            println("[MandatoryLoginBypass] Neutralized MandatoryLoginService.shouldShowForcedLogin() -> Forced login suppressed.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] MandatoryLoginService.shouldShowForcedLogin note: ${e.message}")
        }

        // 2. MandatoryLoginService.enableForcedLogin(Z)Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "enableForcedLogin",
                returnType = "Z",
                parameters = listOf("Z"),
            ).method.replaceWithReturnBoolean(false)
            println("[MandatoryLoginBypass] Neutralized MandatoryLoginService.enableForcedLogin() -> Dynamic forced login suppressed.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] MandatoryLoginService.enableForcedLogin note: ${e.message}")
        }

        // 3. MandatoryLoginService.shouldShowLoginTabFirst()Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "shouldShowLoginTabFirst",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[MandatoryLoginBypass] Neutralized MandatoryLoginService.shouldShowLoginTabFirst() -> Feed tab prioritized.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] MandatoryLoginService.shouldShowLoginTabFirst note: ${e.message}")
        }

        // 4. MandatoryLoginService.tryShowMandatoryLoginPage(...)V -> return-void
        try {
            val method = try {
                Fingerprint(
                    definingClass = MANDATORY_LOGIN_SERVICE,
                    name = "tryShowMandatoryLoginPage",
                    returnType = "V",
                ).method
            } catch (_: Exception) {
                // Obfuscation / overload fallback: match by name and returnType V with Activity first parameter
                Fingerprint(
                    definingClass = MANDATORY_LOGIN_SERVICE,
                    name = "tryShowMandatoryLoginPage",
                    returnType = "V",
                    custom = { m, _ ->
                        m.parameters.firstOrNull()?.type == "Landroid/app/Activity;"
                    },
                ).method
            }
            method.replaceWithReturnVoid()
            println("[MandatoryLoginBypass] Neutralized MandatoryLoginService.tryShowMandatoryLoginPage() -> Fullscreen login prompt neutralized.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] MandatoryLoginService.tryShowMandatoryLoginPage note: ${e.message}")
        }

        println("[MandatoryLoginBypass] Applied $patched hooks -> Mandatory login wall bypassed.")
    }
}
