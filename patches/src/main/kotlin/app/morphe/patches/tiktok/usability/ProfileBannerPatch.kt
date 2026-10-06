package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

val profileBannerPatch = bytecodePatch(
    name = "Enable Profile Banner",
    description = "Unlocks the custom profile banner (background header cover) feature on user profiles and enables the banner selection and editing tools in Edit Profile.",
    default = true,
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

        // 2. Hook Snapshot constructor so allowListValue (abValue) is always 1
        // LX/0OSK;->LIZ()LX/0OSM; returns a snapshot instance passed to ProfilePlatformViewModel
        // to populate the Retrofit query 'profile_bg_in_allow_list'. Forcing the snapshot's
        // allowListValue to 1 prevents the TikTok backend from stripping the profile background.
        val snapshotMethod = expClass.methods.first { m ->
            m.parameterTypes.isEmpty() && m.returnType.startsWith("L")
        }
        val snapshotClass = mutableClassDefBy(snapshotMethod.returnType)
        val snapshotConstructor = snapshotClass.methods.first { m ->
            m.name == "<init>" && m.parameterTypes.size == 2 && m.parameterTypes[1] == "I"
        }
        snapshotConstructor.addInstructions(
            0,
            """
                const/4 p2, 0x1
            """.trimIndent(),
        )
        println("[Enable Profile Banner] Hooked ${snapshotClass.type}-><init>(Ljava/lang/String;I)V -> forced allowListValue (p2) to 1.")
        patched++

        println("[Enable Profile Banner] Applied $patched hook(s) -> profile banner enabled.")
    }
}
