package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

val fixSpotifyLoginPatch = bytecodePatch(
    name = "Fix Spotify Login",
    description = "Restores the 'Add to Spotify' music button after patching by routing the Spotify app sign-in, which rejects the modified APK signature, through Spotify's Web-based OAuth.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
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

        println("[Fix Spotify Login] Applied 1 hook -> Spotify SSO routed to Web OAuth.")
    }
}
