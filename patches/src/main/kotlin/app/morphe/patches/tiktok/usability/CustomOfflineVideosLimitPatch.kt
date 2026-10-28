package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val customOfflineVideosLimitPatch = bytecodePatch(
    name = "Offline Video Limit",
    description = "Customizes the maximum number of videos saved for offline viewing.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val customLimit by intOption(
        key = "customLimit",
        title = "Custom Offline Videos Limit",
        description = "Maximum number of offline videos that can be cached for offline playback (default: 1000).",
        default = 1000,
        required = false,
    )

    execute {
        var patched = 0
        val chosenLimit = customLimit?.takeIf { it in 1..50000 } ?: 1000

        // 1. Initialize targetLimit in TikTokOfflineVideosHook.<clinit>
        try {
            val hookClinitFp = Fingerprint(
                definingClass = Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK,
                name = "<clinit>",
            )
            val method = hookClinitFp.method
            method.ensureRegisterCount(1)
            val instructions = method.implementation!!.instructions
            val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            val insertIdx = if (returnIdx != -1) returnIdx else 0
            method.addInstructions(
                insertIdx,
                """
                    const v0, $chosenLimit
                    sput v0, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->targetLimit:I
                """.trimIndent(),
            )
            println("[Offline Video Limit] Initialized targetLimit = $chosenLimit.")
            patched++
        } catch (e: Exception) {
            println("[Offline Video Limit] TikTokOfflineVideosHook.<clinit> note: ${e.message}")
        }

        // 2. Dynamic discovery through OfflineModeSheetPageAssem.onAssemPostCreate -> Limits list provider
        try {
            val assemClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
            )
            val assemClass = assemClassFp.classDef
            val postCreateMethod = assemClass.methods.firstOrNull { it.name == "onAssemPostCreate" }
            var listProviderMethodRef: MethodReference? = null

            if (postCreateMethod?.implementation != null) {
                for (instruction in postCreateMethod.implementation!!.instructions) {
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.returnType == "Ljava/util/List;" && ref.parameterTypes.isEmpty()) {
                        listProviderMethodRef = ref
                        break
                    }
                }
            }

            if (listProviderMethodRef != null) {
                val listProviderMethod = Fingerprint(
                    definingClass = listProviderMethodRef.definingClass,
                    name = listProviderMethodRef.name,
                    returnType = "Ljava/util/List;",
                ).method

                val returnIndices = listProviderMethod.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    listProviderMethod.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static/range {v$reg .. v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->getOfflineLimits(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Offline Video Limit] Hooked limits list provider (${listProviderMethodRef.definingClass}->${listProviderMethodRef.name}) at ${returnIndices.size} return point(s).")
                    patched++
                }
            } else {
                println("[Offline Video Limit] Limits list provider method not found in onAssemPostCreate.")
            }
        } catch (e: Exception) {
            println("[Offline Video Limit] onAssemPostCreate note: ${e.message}")
        }

        // 3. Dynamic discovery through OfflineModeSheetPageAssem -> Title formatter
        var titleCountPluralId: Int? = null
        var titleStringId: Int? = null
        var titleFormatterHooked = false
        var titleFormatterRef: MethodReference? = null
        try {
            val assemClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
            )
            val assemClass = assemClassFp.classDef
            var titleFormatterMethodRef: MethodReference? = null

            for (method in assemClass.methods) {
                val insns = method.implementation?.instructions ?: continue
                for (instruction in insns) {
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.returnType == "Ljava/lang/String;" &&
                        ref.parameterTypes.size == 2 &&
                        ref.parameterTypes[0] == "I" &&
                        ref.parameterTypes[1] == "Landroid/app/Activity;"
                    ) {
                        titleFormatterMethodRef = ref
                        break
                    }
                }
                if (titleFormatterMethodRef != null) break
            }

            if (titleFormatterMethodRef != null) {
                val titleFormatterMethod = Fingerprint(
                    definingClass = titleFormatterMethodRef.definingClass,
                    name = titleFormatterMethodRef.name,
                    parameters = listOf("I", "Landroid/app/Activity;"),
                    returnType = "Ljava/lang/String;",
                ).method

                titleCountPluralId = extractCountPluralId(titleFormatterMethod)
                titleStringId = extractStringId(titleFormatterMethod)

                val returnIndices = titleFormatterMethod.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    titleFormatterMethod.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static {p0, p1, v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatTitle(ILandroid/app/Activity;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Offline Video Limit] Hooked title formatter (${titleFormatterMethodRef.definingClass}->${titleFormatterMethodRef.name}) at ${returnIndices.size} return point(s).")
                    titleFormatterHooked = true
                    titleFormatterRef = titleFormatterMethodRef
                    patched++
                }
            } else {
                println("[Offline Video Limit] Title formatter method not found in OfflineModeSheetPageAssem.")
            }
        } catch (e: Exception) {
            println("[Offline Video Limit] Title formatter note: ${e.message}")
        }

        if (titleFormatterHooked && titleCountPluralId == null && titleStringId == null) {
            throw PatchException(
                "[Offline Video Limit] Title resource ids not found in ${titleFormatterRef?.definingClass}->${titleFormatterRef?.name}; the custom limit title would show wrong text.",
            )
        }

        if (titleCountPluralId != null || titleStringId != null) {
            try {
                val hookClinitFp = Fingerprint(
                    definingClass = Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK,
                    name = "<clinit>",
                )
                val method = hookClinitFp.method
                method.ensureRegisterCount(1)
                val instructions = method.implementation!!.instructions
                val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
                val insertIdx = if (returnIdx != -1) returnIdx else 0

                val instructionsToInject = buildString {
                    if (titleCountPluralId != null) {
                        appendLine("const v0, $titleCountPluralId")
                        appendLine("sput v0, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->titleCountPluralId:I")
                    }
                    if (titleStringId != null) {
                        appendLine("const v0, $titleStringId")
                        appendLine("sput v0, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->titleStringId:I")
                    }
                }.trim()

                method.addInstructions(insertIdx, instructionsToInject)
                println("[Offline Video Limit] Title resources: count plural=$titleCountPluralId, string=$titleStringId.")
            } catch (e: Exception) {
                println("[Offline Video Limit] TikTokOfflineVideosHook.<clinit> title resources note: ${e.message}")
            }
        }

        // 4. Dynamic discovery through OfflineModeSheetPageAssem -> Progress subtitle
        try {
            val assemClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
            )
            val assemClass = assemClassFp.classDef
            var progressSubtitleMethodRef: MethodReference? = null

            for (method in assemClass.methods) {
                val insns = method.implementation?.instructions ?: continue
                for (instruction in insns) {
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    if (ref.name == "getSubtitle" &&
                        ref.returnType == "Ljava/lang/String;" &&
                        ref.parameterTypes.size >= 2 &&
                        ref.parameterTypes[0] == "Landroid/content/Context;" &&
                        ref.parameterTypes[1] == "I"
                    ) {
                        progressSubtitleMethodRef = ref
                        break
                    }
                }
                if (progressSubtitleMethodRef != null) break
            }

            if (progressSubtitleMethodRef != null) {
                val subtitleMethod = Fingerprint(
                    definingClass = progressSubtitleMethodRef.definingClass,
                    name = progressSubtitleMethodRef.name,
                    returnType = "Ljava/lang/String;",
                    custom = { method, _ ->
                        method.parameterTypes.size >= 2 &&
                            method.parameterTypes[0] == "Landroid/content/Context;" &&
                            method.parameterTypes[1] == "I"
                    },
                ).method

                val returnIndices = subtitleMethod.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    subtitleMethod.addInstructionsAtControlFlowLabel(
                        returnIndex,
                        """
                            invoke-static {p1, p2, v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatProgressSubtitle(Landroid/content/Context;ILjava/lang/String;)Ljava/lang/String;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Offline Video Limit] Hooked progress subtitle (${progressSubtitleMethodRef.definingClass}->${progressSubtitleMethodRef.name}) at ${returnIndices.size} return point(s).")
                    patched++
                }
            } else {
                println("[Offline Video Limit] Progress subtitle method not found in OfflineModeSheetPageAssem.")
            }
        } catch (e: Exception) {
            println("[Offline Video Limit] Progress subtitle note: ${e.message}")
        }

        // 5. Radio item cell class discovery and title/subtitle hooks
        try {
            // This fingerprint has no class or string anchor, so it is evaluated for every method in
            // the APK. The field check only depends on the class, so compute it once per class.
            var lastClassDef: ClassDef? = null
            var lastClassHasRadioCellFields = false
            val radioCellFingerprint = Fingerprint(
                custom = { method, classDef ->
                    if (classDef !== lastClassDef) {
                        lastClassDef = classDef
                        val fieldTypes = classDef.fields.map { it.type }.toSet()
                        lastClassHasRadioCellFields = fieldTypes.contains("I") &&
                            fieldTypes.contains("Landroid/app/Activity;") &&
                            fieldTypes.contains("Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;")
                    }
                    lastClassHasRadioCellFields &&
                        method.implementation?.instructions?.any { insn ->
                            val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
                            ref?.name == "<init>" &&
                                ref.parameterTypes.size >= 4 &&
                                ref.parameterTypes[1] == "Ljava/lang/String;" &&
                                ref.parameterTypes[3] == "Ljava/lang/String;"
                        } == true
                },
            )

            val method = radioCellFingerprint.method
            val instructions = method.implementation?.instructions

            if (instructions != null) {
                var hookIndex = -1
                var titleReg = -1
                var subtitleReg = -1

                for ((idx, insn) in instructions.withIndex()) {
                    if (insn.opcode == Opcode.INVOKE_DIRECT || insn.opcode == Opcode.INVOKE_DIRECT_RANGE) {
                        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (ref.name == "<init>" &&
                            ref.parameterTypes.size >= 4 &&
                            ref.parameterTypes[1] == "Ljava/lang/String;" &&
                            ref.parameterTypes[3] == "Ljava/lang/String;"
                        ) {
                            hookIndex = idx
                            if (insn is RegisterRangeInstruction) {
                                titleReg = insn.startRegister + 2
                                subtitleReg = insn.startRegister + 4
                            } else if (insn is FiveRegisterInstruction) {
                                titleReg = insn.registerE
                                subtitleReg = insn.registerG
                            }
                            break
                        }
                    }
                }

                if (hookIndex != -1 && titleReg != -1 && subtitleReg != -1) {
                    method.addInstructions(
                        hookIndex,
                        """
                            invoke-static/range {v$titleReg .. v$titleReg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatRadioTitle(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$titleReg
                            invoke-static {v$titleReg, v$subtitleReg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatRadioSubtitle(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$subtitleReg
                        """.trimIndent(),
                    )
                    println("[Offline Video Limit] Hooked radio item title and subtitle in ${radioCellFingerprint.classDef.type}->${method.name} at instruction $hookIndex.")
                    patched++
                } else {
                    println("[Offline Video Limit] Constructor for radio item not found in ${radioCellFingerprint.classDef.type}->${method.name}.")
                }
            }
        } catch (e: Exception) {
            println("[Offline Video Limit] Radio item cell note: ${e.message}")
        }

        if (patched == 0) {
            println("[Offline Video Limit] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Offline Video Limit] Applied $patched offline videos limit hook(s).")
        }
    }
}

private val QUANTITY_STRING_PARAMS = listOf("I", "I")
private val GET_STRING_PARAMS = listOf("I")

private fun findNarrowLiteralBefore(
    instructions: List<Instruction>,
    beforeIndex: Int,
    register: Int,
): Int? {
    for (i in (beforeIndex - 1) downTo 0) {
        val insn = instructions[i]
        val literalInsn = insn as? NarrowLiteralInstruction ?: continue
        val regInsn = insn as? OneRegisterInstruction ?: continue
        if (regInsn.registerA == register) {
            return literalInsn.narrowLiteral
        }
    }
    return null
}

private fun isPrecededByGetCount(instructions: List<Instruction>, beforeIndex: Int): Boolean {
    for (i in (beforeIndex - 1) downTo 0) {
        val ref = (instructions[i] as? ReferenceInstruction)?.reference as? MethodReference ?: continue
        val name = ref.name
        if (name == "getCount") return true
        if (name == "getMinutes") return false
    }
    return false
}

private fun isResourcesGetQuantityString(ref: MethodReference): Boolean {
    if (ref.name != "getQuantityString") return false
    if (ref.definingClass != "Landroid/content/res/Resources;") return false
    if (ref.returnType != "Ljava/lang/String;") return false
    return ref.parameterTypes == QUANTITY_STRING_PARAMS
}

private fun resolveCountPluralLiteral(
    insn: Instruction,
    instructions: List<Instruction>,
    index: Int,
): Int? {
    val fiveReg = insn as? FiveRegisterInstruction ?: return null
    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    if (!isResourcesGetQuantityString(ref)) return null
    if (!isPrecededByGetCount(instructions, index)) return null
    return findNarrowLiteralBefore(instructions, index, fiveReg.registerD)
}

private fun extractCountPluralId(method: Method): Int? {
    val instructions = method.implementation?.instructions?.toList() ?: return null
    for ((index, insn) in instructions.withIndex()) {
        val literal = resolveCountPluralLiteral(insn, instructions, index)
        if (literal != null) return literal
    }
    return null
}

private fun isGetStringMethod(ref: MethodReference): Boolean {
    if (ref.name != "getString") return false
    if (ref.returnType != "Ljava/lang/String;") return false
    return ref.parameterTypes == GET_STRING_PARAMS
}

private fun resolveStringLiteral(
    insn: Instruction,
    instructions: List<Instruction>,
    index: Int,
): Int? {
    val fiveReg = insn as? FiveRegisterInstruction ?: return null
    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    if (!isGetStringMethod(ref)) return null
    return findNarrowLiteralBefore(instructions, index, fiveReg.registerD)
}

private fun extractStringId(method: Method): Int? {
    val instructions = method.implementation?.instructions?.toList() ?: return null
    for ((index, insn) in instructions.withIndex()) {
        val literal = resolveStringLiteral(insn, instructions, index)
        if (literal != null) return literal
    }
    return null
}
