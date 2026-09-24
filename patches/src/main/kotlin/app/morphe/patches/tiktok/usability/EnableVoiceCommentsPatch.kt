package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnBooleanObject
import app.morphe.patches.shared.replaceWithReturnIntegerObject

val enableVoiceCommentsPatch = bytecodePatch(
    name = "Enable Voice Comments",
    description = "Forces the native voice comment recording button in comment input bars, bypassing regional rollout restrictions and remote server blocks.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook audio_comment_publish AB experiment provider -> return Integer(1)
        // Enables TikTok's internal audio_comment_publish experiment across comment panels, quick reply bars, and keyboard view models.
        try {
            Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("audio_comment_publish"),
            ).method.replaceWithReturnIntegerObject(1)
            println("[Enable Voice Comments] Hooked audio_comment_publish experiment -> return Integer(1).")
            patched++
        } catch (e: Exception) {
            println("[Enable Voice Comments] audio_comment_publish note: ${e.message}")
        }

        // 2. Hook comment_audio_publish_entry_forbidden gate -> return true
        // Bypasses remote settings block and forces audio publish permission for the comment context.
        try {
            Fingerprint(
                returnType = "Z",
                parameters = listOf("Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;"),
                strings = listOf("comment_audio_publish_entry_forbidden"),
            ).method.replaceWithReturnBoolean(true)
            println("[Enable Voice Comments] Hooked comment_audio_publish_entry_forbidden gate -> return true.")
            patched++
        } catch (e: Exception) {
            println("[Enable Voice Comments] comment_audio_publish_entry_forbidden note: ${e.message}")
        }

        // 3. Hook CommentKeyboardModel.getForceDisableCommentAudio()Z -> return false
        // Neutralizes model-level force disable flag for voice comments.
        try {
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/comment/model/CommentKeyboardModel;",
                name = "getForceDisableCommentAudio",
                returnType = "Z",
                parameters = emptyList(),
            ).method.replaceWithReturnBoolean(false)
            println("[Enable Voice Comments] Hooked CommentKeyboardModel.getForceDisableCommentAudio -> return false.")
            patched++
        } catch (e: Exception) {
            println("[Enable Voice Comments] CommentKeyboardModel note: ${e.message}")
        }

        // 4. Hook VEAudioRecorder service ready check -> return true
        // Bypasses CreativeToolsPluginService dynamic plugin availability check to ensure VEAudioRecorder initializes.
        try {
            Fingerprint(
                returnType = "Z",
                parameters = listOf("Landroid/content/Context;"),
                custom = { method, classDef ->
                    method.definingClass.startsWith("LX/") &&
                    classDef.methods.any {
                        it.returnType == "Lcom/ss/android/vesdk/VEAudioRecorder;"
                    }
                },
            ).method.replaceWithReturnBoolean(true)
            println("[Enable Voice Comments] Hooked VEAudioRecorder plugin ready check -> return true.")
            patched++
        } catch (e: Exception) {
            println("[Enable Voice Comments] VEAudioRecorder plugin note: ${e.message}")
        }

        // 5. Hook comment_audio_asr_translate_enable experiment provider -> return Boolean.TRUE
        // Activates speech-to-text / ASR transcription and translation features for audio comments.
        try {
            Fingerprint(
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
                strings = listOf("comment_audio_asr_translate_enable"),
            ).method.replaceWithReturnBooleanObject(true)
            println("[Enable Voice Comments] Hooked comment_audio_asr_translate_enable experiment -> return Boolean.TRUE.")
            patched++
        } catch (e: Exception) {
            println("[Enable Voice Comments] comment_audio_asr_translate_enable note: ${e.message}")
        }

        if (patched == 0) {
            throw PatchException("Enable Voice Comments: All 5 fingerprint hooks failed to match.")
        }

        println("[Enable Voice Comments] Successfully applied $patched hook(s) -> native voice comments enabled.")
    }
}
