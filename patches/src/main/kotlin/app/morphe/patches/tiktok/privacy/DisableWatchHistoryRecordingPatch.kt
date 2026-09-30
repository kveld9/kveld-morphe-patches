package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnVoid

val disableWatchHistoryRecordingPatch = bytecodePatch(
    name = "Disable Watch History Recording",
    description = "Prevents viewed videos from being recorded in account watch history, playback duration stores, and local history caches.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

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

        println("[Disable Watch History Recording] Applied $patched watch history suppression hook(s) -> Video watch history recording neutralized.")
    }
}
