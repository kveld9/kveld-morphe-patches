package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val alwaysShowPublishDatePatch = bytecodePatch(
    name = "Always Show Publish Date",
    description = "Forces video publish/upload date to remain visible in video author information across all feed types, with optional creator country code tags and sensitive warning suppression.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val showAuthorRegion by booleanOption(
        key = "showAuthorRegion",
        title = "Show Author Region",
        description = "Displays the creator's country or region code next to their username in video author info across feeds and deep-linked detail views.",
        default = false,
        required = false,
    )

    val skipContentWarnings by booleanOption(
        key = "skipContentWarnings",
        title = "Skip Content Warnings",
        description = "Bypasses and clears sensitive content warnings, graphic media blur overlays, and age gates on feed videos.",
        default = false,
        required = false,
    )

    execute {
        var patched = 0
        var vmMethodFound = false

        // 1. Hook VideoAuthorInfoVM.paramSync2StateAccept
        try {
            val fp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoVM;",
                custom = { method, _ ->
                    method.name == "paramSync2StateAccept" &&
                        "Lcom/ss/android/ugc/aweme/feed/model/VideoItemParams;" in method.parameterTypes
                },
            )
            val method = fp.method
            vmMethodFound = true
            val instructions = method.implementation!!.instructions

            val regionStart = instructions.indexOfFirst { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                reference is StringReference && reference.string == "v3"
            }
            val regionEnd = instructions.withIndex().indexOfFirst { (index, instruction) ->
                if (index <= regionStart) return@indexOfFirst false
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                reference?.name == "getCreateTime" &&
                    reference.parameterTypes.isEmpty() &&
                    reference.returnType == "J"
            }

            // Collect all insertions in VideoAuthorInfoVM as (targetIndex, code) pairs
            val vmInjections = mutableListOf<Pair<Int, String>>()

            // 1a. Bypass early empty-state return (LX/0Age;->LIZ(Aweme;)Z) before regionStart
            if (regionStart > 0) {
                val earlyReturnIndex = instructions.withIndex().indexOfFirst { (index, instruction) ->
                    if (index >= regionStart) return@indexOfFirst false
                    if (instruction.opcode != Opcode.INVOKE_STATIC) return@indexOfFirst false
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@indexOfFirst false
                    if (ref.returnType != "Z" || ref.parameterTypes != listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;")) return@indexOfFirst false
                    if (index + 2 >= instructions.size) return@indexOfFirst false
                    if (method.getInstruction(index + 1).opcode != Opcode.MOVE_RESULT) return@indexOfFirst false
                    if (method.getInstruction(index + 2).opcode != Opcode.IF_EQZ) return@indexOfFirst false
                    (1..4).any { offset ->
                        index + 2 + offset < instructions.size &&
                            method.getInstruction(index + 2 + offset).opcode == Opcode.NEW_INSTANCE
                    }
                }
                if (earlyReturnIndex >= 0) {
                    val resultReg = method.getInstruction<OneRegisterInstruction>(earlyReturnIndex + 1).registerA
                    vmInjections.add(earlyReturnIndex + 2 to "const/4 v$resultReg, 0x0")
                    println("[Always Show Publish Date] Located early empty-state return bypass at index $earlyReturnIndex.")
                }

                // 1b. Unblock pre-region gates right before regionStart (LX/07Ry;->LIZ and LX/07xU;->LJJIJL)
                val preRegionGateIndices = instructions.withIndex()
                    .filter { (index, instruction) ->
                        index in (regionStart - 15) until regionStart &&
                            instruction.opcode == Opcode.INVOKE_STATIC &&
                            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z" &&
                            index + 2 < instructions.size &&
                            method.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT &&
                            method.getInstruction(index + 2).opcode == Opcode.IF_EQZ
                    }
                    .map { it.index }

                preRegionGateIndices.forEach { index ->
                    val resultReg = method.getInstruction<OneRegisterInstruction>(index + 1).registerA
                    vmInjections.add(index + 2 to "const/4 v$resultReg, 0x1")
                }
                if (preRegionGateIndices.isNotEmpty()) {
                    println("[Always Show Publish Date] Located ${preRegionGateIndices.size} pre-region gate(s).")
                }
            }

            // 1c. Feed string gates inside regionStart..regionEnd
            if (regionStart >= 0 && regionEnd > regionStart) {
                val gateCallIndices = instructions.withIndex()
                    .filter { (index, instruction) ->
                        index in regionStart..regionEnd &&
                            (instruction as? Instruction)?.opcode == Opcode.INVOKE_STATIC &&
                            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                                ref.returnType == "Z" && ref.parameterTypes == listOf("Ljava/lang/String;")
                            } == true &&
                            index + 1 < instructions.size &&
                            method.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT
                    }
                    .map { it.index }

                gateCallIndices.forEach { index ->
                    val resultRegister = method.getInstruction<OneRegisterInstruction>(index + 1).registerA
                    vmInjections.add(index + 2 to "const/16 v$resultRegister, 0x1")
                }
                println("[Always Show Publish Date] Located ${gateCallIndices.size} feed type gate(s).")
            }

            // Apply all VM injections from highest index to lowest to maintain index stability
            vmInjections.sortByDescending { it.first }
            vmInjections.forEach { (index, code) ->
                method.addInstructions(index, code)
                patched++
            }
            println("[Always Show Publish Date] Applied ${vmInjections.size} VM hook(s).")

            // 8b: Author region hook via VideoAuthorInfoVM
            if (showAuthorRegion == true) {
                val paramReg = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
                method.addInstructions(
                    0,
                    "invoke-static/range {$paramReg .. $paramReg}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->onVideoItemParams(Ljava/lang/Object;)V",
                )
                println("[Always Show Publish Date] Hooked VideoAuthorInfoVM.paramSync2StateAccept -> Author region sync active.")
                patched++
            }

            // 8d: Skip content warnings
            // Justification: VideoAuthorInfoVM.paramSync2StateAccept(VideoItemParams) is the authoritative
            // state entry point where video parameters and warning metadata are ingested before binding
            // to view states. Clearing warnings here via SensitiveWarnings.clear(params) prevents warning
            // overlays and blur masks from ever attaching to feed items.
            if (skipContentWarnings == true) {
                val paramReg = if (AccessFlags.STATIC.isSet(method.accessFlags)) "p0" else "p1"
                method.addInstructions(
                    0,
                    "invoke-static/range {$paramReg .. $paramReg}, ${Constants.TIKTOK_EXTENSION_SENSITIVE_WARNINGS_HOOK}->clear(Ljava/lang/Object;)V",
                )
                println("[Always Show Publish Date] Prepended SensitiveWarnings.clear at index 0.")
                patched++
            }
        } catch (e: Exception) {
            println("[Always Show Publish Date] VideoAuthorInfoVM note: ${e.message}")
        }

        if (skipContentWarnings == true && !vmMethodFound) {
            throw PatchException("Anchor VideoAuthorInfoVM.paramSync2StateAccept(VideoItemParams) not found for skipContentWarnings")
        }

        // 8b: Deep-link without feed activity hooks
        if (showAuthorRegion == true) {
            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
                    name = "onCreate",
                    parameters = listOf("Landroid/os/Bundle;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->install(Landroid/app/Activity;)V
                    """.trimIndent(),
                )
                println("[Always Show Publish Date] Hooked MainActivity.onCreate -> author region monitor active.")
                patched++
            } catch (e: Exception) {
                println("[Always Show Publish Date] MainActivity note: ${e.message}")
            }

            try {
                Fingerprint(
                    definingClass = "Lcom/ss/android/ugc/aweme/detail/ui/DetailActivity;",
                    name = "onCreate",
                    parameters = listOf("Landroid/os/Bundle;"),
                    returnType = "V",
                ).method.addInstructions(
                    0,
                    """
                        invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_AUTHOR_REGION_HOOK}->install(Landroid/app/Activity;)V
                    """.trimIndent(),
                )
                println("[Always Show Publish Date] Hooked DetailActivity.onCreate -> author region monitor active.")
                patched++
            } catch (e: Exception) {
                println("[Always Show Publish Date] DetailActivity note: ${e.message}")
            }
        }

        // 2. Hook VideoAuthorInfoRelationAssem (onViewCreated and hs)
        var observerClassName: String? = null
        try {
            val fpAssem = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoRelationAssem;",
            )

            // 2a. onViewCreated: force post-time Assem binding gates to true
            fpAssem.classDef.methods.find { it.name == "onViewCreated" }?.let { method ->
                val instructions = method.implementation!!.instructions
                val subscribeIndex = instructions.withIndex().indexOfLast { (_, instruction) ->
                    (instruction.opcode == Opcode.INVOKE_STATIC || instruction.opcode == Opcode.INVOKE_STATIC_RANGE) &&
                        ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "LJIIJ"
                }

                if (subscribeIndex > 0) {
                    for (i in maxOf(0, subscribeIndex - 6) until subscribeIndex) {
                        if (instructions[i].opcode == Opcode.NEW_INSTANCE) {
                            val typeRef = (instructions[i] as? ReferenceInstruction)?.reference?.toString()
                            if (typeRef != null && typeRef.startsWith("L") && typeRef.endsWith(";")) {
                                observerClassName = typeRef
                            }
                        }
                    }

                    val assemGateIndices = instructions.withIndex()
                        .filter { (index, instruction) ->
                            index in subscribeIndex..(subscribeIndex + 50) &&
                                instruction.opcode == Opcode.INVOKE_STATIC &&
                                ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { ref ->
                                    ref.returnType == "Z" && ref.parameterTypes.isEmpty()
                                } == true &&
                                index + 2 < instructions.size &&
                                method.getInstruction(index + 1).opcode == Opcode.MOVE_RESULT &&
                                method.getInstruction(index + 2).opcode == Opcode.IF_EQZ
                        }
                        .map { it.index }

                    assemGateIndices.asReversed().forEach { index ->
                        val resultReg = method.getInstruction<OneRegisterInstruction>(index + 1).registerA
                        method.addInstructions(
                            index + 2,
                            """
                                const/4 v$resultReg, 0x1
                            """.trimIndent(),
                        )
                        patched++
                    }
                    if (assemGateIndices.isNotEmpty()) {
                        println("[Always Show Publish Date] Unblocked ${assemGateIndices.size} post-time binding gate(s) in VideoAuthorInfoRelationAssem.onViewCreated.")
                    }
                }
            }

            // 2b. hs: prevent hiding post-time view (LLLLJI) due to width truncation
            val hsMethod = fpAssem.classDef.methods.find { method ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference
                    ref is StringReference && ref.string.contains("[ensureTitleViewWidth]")
                } == true
            } ?: fpAssem.classDef.methods.find { it.name == "hs" }

            hsMethod?.let { method ->
                val instructions = method.implementation!!.instructions
                val truncateIndices = instructions.withIndex()
                    .filter { (index, instruction) ->
                        instruction.opcode == Opcode.CONST_16 &&
                            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 8 &&
                            index + 1 < instructions.size &&
                            instructions[index + 1].opcode == Opcode.INVOKE_STATIC &&
                            ((instructions[index + 1] as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "V"
                    }
                    .map { it.index }

                truncateIndices.asReversed().forEach { index ->
                    val reg = method.getInstruction<OneRegisterInstruction>(index).registerA
                    method.addInstructions(
                        index + 1,
                        """
                            const/16 v$reg, 0x0
                        """.trimIndent(),
                    )
                    patched++
                }
                if (truncateIndices.isNotEmpty()) {
                    println("[Always Show Publish Date] Neutralized post-time width truncation in VideoAuthorInfoRelationAssem.hs.")
                }
            }
        } catch (e: Exception) {
            println("[Always Show Publish Date] VideoAuthorInfoRelationAssem note: ${e.message}")
        }

        // 3. Hook post-time observer lambda if located
        if (observerClassName != null) {
            try {
                val fpObserver = Fingerprint(
                    definingClass = observerClassName!!,
                )
                fpObserver.classDef.methods.find { it.name == "invoke" && it.parameterTypes.size == 2 }?.let { method ->
                    val instructions = method.implementation!!.instructions
                    val hideIndices = instructions.withIndex()
                        .filter { (index, instruction) ->
                            instruction.opcode == Opcode.CONST_16 &&
                                (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 8 &&
                                index + 1 < instructions.size &&
                                instructions[index + 1].opcode == Opcode.INVOKE_STATIC &&
                                ((instructions[index + 1] as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "V"
                        }
                        .map { it.index }

                    hideIndices.asReversed().forEach { index ->
                        val reg = method.getInstruction<OneRegisterInstruction>(index).registerA
                        method.addInstructions(
                            index + 1,
                            """
                                const/16 v$reg, 0x0
                            """.trimIndent(),
                        )
                        patched++
                    }
                    if (hideIndices.isNotEmpty()) {
                        println("[Always Show Publish Date] Neutralized visibility-hide in observer $observerClassName.")
                    }
                }
            } catch (e: Exception) {
                println("[Always Show Publish Date] Observer note: ${e.message}")
            }
        }

        println("[Always Show Publish Date] Applied $patched publish date visibility hook(s).")
    }
}
