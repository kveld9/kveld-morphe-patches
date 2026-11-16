package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private enum class WakeLockCallType {
    ACQUIRE,
    RELEASE,
}

// Leaves android.permission.WAKE_LOCK declared in AndroidManifest.xml on purpose.
// Removing the manifest permission turns acquire() calls into fatal SecurityException crashes;
// neutralizing the call sites in bytecode prevents the CPU wake lock safely.
@Suppress("unused")
val wakeLockNeutralizerPatch = bytecodePatch(
    name = "Wake Locks Off (Universal)",
    description = "Stops apps from keeping the phone awake with wake locks.",
    default = false,
) {
    execute {
        var acquireCalls = 0
        var releaseCalls = 0
        var touchedClasses = 0

        classDefForEach { classDef ->
            if (!hasAnyTargetInstruction(classDef)) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var classModified = false

            mutableClass.methods.forEach { method ->
                val mutations = scanMethodMutations(method)
                if (mutations.isNotEmpty()) {
                    classModified = true
                    applyMutations(method, mutations)
                    for ((_, type) in mutations) {
                        when (type) {
                            WakeLockCallType.ACQUIRE -> acquireCalls++
                            WakeLockCallType.RELEASE -> releaseCalls++
                        }
                    }
                }
            }

            if (classModified) {
                touchedClasses++
            }
        }

        val total = acquireCalls + releaseCalls
        if (total == 0) {
            println("[Wake Locks Off (Universal)] Target APK does not acquire wake locks (0 call sites found).")
            return@execute
        }

        println(
            "[Wake Locks Off (Universal)] Neutralized $total call site(s) across $touchedClasses class(es) " +
                "($acquireCalls acquire, $releaseCalls release)."
        )
    }
}

private fun hasAnyTargetInstruction(classDef: ClassDef): Boolean {
    for (method in classDef.methods) {
        val instructions = method.instructionsOrNull ?: continue
        for (inst in instructions) {
            if (matchWakeLockCall(inst) != null) return true
        }
    }
    return false
}

private fun matchWakeLockCall(inst: Instruction): WakeLockCallType? {
    if (inst.opcode != Opcode.INVOKE_VIRTUAL && inst.opcode != Opcode.INVOKE_VIRTUAL_RANGE) return null
    val refInst = inst as? ReferenceInstruction ?: return null
    val methodRef = refInst.reference as? MethodReference ?: return null

    val type = when (methodRef.name) {
        "acquire" -> {
            val params = methodRef.parameterTypes
            val matchesParams = params.isEmpty() || (params.size == 1 && params[0] == "J")
            if (matchesParams && methodRef.returnType == "V") {
                WakeLockCallType.ACQUIRE
            } else {
                null
            }
        }
        "release" -> {
            val params = methodRef.parameterTypes
            val matchesParams = params.isEmpty() || (params.size == 1 && params[0] == "I")
            if (matchesParams && methodRef.returnType == "V") {
                WakeLockCallType.RELEASE
            } else {
                null
            }
        }
        else -> null
    } ?: return null

    if (methodRef.definingClass != "Landroid/os/PowerManager\$WakeLock;") return null

    return type
}

private fun scanMethodMutations(method: Method): List<Pair<Int, WakeLockCallType>> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val mutations = mutableListOf<Pair<Int, WakeLockCallType>>()

    for ((index, inst) in instructions.withIndex()) {
        val callType = matchWakeLockCall(inst) ?: continue
        mutations.add(index to callType)
    }

    return mutations
}

private fun applyMutations(method: MutableMethod, mutations: List<Pair<Int, WakeLockCallType>>) {
    // Sort descending by instruction index to preserve preceding instruction indices
    val sorted = mutations.sortedByDescending { it.first }
    for ((index, _) in sorted) {
        method.replaceInstruction(index, "nop")
    }
}
