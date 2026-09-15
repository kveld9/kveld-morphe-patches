package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

import com.android.tools.smali.dexlib2.AccessFlags

val playbackSpeedPatch = bytecodePatch(
    name = "Playback Speed Persistence",
    description = "Persists selected video playback speed across all feed videos and application restarts.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Hook Aweme.getParameterizedSpeed()F (Feed playback engine speed resolution)
        try {
            val speedMethod = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                name = "getParameterizedSpeed",
                returnType = "F",
            ).method
            speedMethod.removeInstructions(0, speedMethod.implementation!!.instructions.count())
            speedMethod.addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_SPEED_HOOK}->getPlaybackSpeed(Ljava/lang/Object;)F
                    move-result v0
                    return v0
                """.trimIndent(),
            )
            println("[Playback Speed Persistence] Hooked Aweme.getParameterizedSpeed() -> Persistent feed speed resolution active.")
            patched++
        } catch (e: Exception) {
            println("[Playback Speed Persistence] Aweme.getParameterizedSpeed note: ${e.message}")
        }

        // 2. Hook UI Speed Selection Handler (User selecting speed in TuxSheet / long press / share panel)
        try {
            val speedSelectFp = Fingerprint(
                strings = listOf("swipe_up_lock_persist", "click_share_button", "long_press"),
                parameters = listOf("F", "Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Ljava/lang/String;", "Ljava/lang/String;"),
                returnType = "V",
            )
            val speedReg = if (AccessFlags.STATIC.isSet(speedSelectFp.method.accessFlags)) "p0" else "p1"
            speedSelectFp.method.addInstructions(
                0,
                """
                    invoke-static {$speedReg}, ${Constants.TIKTOK_EXTENSION_SPEED_HOOK}->onSpeedSelected(F)V
                """.trimIndent(),
            )
            println("[Playback Speed Persistence] Hooked native speed selection handler (${speedSelectFp.classDef.type}->${speedSelectFp.method.name}) -> Real-time speed persistence active.")
            patched++

            // 3. Hook Speed Selection UI Active Indicator in the same class (ensures UI sheet radio button reflects persistent speed)
            val queryMethods = speedSelectFp.classDef.methods.filter {
                it.implementation != null &&
                    it.parameterTypes == listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;") &&
                    it.returnType == "F"
            }
            queryMethods.forEach { method ->
                val awemeReg = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
                method.removeInstructions(0, method.implementation!!.instructions.count())
                method.addInstructions(
                    0,
                    """
                        invoke-static {$awemeReg}, ${Constants.TIKTOK_EXTENSION_SPEED_HOOK}->getPlaybackSpeed(Ljava/lang/Object;)F
                        move-result v0
                        return v0
                    """.trimIndent(),
                )
            }
            if (queryMethods.isNotEmpty()) {
                println("[Playback Speed Persistence] Hooked speed query methods (${queryMethods.size} method(s)) -> UI dialog synchronization active.")
                patched++
            }
        } catch (e: Exception) {
            println("[Playback Speed Persistence] Speed selection handler note: ${e.message}")
        }

        println("[Playback Speed Persistence] Applied $patched playback speed hook(s) -> Persistent speed active.")
    }
}
