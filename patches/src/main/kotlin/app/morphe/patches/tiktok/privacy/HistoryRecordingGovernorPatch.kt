package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid

val historyRecordingGovernorPatch = bytecodePatch(
    name = "History Recording Governor",
    description = "Prevents TikTok from recording watch history and search history via per-surface toggles.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val disableWatchHistory by booleanOption(
        key = "disableWatchHistory",
        default = true,
        title = "Disable Watch History",
        description = "Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches.",
        required = false,
    )

    val disableSearchHistory by booleanOption(
        key = "disableSearchHistory",
        default = true,
        title = "Disable Search History",
        description = "Prevents search queries and keywords from being recorded in local history, databases, and analytics stores.",
        required = false,
    )

    execute {
        if (disableWatchHistory != true && disableSearchHistory != true) {
            println("[History Recording Governor] Both toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        if (disableWatchHistory == true) {
            // 1. Neutralize AwemeStatsApi video view reporting routine (AwemeStatsApi.LIZIZ(...)V)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
                name = "LIZIZ",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            // 2. Neutralize AwemeStatsApi batch video view flush routine (AwemeStatsApi.LIZ(List)V)
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
                name = "LIZ",
                returnType = "V",
                parameters = listOf("Ljava/util/List;"),
            ).method.replaceWithReturnVoid()
            patched++

            // 3. Neutralize PlaybackHistoryManager recordVideoPlayback routine (LX/03l3;->LIZ in v47.1.4, was LX/03kz;)
            Fingerprint(
                definingClass = "LX/03l3;",
                name = "LIZ",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            // 4. Neutralize PlaybackHistoryManager recordWatchDuration routine (LX/03l3;->LIZIZ in v47.1.4, was LX/03kz;)
            Fingerprint(
                definingClass = "LX/03l3;",
                name = "LIZIZ",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            // 5. Neutralize fast stats report routine (LX/0aZr;->subscribe in v47.1.4, was LX/0a8D;)
            Fingerprint(
                definingClass = "LX/0aZr;",
                name = "subscribe",
                returnType = "V",
            ).method.replaceWithReturnVoid()
            patched++

            println("[History Recording Governor] Applied watch history suppression hooks -> Video watch history recording neutralized.")
        }

        if (disableSearchHistory == true) {
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

            println("[History Recording Governor] Applied search history suppression hooks -> Search history recording neutralized.")
        }

        if (patched == 0) {
            println("[History Recording Governor] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[History Recording Governor] Applied $patched history suppression hook(s).")
        }
    }
}
