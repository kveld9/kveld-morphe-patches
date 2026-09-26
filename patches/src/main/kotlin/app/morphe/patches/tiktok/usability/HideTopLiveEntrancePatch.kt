package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull

val hideTopLiveEntrancePatch = bytecodePatch(
    name = "Hide Top-Left LIVE Button",
    description = "Removes the top-left LIVE broadcast button and tab entry point from the top navigation bar.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Hook LiveIconGenerator in top toolbar
        val liveGenClass = "Lcom/bytedance/tiktok/homepage/mainfragment/toolbar/LiveIconGenerator;"

        Fingerprint(
            definingClass = liveGenClass,
            name = "enabled",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Top-Left LIVE Button] Hooked LiveIconGenerator.enabled() -> false")
        patched++

        Fingerprint(
            definingClass = liveGenClass,
            name = "LIZLLL",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Top-Left LIVE Button] Hooked LiveIconGenerator.LIZLLL() -> false")
        patched++

        val viewMethod = Fingerprint(
            definingClass = liveGenClass,
            returnType = "Landroid/view/View;",
            parameters = listOf("Landroid/content/Context;"),
        ).method
        viewMethod.replaceWithReturnNull()
        println("[Hide Top-Left LIVE Button] Hooked LiveIconGenerator.${viewMethod.name}() -> null")
        patched++

        // 2. Hook LiveTabProtocol (top tab variant)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/toptab/LiveTabProtocol;",
            name = "enable",
            returnType = "Z",
            parameters = emptyList(),
        ).method.replaceWithReturnBoolean(false)
        println("[Hide Top-Left LIVE Button] Hooked LiveTabProtocol.enable() -> false")
        patched++

        println("[Hide Top-Left LIVE Button] Successfully applied $patched hook(s).")
    }
}
