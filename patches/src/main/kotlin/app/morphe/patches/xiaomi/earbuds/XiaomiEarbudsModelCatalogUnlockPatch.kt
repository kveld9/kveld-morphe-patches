package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val xiaomiEarbudsModelCatalogUnlockPatch = bytecodePatch(
    name = "Xiaomi Earbuds Model Catalog Unlock",
    description = "Forces DeviceInfoListCache.isShowProduct to return true, bypassing version-gating and distribution restrictions so that all device models are always displayed and discoverable.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/cache/DeviceInfoListCache;",
            name = "isShowProduct",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
        }

        println("[Xiaomi Earbuds Model Catalog Unlock] Unlocked full device model catalog (isShowProduct -> true).")
    }
}
