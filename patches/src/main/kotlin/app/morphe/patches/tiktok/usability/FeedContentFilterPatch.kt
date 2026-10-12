package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val feedContentFilterPatch = bytecodePatch(
    name = "Filter Feed Content",
    description = "Hides stories, photos and videos by views/likes.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val minViews by intOption(
        key = "minViews",
        title = "Min views",
        description = "Hide below this. 0 = off.",
        default = 0,
        required = false,
    )

    val maxViews by intOption(
        key = "maxViews",
        title = "Max views",
        description = "Hide above this. 0 = off.",
        default = 0,
        required = false,
    )

    val minLikes by intOption(
        key = "minLikes",
        title = "Min likes",
        description = "Hide below this. 0 = off.",
        default = 0,
        required = false,
    )

    val maxLikes by intOption(
        key = "maxLikes",
        title = "Max likes",
        description = "Hide above this. 0 = off.",
        default = 0,
        required = false,
    )

    val hideStories by booleanOption(
        key = "hideStories",
        default = true,
        title = "Stories",
        description = "Hides story posts.",
        required = false,
    )

    val hidePhotoPosts by booleanOption(
        key = "hidePhotoPosts",
        default = false,
        title = "Photo posts",
        description = "Hides photo posts.",
        required = false,
    )

    execute {
        var patched = 0
        val hook = Constants.TIKTOK_EXTENSION_CONTENT_FILTER_HOOK

        // 1. Push patch-time configuration into the runtime hook.
        try {
            val minV = (minViews ?: 0).coerceAtLeast(0).toLong()
            val maxV = (maxViews ?: 0).coerceAtLeast(0).toLong()
            val minL = (minLikes ?: 0).coerceAtLeast(0).toLong()
            val maxL = (maxLikes ?: 0).coerceAtLeast(0).toLong()
            val clinit = Fingerprint(
                definingClass = hook,
                name = "<clinit>",
            ).method
            clinit.ensureRegisterCount(2)
            val instructions = clinit.implementation!!.instructions
            val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            val insertIdx = if (returnIdx != -1) returnIdx else 0
            clinit.addInstructions(
                insertIdx,
                """
                    const-wide v0, 0x${minV.toString(16)}
                    sput-wide v0, $hook->minViews:J
                    const-wide v0, 0x${maxV.toString(16)}
                    sput-wide v0, $hook->maxViews:J
                    const-wide v0, 0x${minL.toString(16)}
                    sput-wide v0, $hook->minLikes:J
                    const-wide v0, 0x${maxL.toString(16)}
                    sput-wide v0, $hook->maxLikes:J
                    const/4 v0, ${if (hideStories != false) "0x1" else "0x0"}
                    sput-boolean v0, $hook->hideStories:Z
                    const/4 v0, ${if (hidePhotoPosts == true) "0x1" else "0x0"}
                    sput-boolean v0, $hook->hidePhotoPosts:Z
                """.trimIndent(),
            )
            println("[Feed Content Filter] Pushed configuration (views ${minViews ?: 0}..${maxViews ?: 0}, likes ${minLikes ?: 0}..${maxLikes ?: 0}, stories=$hideStories, photos=$hidePhotoPosts).")
            patched++
        } catch (e: Exception) {
            println("[Feed Content Filter] Configuration note: ${e.message}")
        }

        // 2. Hook FeedApiService.fetchFeedList return points (live FYP responses).
        try {
            val method = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/FeedApiService;",
                name = "fetchFeedList",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
            ).method
            val returnIndices = method.implementation?.instructions?.withIndex()
                ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()
            returnIndices.asReversed().forEach { (returnIndex, reg) ->
                method.addInstructionsAtControlFlowLabel(
                    returnIndex,
                    "invoke-static {v$reg}, $hook->filterContentInFeedItemList(Ljava/lang/Object;)V",
                )
            }
            if (returnIndices.isNotEmpty()) {
                println("[Feed Content Filter] Hooked FeedApiService.fetchFeedList (${returnIndices.size} return(s)).")
                patched++
            }
        } catch (e: Exception) {
            println("[Feed Content Filter] FeedApiService note: ${e.message}")
        }

        println("[Feed Content Filter] Applied $patched content filter hook(s).")
    }
}
