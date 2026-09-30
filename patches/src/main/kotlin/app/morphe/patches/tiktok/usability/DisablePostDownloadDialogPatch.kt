package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val disablePostDownloadDialogPatch = bytecodePatch(
    name = "Disable Post-Download Share Dialog",
    description = "Suppresses the automatic 'Share to' and friend suggestions bottom sheet that pops up after finishing a download.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        val showFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/internalshare/impl/fragment/DownloadAndShareFragment;",
            strings = listOf("after_video_saved_share_to_nscreen"),
            returnType = "V",
        )
        val showMethod = showFp.method
        showMethod.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        println("[Disable Post-Download Share Dialog] Hooked DownloadAndShareFragment.${showMethod.name} -> Suppressed post-download popup.")
        patched++

        println("[Disable Post-Download Share Dialog] Applied $patched post-download dialog suppression hook(s).")
    }
}
