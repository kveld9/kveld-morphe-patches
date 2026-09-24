package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull

val hideSuggestedSearchesPatch = bytecodePatch(
    name = "Hide Suggested Searches",
    description = "Removes the 'Podría interesarte' (You may like / Guess Search) suggested keywords section from the search intermediate page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Filter guess_search card from search intermediate data payload
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
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterGuessSearchRaw(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                    invoke-static {p2}, ${Constants.TIKTOK_EXTENSION_SEARCH_HOOK}->filterGuessSearchResponse(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] RecomDataWrapper.<init> note: ${e.message}")
        }

        // 2. Suppress cached preloaded guess search data (JSONObject)
        try {
            Fingerprint(
                definingClass = "LX/0HMZ;",
                name = "LIZ",
                returnType = "Lorg/json/JSONObject;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] GuessSearchPreload.LIZ note: ${e.message}")
        }

        // 3. Suppress cached preloaded guess search data (String)
        try {
            Fingerprint(
                definingClass = "LX/0HMZ;",
                name = "LIZIZ",
                returnType = "Ljava/lang/String;",
            ).method.replaceWithReturnNull()
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] GuessSearchPreload.LIZIZ note: ${e.message}")
        }

        // 4. Force disable native guess search fallback flag in search middle page
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/search/middle/DynamicSingleIntermediateFragmentNew;",
                name = "yU",
                returnType = "Z",
            ).method.replaceWithReturnBoolean(false)
            patched++
        } catch (e: Exception) {
            println("[Hide Suggested Searches] DynamicSingleIntermediateFragmentNew.yU note: ${e.message}")
        }

        println("[Hide Suggested Searches] Applied $patched suggested search suppression hook(s) -> 'Podría interesarte' neutralized.")
    }
}
