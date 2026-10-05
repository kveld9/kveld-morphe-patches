package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.sharedExtensionPatch

val autoPauseFirstVideoPatch = bytecodePatch(
    name = "Auto-Pause First Video",
    description = "Automatically pauses the first video when opening TikTok, allowing the application to finish background initialization and preventing playback lag.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        var patched = 0

        val playerControllerFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
        )

        // 1. Hook PlayerController.l0 right before return to pause first video after engine setup
        val l0Method = playerControllerFp.classDef.methods.firstOrNull {
            it.name == "l0" && it.parameterTypes.size == 3 && it.returnType == "Ljava/lang/String;"
        } ?: error("Could not find l0(Aweme, I, Z)Ljava/lang/String; method in PlayerController")

        val l0ReturnIndex = l0Method.implementation!!.instructions.indexOfLast {
            it.opcode.name.startsWith("return")
        }
        if (l0ReturnIndex == -1) error("Could not find return instruction in PlayerController.l0")

        l0Method.addInstructionsAtControlFlowLabel(
            l0ReturnIndex,
            """
                invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_AUTOPAUSE_HOOK}->onFirstVideoLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
            """.trimIndent(),
        )
        println("[Auto-Pause First Video] Hooked PlayerController.l0 -> Post-load pause & play icon active.")
        patched++

        // 2. Hook PlayerController.W6 to block automatic playback resume by tryResumePlay
        val w6Method = playerControllerFp.classDef.methods.firstOrNull {
            it.name == "W6" && it.parameterTypes.size == 3 && it.returnType == "Ljava/lang/String;"
        } ?: error("Could not find W6(Aweme, I, Z)Ljava/lang/String; method in PlayerController")

        w6Method.addInstructions(
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
        println("[Auto-Pause First Video] Hooked PlayerController.W6 -> Handover tryResumePlay suppressed.")
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

        println("[Auto-Pause First Video] Applied $patched hook(s) -> First video auto-pause active.")
    }
}
