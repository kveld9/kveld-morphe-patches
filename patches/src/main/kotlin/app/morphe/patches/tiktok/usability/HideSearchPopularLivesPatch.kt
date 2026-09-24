package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val hideSearchPopularLivesPatch = bytecodePatch(
    name = "Hide Popular Lives In Search",
    description = "Removes the 'LIVE populares' (Popular LIVEs) recommendation card and stream broadcasts from the search intermediate page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Filter live_popular card from search intermediate data payload
        try {
            val recomWrapperFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/discover/model/suggest/RecomDataWrapper;",
                name = "<init>",
                returnType = "V",
                parameters = listOf(
                    "Ljava/lang/String;",
                    "Lcom/ss/android/ugc/aweme/discover/model/suggest/SuggestWordResponse;",
                ),
            )
            recomWrapperFp.method.addInstructions(
                1,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesRaw(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterPopularLivesResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Hide Popular Lives In Search] RecomDataWrapper.<init> note: ${e.message}")
        }

        println("[Hide Popular Lives In Search] Applied $patched popular lives suppression hook(s) -> 'LIVE populares' neutralized.")
    }
}
