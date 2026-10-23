package app.morphe.patches.universal

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.apk.ApkSignatureScheme
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lcom/kveld9/morphe/extension/SpoofSignaturePatch;"

private data class CallSiteRedirect(
    val definingClass: String,
    val name: String,
    val returnType: String,
    val parameterTypes: List<String>,
    val targetDescriptor: String,
)

private data class PendingCallSiteRewrite(
    val instructionIndex: Int,
    val newInvoke: String,
)

private data class RedirectResult(
    val redirectedCount: Int,
    val skippedHighRegisters: Int,
)

private fun isExtensionClass(type: String): Boolean =
    type.startsWith("Lcom/kveld9/morphe/extension") || type.startsWith("Lapp/morphe/extension")

private fun extractInstructionRegisters(instruction: Instruction): List<Int>? {
    return when (instruction) {
        is FiveRegisterInstruction -> listOf(
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
        ).take(instruction.registerCount)
        is RegisterRangeInstruction -> (instruction.startRegister until instruction.startRegister + instruction.registerCount).toList()
        else -> null
    }
}

private fun buildRedirectInvoke(instruction: Instruction, registers: List<Int>, targetMethod: String): String {
    return if (instruction is RegisterRangeInstruction) {
        "invoke-static/range {v${registers.first()} .. v${registers.last()}}, $targetMethod"
    } else {
        "invoke-static {${registers.joinToString(", ") { "v$it" }}}, $targetMethod"
    }
}

private fun isVirtualInvoke(opcode: Opcode): Boolean =
    opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE

private fun isRedirectCandidateMatch(candidate: CallSiteRedirect, ref: MethodReference): Boolean {
    if (candidate.definingClass != ref.definingClass) return false
    if (candidate.returnType != ref.returnType) return false
    return candidate.parameterTypes == ref.parameterTypes
}

private fun findMatchingRedirect(
    instruction: Instruction,
    redirectsByName: Map<String, List<CallSiteRedirect>>,
): CallSiteRedirect? {
    if (!isVirtualInvoke(instruction.opcode)) return null
    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    val candidates = redirectsByName[ref.name] ?: return null
    return candidates.firstOrNull { isRedirectCandidateMatch(it, ref) }
}

private fun hasHighRegisters(registers: List<Int>?): Boolean =
    registers == null || registers.any { it > 15 }

private fun tryBuildCallSiteRewrite(
    index: Int,
    inst: Instruction,
    redirectsByName: Map<String, List<CallSiteRedirect>>,
    onSkippedHighRegister: () -> Unit,
): PendingCallSiteRewrite? {
    val redirect = findMatchingRedirect(inst, redirectsByName) ?: return null
    val registers = extractInstructionRegisters(inst)
    if (hasHighRegisters(registers)) {
        onSkippedHighRegister()
        return null
    }
    val newInvoke = buildRedirectInvoke(inst, registers!!, redirect.targetDescriptor)
    return PendingCallSiteRewrite(index, newInvoke)
}

private fun scanMethodCallSites(
    method: Method,
    redirectsByName: Map<String, List<CallSiteRedirect>>,
    onSkippedHighRegister: () -> Unit,
): List<PendingCallSiteRewrite> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val rewrites = mutableListOf<PendingCallSiteRewrite>()

    for ((index, inst) in instructions.withIndex()) {
        val rewrite = tryBuildCallSiteRewrite(index, inst, redirectsByName, onSkippedHighRegister)
        if (rewrite != null) {
            rewrites.add(rewrite)
        }
    }

    return rewrites
}

private fun scanClassMutations(
    classDef: ClassDef,
    redirectsByName: Map<String, List<CallSiteRedirect>>,
    onSkippedHighRegister: () -> Unit,
): List<Pair<Int, List<PendingCallSiteRewrite>>> {
    val result = mutableListOf<Pair<Int, List<PendingCallSiteRewrite>>>()
    for ((methodIndex, method) in classDef.methods.withIndex()) {
        val rewrites = scanMethodCallSites(method, redirectsByName, onSkippedHighRegister)
        if (rewrites.isNotEmpty()) {
            result.add(methodIndex to rewrites)
        }
    }
    return result
}

private fun applyCallSiteRewrites(
    method: MutableMethod,
    rewrites: List<PendingCallSiteRewrite>,
) {
    for (rewrite in rewrites.sortedByDescending { it.instructionIndex }) {
        method.replaceInstruction(rewrite.instructionIndex, rewrite.newInvoke)
    }
}

private fun BytecodePatchContext.applyRedirects(
    redirects: List<CallSiteRedirect>,
): RedirectResult {
    val redirectsByName = redirects.groupBy { it.name }
    var redirected = 0
    var skippedHighRegisters = 0

    classDefForEach { classDef ->
        if (isExtensionClass(classDef.type)) return@classDefForEach
        val mutations = scanClassMutations(classDef, redirectsByName) { skippedHighRegisters++ }
        if (mutations.isEmpty()) return@classDefForEach

        val mutableClass = mutableClassDefBy(classDef)
        val mutableMethods = mutableClass.methods.toList()
        for ((methodIndex, rewrites) in mutations) {
            applyCallSiteRewrites(mutableMethods[methodIndex], rewrites)
            redirected += rewrites.size
        }
    }

    return RedirectResult(redirected, skippedHighRegisters)
}

private fun escapeSmaliString(value: String): String =
    value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

private fun BytecodePatchContext.bakeStringMethod(methodName: String, value: String): Boolean {
    val extClass = mutableClassDefByOrNull(EXTENSION_CLASS) ?: return false
    val method = extClass.methods.firstOrNull { it.name == methodName } ?: return false
    val impl = method.implementation ?: return false
    method.clearTryBlocks()
    method.ensureRegisterCount(1)
    method.removeInstructions(0, impl.instructions.count())
    method.addInstructions(0, "const-string v0, \"${escapeSmaliString(value)}\"\nreturn-object v0")
    return true
}

// ============================================================================
// PATCH 1: Report Play Store as installer
// ============================================================================

private val INSTALLER_REDIRECTS = listOf(
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/PackageManager;",
        name = "getInstallerPackageName",
        returnType = "Ljava/lang/String;",
        parameterTypes = listOf("Ljava/lang/String;"),
        targetDescriptor = "$EXTENSION_CLASS->getInstallerPackageName(Landroid/content/pm/PackageManager;Ljava/lang/String;)Ljava/lang/String;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/PackageManager;",
        name = "getInstallSourceInfo",
        returnType = "Landroid/content/pm/InstallSourceInfo;",
        parameterTypes = listOf("Ljava/lang/String;"),
        targetDescriptor = "$EXTENSION_CLASS->getInstallSourceInfo(Landroid/content/pm/PackageManager;Ljava/lang/String;)Landroid/content/pm/InstallSourceInfo;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/InstallSourceInfo;",
        name = "getInstallingPackageName",
        returnType = "Ljava/lang/String;",
        parameterTypes = emptyList(),
        targetDescriptor = "$EXTENSION_CLASS->getInstallingPackageName(Landroid/content/pm/InstallSourceInfo;)Ljava/lang/String;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/InstallSourceInfo;",
        name = "getInitiatingPackageName",
        returnType = "Ljava/lang/String;",
        parameterTypes = emptyList(),
        targetDescriptor = "$EXTENSION_CLASS->getInitiatingPackageName(Landroid/content/pm/InstallSourceInfo;)Ljava/lang/String;",
    ),
)

@Suppress("unused")
val universalReportPlayStoreInstallerPatch = bytecodePatch(
    name = "Report Play Store as installer",
    description = "Tells apps that check their install source that they came from Google Play.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)

    execute {
        val ownPackage = packageMetadata.packageName
        bakeStringMethod("ownPackage", ownPackage)

        val result = applyRedirects(INSTALLER_REDIRECTS)
        val highRegMsg = if (result.skippedHighRegisters > 0) {
            " (${result.skippedHighRegisters} call site(s) with registers > v15 left unhooked)"
        } else {
            ""
        }

        if (result.redirectedCount == 0) {
            println("[Report Play Store as installer] Skipped: No installer lookup call sites found$highRegMsg.")
            return@execute
        }

        println("[Report Play Store as installer] Redirected ${result.redirectedCount} installer lookup(s)$highRegMsg.")
    }
}

// ============================================================================
// PATCH 2: Spoof signature
// ============================================================================

private val SIGNATURE_REDIRECTS = listOf(
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/PackageManager;",
        name = "getPackageInfo",
        returnType = "Landroid/content/pm/PackageInfo;",
        parameterTypes = listOf("Ljava/lang/String;", "I"),
        targetDescriptor = "$EXTENSION_CLASS->getPackageInfo(Landroid/content/pm/PackageManager;Ljava/lang/String;I)Landroid/content/pm/PackageInfo;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/PackageManager;",
        name = "getPackageInfo",
        returnType = "Landroid/content/pm/PackageInfo;",
        parameterTypes = listOf("Ljava/lang/String;", "Landroid/content/pm/PackageManager\$PackageInfoFlags;"),
        targetDescriptor = "$EXTENSION_CLASS->getPackageInfo(Landroid/content/pm/PackageManager;Ljava/lang/String;Landroid/content/pm/PackageManager\$PackageInfoFlags;)Landroid/content/pm/PackageInfo;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/PackageManager;",
        name = "hasSigningCertificate",
        returnType = "Z",
        parameterTypes = listOf("Ljava/lang/String;", "[B", "I"),
        targetDescriptor = "$EXTENSION_CLASS->hasSigningCertificate(Landroid/content/pm/PackageManager;Ljava/lang/String;[BI)Z",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/SigningInfo;",
        name = "getApkContentsSigners",
        returnType = "[Landroid/content/pm/Signature;",
        parameterTypes = emptyList(),
        targetDescriptor = "$EXTENSION_CLASS->getApkContentsSigners(Landroid/content/pm/SigningInfo;)[Landroid/content/pm/Signature;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/SigningInfo;",
        name = "getSigningCertificateHistory",
        returnType = "[Landroid/content/pm/Signature;",
        parameterTypes = emptyList(),
        targetDescriptor = "$EXTENSION_CLASS->getSigningCertificateHistory(Landroid/content/pm/SigningInfo;)[Landroid/content/pm/Signature;",
    ),
    CallSiteRedirect(
        definingClass = "Landroid/content/pm/SigningInfo;",
        name = "hasMultipleSigners",
        returnType = "Z",
        parameterTypes = emptyList(),
        targetDescriptor = "$EXTENSION_CLASS->hasMultipleSigners(Landroid/content/pm/SigningInfo;)Z",
    ),
)

private fun extractOriginalCertificatesHex(packageMetadata: PackageMetadata): String? {
    val certs = packageMetadata.signingCertificates[ApkSignatureScheme.V3]?.takeIf { it.isNotEmpty() }
        ?: packageMetadata.signingCertificates[ApkSignatureScheme.V2]?.takeIf { it.isNotEmpty() }
        ?: return null

    return certs.joinToString(",") { cert ->
        cert.encoded.joinToString("") { "%02x".format(it) }
    }
}

@Suppress("unused")
val universalSpoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Presents the original release signature so modified-app checks pass.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)

    execute {
        val certsHex = extractOriginalCertificatesHex(packageMetadata)
        if (certsHex.isNullOrEmpty()) {
            println("[Spoof signature] Skipped: APK has no V2/V3 signature to keep.")
            return@execute
        }

        bakeStringMethod("ownPackage", packageMetadata.packageName)
        bakeStringMethod("certificates", certsHex)

        val result = applyRedirects(SIGNATURE_REDIRECTS)
        val highRegMsg = if (result.skippedHighRegisters > 0) {
            " (${result.skippedHighRegisters} call site(s) with registers > v15 left unhooked)"
        } else {
            ""
        }

        if (result.redirectedCount == 0) {
            println("[Spoof signature] Skipped: No signature lookup call sites found$highRegMsg.")
            return@execute
        }

        println("[Spoof signature] Redirected ${result.redirectedCount} signature lookup(s)$highRegMsg.")
    }
}
