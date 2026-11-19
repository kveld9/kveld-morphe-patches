package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount

val blockCoinRewardsPatch = bytecodePatch(
    name = "Block Coin Rewards",
    description = "Removes floating reward and ad pendant widgets from the feed and the Rewards entry from your profile.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK_LITE)

    execute {
        var patched = 0

        // Touchpoint timer pendants (watch-time reward widgets, ad timers and task events).
        Fingerprint(
            definingClass = "LX/8pk;",
            name = "LD",
            parameters = listOf("Landroid/content/Context;", "Landroid/view/ViewGroup;"),
            returnType = "V",
        ).method.addInstructions(0, "return-void")
        println("[Block Coin Rewards] Neutralized timer pendant presenters (LX/8pk;->LD).")
        patched++

        // Touchpoint normal pendants (icon and label floating widget).
        Fingerprint(
            definingClass = "LX/8QW;",
            name = "LB",
            parameters = listOf("Landroid/view/ViewGroup;"),
            returnType = "V",
        ).method.addInstructions(0, "return-void")
        println("[Block Coin Rewards] Neutralized normal pendant presenter (LX/8QW;->LB).")
        patched++

        // Profile quick actions: the incentive entry provider adds the Rewards pill.
        val profileRewards = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/incentive/IncentiveServiceImpl;",
            name = "LIIIII",
            returnType = "LX/Bg5;",
        ).method
        profileRewards.ensureRegisterCount(1)
        profileRewards.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        println("[Block Coin Rewards] Neutralized IncentiveServiceImpl.LIIIII() -> profile Rewards entry suppressed.")
        patched++

        println("[Block Coin Rewards] Applied $patched hooks -> coin rewards and reward pendants blocked.")
    }
}
