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
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0
        val targetClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;"

        // 1. Hook Qr(ViewGroup, int, Object) to permanently force GONE visibility and non-clickable
        val qrMethod = Fingerprint(
            definingClass = targetClass,
            custom = { m, _ ->
                m.parameterTypes.size == 3 &&
                    m.parameterTypes[0] == "Landroid/view/ViewGroup;" &&
                    m.parameterTypes[1] == "I" &&
                    m.returnType == "V"
            },
        ).method

        qrMethod.clearTryBlocks()
        val count = qrMethod.implementation!!.instructions.count()
        qrMethod.removeInstructions(0, count)
        qrMethod.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFollowButton(Landroid/view/View;)V
                return-void
            """.trimIndent(),
        )
        println("[Hide Profile Photo Follow Button] Hooked FeedAvatarDefaultAssem.${qrMethod.name}() -> Permanently GONE (0x8) and non-clickable.")
        patched++

        // 2. Hook onViewCreated to initialize follow_view_container as GONE immediately after view binding
        val onViewCreatedMethod = Fingerprint(
            definingClass = targetClass,
            name = "onViewCreated",
            returnType = "V",
            parameters = listOf("Landroid/view/View;"),
        ).method

        val instructions = onViewCreatedMethod.implementation!!.instructions
        val iputIndex = instructions.indexOfFirst {
            val refStr = (it as? ReferenceInstruction)?.reference?.toString() ?: ""
            refStr.contains("FeedAvatarDefaultAssem;->") && refStr.contains(":Landroid/view/ViewGroup;")
        }

        if (iputIndex != -1) {
            val regA = (instructions[iputIndex] as TwoRegisterInstruction).registerA
            onViewCreatedMethod.addInstructions(
                iputIndex + 1,
                """
                    invoke-static {v$regA}, ${Constants.TIKTOK_EXTENSION_MEDIA_HOOK}->hideFollowButton(Landroid/view/View;)V
                """.trimIndent(),
            )
            println("[Hide Profile Photo Follow Button] Hooked FeedAvatarDefaultAssem.onViewCreated() -> Immediate GONE initialization on v$regA.")
            patched++
        }

        println("[Hide Profile Photo Follow Button] Successfully applied $patched hook(s).")
    }
}
