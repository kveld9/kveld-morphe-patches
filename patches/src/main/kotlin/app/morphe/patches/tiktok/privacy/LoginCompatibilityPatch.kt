package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

val loginCompatibilityPatch = bytecodePatch(
    name = "Login Compatibility Fix",
    description = "Restores Google and Spotify sign-in on the re-signed APK via Web-OAuth fallbacks, with per-provider toggles.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val fixGoogleLogin by booleanOption(
        key = "fixGoogleLogin",
        default = true,
        title = "Fix Google Login",
        description = "Restores Google account sign-in after patching by forcing fallback to Web-based OAuth when Google Play Services rejects the modified APK signature.",
        required = false,
    )

    val fixSpotifyLogin by booleanOption(
        key = "fixSpotifyLogin",
        default = true,
        title = "Fix Spotify Login",
        description = "Restores 'Add to Spotify' after signing in on the web.",
        required = false,
    )

    execute {
        if (fixGoogleLogin != true && fixSpotifyLogin != true) {
            println("[Login Compatibility Fix] Both toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        if (fixGoogleLogin == true) {
            // 1. GoogleAuth.isAvailable()Z -> return false
            try {
                Fingerprint(
                    definingClass = "Lcom/bytedance/lobby/google/GoogleAuth;",
                    name = "isAvailable",
                    returnType = "Z",
                    parameters = emptyList(),
                ).method.replaceWithReturnBoolean(false)
                println("[Login Compatibility Fix] Forced GoogleAuth.isAvailable() -> false (Web OAuth fallback active).")
                patched++
            } catch (e: Exception) {
                println("[Login Compatibility Fix] GoogleAuth.isAvailable note: ${e.message}")
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
                    println("[Login Compatibility Fix] Forced GoogleOneTapAuth.isAvailable() -> false.")
                    patched++
                } catch (e: Exception) {
                    println("[Login Compatibility Fix] GoogleOneTapAuth.isAvailable note: ${e.message}")
                }
            }
        }

        if (fixSpotifyLogin == true) {
            // The Spotify auth SDK lives in the df_music_dsp dynamic feature dex, which the patcher
            // does not load. Its SSO intent is launched through this base-dex startActivityForResult
            // wrapper, so the extension intercepts it there. Parameters sit in high registers
            // (v28+), so the call must use the /range form.
            Fingerprint(
                accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
                returnType = "V",
                parameters = listOf("I", "Landroid/app/Activity;", "Landroid/content/Intent;", "L"),
                strings = listOf("(Landroid/content/Intent;I)V", "android/app/Activity", "startActivityForResult"),
            ).method.addInstructionsWithLabels(
                0,
                """
                invoke-static/range {p0 .. p2},${Constants.TIKTOK_EXTENSION_SPOTIFY_AUTH_HOOK}->interceptStartActivityForResult(ILandroid/app/Activity;Landroid/content/Intent;)Z
                move-result v0
                if-eqz v0, :continue
                return-void
                :continue
                nop
                """.trimIndent()
            )
            println("[Login Compatibility Fix] Hooked startActivityForResult -> Spotify SSO routed to Web OAuth.")
            patched++
        }

        if (patched == 0) {
            println("[Login Compatibility Fix] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Login Compatibility Fix] Applied $patched login compatibility hook(s).")
        }
    }
}
