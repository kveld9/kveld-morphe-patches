package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val customOfflineVideosLimitPatch = bytecodePatch(
    name = "Custom Offline Videos Limit",
    description = "Customizes the maximum number of videos available for offline download caching.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    val customLimit by intOption(
        key = "customLimit",
        title = "Custom Offline Videos Limit",
        description = "Maximum number of offline videos that can be cached for offline playback (default: 200).",
        default = 200,
        required = false,
    )

    execute {
        var patched = 0
        val chosenLimit = customLimit?.takeIf { it in 1..50000 } ?: 200

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
            println("[Custom Offline Videos Limit] Initialized targetLimit = $chosenLimit.")
            patched++
        } catch (e: Exception) {
            println("[Custom Offline Videos Limit] TikTokOfflineVideosHook.<clinit> note: ${e.message}")
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
                    listProviderMethod.addInstructions(
                        returnIndex,
                        """
                            invoke-static {v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->getOfflineLimits(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Custom Offline Videos Limit] Hooked limits list provider (${listProviderMethodRef.definingClass}->${listProviderMethodRef.name}) at ${returnIndices.size} return point(s).")
                    patched++
                }
            } else {
                println("[Custom Offline Videos Limit] Limits list provider method not found in onAssemPostCreate.")
            }
        } catch (e: Exception) {
            println("[Custom Offline Videos Limit] onAssemPostCreate note: ${e.message}")
        }

        // 3. Dynamic discovery through OfflineModeSheetPageAssem.Kr -> Title formatter
        try {
            val assemClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
            )
            val assemClass = assemClassFp.classDef
            val krMethod = assemClass.methods.firstOrNull { it.name == "Kr" }
            var titleFormatterMethodRef: MethodReference? = null

            if (krMethod?.implementation != null) {
                for (instruction in krMethod.implementation!!.instructions) {
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
            }

            if (titleFormatterMethodRef != null) {
                val titleFormatterMethod = Fingerprint(
                    definingClass = titleFormatterMethodRef.definingClass,
                    name = titleFormatterMethodRef.name,
                    parameters = listOf("I", "Landroid/app/Activity;"),
                    returnType = "Ljava/lang/String;",
                ).method

                val returnIndices = titleFormatterMethod.implementation?.instructions?.withIndex()
                    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
                    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
                    ?.toList() ?: emptyList()

                returnIndices.asReversed().forEach { (returnIndex, reg) ->
                    titleFormatterMethod.addInstructions(
                        returnIndex,
                        """
                            invoke-static {p0, p1, v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatTitle(ILandroid/app/Activity;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Custom Offline Videos Limit] Hooked title formatter (${titleFormatterMethodRef.definingClass}->${titleFormatterMethodRef.name}) at ${returnIndices.size} return point(s).")
                    patched++
                }
            } else {
                println("[Custom Offline Videos Limit] Title formatter method not found in Kr.")
            }
        } catch (e: Exception) {
            println("[Custom Offline Videos Limit] Kr note: ${e.message}")
        }

        // 4. Dynamic discovery through OfflineModeSheetPageAssem.Sr -> Progress subtitle
        try {
            val assemClassFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;",
            )
            val assemClass = assemClassFp.classDef
            val srMethod = assemClass.methods.firstOrNull { it.name == "Sr" }
            var progressSubtitleMethodRef: MethodReference? = null

            if (srMethod?.implementation != null) {
                for (instruction in srMethod.implementation!!.instructions) {
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
                    subtitleMethod.addInstructions(
                        returnIndex,
                        """
                            invoke-static {p1, p2, v$reg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatProgressSubtitle(Landroid/content/Context;ILjava/lang/String;)Ljava/lang/String;
                            move-result-object v$reg
                        """.trimIndent(),
                    )
                }

                if (returnIndices.isNotEmpty()) {
                    println("[Custom Offline Videos Limit] Hooked progress subtitle (${progressSubtitleMethodRef.definingClass}->${progressSubtitleMethodRef.name}) at ${returnIndices.size} return point(s).")
                    patched++
                }
            } else {
                println("[Custom Offline Videos Limit] Progress subtitle method not found in Sr.")
            }
        } catch (e: Exception) {
            println("[Custom Offline Videos Limit] Sr note: ${e.message}")
        }

        // 5. Radio item cell class discovery and subtitle hook
        try {
            val radioCellFingerprint = Fingerprint(
                custom = { method, classDef ->
                    val fieldTypes = classDef.fields.map { it.type }.toSet()
                    fieldTypes.contains("I") &&
                        fieldTypes.contains("Landroid/app/Activity;") &&
                        fieldTypes.contains("Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;") &&
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
                            invoke-static {v$titleReg, v$subtitleReg}, ${Constants.TIKTOK_EXTENSION_OFFLINE_VIDEOS_HOOK}->formatRadioSubtitle(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$subtitleReg
                        """.trimIndent(),
                    )
                    println("[Custom Offline Videos Limit] Hooked radio item subtitle in ${radioCellFingerprint.classDef.type}->${method.name} at instruction $hookIndex.")
                    patched++
                } else {
                    println("[Custom Offline Videos Limit] Constructor for radio item subtitle not found in ${radioCellFingerprint.classDef.type}->${method.name}.")
                }
            }
        } catch (e: Exception) {
            println("[Custom Offline Videos Limit] Radio item cell note: ${e.message}")
        }

        println("[Custom Offline Videos Limit] Applied $patched offline videos limit hook(s).")
    }
}
