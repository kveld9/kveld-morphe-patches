package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

private const val MANDATORY_LOGIN_SERVICE = "Lcom/ss/android/ugc/aweme/services/MandatoryLoginService;"
private const val GUEST_MODE_SERVICE_IMPL = "Lcom/ss/android/ugc/aweme/account/guestmode/GuestModeServiceImpl;"

val mandatoryLoginBypassPatch = bytecodePatch(
    name = "Bypass Mandatory Login",
    description = "Neutralizes mandatory login walls, dynamic regional forced login gates, and guest browsing restrictions.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

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

        // 4. MandatoryLoginService.tryShowMandatoryLoginPage(...)V -> notify listener and return-void
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
            val listenerReg = if (method.parameters.isNotEmpty()) "p${method.parameters.size}" else "p1"
            method.addInstructions(
                0,
                """
                    invoke-static {$listenerReg}, Lcom/kveld9/morphe/extension/tiktok/TikTokLoginHook;->notifyLoginResult(Ljava/lang/Object;)V
                    return-void
                """.trimIndent(),
            )
            println("[MandatoryLoginBypass] Hooked MandatoryLoginService.tryShowMandatoryLoginPage() -> Notified login completion and suppressed popup.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] MandatoryLoginService.tryShowMandatoryLoginPage note: ${e.message}")
        }

        // 5. GuestModeServiceImpl.isGuestMode()Z -> delegate to TikTokLoginHook.isGuestMode()
        try {
            val isGuestModeMethod = Fingerprint(
                definingClass = GUEST_MODE_SERVICE_IMPL,
                name = "isGuestMode",
                returnType = "Z",
                parameters = emptyList(),
            ).method
            isGuestModeMethod.addInstructions(
                0,
                """
                    invoke-static {}, Lcom/kveld9/morphe/extension/tiktok/TikTokLoginHook;->isGuestMode()Z
                    move-result v0
                    return v0
                """.trimIndent(),
            )
            println("[MandatoryLoginBypass] Hooked GuestModeServiceImpl.isGuestMode() -> Active guest browsing mode enabled.")
            patched++
        } catch (e: Exception) {
            println("[MandatoryLoginBypass] GuestModeServiceImpl.isGuestMode note: ${e.message}")
        }

        println("[MandatoryLoginBypass] Applied $patched hooks -> Mandatory login wall bypassed.")
    }
}
