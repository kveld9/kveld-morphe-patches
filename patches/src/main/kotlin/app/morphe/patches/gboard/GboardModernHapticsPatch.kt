package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.LocaleUtils
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

private const val VIBRATION_EFFECT_MIN_SDK = "vibration_effect_min_sdk"

// Gboard ships an API level no device reports, which keeps the haptic primitive path dead code.
private const val STOCK_MIN_SDK = 1024L

private val CONST_WIDE_OPCODES = setOf(
    Opcode.CONST_WIDE_16,
    Opcode.CONST_WIDE_32,
    Opcode.CONST_WIDE,
)

val gboardModernHapticsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    dependsOn(sharedExtensionPatch)

    execute {
        val fp = Fingerprint(
            name = "<clinit>",
            returnType = "V",
            filters = listOf(string(VIBRATION_EFFECT_MIN_SDK)),
        )
        val method = fp.method
        val body = method.instructions.toList()
        val nameIndex = fp.instructionMatches.first().index

        // const-string, const-wide, invoke-static: the default is the flag's own constant.
        val defaultIndex = (nameIndex + 1 until minOf(nameIndex + 4, body.size))
            .firstOrNull { body[it].opcode in CONST_WIDE_OPCODES }
            ?: throw PatchException("[Modern Haptics] No const-wide default follows \"$VIBRATION_EFFECT_MIN_SDK\"")
        val literal = (body[defaultIndex] as WideLiteralInstruction).wideLiteral
        if (literal != STOCK_MIN_SDK) {
            throw PatchException("[Modern Haptics] \"$VIBRATION_EFFECT_MIN_SDK\" default is $literal, expected $STOCK_MIN_SDK")
        }
        if (body[defaultIndex + 1].opcode != Opcode.INVOKE_STATIC) {
            throw PatchException("[Modern Haptics] Unexpected factory shape after \"$VIBRATION_EFFECT_MIN_SDK\"")
        }
        val reg = (body[defaultIndex] as OneRegisterInstruction).registerA

        method.addInstructions(
            defaultIndex + 1,
            """
                invoke-static {}, ${Constants.GBOARD_EXTENSION_CLASS}->getVibrationEffectMinSdk()J
                move-result-wide v$reg
            """.trimIndent(),
        )

        val targetClass = LocaleUtils.cleanClassName(fp.originalClassDef.type)
        println("[Modern Haptics] Applied 1 hook -> $VIBRATION_EFFECT_MIN_SDK in $targetClass.<clinit>() now follows the in-app toggle.")
    }
}
