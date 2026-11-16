/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2470
 *
 * Original code hard forked from:
 * https://github.com/ReVanced/revanced-patches/blob/724e6d61b2ecd868c1a9a37d465a688e83a74799/patches/src/main/kotlin/app/revanced/patches/all/misc/versioncode/ChangeVersionCodePatch.kt
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lcom/kveld9/morphe/extension/DisablePlayStoreUpdatesPatch;"

private object PlayStoreUpdatesState {
    var originalVersionCode: Int = 0
    var alreadyDetached: Boolean = false

    fun reset() {
        originalVersionCode = 0
        alreadyDetached = false
    }
}

@Suppress("unused")
private val disablePlayStoreUpdatesResourcePatch = resourcePatch {
    execute {
        PlayStoreUpdatesState.reset()

        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Disable Play Store updates] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        document(manifestFile.absolutePath).use { doc ->
            val manifest = doc.documentElement
            val raw = manifest.getAttribute("android:versionCode").takeIf { it.isNotBlank() }
                ?: manifest.getAttributeNS(ANDROID_XML_NAMESPACE, "versionCode").takeIf { it.isNotBlank() }
                ?: manifest.getAttribute("versionCode").takeIf { it.isNotBlank() }
                ?: "0"

            val parsed = raw.toIntOrNull() ?: 0
            if (parsed == Int.MAX_VALUE) {
                PlayStoreUpdatesState.alreadyDetached = true
                PlayStoreUpdatesState.originalVersionCode = Int.MAX_VALUE
                println("[Disable Play Store updates] APK was already detached (manifest versionCode is already Int.MAX_VALUE); in-app checks will keep seeing Int.MAX_VALUE.")
                return@use
            }

            PlayStoreUpdatesState.originalVersionCode = parsed
            if (PlayStoreUpdatesState.originalVersionCode == 0) {
                println("[Disable Play Store updates] Warning: manifest versionCode missing or unparsable, using 0.")
            }
        }
    }

    finalize {
        if (PlayStoreUpdatesState.alreadyDetached) {
            return@finalize
        }

        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            return@finalize
        }

        document(manifestFile.absolutePath).use { doc ->
            val manifest = doc.documentElement
            manifest.setAttribute("android:versionCode", Int.MAX_VALUE.toString())
        }
    }
}

@Suppress("unused")
val universalDisablePlayStoreUpdatesPatch = bytecodePatch(
    name = "Disable Play Store updates",
    description = "Disables Play Store updates by setting the version code to the maximum allowed.",
    default = false,
) {
    dependsOn(disablePlayStoreUpdatesResourcePatch)
    dependsOn(sharedExtensionPatch)

    finalize {
        if (!PlayStoreUpdatesState.alreadyDetached) {
            val extClass = mutableClassDefByOrNull(EXTENSION_CLASS)
            val originalVersionMethod = extClass?.methods?.firstOrNull { it.name == "originalVersionCode" }
            if (originalVersionMethod != null) {
                val impl = originalVersionMethod.implementation
                if (impl != null) {
                    originalVersionMethod.clearTryBlocks()
                    originalVersionMethod.ensureRegisterCount(1)
                    originalVersionMethod.removeInstructions(0, impl.instructions.count())
                    originalVersionMethod.addInstructions(0, "const v0, ${PlayStoreUpdatesState.originalVersionCode}\nreturn v0")
                }
            }
        }

        var patched = 0
        var versionCodeFieldHooks = 0
        var longVersionCodeHooks = 0
        var touchedClasses = 0
        var skippedHighRegisters = 0

        classDefForEach { classDef ->
            if (classDef.type.startsWith("Lcom/kveld9/morphe/extension") ||
                classDef.type.startsWith("Lapp/morphe/extension")
            ) {
                return@classDefForEach
            }

            var mutableMethods: List<MutableMethod>? = null
            var classModified = false

            for ((methodIndex, method) in classDef.methods.withIndex()) {
                val mutations = scanMethodMutations(method)
                if (mutations.isNotEmpty()) {
                    val methods = mutableMethods ?: mutableClassDefBy(classDef).methods.toList().also {
                        mutableMethods = it
                    }
                    applyMutations(
                        methods[methodIndex],
                        mutations,
                        onApplied = { type ->
                            classModified = true
                            when (type) {
                                MutationType.FIELD_ACCESS -> versionCodeFieldHooks++
                                MutationType.LONG_METHOD_CALL -> longVersionCodeHooks++
                            }
                            patched++
                        },
                        onSkippedHighRegister = {
                            skippedHighRegisters++
                        },
                    )
                }
            }

            if (classModified) {
                touchedClasses++
            }
        }

        val highRegMsg = if (skippedHighRegisters > 0) " ($skippedHighRegisters high-register call site(s) left unhooked)" else ""

        if (patched == 0) {
            val statusMsg = if (PlayStoreUpdatesState.alreadyDetached) {
                "Manifest versionCode was already ${Int.MAX_VALUE}"
            } else {
                "Set manifest versionCode to ${PlayStoreUpdatesState.originalVersionCode} -> ${Int.MAX_VALUE}"
            }
            println(
                "[Disable Play Store updates] $statusMsg (0 bytecode version check calls found)$highRegMsg."
            )
            return@finalize
        }

        val statusMsg = if (PlayStoreUpdatesState.alreadyDetached) {
            "Manifest versionCode was already ${Int.MAX_VALUE}"
        } else {
            "Set manifest versionCode to ${PlayStoreUpdatesState.originalVersionCode} -> ${Int.MAX_VALUE}"
        }
        println(
            "[Disable Play Store updates] $statusMsg and applied $patched version check hook(s) across $touchedClasses class(es) " +
                "($versionCodeFieldHooks versionCode field reads, $longVersionCodeHooks getLongVersionCode calls)$highRegMsg."
        )
    }
}

private enum class MutationType {
    FIELD_ACCESS,
    LONG_METHOD_CALL,
}

private sealed class VersionCodeMutation(val instructionIndex: Int) {
    class FieldAccess(
        index: Int,
        val packageInfoRegister: Int,
        val moveResultRegister: Int,
    ) : VersionCodeMutation(index)

    class LongMethodCall(
        index: Int,
        val register: Int,
    ) : VersionCodeMutation(index)
}

private fun isVersionCodeFieldAccess(inst: Instruction): Boolean {
    if (inst.opcode != Opcode.IGET) return false
    val fieldRef = (inst as? ReferenceInstruction)?.reference as? FieldReference ?: return false
    if (fieldRef.name != "versionCode") return false
    if (fieldRef.type != "I") return false
    return fieldRef.definingClass == "Landroid/content/pm/PackageInfo;"
}

private fun isGetLongVersionCodeCall(inst: Instruction): Boolean {
    val opcode = inst.opcode
    if (opcode != Opcode.INVOKE_VIRTUAL && opcode != Opcode.INVOKE_VIRTUAL_RANGE) return false
    val methodRef = (inst as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    if (methodRef.name != "getLongVersionCode") return false
    if (methodRef.returnType != "J") return false
    if (methodRef.parameterTypes.isNotEmpty()) return false
    return methodRef.definingClass == "Landroid/content/pm/PackageInfo;"
}

private fun scanMethodMutations(method: Method): List<VersionCodeMutation> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val mutations = mutableListOf<VersionCodeMutation>()

    for ((index, inst) in instructions.withIndex()) {
        if (isVersionCodeFieldAccess(inst)) {
            val twoReg = inst as? TwoRegisterInstruction ?: continue
            mutations.add(
                VersionCodeMutation.FieldAccess(
                    index = index,
                    packageInfoRegister = twoReg.registerB,
                    moveResultRegister = twoReg.registerA,
                )
            )
        } else if (isGetLongVersionCodeCall(inst)) {
            if (index + 1 >= instructions.size) continue
            if (instructions[index + 1].opcode != Opcode.MOVE_RESULT_WIDE) continue

            val register = when (inst) {
                is FiveRegisterInstruction -> inst.registerC
                is RegisterRangeInstruction -> inst.startRegister
                else -> null
            } ?: continue

            mutations.add(
                VersionCodeMutation.LongMethodCall(
                    index = index,
                    register = register,
                )
            )
        }
    }

    return mutations
}

private fun applyMutations(
    method: MutableMethod,
    mutations: List<VersionCodeMutation>,
    onApplied: (MutationType) -> Unit,
    onSkippedHighRegister: () -> Unit,
) {
    val sortedMutations = mutations.sortedByDescending { it.instructionIndex }

    for (mutation in sortedMutations) {
        when (mutation) {
            is VersionCodeMutation.FieldAccess -> {
                if (mutation.moveResultRegister >= 16) {
                    onSkippedHighRegister()
                    continue
                }
                method.replaceInstruction(
                    mutation.instructionIndex,
                    "invoke-static/range { v${mutation.packageInfoRegister} .. v${mutation.packageInfoRegister} }, " +
                        "$EXTENSION_CLASS->getVersionCode(Landroid/content/pm/PackageInfo;)I"
                )
                method.addInstruction(
                    mutation.instructionIndex + 1,
                    "move-result v${mutation.moveResultRegister}"
                )
                onApplied(MutationType.FIELD_ACCESS)
            }
            is VersionCodeMutation.LongMethodCall -> {
                method.replaceInstruction(
                    mutation.instructionIndex,
                    "invoke-static/range { v${mutation.register} .. v${mutation.register} }, " +
                        "$EXTENSION_CLASS->getVersionCodeLong(Landroid/content/pm/PackageInfo;)J"
                )
                onApplied(MutationType.LONG_METHOD_CALL)
            }
        }
    }
}
