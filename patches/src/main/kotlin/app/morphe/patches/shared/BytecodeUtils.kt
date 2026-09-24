package app.morphe.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * Clears exception try-catch blocks from a method implementation before replacing instructions.
 * Prevents Dalvik / ART VerifyError caused by dangling try blocks referencing wiped instruction offsets.
 */
fun Method.clearTryBlocks() {
    val impl = implementation ?: return
    var clazz: Class<*>? = impl.javaClass
    while (clazz != null) {
        try {
            val field = clazz.getDeclaredField("tryBlocks")
            field.isAccessible = true
            val list = field.get(impl) as? MutableList<*>
            if (list != null) {
                try {
                    list.clear()
                } catch (_: Exception) {
                    field.set(impl, mutableListOf<Any>())
                }
            }
            break
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        } catch (_: Exception) {
            break
        }
    }
}

fun Method.ensureRegisterCount(min: Int) {
    val impl = implementation ?: return
    if (impl.registerCount < min) {
        var clazz: Class<*>? = impl.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField("registerCount")
                field.isAccessible = true
                field.setInt(impl, min)
                break
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            } catch (_: Exception) {
                break
            }
        }
    }
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with a boolean return value.
 */
fun MutableMethod.replaceWithReturnBoolean(value: Boolean) {
    val impl = implementation ?: return
    clearTryBlocks()
    ensureRegisterCount(1)
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, "const/4 v0, ${if (value) "0x1" else "0x0"}\nreturn v0")
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with a void return.
 */
fun MutableMethod.replaceWithReturnVoid() {
    val impl = implementation ?: return
    clearTryBlocks()
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, "return-void")
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with an integer return value.
 */
fun MutableMethod.replaceWithReturnInt(value: Int) {
    val impl = implementation ?: return
    clearTryBlocks()
    ensureRegisterCount(1)
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, "const/4 v0, $value\nreturn v0")
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with a null object return.
 */
fun MutableMethod.replaceWithReturnNull() {
    val impl = implementation ?: return
    clearTryBlocks()
    ensureRegisterCount(1)
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with a boxed Integer return.
 */
fun MutableMethod.replaceWithReturnIntegerObject(value: Int) {
    val impl = implementation ?: return
    clearTryBlocks()
    ensureRegisterCount(1)
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, """
        const/4 v0, $value
        invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
        move-result-object v0
        return-object v0
    """.trimIndent())
}

/**
 * Safely purges try-catch ranges and replaces the entire method body with a boxed Boolean return.
 */
fun MutableMethod.replaceWithReturnBooleanObject(value: Boolean) {
    val impl = implementation ?: return
    clearTryBlocks()
    ensureRegisterCount(1)
    removeInstructions(0, impl.instructions.count())
    addInstructions(0, """
        sget-object v0, Ljava/lang/Boolean;->${if (value) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
        return-object v0
    """.trimIndent())
}


inline fun <reified T : Reference> Instruction.getReference(): T? =
    (this as? ReferenceInstruction)?.reference as? T

fun MutableClass.findMutableMethodOf(method: MethodReference): MutableMethod =
    this.methods.first { MethodUtil.methodSignaturesMatch(it, method) }

fun MutableMethod.addInstructionsAtControlFlowLabel(
    insertIndex: Int,
    instructions: String,
    vararg externalLabels: ExternalLabel,
) {
    addInstruction(insertIndex + 1, getInstruction(insertIndex))
    addInstructionsWithLabels(insertIndex + 1, instructions, *externalLabels)
    removeInstruction(insertIndex)
}

val Method.numberOfParameterRegisters: Int
    get() {
        var count = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
        for (param in parameters) {
            count += if (param.type == "J" || param.type == "D") 2 else 1
        }
        return count
    }

