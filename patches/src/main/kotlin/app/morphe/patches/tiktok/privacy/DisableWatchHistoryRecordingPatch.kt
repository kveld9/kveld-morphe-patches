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

        // 3. Neutralize PlaybackHistoryManager recordVideoPlayback routine (LX/03kz;->LIZ)
        Fingerprint(
            definingClass = "LX/03kz;",
            name = "LIZ",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        patched++

        // 4. Neutralize PlaybackHistoryManager recordWatchDuration routine (LX/03kz;->LIZIZ)
        Fingerprint(
            definingClass = "LX/03kz;",
            name = "LIZIZ",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        patched++

        // 5. Neutralize fast stats report routine (LX/0a8D;->subscribe)
        Fingerprint(
            definingClass = "LX/0a8D;",
            name = "subscribe",
            returnType = "V",
        ).method.replaceWithReturnVoid()
        patched++

        println("[Disable Watch History Recording] Applied $patched watch history suppression hook(s) -> Video watch history recording neutralized.")
    }
}
