package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

val fixGoogleLoginPatch = bytecodePatch(
    name = "Fix Google Login",
    description = "Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. GoogleAuth.isAvailable()Z -> return false
        try {
            Fingerprint(
                definingClass = "Lcom/bytedance/lobby/google/GoogleAuth;",
                name = "isAvailable",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Fix Google Login] Forced GoogleAuth.isAvailable() -> false (Web OAuth fallback active).")
            patched++
        } catch (e: Exception) {
            println("[Fix Google Login] GoogleAuth.isAvailable note: ${e.message}")
        }

        // 2. GoogleOneTapAuth.isAvailable()Z -> return false (if present in variant)
        if (classDefByOrNull("Lcom/bytedance/lobby/google/GoogleOneTapAuth;") != null) {
            try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/lobby/google/GoogleOneTapAuth;",
                    name = "isAvailable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                println("[Fix Google Login] Forced GoogleOneTapAuth.isAvailable() -> false.")
                patched++
            } catch (e: Exception) {
                println("[Fix Google Login] GoogleOneTapAuth.isAvailable note: ${e.message}")
            }
        }

        println("[Fix Google Login] Applied $patched Google sign-in fallback hook(s).")
    }
}
