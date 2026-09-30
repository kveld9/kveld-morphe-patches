package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid

val disableSearchHistoryRecordingPatch = bytecodePatch(
    name = "Disable Search History Recording",
    description = "Prevents search queries and keywords from being recorded in local history, databases, and analytics stores.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Neutralize Search History Manager recordSearchHistory routine (LX/0D1m; in v47.1.4, was LX/0D1i;)
        Fingerprint(
            definingClass = "LX/0D1m;",
            name = "LIZ",
            returnType = "V",
            parameters = listOf(
                "Lcom/ss/android/ugc/aweme/search/model/SearchHistory;",
                "Ljava/lang/String;",
            ),
        ).method.replaceWithReturnVoid()
        patched++

        // 2. Neutralize ManualSearchPvStore history tracking routine
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/search/pages/middlepage/history/ManualSearchPvStore;",
            name = "LJIIJ",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
        ).method.replaceWithReturnVoid()
        patched++

        // 3. Suppress server-pushed top history recommendations
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/discover/model/suggest/SuggestWordResponse;",
            name = "getTopHistoryWords",
            returnType = "Ljava/util/List;",
        ).method.replaceWithReturnNull()
        patched++

        println("[Disable Search History Recording] Applied $patched search history suppression hook(s) -> Search history recording neutralized.")
    }
}
