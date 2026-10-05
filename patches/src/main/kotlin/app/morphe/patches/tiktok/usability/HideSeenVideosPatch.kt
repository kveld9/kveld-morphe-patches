package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val hideSeenVideosPatch = bytecodePatch(
    name = "Hide Seen Videos",
    description = "Filters previously watched videos from incoming For You feed batches.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        // 1. Hook PlayerController playback progress and completion callbacks
        val playerControllerFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
        )

        val onProgressMethod = playerControllerFp.classDef.methods.firstOrNull {
            it.name == "onPlayProgressChange" &&
                it.parameterTypes == listOf("Ljava/lang/String;", "J", "J") &&
                it.returnType == "V"
        }
        if (onProgressMethod != null) {
            onProgressMethod.addInstructions(
                0,
                """
                    invoke-static/range {p1 .. p5}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->onPlayProgressChange(Ljava/lang/String;JJ)V
                """.trimIndent(),
            )
            println("[Hide Seen Videos] Hooked PlayerController.onPlayProgressChange -> Playback threshold tracker active.")
            patched++
        } else {
            println("[Hide Seen Videos] Skipped: PlayerController.onPlayProgressChange(String, long, long) not found.")
        }

        val onCompletedMethod = playerControllerFp.classDef.methods.firstOrNull {
            it.name == "onPlayCompleted" &&
                it.parameterTypes == listOf("Ljava/lang/String;") &&
                it.returnType == "V"
        }
        if (onCompletedMethod != null) {
            onCompletedMethod.addInstructions(
                0,
                """
                    invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->onPlayCompleted(Ljava/lang/String;)V
                """.trimIndent(),
            )
            println("[Hide Seen Videos] Hooked PlayerController.onPlayCompleted -> Completion tracker active.")
            patched++
        } else {
            println("[Hide Seen Videos] Skipped: PlayerController.onPlayCompleted(String) not found.")
        }

        // 2. Hook FeedApiService.fetchFeedList return points (live FYP network responses)
        val feedApiFingerprint = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
            name = "fetchFeedList",
            returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
        )
        val feedApiMethod = feedApiFingerprint.method
        val feedApiReturns = feedApiMethod.implementation?.instructions?.withIndex()
            ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
            ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
            ?.toList() ?: emptyList()

        feedApiReturns.asReversed().forEach { (returnIndex, reg) ->
            feedApiMethod.addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_SEEN_VIDEO_HOOK}->filterSeenVideosInFeedItemList(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }
        if (feedApiReturns.isNotEmpty()) {
            println("[Hide Seen Videos] Hooked FeedApiService.fetchFeedList() (${feedApiReturns.size} return point(s)) -> FYP incoming batch filter active.")
            patched++
        }

        println("[Hide Seen Videos] Applied $patched seen video filter hook(s).")
    }
}
