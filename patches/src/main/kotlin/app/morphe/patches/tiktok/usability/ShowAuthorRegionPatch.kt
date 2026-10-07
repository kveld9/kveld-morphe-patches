package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags

val showAuthorRegionPatch = bytecodePatch(
    name = "Show Author Region",
    description = "Displays the creator's country or region code next to their username in video author info across feeds and deep-linked detail views.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // 1. Hook VideoAuthorInfoVM.paramSync2StateAccept -> TikTokAuthorRegionHook.onVideoItemParams
        val vmMethod = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;",
            custom = { method, _ ->
                method.name == "paramSync2StateAccept" &&
                    "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;" in method.parameterTypes
            },
        ).method
        val paramReg = if (AccessFlags.STATIC.isSet(vmMethod.accessFlags)) "p0" else "p1"
        vmMethod.addInstructions(
            0,
            "invoke-static/range {$paramReg .. $paramReg}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->onVideoItemParams(Ljava/lang/Object;)V",
        )
        patched++

        // 2. Hook MainActivity.onCreate -> TikTokAuthorRegionHook.install
        val mainActivityMethod = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
            name = "onCreate",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        ).method
        mainActivityMethod.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->install(Landroid/app/Activity;)V
            """.trimIndent(),
        )
        patched++

        // 3. Hook DetailActivity.onCreate -> TikTokAuthorRegionHook.install
        val detailActivityMethod = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/detail/ui/DetailActivity;",
            name = "onCreate",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        ).method
        detailActivityMethod.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->install(Landroid/app/Activity;)V
            """.trimIndent(),
        )
        patched++

        println("[Show Author Region] Applied $patched author region hook(s).")
    }
}
