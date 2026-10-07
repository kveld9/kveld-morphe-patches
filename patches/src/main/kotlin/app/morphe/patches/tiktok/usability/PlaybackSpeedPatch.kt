package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Locates the AB-helper class exposing the boolean/int experiment lookups and returns
 * its type, or null when the obfuscated helper shape shifted.
 */
private fun BytecodePatchContext.findAbHelperType(): String? {
    var helperType: String? = null
    classDefForEach { classDef ->
        if (helperType != null) return@classDefForEach
        val methods = classDef.methods
        val hasGetter = methods.any { it.name == "LJIIIZ" && it.parameterTypes.isEmpty() }
        if (!hasGetter) return@classDefForEach
        val hasBoolGate = methods.any {
            it.name == "LIZJ" &&
                it.parameterTypes.map { p -> p.toString() } == listOf("I", "Ljava/lang/String;", "Z", "Z") &&
                it.returnType == "Z"
        }
        val hasIntGate = methods.any {
            it.name == "LJIIJJI" &&
                it.parameterTypes.map { p -> p.toString() } == listOf("I", "I", "Ljava/lang/String;", "Z") &&
                it.returnType == "I"
        }
        if (hasBoolGate && hasIntGate) helperType = classDef.type
    }
    return helperType
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

        // 5. Hold-and-slide 2x speed lock (native long-press speed-up rollout gates).
        // The gates are read through the AB-helper class (obfuscated, triaged per target
        // version); hooks are key-scoped so other experiments are untouched.
        if (enableSpeedLock == true) {
            try {
                val helper = findAbHelperType()
                    ?: error("AB-helper class (LJIIIZ/LIZJ/LJIIJJI) not found")
                println("[Playback Speed Persistence] AB-helper class: $helper.")

                val boolGate = Fingerprint(
                    definingClass = helper,
                    name = "LIZJ",
                    parameters = listOf("I", "Ljava/lang/String;", "Z", "Z"),
                    returnType = "Z",
                ).method
                boolGate.ensureRegisterCount(2)
                val boolReturns = boolGate.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()
                boolReturns.asReversed().forEachIndexed { ordinal, (returnIndex, reg) ->
                    // Only the return register is live at a return point; scratch must differ.
                    // Descending order keeps earlier indices valid after each insertion.
                    val scratch = if (reg == 0) 1 else 0
                    boolGate.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            if-eqz p2, :speed_lock_keep_stock_$ordinal
                            const-string v$scratch, "long_press_speed_up_enable"
                            invoke-virtual {p2, v$scratch}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                            move-result v$scratch
                            if-eqz v$scratch, :speed_lock_keep_stock_$ordinal
                            const/4 v$reg, 0x1
                            :speed_lock_keep_stock_$ordinal
                            nop
                        """.trimIndent(),
                    )
                }
                if (boolReturns.isNotEmpty()) {
                    println("[Playback Speed Persistence] Forced long_press_speed_up_enable gate -> true (${boolReturns.size} return(s)).")
                    patched++
                }

                val intGate = Fingerprint(
                    definingClass = helper,
                    name = "LJIIJJI",
                    parameters = listOf("I", "I", "Ljava/lang/String;", "Z"),
                    returnType = "I",
                ).method
                intGate.ensureRegisterCount(2)
                val intReturns = intGate.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()
                intReturns.asReversed().forEachIndexed { ordinal, (returnIndex, reg) ->
                    // Only the return register is live at a return point; scratch must differ.
                    // Descending order keeps earlier indices valid after each insertion.
                    val scratch = if (reg == 0) 1 else 0
                    intGate.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            if-eqz p3, :speed_lock_keep_distance_$ordinal
                            const-string v$scratch, "long_press_speed_up_lock"
                            invoke-virtual {p3, v$scratch}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                            move-result v$scratch
                            if-eqz v$scratch, :speed_lock_keep_distance_$ordinal
                            if-lez v$reg, :speed_lock_keep_distance_$ordinal
                            const/16 v$reg, 0x8c
                            :speed_lock_keep_distance_$ordinal
                            nop
                        """.trimIndent(),
                    )
                }
                if (intReturns.isNotEmpty()) {
                    println("[Playback Speed Persistence] Clamped long_press_speed_up_lock distance -> 140dp fallback (${intReturns.size} return(s)).")
                    patched++
                }
            } catch (e: Exception) {
                println("[Playback Speed Persistence] Long-press speed-up lock note: ${e.message}")
            }
        }

        println("[Playback Speed Persistence] Applied $patched playback speed hook(s) -> Persistent speed active.")
    }
}
