package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Locates the first AB-gate lookup call with the given signature inside [method] and returns
 * the index of its `move-result` instruction together with the result register.
 */
private fun findAbGateResult(
    method: MutableMethod,
    parameterTypes: List<String>,
    returnType: String,
): Pair<Int, Int>? {
    val instructions = method.implementation?.instructions ?: return null
    for ((index, instruction) in instructions.withIndex()) {
        if (instruction.opcode?.name?.startsWith("INVOKE_") != true) continue
        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
        if (ref.parameterTypes.map { it.toString() } != parameterTypes || ref.returnType != returnType) continue
        val moveResult = instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
        if (moveResult.opcode != Opcode.MOVE_RESULT) continue
        return (index + 1) to moveResult.registerA
    }
    return null
}

val playbackSpeedPatch = bytecodePatch(
    name = "Playback Speed Persistence",
    description = "Persists selected video playback speed across all feed videos and application restarts, and optionally enables the native hold-and-slide 2x speed lock gesture.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val enableSpeedLock by booleanOption(
        key = "enableSpeedLock",
        default = true,
        title = "Hold-And-Slide 2x Speed Lock",
        description = "Enables TikTok's native hold, slide down, and release gesture for locking playback at 2x speed (140dp slide distance fallback when the server returns no distance).",
        required = false,
    )

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

        // 4. Hook PlayerManager.setSpeed(F)V (Core playback engine speed dispatcher)
        // Prevents search, profile, and non-FYP controllers from resetting playback speed back to 1.0f
        try {
            val playerManagerFp = Fingerprint(
                strings = listOf("PlayerManager con useV3:"),
            )
            val setSpeedMethod = playerManagerFp.classDef.methods.firstOrNull {
                it.implementation != null &&
                    it.parameterTypes == listOf("F") &&
                    it.returnType == "V" &&
                    it.name != "seek"
            } ?: error("Could not find setSpeed(F)V method in PlayerManager (${playerManagerFp.classDef.type})")

            val speedParamReg = if (AccessFlags.STATIC.isSet(setSpeedMethod.accessFlags)) "p0" else "p1"
            setSpeedMethod.addInstructions(
                0,
                """
                    invoke-static {$speedParamReg}, ${Constants.TIKTOK_EXTENSION_SPEED_HOOK}->resolvePlayerSpeed(F)F
                    move-result $speedParamReg
                """.trimIndent(),
            )
            println("[Playback Speed Persistence] Hooked core PlayerManager.setSpeed (${playerManagerFp.classDef.type}->${setSpeedMethod.name}) -> Global speed persistence across Search and Profiles active.")
            patched++
        } catch (e: Exception) {
            println("[Playback Speed Persistence] PlayerManager.setSpeed note: ${e.message}")
        }

        // 5. Hold-and-slide 2x speed lock (native long-press speed-up rollout gates)
        if (enableSpeedLock == true) {
            try {
                val enableMethod = Fingerprint(
                    name = "<clinit>",
                    returnType = "V",
                    parameters = emptyList(),
                    strings = listOf("long_press_speed_up_enable"),
                ).method
                val (moveIndex, resultReg) = findAbGateResult(
                    enableMethod,
                    listOf("I", "Ljava/lang/String;", "Z", "Z"),
                    "Z",
                ) ?: error("Long-press speed-up enable gate lookup not found")
                enableMethod.addInstructions(
                    moveIndex + 1,
                    "const/4 v$resultReg, 0x1",
                )
                println("[Playback Speed Persistence] Forced long-press speed-up enable gate -> true.")
                patched++
            } catch (e: Exception) {
                println("[Playback Speed Persistence] Long-press speed-up enable note: ${e.message}")
            }

            try {
                val lockMethod = Fingerprint(
                    name = "invoke",
                    returnType = "Ljava/lang/Object;",
                    parameters = emptyList(),
                    strings = listOf("long_press_speed_up_lock"),
                ).method
                val (moveIndex, resultReg) = findAbGateResult(
                    lockMethod,
                    listOf("I", "I", "Ljava/lang/String;", "Z"),
                    "I",
                ) ?: error("Long-press speed-up lock distance lookup not found")
                lockMethod.addInstructions(
                    moveIndex + 1,
                    """
                        if-lez v$resultReg, :speed_lock_keep_distance
                        const/16 v$resultReg, 0x8c
                        :speed_lock_keep_distance
                        nop
                    """.trimIndent(),
                )
                println("[Playback Speed Persistence] Clamped long-press speed-up lock distance -> 140dp fallback.")
                patched++
            } catch (e: Exception) {
                println("[Playback Speed Persistence] Long-press speed-up lock note: ${e.message}")
            }
        }

        println("[Playback Speed Persistence] Applied $patched playback speed hook(s) -> Persistent speed active.")
    }
}
