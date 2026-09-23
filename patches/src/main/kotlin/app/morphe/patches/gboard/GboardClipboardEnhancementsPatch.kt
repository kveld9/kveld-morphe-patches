package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gboardClipboardEnhancementsPatch = bytecodePatch(
    name = "Clipboard Enhancements",
    description = "Extends unpinned clipboard history retention duration, raises the maximum number of displayed unpinned clips, and allows customizing keyboard grid columns.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val unpinnedClipLimit by stringOption(
        key = "unpinnedClipLimit",
        title = "Unpinned clip limit",
        description = "Maximum number of unpinned clipboard history items to display in the UI (default: 50, range: 5..100).",
        default = "50",
        required = false,
    )

    val retentionHours by stringOption(
        key = "retentionHours",
        title = "Retention time limit (hours)",
        description = "Number of hours to retain unpinned clips in history before automatic cleanup (e.g. 12, 24 for 1 day, 48 for 2 days, 168 for 7 days. Default: 24).",
        default = "24",
        required = false,
    )

    val gridColumns by stringOption(
        key = "gridColumns",
        title = "Clipboard grid columns",
        description = "Number of columns in the clipboard keyboard layout (1, 2, or 3. Default: 2).",
        default = "2",
        required = false,
    )

    execute {
        var patched = 0
        val parsedLimit = unpinnedClipLimit?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }?.coerceIn(5, 100) ?: 50
        val parsedHours = retentionHours?.let { Regex("""\d+""").find(it)?.value?.toLongOrNull() }?.coerceAtLeast(1L) ?: 24L
        val parsedColumns = gridColumns?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }?.coerceIn(1, 3) ?: 2
        val retentionMillis = parsedHours * 3600L * 1000L

        // 1. Extend SQLite retention TTL & UI query cutoff window
        try {
            val fpTtl = Fingerprint(
                strings = listOf("getUnpinnedItemTimeLimitInMilliSeconds"),
                returnType = "J",
                parameters = listOf("Landroid/content/Context;"),
            )
            fpTtl.method.apply {
                clearTryBlocks()
                removeInstructions(0, implementation!!.instructions.count())
                addInstructions(
                    0,
                    """
                        const-wide v0, $retentionMillis
                        return-wide v0
                    """.trimIndent(),
                )
            }
            println("[Clipboard Enhancements] Overrode retention limit -> $parsedHours hour(s) ($retentionMillis ms).")
            patched++
        } catch (e: Exception) {
            println("[Clipboard Enhancements] Retention TTL note: ${e.message}")
        }

        // 2. Raise UI unpinned clips throttle from 5 to parsedLimit
        try {
            val fpLoader = Fingerprint(
                strings = listOf("timestamp DESC limit %d", "(%s & %d) = 0 AND (%s & %d) = 0 AND %s >= ?"),
                returnType = "Ljava/lang/Object;",
                parameters = emptyList(),
            )
            val method = fpLoader.method
            val targetIndices = method.implementation?.instructions?.withIndex()
                ?.filter {
                    it.value.opcode == Opcode.CONST_4 &&
                        (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 5
                }
                ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                ?.toList() ?: emptyList()

            targetIndices.asReversed().forEach { (idx, reg) ->
                method.replaceInstruction(
                    idx,
                    "const/16 v$reg, $parsedLimit",
                )
            }
            if (targetIndices.isNotEmpty()) {
                println("[Clipboard Enhancements] Injected unpinned clips limit ($parsedLimit items) across ${targetIndices.size} opcode site(s).")
                patched++
            }
        } catch (e: Exception) {
            println("[Clipboard Enhancements] UI Loader limit note: ${e.message}")
        }

        // 3. Customize clipboard grid columns
        try {
            val fpKeyboard = Fingerprint(
                definingClass = "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
                returnType = "I",
                parameters = emptyList(),
            )
            fpKeyboard.method.apply {
                clearTryBlocks()
                removeInstructions(0, implementation!!.instructions.count())
                addInstructions(
                    0,
                    """
                        const/4 v0, $parsedColumns
                        return v0
                    """.trimIndent(),
                )
            }
            println("[Clipboard Enhancements] Overrode clipboard grid columns -> $parsedColumns column(s).")
            patched++
        } catch (e: Exception) {
            println("[Clipboard Enhancements] Grid columns note: ${e.message}")
        }

        println("[Clipboard Enhancements] Applied $patched clipboard enhancement hook(s) (limit: $parsedLimit clips, retention: $parsedHours hours, columns: $parsedColumns).")
    }
}

private fun Method.clearTryBlocks() {
    val impl = implementation ?: return
    var clazz: Class<*>? = impl.javaClass
    while (clazz != null) {
        try {
            val field = clazz.getDeclaredField("tryBlocks")
            field.isAccessible = true
            (field.get(impl) as? MutableList<*>)?.clear()
            break
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        } catch (_: Exception) {
            break
        }
    }
}
