package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val autoPauseFirstVideoPatch = bytecodePatch(
    name = "Auto-Pause First Video",
    description = "Automatically pauses the first video when opening TikTok, allowing the application to finish background initialization and preventing playback lag.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        try {
            val playerControllerFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
            )

            // 1. Hook PlayerController.d0 right before return to pause first video after engine setup
            val d0Method = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "d0" && it.parameterTypes.size == 3 && it.returnType == "Ljava/lang/String;"
            } ?: error("Could not find d0(Aweme, I, Z)Ljava/lang/String; method in PlayerController")

            val d0ReturnIndex = d0Method.implementation!!.instructions.indexOfLast {
                it.opcode.name.startsWith("return")
            }
            if (d0ReturnIndex == -1) error("Could not find return instruction in PlayerController.d0")

            d0Method.addInstructions(
                d0ReturnIndex,
                """
                    invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_AUTOPAUSE_HOOK}->onFirstVideoLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Auto-Pause First Video] Hooked PlayerController.d0 -> Post-load pause & play icon active.")
            patched++

            // 2. Hook PlayerController.Q6 to block automatic playback resume by tryResumePlay
            val q6Method = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "Q6" && it.parameterTypes.size == 3 && it.returnType == "Ljava/lang/String;"
            } ?: error("Could not find Q6(Aweme, I, Z)Ljava/lang/String; method in PlayerController")

            q6Method.addInstructions(
                0,
                """
                    invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_AUTOPAUSE_HOOK}->shouldBlockQ6(Ljava/lang/Object;Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :cond_proceed_q6
                    const-string v0, ""
                    return-object v0
                    :cond_proceed_q6
                """.trimIndent(),
            )
            println("[Auto-Pause First Video] Hooked PlayerController.Q6 -> Handover tryResumePlay suppressed.")
            patched++

            // 3. Hook PlayerController.onResumePlay(String) to detect user tap and unlock playback
            val onResumePlayMethod = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "onResumePlay" && it.parameterTypes.size == 1 && it.returnType == "V"
            } ?: error("Could not find onResumePlay(Ljava/lang/String;)V method in PlayerController")

            onResumePlayMethod.addInstructions(
                0,
                """
                    invoke-static {}, ${Constants.TIKTOK_EXTENSION_AUTOPAUSE_HOOK}->onUserResumePlay()V
                """.trimIndent(),
            )
            println("[Auto-Pause First Video] Hooked PlayerController.onResumePlay -> User play detection active.")
            patched++

            // 4. Hook PlayerController.onPageScrollStateChanged(int) to unlock auto-play when user swipes feed
            val onScrollMethod = playerControllerFp.classDef.methods.firstOrNull {
                it.name == "onPageScrollStateChanged" && it.parameterTypes.size == 1 && it.returnType == "V"
            } ?: error("Could not find onPageScrollStateChanged(I)V method in PlayerController")

            onScrollMethod.addInstructions(
                0,
                """
                    invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_AUTOPAUSE_HOOK}->onPageScroll(I)V
                """.trimIndent(),
            )
            println("[Auto-Pause First Video] Hooked PlayerController.onPageScrollStateChanged -> Feed scroll detection active.")
            patched++
        } catch (e: Exception) {
            println("[Auto-Pause First Video] PlayerController note: ${e.message}")
        }

        println("[Auto-Pause First Video] Applied $patched hook(s) -> First video auto-pause active.")
    }
}
