package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

val hideAvatarFollowButtonPatch = bytecodePatch(
    name = "Hide Profile Photo Follow Button",
    description = "Hides the plus (+) follow badge on creator profile avatars in the feed and disables its touch interaction.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0
        val targetClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;"

        // 1. Hook cs(ViewGroup, int, Object) to permanently force GONE visibility and non-clickable
        try {
            val csMethod = Fingerprint(
                definingClass = targetClass,
                name = "cs",
                returnType = "V",
            ).method

            csMethod.clearTryBlocks()
            csMethod.ensureRegisterCount(5)
            val count = csMethod.implementation!!.instructions.count()
            csMethod.removeInstructions(0, count)
            csMethod.addInstructions(
                0,
                """
                    if-eqz p1, :cond_skip
                    const/16 v0, 0x8
                    invoke-virtual {p1, v0}, Landroid/view/View;->setVisibility(I)V
                    const/4 v0, 0x0
                    invoke-virtual {p1, v0}, Landroid/view/View;->setClickable(Z)V
                    :cond_skip
                    return-void
                """.trimIndent(),
            )
            println("[Hide Profile Photo Follow Button] Hooked FeedAvatarDefaultAssem.cs() -> Permanently GONE (0x8) and non-clickable.")
            patched++
        } catch (e: Exception) {
            println("[Hide Profile Photo Follow Button] cs note: ${e.message}")
        }

        // 2. Hook onViewCreated to initialize follow_view_container as GONE immediately after view binding
        try {
            val onViewCreatedMethod = Fingerprint(
                definingClass = targetClass,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            ).method

            val instructions = onViewCreatedMethod.implementation!!.instructions
            val iputIndex = instructions.indexOfFirst {
                (it as? ReferenceInstruction)?.reference?.toString()?.contains("FeedAvatarDefaultAssem;->LLLIILIL:Landroid/view/ViewGroup;") == true
            }

            if (iputIndex != -1) {
                val regA = (instructions[iputIndex] as TwoRegisterInstruction).registerA
                onViewCreatedMethod.addInstructions(
                    iputIndex + 1,
                    """
                        if-eqz v$regA, :cond_skip_init
                        const/16 v0, 0x8
                        invoke-virtual {v$regA, v0}, Landroid/view/View;->setVisibility(I)V
                        const/4 v0, 0x0
                        invoke-virtual {v$regA, v0}, Landroid/view/View;->setClickable(Z)V
                        :cond_skip_init
                    """.trimIndent(),
                )
                println("[Hide Profile Photo Follow Button] Hooked FeedAvatarDefaultAssem.onViewCreated() -> Immediate GONE initialization on v$regA.")
                patched++
            }
        } catch (e: Exception) {
            println("[Hide Profile Photo Follow Button] onViewCreated note: ${e.message}")
        }

        println("[Hide Profile Photo Follow Button] Successfully applied $patched hook(s).")
    }
}
