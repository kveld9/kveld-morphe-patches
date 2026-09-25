package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.AccessFlags

private const val AWEME_CLASS = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"

val showSeekbarPatch = bytecodePatch(
    name = "Show Seekbar",
    description = "Restores TikTok's native video seekbar and scrubbing controls where normally hidden or disabled.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Aweme.getVideoControl() -> force draftProgressBar = 1 and showProgressBar = 1
        try {
            val fp = Fingerprint(
                definingClass = AWEME_CLASS,
                name = "getVideoControl",
                returnType = "Lcom/ss/android/ugc/aweme/feed/model/VideoControl;",
            )
            val method = fp.method
            method.ensureRegisterCount(3)
            method.addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->videoControl:Lcom/ss/android/ugc/aweme/feed/model/VideoControl;
                    if-eqz v0, :show_seekbar_init
                    new-instance v0, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;
                    invoke-direct {v0}, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;-><init>()V
                    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->videoControl:Lcom/ss/android/ugc/aweme/feed/model/VideoControl;
                    :show_seekbar_init
                    const/4 v1, 0x1
                    iput v1, v0, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;->draftProgressBar:I
                    iput v1, v0, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;->showProgressBar:I
                    return-object v0
                """.trimIndent(),
            )
            println("[Show Seekbar] Hooked Aweme.getVideoControl() -> Forced draftProgressBar=1 and showProgressBar=1.")
            patched++
        } catch (e: Exception) {
            println("[Show Seekbar] Aweme.getVideoControl note: ${e.message}")
        }

        // 2. SetSeekBarShowType handler -> override hidden types (3 and 4) to visible type 0
        try {
            val fp = Fingerprint(
                strings = listOf("seekbar show type change, change to:"),
            )
            val method = fp.method
            if (method.parameterTypes.size == 1 && method.parameterTypes[0] == "I") {
                val isStatic = (method.accessFlags and AccessFlags.STATIC.value) != 0
                val paramReg = if (isStatic) "p0" else "p1"
                method.addInstructions(
                    0,
                    """
                        invoke-static {$paramReg}, ${Constants.TIKTOK_EXTENSION_SEEKBAR_HOOK}->sanitizeSeekbarShowType(I)I
                        move-result $paramReg
                    """.trimIndent(),
                )
                println("[Show Seekbar] Hooked setSeekBarShowType($paramReg) -> Safe runtime show type normalizer active.")
                patched++
            } else {
                println("[Show Seekbar] Note: setSeekBarShowType signature did not match (I)V.")
            }
        } catch (e: Exception) {
            println("[Show Seekbar] SetSeekBarShowType note: ${e.message}")
        }

        println("[Show Seekbar] Applied $patched seekbar scrubbing hook(s).")
    }
}
