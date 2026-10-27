package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private data class IcuCandidate(
    val optionsClassType: String,
    val notationEnumType: String,
    val enumAType: String,
    val enumBType: String,
)

private fun isDirectInvoke(opcode: Opcode): Boolean =
    opcode == Opcode.INVOKE_DIRECT || opcode == Opcode.INVOKE_DIRECT_RANGE

private fun is3ArgConstructor(ref: MethodReference, targetType: String): Boolean =
    ref.name == "<init>" && ref.definingClass == targetType && ref.parameterTypes.size == 3

private fun find3ArgConstructorCall(
    instructions: List<Instruction>,
    startIndex: Int,
    targetType: String,
): MethodReference? {
    for (i in startIndex until instructions.size) {
        val insn = instructions[i]
        if (!isDirectInvoke(insn.opcode)) continue
        val ref = insn.getReference<MethodReference>() ?: continue
        if (is3ArgConstructor(ref, targetType)) return ref
    }
    return null
}

private fun tryExtractIcuCandidate(
    insn: Instruction,
    instructions: List<Instruction>,
    index: Int,
): IcuCandidate? {
    if (insn.opcode != Opcode.NEW_INSTANCE) return null
    val type = insn.getReference<TypeReference>()?.type ?: return null
    val constructorRef = find3ArgConstructorCall(instructions, index + 1, type) ?: return null
    return IcuCandidate(
        optionsClassType = type,
        notationEnumType = constructorRef.parameterTypes[1].toString(),
        enumAType = constructorRef.parameterTypes[0].toString(),
        enumBType = constructorRef.parameterTypes[2].toString(),
    )
}

private fun findIcuCandidatesInMethod(method: MutableMethod): List<IcuCandidate> {
    val instructions = method.implementation?.instructions?.toList() ?: return emptyList()
    val candidates = mutableListOf<IcuCandidate>()
    for (i in instructions.indices) {
        val candidate = tryExtractIcuCandidate(instructions[i], instructions, i)
        if (candidate != null) {
            candidates.add(candidate)
        }
    }
    return candidates
}

private fun matches4ArgParams(params: List<CharSequence>, candidate: IcuCandidate): Boolean =
    params[0].toString() == candidate.enumAType &&
        params[1].toString() == candidate.notationEnumType &&
        params[2].toString() == candidate.enumBType &&
        params[3].toString() == "I"

private fun is4ArgConstructor(method: MutableMethod, candidate: IcuCandidate): Boolean {
    if (method.name != "<init>") return false
    if (method.parameterTypes.size != 4) return false
    return matches4ArgParams(method.parameterTypes, candidate)
}

private fun hookIcuConstructor(initMethod: MutableMethod, notationEnum: String) {
    initMethod.ensureRegisterCount(6)
    initMethod.addInstructionsWithLabels(
        0,
        """
            sget-object v0, $notationEnum->COMPACT:$notationEnum
            if-ne p2, v0, :cond_exact_counts
            sget-object p2, $notationEnum->STANDARD:$notationEnum
            :cond_exact_counts
            nop
        """.trimIndent(),
    )
}

private fun tryHookIcuCandidate(
    context: BytecodePatchContext,
    candidate: IcuCandidate,
): Boolean {
    val optionsClass = context.mutableClassDefByOrNull(candidate.optionsClassType) ?: return false
    val initMethod = optionsClass.methods.firstOrNull { is4ArgConstructor(it, candidate) } ?: return false
    hookIcuConstructor(initMethod, candidate.notationEnumType)
    return true
}

private fun hookIcuFormatting(
    context: BytecodePatchContext,
    methods1Dot0M: List<MutableMethod>,
) {
    val candidates = methods1Dot0M.flatMap(::findIcuCandidatesInMethod)
    val hookedCandidate = candidates.firstOrNull { tryHookIcuCandidate(context, it) }
        ?: throw PatchException("Failed to find or hook ICU compact formatting options constructor")
    println("[Show Exact Counts] Hooked ICU options ${hookedCandidate.optionsClassType}-><init> COMPACT -> STANDARD.")
}

private fun hookStaticFormatter(method: MutableMethod) {
    method.ensureRegisterCount(3)
    val regCount = method.implementation?.registerCount ?: 3
    val invoke = if (regCount > 16) "invoke-static/range {p0 .. p1}" else "invoke-static {p0, p1}"
    method.addInstructions(
        0,
        """
            $invoke, ${Constants.TIKTOK_EXTENSION_COUNTS_HOOK}->exact(J)Ljava/lang/String;
            move-result-object v0
            return-object v0
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.findFormatterMatches(literal: String): List<MutableMethod> {
    val fp = Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
        returnType = "Ljava/lang/String;",
        parameters = listOf("J"),
        strings = listOf(literal),
    )
    return fp.matchAll().map { it.method }
}

private fun BytecodePatchContext.hookStaticFormatters(methods1Dot0M: List<MutableMethod>): Int {
    val methods1M = findFormatterMatches("1M")
    if (methods1M.isEmpty()) {
        throw PatchException("Found no matches for '1M' static count formatters")
    }
    val distinctMethods = (methods1Dot0M + methods1M).distinctBy { "${it.definingClass}->${it.name}" }
    if (distinctMethods.size > 6) {
        throw PatchException("Found ${distinctMethods.size} static count formatters (expected at most 6)")
    }
    distinctMethods.forEach(::hookStaticFormatter)
    val methodList = distinctMethods.joinToString(", ") { "${it.definingClass}->${it.name}" }
    println("[Show Exact Counts] Hooked ${distinctMethods.size} static count formatter(s): $methodList")
    return distinctMethods.size
}

val showExactCountsPatch = bytecodePatch(
    name = "Show Exact Counts",
    description = "Shows exact numbers on likes, comments, views and followers.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        val methods1Dot0M = findFormatterMatches("1.0M")
        if (methods1Dot0M.isEmpty()) {
            throw PatchException("Found no matches for '1.0M' static count formatters")
        }

        val staticCount = hookStaticFormatters(methods1Dot0M)
        hookIcuFormatting(this, methods1Dot0M)

        val totalHooks = staticCount + 1
        println("[Show Exact Counts] Applied $totalHooks count formatting hook(s).")
    }
}
