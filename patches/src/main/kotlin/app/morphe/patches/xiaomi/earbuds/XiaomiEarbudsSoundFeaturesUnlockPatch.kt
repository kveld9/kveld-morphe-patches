package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val xiaomiEarbudsSoundFeaturesUnlockPatch = bytecodePatch(
    name = "Unlock Sound Features",
    description = "Enables spatial audio and hearing features on all devices.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. DeviceManagerImpl.queryRestrictItem(String, byte)[B -> returns null (lifting all hardware/cloud restriction items)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/manager/DeviceManagerImpl;",
            name = "queryRestrictItem",
            parameters = listOf("Ljava/lang/String;", "B"),
            returnType = "[B",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("DeviceManagerImpl.queryRestrictItem")
        }

        // 2. SpatialAudioVM.isNotSupport96K()Z -> returns false (unbans Spatial Audio on aptX Adaptive 96kHz connections)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/spatialaudio/SpatialAudioVM;",
            name = "isNotSupport96K",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SpatialAudioVM.isNotSupport96K")
        }

        // 3. SoundEffectVM.isEnableSpatialAudio()Z -> returns true (always enables spatial audio setting item)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/soundeffect/SoundEffectVM;",
            name = "isEnableSpatialAudio",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SoundEffectVM.isEnableSpatialAudio")
        }

        println("[Sound Features Unlock] Unlocked sound features and bypassed restrictions across ${hookedMethods.size} targets.")
        if (hookedMethods.isEmpty()) throw PatchException("Sound Features Unlock: no hooks applied, targets shifted.")
    }
}
