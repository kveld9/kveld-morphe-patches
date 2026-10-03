package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

val profileBannerPatch = bytecodePatch(
    name = "Enable Profile Banner",
    description = "Unlocks the custom profile banner (background header cover) feature on user profiles and enables the banner selection and editing tools in Edit Profile.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Hook ProfileBackgroundExp gate (LX/0OSK;->LIZJ(Z)Z) -> true
        val expFp = Fingerprint(
            strings = listOf("ProfileBackgroundExp", "profile_bg_in_allow_list"),
        )
        val expClass = expFp.classDef
        val gateMethod = expClass.methods.first { m ->
            m.returnType == "Z" && m.parameterTypes.size == 1 && m.parameterTypes[0] == "Z"
        }
        gateMethod.replaceWithReturnBoolean(true)
        println("[Enable Profile Banner] Hooked ${expClass.type}->${gateMethod.name}(Z)Z -> true (ProfileBackgroundExp gate enabled).")
        patched++

        println("[Enable Profile Banner] Applied $patched hook(s) -> profile banner enabled.")
    }
}
