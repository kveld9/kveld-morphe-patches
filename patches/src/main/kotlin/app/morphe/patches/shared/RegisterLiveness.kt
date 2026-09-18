package app.morphe.patches.shared

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import java.util.BitSet

/** Every register an instruction names, in operand order, whether it reads or writes it. */
fun Instruction.namedRegisters(): List<Int> = when (this) {
    is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
    is FiveRegisterInstruction ->
        listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
    is ThreeRegisterInstruction -> listOf(registerA, registerB, registerC)
    is TwoRegisterInstruction -> listOf(registerA, registerB)
    is OneRegisterInstruction -> listOf(registerA)
    else -> emptyList()
}

/**
 * Computes which registers are live on entry to each instruction of a method:
 * read on some path from there before they are written.
 *
 * Computed backwards over the entire control-flow graph (branches, switches, try-catch handlers).
 */
class RegisterLiveness private constructor(private val liveIn: Array<BitSet>) {

    val size: Int get() = liveIn.size

    fun liveInto(index: Int): Set<Int> {
        val bits = liveIn[index]
        val registers = mutableSetOf<Int>()
        var register = bits.nextSetBit(0)
        while (register >= 0) {
            registers += register
            register = bits.nextSetBit(register + 1)
        }
        return registers
    }

    companion object {
        private val payloadOpcodes = setOf(
            Opcode.PACKED_SWITCH_PAYLOAD,
            Opcode.SPARSE_SWITCH_PAYLOAD,
            Opcode.ARRAY_PAYLOAD,
        )
        private val switchOpcodes = setOf(Opcode.PACKED_SWITCH, Opcode.SPARSE_SWITCH)

        fun of(method: Method): RegisterLiveness {
            val implementation = requireNotNull(method.implementation) { "Method has no implementation: $method" }
            val instructions = implementation.instructions.toList()
            val count = instructions.size
            val address = IntArray(count + 1)
            val indexAt = HashMap<Int, Int>(count * 2)
            for (index in 0 until count) {
                indexAt[address[index]] = index
                address[index + 1] = address[index] + instructions[index].codeUnits
            }
            fun target(from: Int, offset: Int): Int = indexAt[address[from] + offset]
                ?: throw IllegalArgumentException("A branch at instruction $from of $method lands between instructions.")

            val successors = Array(count) { mutableListOf<Int>() }
            for (index in 0 until count) {
                val instruction = instructions[index]
                val opcode = instruction.opcode
                val out = successors[index]
                when {
                    opcode in payloadOpcodes -> {}
                    opcode in switchOpcodes -> {
                        val payload = instructions[target(index, (instruction as OffsetInstruction).codeOffset)] as SwitchPayload
                        payload.switchElements.forEach { out += target(index, it.offset) }
                        if (index + 1 < count) out += index + 1
                    }
                    opcode == Opcode.FILL_ARRAY_DATA -> if (index + 1 < count) out += index + 1
                    instruction is OffsetInstruction -> {
                        out += target(index, instruction.codeOffset)
                        if (opcode.canContinue() && index + 1 < count) out += index + 1
                    }
                    opcode.canContinue() -> if (index + 1 < count) out += index + 1
                }
            }

            implementation.tryBlocks.forEach { tryBlock ->
                val start = tryBlock.startCodeAddress
                val end = start + tryBlock.codeUnitCount
                val handlers = tryBlock.exceptionHandlers.map { handler ->
                    indexAt[handler.handlerCodeAddress] ?: throw IllegalArgumentException(
                        "A handler of $method starts between instructions.",
                    )
                }
                for (index in 0 until count) {
                    if (address[index] >= start && address[index] < end) successors[index] += handlers
                }
            }

            val use = Array(count) { BitSet() }
            val def = Array(count) { BitSet() }
            for (index in 0 until count) {
                val instruction = instructions[index]
                val opcode = instruction.opcode
                val name = opcode.name.lowercase()
                val wide = "wide" in name || "long" in name || "double" in name
                val twoAddress = name.endsWith("/2addr") || name.endsWith("_2addr")
                val plainWrite = opcode.setsRegister() && !twoAddress && opcode != Opcode.CHECK_CAST
                instruction.namedRegisters().forEachIndexed { position, register ->
                    if (position == 0 && opcode.setsRegister()) {
                        def[index].set(register)
                        if (opcode.setsWideRegister()) def[index].set(register + 1)
                        if (plainWrite) return@forEachIndexed
                    }
                    use[index].set(register)
                    if (wide) use[index].set(register + 1)
                }
            }

            val liveIn = Array(count) { BitSet() }
            var changed = true
            while (changed) {
                changed = false
                for (index in count - 1 downTo 0) {
                    val live = BitSet()
                    successors[index].forEach { live.or(liveIn[it]) }
                    live.andNot(def[index])
                    live.or(use[index])
                    if (live != liveIn[index]) {
                        liveIn[index] = live
                        changed = true
                    }
                }
            }
            return RegisterLiveness(liveIn)
        }
    }
}
