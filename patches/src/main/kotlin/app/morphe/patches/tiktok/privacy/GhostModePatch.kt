package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.RegisterLiveness
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.findMutableMethodOf
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.namedRegisters
import app.morphe.patches.shared.numberOfParameterRegisters
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val MAX_CHAIN_SEARCH_LENGTH = 24

private sealed class ReportChain {
    abstract val definedRegisters: Set<Int>
    data class Sent(val terminalIndex: Int, override val definedRegisters: Set<Int>) : ReportChain()
    data class SuspendedCheck(val compareIndex: Int, override val definedRegisters: Set<Int>) : ReportChain()
}

private val MOVE_RESULT_OPCODES = setOf(Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_WIDE, Opcode.MOVE_RESULT_OBJECT)
private val MOVE_OBJECT_OPCODES = setOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16)
private val COMPARE_OPCODES = setOf(Opcode.IF_EQ, Opcode.IF_NE)

private var guardLabelSeq = 0

private object StoryViewReportFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type.endsWith("/StoryApi;") &&
            method.name in setOf("reportStoryViewed", "reportUserInteraction", "reportStoryReveal")
    },
)

private object StoryFeedServiceReportFingerprint : Fingerprint(
    custom = { method, classDef ->
        method.name == "run" &&
            method.returnType == "V" &&
            method.implementation?.instructions?.any { inst ->
                val str = inst.getReference<StringReference>()?.string
                str != null && str.contains("StoryFeedService") && str.contains("reportStoryViewed")
            } == true
    },
)

private object ProfileViewReportFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type.endsWith("/ProfileViewerApiService;") && method.name == "reportView"
    },
)

private object ProfileRequestResponseFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type.endsWith("/ProfilePlatformViewModel;") &&
            method.returnType == "V" &&
            method.implementation?.instructions?.any { inst ->
                inst.getReference<StringReference>()?.string == "profile_request_response"
            } == true
    },
)

private object TypingStatusSenderFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type.endsWith("/TypingStatusSenderTimer;") &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0] == "Ljava/lang/String;" &&
            method.returnType == "V"
    },
)

val ghostModePatch = bytecodePatch(
    name = "Ghost Mode",
    description = "Enables anonymous profile and story browsing: suppresses outbound profile view records and story view pings while preserving your ability to see who viewed your profile.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        val targets = listOf(
            Triple(StoryViewReportFingerprint, "shouldBlockStoryView", "Story View"),
            Triple(StoryFeedServiceReportFingerprint, "shouldBlockStoryView", "Story Secondary Dispatch"),
            Triple(ProfileViewReportFingerprint, "shouldBlockProfileView", "Profile View"),
            Triple(TypingStatusSenderFingerprint, "shouldBlockTypingStatus", "Typing Indicator"),
        )

        for ((fingerprint, guardMethod, featureName) in targets) {
            try {
                val matches = fingerprint.matchAll()
                val reporters = matches.map { it.method }.filter { it.implementation != null }
                if (reporters.isEmpty()) {
                    println("[Ghost Mode] $featureName note: no concrete method found.")
                    continue
                }

                val (silent, lazy) = reporters.partition { it.returnType == "V" }
                val silentCount = applySilentGuards(silent, guardMethod)
                if (silentCount > 0) {
                    println("[Ghost Mode] $featureName: guarded $silentCount entrypoint(s) directly.")
                    patched += silentCount
                }

                if (lazy.isNotEmpty()) {
                    val skippedCount = skipReportsAtEveryCallSite(lazy, guardMethod)
                    if (skippedCount > 0) {
                        println("[Ghost Mode] $featureName: skipped $skippedCount caller send chain(s).")
                        patched += skippedCount
                    }
                }
            } catch (e: Exception) {
                println("[Ghost Mode] $featureName note: ${e.message}")
            }
        }

        try {
            val prrMatches = ProfileRequestResponseFingerprint.matchAll()
            var prrPatched = 0
            for (match in prrMatches) {
                val method = match.method
                val liveness = RegisterLiveness.of(method)
                val insts = method.instructions.toList()
                val targetIndexes = insts.withIndex()
                    .filter { (_, inst) ->
                        inst.getReference<StringReference>()?.string == "profile_request_response"
                    }
                    .map { it.index }

                for (idx in targetIndexes.sortedDescending()) {
                    val flagReg = findDeadRegister(method, liveness.liveInto(idx)) ?: continue
                    val skipLabel = "morphe_prr_skip_${guardLabelSeq++}"
                    method.addInstructionsWithLabels(
                        idx,
                        """
                            invoke-static {}, ${Constants.TIKTOK_EXTENSION_GHOST_MODE_HOOK}->shouldBlockProfileView()Z
                            move-result v$flagReg
                            if-eqz v$flagReg, :$skipLabel
                            return-void
                            :$skipLabel
                            nop
                        """.trimIndent(),
                    )
                    prrPatched++
                }
            }
            if (prrPatched > 0) {
                println("[Ghost Mode] Profile View: suppressed $prrPatched profile_request_response analytics event site(s).")
                patched += prrPatched
            }
        } catch (e: Exception) {
            println("[Ghost Mode] Profile View note: ${e.message}")
        }

        println("[Ghost Mode] Applied $patched ghost mode hook(s).")
    }
}

private fun applySilentGuards(methods: List<MutableMethod>, guardMethod: String): Int {
    var count = 0
    for (method in methods) {
        if (injectSilentGuard(method, guardMethod)) {
            count++
        }
    }
    return count
}

private fun injectSilentGuard(method: MutableMethod, guardMethod: String): Boolean {
    if (method.returnType != "V") return false
    val impl = method.implementation ?: return false
    val locals = impl.registerCount - method.numberOfParameterRegisters
    if (locals < 1) return false

    val skipLabel = "morphe_guard_skip_${guardLabelSeq++}"
    method.addInstructionsWithLabels(
        0,
        """
            invoke-static {}, ${Constants.TIKTOK_EXTENSION_GHOST_MODE_HOOK}->$guardMethod()Z
            move-result v0
            if-eqz v0, :$skipLabel
            return-void
            :$skipLabel
            nop
        """.trimIndent(),
    )
    return true
}

private fun BytecodePatchContext.skipReportsAtEveryCallSite(
    reporters: List<Method>,
    guardMethod: String,
): Int {
    val wrapperTypes = reporters.map { it.definingClass }.toSet()
    val acceptedOwners = wrapperTypes + wrapperTypes.flatMap { classDefByOrNull(it)?.interfaces.orEmpty() }
    val signatures = reporters.map { it.name to it.parameterTypes.map(CharSequence::toString) }.toSet()

    fun isTargetCall(reference: MethodReference) =
        reference.definingClass in acceptedOwners &&
            (reference.name to reference.parameterTypes.map(CharSequence::toString)) in signatures

    val callers = mutableListOf<Triple<ClassDef, Method, List<Int>>>()
    classDefForEach { classDef ->
        if (classDef.type in wrapperTypes) return@classDefForEach
        classDef.methods.forEach { method ->
            val matchingIndexes = method.implementation?.instructions?.withIndex()
                ?.filter { (_, inst) -> inst.getReference<MethodReference>()?.let(::isTargetCall) == true }
                ?.map { it.index }
                .orEmpty()
            if (matchingIndexes.isNotEmpty()) {
                callers.add(Triple(classDef, method, matchingIndexes))
            }
        }
    }

    if (callers.isEmpty()) {
        return 0
    }

    var totalSkipped = 0
    for ((classDef, method, indexes) in callers) {
        val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
        for (index in indexes.sortedDescending()) {
            try {
                if (skipReportCallChain(mutableMethod, index, guardMethod)) {
                    totalSkipped++
                }
            } catch (e: Exception) {
                println("[Ghost Mode] Call site skip note (${mutableMethod.definingClass}->${mutableMethod.name}@$index): ${e.message}")
            }
        }
    }
    return totalSkipped
}

private fun skipReportCallChain(
    method: MutableMethod,
    callIndex: Int,
    guardMethod: String,
): Boolean {
    val chain = findReportChain(method, callIndex) ?: return false
    val liveness = RegisterLiveness.of(method)
    val guardInvocation = "invoke-static {}, ${Constants.TIKTOK_EXTENSION_GHOST_MODE_HOOK}->$guardMethod()Z"

    return when (chain) {
        is ReportChain.Sent -> {
            val isStoryDispatchCaller = guardMethod == "shouldBlockStoryView" &&
                method.returnType == "V" &&
                method.instructions.any { it.getReference<MethodReference>()?.name == "reportStoryViewed" }

            if (isStoryDispatchCaller) {
                val flagReg = findDeadRegister(method, liveness.liveInto(callIndex)) ?: return false
                val skipLabel = "morphe_ghost_skip_${guardLabelSeq++}"
                method.addInstructionsWithLabels(
                    callIndex,
                    """
                        $guardInvocation
                        move-result v$flagReg
                        if-eqz v$flagReg, :$skipLabel
                        return-void
                        :$skipLabel
                        nop
                    """.trimIndent(),
                )
                true
            } else {
                val landingIndex = chain.terminalIndex + 1
                if (landingIndex >= method.instructions.count()) return false

                val liveAtLanding = liveness.liveInto(landingIndex)
                if (chain.definedRegisters.any { it in liveAtLanding }) return false

                val flagReg = findDeadRegister(method, liveness.liveInto(callIndex) + liveAtLanding) ?: return false
                method.addInstructionsAtControlFlowLabel(
                    callIndex,
                    """
                        $guardInvocation
                        move-result v$flagReg
                        if-nez v$flagReg, :morphe_ghost_skip
                    """.trimIndent(),
                    ExternalLabel("morphe_ghost_skip", method.getInstruction(landingIndex)),
                )
                true
            }
        }
        is ReportChain.SuspendedCheck -> {
            val resultReg = (method.getInstruction(callIndex + 1) as OneRegisterInstruction).registerA
            val compareInst = method.getInstruction(chain.compareIndex)
            val liveAtLanding = liveness.liveInto(chain.compareIndex) - resultReg
            val flagReg = findDeadRegister(method, liveness.liveInto(callIndex) + liveAtLanding) ?: return false

            method.addInstructionsWithLabels(
                chain.compareIndex,
                """
                    goto :morphe_ghost_join
                    const/4 v$resultReg, 0x0
                """.trimIndent(),
                ExternalLabel("morphe_ghost_join", compareInst),
            )
            val skippedInst = method.getInstruction(chain.compareIndex + 1)
            method.addInstructionsAtControlFlowLabel(
                callIndex,
                """
                    $guardInvocation
                    move-result v$flagReg
                    if-nez v$flagReg, :morphe_ghost_skipped
                """.trimIndent(),
                ExternalLabel("morphe_ghost_skipped", skippedInst),
            )
            true
        }
    }
}

private fun findDeadRegister(method: MutableMethod, unsafeRegisters: Set<Int>): Int? {
    val registerCount = method.implementation?.registerCount ?: return null
    val reg = (0 until registerCount).firstOrNull { it !in unsafeRegisters }
    return if (reg != null && reg <= 0xFF) reg else null
}

private fun findReportChain(method: Method, callIndex: Int): ReportChain? {
    val insts = method.instructions.toList()
    val call = insts.getOrNull(callIndex) ?: return null
    if (!isInvokeInstruction(call)) return null

    val next = insts.getOrNull(callIndex + 1) ?: return null
    if (next.opcode != Opcode.MOVE_RESULT_OBJECT) return null

    val initialReg = (next as OneRegisterInstruction).registerA
    var trackedRegisters = setOf(initialReg)
    val definedRegisters = mutableSetOf(initialReg)

    var idx = callIndex + 2
    val maxLimit = minOf(insts.size, callIndex + 2 + MAX_CHAIN_SEARCH_LENGTH)

    while (idx < maxLimit) {
        val inst = insts[idx]
        val opcode = inst.opcode

        if (idx == callIndex + 2 && opcode in COMPARE_OPCODES) {
            val cmp = inst as TwoRegisterInstruction
            if (cmp.registerA in trackedRegisters || cmp.registerB in trackedRegisters) {
                return ReportChain.SuspendedCheck(idx, definedRegisters)
            }
        }

        if (inst is OffsetInstruction || !opcode.canContinue()) return null
        if (opcode in MOVE_RESULT_OPCODES) return null

        if (isInvokeInstruction(inst)) {
            val following = insts.getOrNull(idx + 1)
            val keepsResult = following != null && following.opcode in MOVE_RESULT_OPCODES
            if (inst.namedRegisters().none { it in trackedRegisters }) {
                if (keepsResult) definedRegisters += (following as OneRegisterInstruction).registerA
                idx += if (keepsResult) 2 else 1
                continue
            }
            if (!keepsResult) return ReportChain.Sent(idx, definedRegisters)
            if (following?.opcode != Opcode.MOVE_RESULT_OBJECT) return null

            val movedReg = (following as OneRegisterInstruction).registerA
            trackedRegisters = setOf(movedReg)
            definedRegisters += movedReg
            idx += 2
            continue
        }

        if (opcode in MOVE_OBJECT_OPCODES) {
            val move = inst as TwoRegisterInstruction
            trackedRegisters = if (move.registerB in trackedRegisters) trackedRegisters + move.registerA else trackedRegisters - move.registerA
            definedRegisters += move.registerA
            idx++
            continue
        }

        if (opcode.setsRegister() && opcode != Opcode.CHECK_CAST) {
            val written = (inst as OneRegisterInstruction).registerA
            if (written in trackedRegisters) return null
            definedRegisters += written
            if (opcode.setsWideRegister()) definedRegisters += (written + 1)
        }
        idx++
    }
    return null
}

private fun isInvokeInstruction(inst: Instruction): Boolean =
    inst.getReference<MethodReference>() != null &&
        (inst is FiveRegisterInstruction || inst is RegisterRangeInstruction)
