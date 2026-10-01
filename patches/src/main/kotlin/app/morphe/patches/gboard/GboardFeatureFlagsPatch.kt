package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardFeatureFlagsPatch = bytecodePatch(
    name = "Feature Flags",
    description = "Unlocks hidden Google feature flags and UI customization experiments: redesigned access points menu, key border shape selector, cursor trackpad mode, grammar checker & Smart Compose, proactive suggestions dismiss button, emoji size scale, and Bluetooth microphone.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(gboardCoreIntegrityPatch)

    val enableAccessPointsRedesign by booleanOption(
        key = "enableAccessPointsRedesign",
        default = true,
        title = "Access Points Menu Redesign",
        description = "Enables the redesigned access points menu bar and customization panel (Panel V2).",
        required = false,
    )

    val enableKeyShapeSelection by booleanOption(
        key = "enableKeyShapeSelection",
        default = true,
        title = "Key Shape Selection",
        description = "Enables the key border shape selection UI (Default, Semi-rounded, Round) in theme customization.",
        required = false,
    )

    val enableCursorTrackpad by booleanOption(
        key = "enableCursorTrackpad",
        default = false,
        title = "Cursor Trackpad",
        description = "Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar (experimental).",
        required = false,
    )

    val enableGrammarChecker by booleanOption(
        key = "enableGrammarChecker",
        default = true,
        title = "Grammar Checker & Smart Compose",
        description = "Unlocks Grammar check and Smart Compose / inline suggestions under Text correction preferences.",
        required = false,
    )

    val enableDismissSuggestionsButton by booleanOption(
        key = "enableDismissSuggestionsButton",
        default = true,
        title = "Dismiss Suggestions Button",
        description = "Adds a close button (X) to dismiss proactive suggestions on the suggestion bar.",
        required = false,
    )

    val enableEmojiScale by booleanOption(
        key = "enableEmojiScale",
        default = true,
        title = "Emoji Scale Setting",
        description = "Unlocks the emoji size scaling setting in Gboard appearance preferences.",
        required = false,
    )

    val enableBluetoothMicrophone by booleanOption(
        key = "enableBluetoothMicrophone",
        default = true,
        title = "Bluetooth Microphone",
        description = "Unlocks the 'Use Bluetooth microphone' setting under Voice typing preferences.",
        required = false,
    )

    execute {
        if (enableAccessPointsRedesign != true &&
            enableKeyShapeSelection != true &&
            enableCursorTrackpad != true &&
            enableGrammarChecker != true &&
            enableDismissSuggestionsButton != true &&
            enableEmojiScale != true &&
            enableBluetoothMicrophone != true
        ) {
            println("[Feature Flags] Skipped: All feature flag options are disabled.")
            return@execute
        }

        var patched = 0

        // 1. Access Points Menu Redesign (Panel V2)
        if (enableAccessPointsRedesign == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_access_points_menu_redesign")),
            )
            val matchIndex = fp.instructionMatches.first().index
            val reg = fp.method.getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA
            fp.method.addInstructions(
                matchIndex + 2,
                "const/4 v$reg, 0x1",
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Access Points Redesign: Injected flag override into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
            patched++
        }

        // 2. Key Shape Selection UI
        if (enableKeyShapeSelection == true) {
            val fp = Fingerprint(
                definingClass = "Lxsj;",
                name = "i",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "Z",
            )
            fp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Key Shape Selection: Forced return true in $targetClass.i() to unlock border shape selector UI")
            patched++
        }

        // 3. Cursor Trackpad Mode (2D spacebar navigation & lock)
        if (enableCursorTrackpad == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("free_cursor"), string("free_cursor_lock_mode")),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            var count = 0
            for (match in fp.instructionMatches.sortedByDescending { it.index }) {
                val matchIndex = match.index
                val nextInsn = fp.method.getInstruction<Instruction>(matchIndex + 1)
                val reg = when (nextInsn) {
                    is OneRegisterInstruction -> nextInsn.registerA
                    is FiveRegisterInstruction -> nextInsn.registerD
                    else -> 1
                }
                val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
                fp.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
                count++
            }
            println("[Feature Flags] Cursor Trackpad: Injected $count flag override(s) into $targetClass.<clinit>() -> 2D spacebar trackpad enabled.")
            patched++
        }

        // 4. Grammar Checker & Smart Compose / Inline suggestions
        if (enableGrammarChecker == true) {
            val fpGrammar = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_grammar_checker")),
            )
            val matchGrammar = fpGrammar.instructionMatches.first().index
            val nextInsnGrammar = fpGrammar.method.getInstruction<Instruction>(matchGrammar + 1)
            val regGrammar = when (nextInsnGrammar) {
                is OneRegisterInstruction -> nextInsnGrammar.registerA
                is FiveRegisterInstruction -> nextInsnGrammar.registerD
                else -> 1
            }
            val insertGrammar = if (nextInsnGrammar is OneRegisterInstruction) matchGrammar + 2 else matchGrammar + 1
            fpGrammar.method.addInstructions(insertGrammar, "const/4 v$regGrammar, 0x1")

            val fpInline = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_inline_suggestions_on_client_side")),
            )
            val matchInline = fpInline.instructionMatches.first().index
            val nextInsnInline = fpInline.method.getInstruction<Instruction>(matchInline + 1)
            val regInline = when (nextInsnInline) {
                is OneRegisterInstruction -> nextInsnInline.registerA
                is FiveRegisterInstruction -> nextInsnInline.registerD
                else -> 1
            }
            val insertInline = if (nextInsnInline is OneRegisterInstruction) matchInline + 2 else matchInline + 1
            fpInline.method.addInstructions(insertInline, "const/4 v$regInline, 0x1")

            val targetClass = LocaleUtils.cleanClassName(fpGrammar.originalClassDef.type)
            println("[Feature Flags] Grammar Checker: Injected 2 flag override(s) into $targetClass -> Grammar Check & Smart Compose unlocked.")
            patched++
        }

        // 5. Dismiss Suggestions Button (Close X button)
        if (enableDismissSuggestionsButton == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_close_proactive_suggestions_access_point")),
            )
            val matchIndex = fp.instructionMatches.first().index
            val nextInsn = fp.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is OneRegisterInstruction -> nextInsn.registerA
                is FiveRegisterInstruction -> nextInsn.registerD
                else -> 1
            }
            val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
            fp.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Dismiss Suggestions: Injected flag override into $targetClass.<clinit>() at opcode index $insertIndex")
            patched++
        }

        // 6. Emoji Scale Setting
        if (enableEmojiScale == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("emoji_scale_supported")),
            )
            val matchIndex = fp.instructionMatches.first().index
            val nextInsn = fp.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is OneRegisterInstruction -> nextInsn.registerA
                is FiveRegisterInstruction -> nextInsn.registerD
                else -> 1
            }
            val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
            fp.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Emoji Scale: Injected flag override into $targetClass.<clinit>() at opcode index $insertIndex")
            patched++
        }

        // 7. Bluetooth Microphone Setting
        if (enableBluetoothMicrophone == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_use_bluetooth_setting")),
            )
            val matchIndex = fp.instructionMatches.first().index
            val nextInsn = fp.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is OneRegisterInstruction -> nextInsn.registerA
                is FiveRegisterInstruction -> nextInsn.registerD
                else -> 1
            }
            val insertIndex = if (nextInsn is OneRegisterInstruction) matchIndex + 2 else matchIndex + 1
            fp.method.addInstructions(insertIndex, "const/4 v$reg, 0x1")
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Bluetooth Microphone: Injected flag override into $targetClass.<clinit>() at opcode index $insertIndex")
            patched++
        }

        println("[Feature Flags] Applied $patched feature flag override(s) -> features unlocked.")
    }
}
