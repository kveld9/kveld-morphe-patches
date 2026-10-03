package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardFeatureFlagsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

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
        default = true,
        title = "Cursor Trackpad",
        description = "Enables 2D trackpad cursor navigation and cursor lock mode by holding the spacebar (experimental).",
        required = false,
    )

    val enableDismissSuggestionsButton by booleanOption(
        key = "enableDismissSuggestionsButton",
        default = true,
        title = "Dismiss Suggestions Button",
        description = "Adds a close button (X) to dismiss proactive suggestions on the suggestion bar.",
        required = false,
    )

    val enableGrammarChecker by booleanOption(
        key = "enableGrammarChecker",
        default = true,
        title = "Grammar Checker & Smart Compose",
        description = "Unlocks client-side grammar correction and Smart Compose inline predictions.",
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
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isAccessPointsRedesignEnabled()Z
                    move-result v$reg
                """.trimIndent(),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Access Points Redesign: Injected dynamic flag hook into $targetClass.<clinit>() at opcode index ${matchIndex + 2}")
            patched++
        }

        // 2. Key Shape Selection UI (more_pill_keys Phenotype flag -> dynamic Morphe preference)
        if (enableKeyShapeSelection == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("more_pill_keys")),
            )
            val matchIndex = fp.instructionMatches.first().index
            val nextInsn = fp.method.getInstruction<Instruction>(matchIndex + 1)
            val reg = when (nextInsn) {
                is FiveRegisterInstruction -> nextInsn.registerD
                is OneRegisterInstruction -> nextInsn.registerA
                else -> 2
            }
            fp.method.addInstructions(
                matchIndex + 1,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isKeyShapeSelectionEnabled()Z
                    move-result v$reg
                """.trimIndent(),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Key Shape Selection: Injected dynamic flag hook for more_pill_keys into $targetClass.<clinit>() at opcode index ${matchIndex + 1}")
            patched++
        }

        // 3. Cursor Trackpad Mode (2D spacebar navigation & lock)
        if (enableCursorTrackpad == true) {
            val fp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(
                    string("free_cursor"),
                    string("free_cursor_lock_mode"),
                    string("free_cursor_trackpadlike"),
                ),
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
                fp.method.addInstructions(
                    insertIndex,
                    """
                        invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isCursorTrackpadEnabled()Z
                        move-result v$reg
                    """.trimIndent(),
                )
                count++
            }
            println("[Feature Flags] Cursor Trackpad: Injected $count dynamic flag hook(s) into $targetClass.<clinit>() -> 2D spacebar trackpad enabled.")
            patched++
        }

        // 4. Dismiss Suggestions Button (Close X button)
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
            fp.method.addInstructions(
                insertIndex,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isDismissSuggestionsEnabled()Z
                    move-result v$reg
                """.trimIndent(),
            )
            val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
            println("[Feature Flags] Dismiss Suggestions: Injected dynamic flag hook into $targetClass.<clinit>() at opcode index $insertIndex")
            patched++
        }

        // 5. Grammar Checker & Smart Compose / Inline suggestions
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
            fpGrammar.method.addInstructions(
                insertGrammar,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isGrammarCheckerEnabled()Z
                    move-result v$regGrammar
                """.trimIndent(),
            )

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
            fpInline.method.addInstructions(
                insertInline,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isGrammarCheckerEnabled()Z
                    move-result v$regInline
                """.trimIndent(),
            )

            val targetClass = LocaleUtils.cleanClassName(fpGrammar.originalClassDef.type)
            println("[Feature Flags] Grammar Checker: Injected 2 dynamic flag hook(s) into $targetClass -> Grammar Check & Smart Compose unlocked.")
            patched++
        }

        // 6. Bluetooth Microphone Setting
        if (enableBluetoothMicrophone == true) {
            val fpBt = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                filters = listOf(string("enable_use_bluetooth_setting")),
            )
            val matchIndexBt = fpBt.instructionMatches.first().index
            val nextInsnBt = fpBt.method.getInstruction<Instruction>(matchIndexBt + 1)
            val regBt = when (nextInsnBt) {
                is OneRegisterInstruction -> nextInsnBt.registerA
                is FiveRegisterInstruction -> nextInsnBt.registerD
                else -> 1
            }
            val insertIndexBt = if (nextInsnBt is OneRegisterInstruction) matchIndexBt + 2 else matchIndexBt + 1
            fpBt.method.addInstructions(
                insertIndexBt,
                """
                    invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->isBluetoothMicEnabled()Z
                    move-result v$regBt
                """.trimIndent(),
            )
            val targetClassBt = LocaleUtils.cleanClassName(fpBt.originalClassDef.type)
            println("[Feature Flags] Bluetooth Microphone: Injected dynamic flag hook into $targetClassBt.<clinit>() at opcode index $insertIndexBt")
            patched++
        }

        // 7. Emoji Scale Setting (enables keyboard engine support for emoji scaling)
        val fpEmoji = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string("emoji_scale_supported")),
        )
        val matchEmoji = fpEmoji.instructionMatches.first().index
        val nextInsnEmoji = fpEmoji.method.getInstruction<Instruction>(matchEmoji + 1)
        val regEmoji = when (nextInsnEmoji) {
            is OneRegisterInstruction -> nextInsnEmoji.registerA
            is FiveRegisterInstruction -> nextInsnEmoji.registerD
            else -> 1
        }
        val insertIndexEmoji = if (nextInsnEmoji is OneRegisterInstruction) matchEmoji + 2 else matchEmoji + 1
        fpEmoji.method.addInstructions(insertIndexEmoji, "const/4 v$regEmoji, 0x1")
        val targetClassEmoji = LocaleUtils.cleanClassName(fpEmoji.originalClassDef.type)
        println("[Feature Flags] Emoji Scale Setting: Injected flag override into $targetClassEmoji.<clinit>() at opcode index $insertIndexEmoji")
        patched++

        // 8. Hook EmojiKeyboardUtils.getPrefKeyboardEmojiScale to return dynamic Morphe scale
        val fpEmojiScale = Fingerprint(
            strings = listOf("EmojiKeyboardUtils.java", "Failed to parse emoji scale setting!"),
            returnType = "F",
        )
        fpEmojiScale.method.apply {
            clearTryBlocks()
            ensureRegisterCount(2)
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    invoke-static {p0}, ${Constants.GBOARD_EXTENSION_CLASS}->getEmojiScale(Landroid/content/Context;)F
                    move-result v0
                    return v0
                """.trimIndent(),
            )
        }
        println("[Feature Flags] EmojiKeyboardUtils: Hooked getPrefKeyboardEmojiScale to dynamic Morphe scale.")
        patched++

        println("[Feature Flags] Applied $patched feature flag override(s) cleanly (native settings unpolluted).")
    }
}
