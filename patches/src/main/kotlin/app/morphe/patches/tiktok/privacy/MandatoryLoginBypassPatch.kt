package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patches.shared.replaceWithReturnBoolean

private const val MANDATORY_LOGIN_SERVICE = "Lcom/ss/android/ugc/aweme/services/MandatoryLoginService;"
private const val GUEST_MODE_SERVICE_IMPL = "Lcom/ss/android/ugc/aweme/account/guestmode/GuestModeServiceImpl;"
private const val LITE_MANDATORY_LOGIN_SERVICE = "Lcom/ss/android/ugc/aweme/mini_account_impl/MandatoryLoginService;"

val mandatoryLoginBypassPatch = bytecodePatch(
    name = "Bypass Mandatory Login",
    description = "Lets you browse videos and feeds without creating an account or logging in.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_LITE)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0
        val isLite = packageMetadata.packageName == Constants.TIKTOK_LITE_PACKAGE_NAME

        if (isLite) {
            // Lite gate: LCC(String, String)Z is the master forced-login decision
            // (LC + isLogin + pre-login journey + region force-login). False skips the wall.
            Fingerprint(
                definingClass = LITE_MANDATORY_LOGIN_SERVICE,
                name = "LCC",
                parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[Bypass Mandatory Login] Neutralized Lite MandatoryLoginService.LCC() -> Forced login decision suppressed.")
            patched++

            // Lite fragment gate: MainRootFragment.onViewCreated calls L(String)Z and
            // skips the login panel when it returns false (if-nez v9, +079h).
            Fingerprint(
                definingClass = LITE_MANDATORY_LOGIN_SERVICE,
                name = "L",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[Bypass Mandatory Login] Neutralized Lite MandatoryLoginService.L() -> Feed fragment login check suppressed.")
            patched++

            // Lite shower: LBL(String, X/7Db)V forwards to IAccountService.LIIL (login page). Stub it.
            Fingerprint(
                definingClass = LITE_MANDATORY_LOGIN_SERVICE,
                name = "LBL",
                parameters = listOf("Ljava/lang/String;", "LX/7Db;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Bypass Mandatory Login] Neutralized Lite MandatoryLoginService.LBL() -> Login page display suppressed.")
            patched++

            println("[Bypass Mandatory Login] Applied $patched hooks -> Mandatory login wall bypassed on Lite.")
            return@execute
        }

        // 1. MandatoryLoginService.shouldShowForcedLogin(Z)Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "shouldShowForcedLogin",
                returnType = "Z",
                parameters = listOf("Z"),
            ).method.replaceWithReturnBoolean(false)
            println("[Bypass Mandatory Login] Neutralized MandatoryLoginService.shouldShowForcedLogin() -> Forced login suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Mandatory Login] MandatoryLoginService.shouldShowForcedLogin note: ${e.message}")
        }

        // 2. MandatoryLoginService.enableForcedLogin(Z)Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "enableForcedLogin",
                returnType = "Z",
                parameters = listOf("Z"),
            ).method.replaceWithReturnBoolean(false)
            println("[Bypass Mandatory Login] Neutralized MandatoryLoginService.enableForcedLogin() -> Dynamic forced login suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Mandatory Login] MandatoryLoginService.enableForcedLogin note: ${e.message}")
        }

        // 3. MandatoryLoginService.shouldShowLoginTabFirst()Z -> return false
        try {
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "shouldShowLoginTabFirst",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            println("[Bypass Mandatory Login] Neutralized MandatoryLoginService.shouldShowLoginTabFirst() -> Feed tab prioritized.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Mandatory Login] MandatoryLoginService.shouldShowLoginTabFirst note: ${e.message}")
        }

        // 4. MandatoryLoginService.tryShowMandatoryLoginPage(...)V -> notify listener and return-void
        val candidates = try {
            // Explicit overload selection scoped to the class: fail hard on
            // ambiguity instead of silently patching the first match.
            Fingerprint(
                definingClass = MANDATORY_LOGIN_SERVICE,
                name = "tryShowMandatoryLoginPage",
                returnType = "V",
            ).matchAll(classDefBy(MANDATORY_LOGIN_SERVICE))
                .map { it.method }
                .filter { it.parameters.firstOrNull()?.type == "Landroid/app/Activity;" }
        } catch (e: Exception) {
            println("[Bypass Mandatory Login] MandatoryLoginService.tryShowMandatoryLoginPage note: ${e.message}")
            throw e
        }

        require(candidates.size == 1) {
            "Ambiguous tryShowMandatoryLoginPage overloads: ${candidates.size}"
        }
        val method = candidates.first()
        val listenerReg = if (method.parameters.isNotEmpty()) "p${method.parameters.size}" else "p1"
        method.addInstructions(
            0,
            """
                invoke-static {$listenerReg}, ${Constants.TIKTOK_EXTENSION_LOGIN_HOOK}->notifyLoginResult(Ljava/lang/Object;)V
                return-void
            """.trimIndent(),
        )
        println("[Bypass Mandatory Login] Hooked MandatoryLoginService.tryShowMandatoryLoginPage() -> Notified login completion and suppressed popup.")
        patched++

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
                    invoke-static {}, ${Constants.TIKTOK_EXTENSION_LOGIN_HOOK}->isGuestMode()Z
                    move-result v0
                    return v0
                """.trimIndent(),
            )
            println("[Bypass Mandatory Login] Hooked GuestModeServiceImpl.isGuestMode() -> Active guest browsing mode enabled.")
            patched++
        } catch (e: Exception) {
            println("[Bypass Mandatory Login] GuestModeServiceImpl.isGuestMode note: ${e.message}")
        }

        if (patched == 0) {
            println("[Bypass Mandatory Login] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Bypass Mandatory Login] Applied $patched hooks -> Mandatory login wall bypassed.")
        }
    }
}
