package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import app.morphe.patches.shared.sharedExtensionPatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardFeatureFlagsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    dependsOn(gboardCoreIntegrityPatch)

    val enableAccessPointsRedesign by booleanOption(
        key = "enableAccessPointsRedesign",
        default = true,
        title = "New toolbar menu",
        description = "New access points menu and panel.",
        required = false,
    )

    val enableKeyShapeSelection by booleanOption(
        key = "enableKeyShapeSelection",
        default = true,
        title = "Key shapes",
        description = "Round and borderless key styles.",
        required = false,
    )

    val enableCursorTrackpad by booleanOption(
        key = "enableCursorTrackpad",
        default = true,
        title = "Cursor trackpad",
        description = "Move cursor by sliding on spacebar.",
        required = false,
    )

    val enableDismissSuggestionsButton by booleanOption(
        key = "enableDismissSuggestionsButton",
        default = true,
        title = "Close button on suggestions",
        description = "Adds an X to dismiss suggestions.",
        required = false,
    )

    val enableGrammarChecker by booleanOption(
        key = "enableGrammarChecker",
        default = true,
        title = "Grammar check",
        description = "Grammar correction and smart predictions.",
        required = false,
    )

    val enableBluetoothMicrophone by booleanOption(
        key = "enableBluetoothMicrophone",
        default = true,
        title = "Bluetooth mic",
        description = "Use Bluetooth microphone for dictation.",
        required = false,
    )

    execute {
        var patched = 0

        fun flagFingerprint(vararg flags: String) = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = flags.map { string(it) },
        )

        fun hookFlag(flag: String, hook: String, label: String) {
            val fp = flagFingerprint(flag)
            fp.method.overrideFlagWithHook(fp.instructionMatches.first().index, flag, hook)
            val targetClass = cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] $label: Injected isolated flag hook for $flag into $targetClass.<clinit>()")
            patched++
        }

        // 1. Access Points Menu Redesign (Panel V2)
        if (enableAccessPointsRedesign == true) {
            hookFlag("enable_access_points_menu_redesign", "isAccessPointsRedesignEnabled", "Access Points Redesign")
        }

        // 2. Key Shape Selection UI (more_pill_keys Phenotype flag -> dynamic Morphe preference)
        if (enableKeyShapeSelection == true) {
            hookFlag("more_pill_keys", "isKeyShapeSelectionEnabled", "Key Shape Selection")
        }

        // 3. Cursor Trackpad Mode (2D spacebar navigation & lock)
        if (enableCursorTrackpad == true) {
            val cursorFlags = listOf("free_cursor", "free_cursor_lock_mode", "free_cursor_trackpadlike")
            val fp = flagFingerprint(*cursorFlags.toTypedArray())
            // Descending order keeps earlier match indices valid while instructions are inserted.
            fp.instructionMatches.zip(cursorFlags).sortedByDescending { it.first.index }.forEach { (match, flag) ->
                fp.method.overrideFlagWithHook(match.index, flag, "isCursorTrackpadEnabled")
            }
            val targetClass = cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Cursor Trackpad: Injected ${cursorFlags.size} isolated flag hook(s) into $targetClass.<clinit>() -> 2D spacebar trackpad enabled.")
            patched++
        }

        // 4. Dismiss Suggestions Button (Close X button)
        if (enableDismissSuggestionsButton == true) {
            hookFlag("enable_close_proactive_suggestions_access_point", "isDismissSuggestionsEnabled", "Dismiss Suggestions")
        }

        // 5. Grammar Checker & Smart Compose / Inline suggestions
        if (enableGrammarChecker == true) {
            hookFlag("enable_grammar_checker", "isGrammarCheckerEnabled", "Grammar Checker")
            hookFlag("enable_inline_suggestions_on_client_side", "isGrammarCheckerEnabled", "Inline Suggestions")
        }

        // 6. Bluetooth Microphone Setting
        if (enableBluetoothMicrophone == true) {
            hookFlag("enable_use_bluetooth_setting", "isBluetoothMicEnabled", "Bluetooth Microphone")
        }

        // 7. Emoji Scale Setting (enables keyboard engine support for emoji scaling)
        val fpEmoji = flagFingerprint("emoji_scale_supported")
        fpEmoji.method.overrideFlagDefault(fpEmoji.instructionMatches.first().index, "emoji_scale_supported") { reg ->
            "const/4 v$reg, 0x1"
        }
        val targetClassEmoji = cleanClassName(fpEmoji.originalClassDef.type)
        println("[Feature Flags] Emoji Scale Setting: Injected isolated flag override into $targetClassEmoji.<clinit>()")
        patched++

        if (patched == 0) {
            println("[Feature Flags] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Feature Flags] Applied $patched feature flag override(s) cleanly (native settings unpolluted).")
        }
    }
}

/**
 * Overrides the boolean default handed to a Phenotype flag factory without leaking into sibling flags.
 *
 * Flag holder `<clinit>` methods hoist one `const/4 vN, 0x0` and pass `vN` to every following
 * `a(String, Z)` factory call. Writing the override into `vN` would therefore force every later flag
 * in the method to the same value (e.g. `enable_use_bluetooth_setting` -> `hide_offline_speech_recognition`).
 * The override is written right before the factory call and the original constant is restored right
 * after its `move-result-object`, so only the targeted flag observes it.
 */
private fun MutableMethod.overrideFlagDefault(stringIndex: Int, flag: String, valueSmali: (Int) -> String) {
    val body = instructions.toList()
    val invokeIndex = (stringIndex + 1 until minOf(stringIndex + 4, body.size))
        .firstOrNull { body[it].opcode == Opcode.INVOKE_STATIC }
        ?: throw PatchException("[Feature Flags] No factory invoke follows \"$flag\"")
    val reg = (body[invokeIndex] as FiveRegisterInstruction).registerD

    val resultIndex = invokeIndex + 1
    val result = body.getOrNull(resultIndex)
    if (result?.opcode != Opcode.MOVE_RESULT_OBJECT || (result as OneRegisterInstruction).registerA == reg) {
        throw PatchException("[Feature Flags] Unexpected factory result shape after \"$flag\"")
    }

    val originalIndex = (invokeIndex - 1 downTo 0).firstOrNull {
        val insn = body[it]
        insn is OneRegisterInstruction && insn.registerA == reg
    } ?: throw PatchException("[Feature Flags] No default writer found for \"$flag\"")
    val original = body[originalIndex]
    if (original.opcode != Opcode.CONST_4) {
        throw PatchException("[Feature Flags] Default of \"$flag\" is ${original.opcode}, expected CONST_4")
    }
    val literal = (original as NarrowLiteralInstruction).narrowLiteral

    addInstruction(resultIndex + 1, "const/4 v$reg, 0x${Integer.toHexString(literal)}")
    addInstructions(invokeIndex, valueSmali(reg))
}

private fun MutableMethod.overrideFlagWithHook(stringIndex: Int, flag: String, hook: String) =
    overrideFlagDefault(stringIndex, flag) { reg ->
        """
            invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->$hook()Z
            move-result v$reg
        """.trimIndent()
    }
