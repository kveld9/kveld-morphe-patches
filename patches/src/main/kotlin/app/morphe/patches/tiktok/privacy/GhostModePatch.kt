package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val ghostModePatch = bytecodePatch(
    name = "Ghost Mode",
    description = "Enables anonymous profile and story browsing: suppresses outbound profile view records and story view pings while preserving your ability to see who viewed your profile.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Profile Outbound: Intercept ProfileViewerApiService.reportView
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/profile/business/ci/viewer/api/ProfileViewerApiService;",
                name = "reportView",
                parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "LX/02um;",
            ).method.addInstructions(
                0,
                """
                    const-string v0, ""
                    invoke-static {v0}, LX/02um;->LJJJJZI(Ljava/lang/Object;)LX/06pH;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
            println("[Ghost Mode] Neutralized ProfileViewerApiService.reportView() -> outbound profile view suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Ghost Mode] ProfileViewerApiService.reportView note: ${e.message}")
        }

        // 2. Story Outbound: Intercept LX/07Rx.LIZIZ (StoryApi.reportStoryViewed)
        try {
            Fingerprint(
                definingClass = "LX/07Rx;",
                name = "LIZIZ",
                parameters = listOf("Ljava/lang/Boolean;", "Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Ghost Mode] Neutralized LX/07Rx.LIZIZ() -> outbound story view suppressed.")
            patched++
        } catch (e: Exception) {
            println("[Ghost Mode] LX/07Rx.LIZIZ note: ${e.message}")
        }

        println("[Ghost Mode] Applied $patched ghost mode hook(s).")
    }
}
