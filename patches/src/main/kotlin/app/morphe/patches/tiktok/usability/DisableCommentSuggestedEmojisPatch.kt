package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

val disableCommentSuggestedEmojisPatch = bytecodePatch(
    name = "Disable Comment Suggested Emojis",
    description = "Removes the horizontal bar of suggested quick emojis displayed above the comment input box.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook ExposedEmojiPanelTrigger.wr(...)Z -> return false
        // Prevents triggering and attaching HorizontalEmojiMiniPanelAssemForKeyboard to the active comment keyboard.
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/ExposedEmojiPanelTrigger;",
                returnType = "Z",
                custom = { method, _ ->
                    method.parameterTypes.firstOrNull()?.toString() == "Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;"
                },
            ).method.replaceWithReturnBoolean(false)
            println("[Disable Comment Suggested Emojis] Hooked ExposedEmojiPanelTrigger.wr -> return false (keyboard emoji bar trigger neutralized).")
            patched++
        } catch (e: Exception) {
            println("[Disable Comment Suggested Emojis] ExposedEmojiPanelTrigger note: ${e.message}")
        }

        // 2. Hook CommentPanelFakeInput.Gt()Z -> return false
        // Prevents attaching HorizontalEmojiMiniPanelAssem to the passive/feed comment bar.
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/CommentPanelFakeInput;",
                returnType = "Z",
                parameters = emptyList(),
                custom = { method, _ ->
                    method.implementation?.instructions?.any { instruction ->
                        val ref = (instruction as? ReferenceInstruction)?.reference?.toString() ?: ""
                        ref.contains("getForceDisableExposedEmoji") || ref.contains("PersonalizedEmojiExperiment")
                    } == true
                },
            ).method.replaceWithReturnBoolean(false)
            println("[Disable Comment Suggested Emojis] Hooked CommentPanelFakeInput.Gt -> return false (passive comment emoji bar neutralized).")
            patched++
        } catch (e: Exception) {
            println("[Disable Comment Suggested Emojis] CommentPanelFakeInput note: ${e.message}")
        }

        // 3. Hook CommentKeyboardModel.getForceDisableExposedEmoji()Z -> return true
        // Enforces TikTok's internal model flag to force-disable exposed emojis across all comment view models and cells.
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
                name = "getForceDisableExposedEmoji",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(true)
            println("[Disable Comment Suggested Emojis] Hooked CommentKeyboardModel.getForceDisableExposedEmoji -> return true.")
            patched++
        } catch (e: Exception) {
            println("[Disable Comment Suggested Emojis] CommentKeyboardModel note: ${e.message}")
        }

        // 4. Hook PersonalizedEmojiExperiment.LIZ()Z -> return true
        // Forces TikTok's internal hideExposeEmoji experiment flag to true, disabling emoji preloading, layout spacing, and telemetry.
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/comment/experiment/PersonalizedEmojiExperiment;",
                returnType = "Z",
                parameters = emptyList(),
                custom = { method, _ ->
                    method.implementation?.instructions?.any { instruction ->
                        (instruction as? ReferenceInstruction)?.reference?.toString()?.contains("hideExposeEmoji") == true
                    } == true
                },
            ).method.replaceWithReturnBoolean(true)
            println("[Disable Comment Suggested Emojis] Hooked PersonalizedEmojiExperiment.LIZ -> return true (hideExposeEmoji enabled).")
            patched++
        } catch (e: Exception) {
            println("[Disable Comment Suggested Emojis] PersonalizedEmojiExperiment note: ${e.message}")
        }

        if (patched == 0) {
            throw PatchException("Disable Comment Suggested Emojis: All 4 fingerprint hooks failed to match.")
        }

        println("[Disable Comment Suggested Emojis] Successfully applied $patched hook(s) -> comment suggested emojis disabled.")
    }
}
