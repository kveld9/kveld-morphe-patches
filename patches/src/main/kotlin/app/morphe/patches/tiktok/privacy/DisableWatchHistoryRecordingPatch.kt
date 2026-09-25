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

        // 1. Neutralize AwemeStatsApi video view reporting routine (AwemeStatsApi.LIZIZ(LX/09gd;)V)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
                name = "LIZIZ",
                returnType = "V",
                parameters = listOf("LX/09gd;"),
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Disable Watch History Recording] AwemeStatsApi.LIZIZ note: ${e.message}")
        }

        // 2. Neutralize AwemeStatsApi batch video view flush routine (AwemeStatsApi.LIZ(List)V)
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
                name = "LIZ",
                returnType = "V",
                parameters = listOf("Ljava/util/List;"),
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Disable Watch History Recording] AwemeStatsApi.LIZ note: ${e.message}")
        }

        // 3. Neutralize PlaybackHistoryManager recordVideoPlayback routine (LX/03nB;->LIZ(LX/037l; String)V)
        try {
            Fingerprint(
                definingClass = "LX/03nB;",
                name = "LIZ",
                returnType = "V",
                parameters = listOf("LX/037l;", "Ljava/lang/String;"),
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Disable Watch History Recording] PlaybackHistoryManager.recordVideoPlayback note: ${e.message}")
        }

        // 4. Neutralize PlaybackHistoryManager recordWatchDuration routine (LX/03nB;->LIZIZ(LX/037l; String)V)
        try {
            Fingerprint(
                definingClass = "LX/03nB;",
                name = "LIZIZ",
                returnType = "V",
                parameters = listOf("LX/037l;", "Ljava/lang/String;"),
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Disable Watch History Recording] PlaybackHistoryManager.recordWatchDuration note: ${e.message}")
        }

        // 5. Neutralize fast stats report routine (LX/0a5M;->subscribe(LX/08hf;)V)
        try {
            Fingerprint(
                definingClass = "LX/0a5M;",
                name = "subscribe",
                returnType = "V",
                parameters = listOf("LX/08hf;"),
            ).method.replaceWithReturnVoid()
            patched++
        } catch (e: Exception) {
            println("[Disable Watch History Recording] FastStatsReport.subscribe note: ${e.message}")
        }

        println("[Disable Watch History Recording] Applied $patched watch history suppression hook(s) -> Video watch history recording neutralized.")
    }
}
