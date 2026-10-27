package app.morphe.patches.tiktok.performance

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.addInstructionsAtControlFlowLabel
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val disableQuicPatch = bytecodePatch(
    name = "Disable QUIC",
    description = "Loads videos, uploads, and live connections over standard HTTPS instead of QUIC.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        val videoLoaderMethod = try {
            Fingerprint(
                definingClass = "Lcom/ss/mediakit/medialoader/AVMDLDataLoader;",
                name = "setConfigureInternal",
                parameters = listOf("Lcom/ss/mediakit/medialoader/AVMDLDataLoaderConfigure;"),
                returnType = "V",
            ).method
        } catch (e: Exception) {
            throw PatchException("Target 1 anchor not found: AVMDLDataLoader.setConfigureInternal(AVMDLDataLoaderConfigure)V", e)
        }
        hookVideoLoader(videoLoaderMethod)

        val liveMethod = try {
            Fingerprint(
                definingClass = "Lorg/chromium/wschannel/CronetFrontierConnection;",
                name = "covertTransportMode",
                parameters = listOf("I"),
            ).method
        } catch (e: Exception) {
            throw PatchException("Target 2 anchor not found: CronetFrontierConnection.covertTransportMode(I)", e)
        }
        hookLiveConnection(liveMethod)

        val uploadMethod = try {
            Fingerprint(
                definingClass = "Lcom/ss/bduploader/BDUploadUtil;",
                name = "xquicAvalilable",
                returnType = "Z",
                parameters = emptyList(),
            ).method
        } catch (e: Exception) {
            throw PatchException("Target 3 anchor not found: BDUploadUtil.xquicAvalilable()Z", e)
        }
        hookUploadUtil(uploadMethod)

        println("[Disable QUIC] Applied 3 QUIC suppression hook(s).")
    }
}

private fun buildVideoLoaderInstructions(p1Reg: Int): String {
    val targetClass = "Lcom/ss/mediakit/medialoader/AVMDLDataLoaderConfigure;"
    if (p1Reg > 15) {
        return """
            const/4 v0, 0x0
            move-object/16 v1, p1
            iput v0, v1, $targetClass->mEnableTTQuicHeLoader:I
            iput v0, v1, $targetClass->mEnableTTQuicHeLoaderCheckWithNetworkType:I
        """.trimIndent()
    }
    return """
        const/4 v0, 0x0
        iput v0, p1, $targetClass->mEnableTTQuicHeLoader:I
        iput v0, p1, $targetClass->mEnableTTQuicHeLoaderCheckWithNetworkType:I
    """.trimIndent()
}

private fun hookVideoLoader(method: MutableMethod) {
    val impl = method.implementation
        ?: throw PatchException("AVMDLDataLoader.setConfigureInternal has no implementation")
    method.ensureRegisterCount(3)
    val p1Reg = impl.registerCount - 1
    method.addInstructions(0, buildVideoLoaderInstructions(p1Reg))
    println("[Disable QUIC] Hooked AVMDLDataLoader.setConfigureInternal() -> Disabled video loader QUIC engine.")
}

private fun findReturnObjectPoints(method: MutableMethod): List<Pair<Int, Int>> {
    val impl = method.implementation ?: return emptyList()
    return impl.instructions.withIndex()
        .filter { it.value.opcode == Opcode.RETURN_OBJECT }
        .map { it.index to (it.value as OneRegisterInstruction).registerA }
        .toList()
}

private fun injectTlsOverrideAtReturn(
    method: MutableMethod,
    returnIndex: Int,
    reg: Int,
    enumType: String,
) {
    if (reg > 15) {
        throw PatchException("Return register v$reg exceeds 4-bit limit in covertTransportMode")
    }
    val scratchReg = if (reg == 0) 1 else 0
    val label = "skip_quic_$returnIndex"
    method.addInstructionsAtControlFlowLabel(
        returnIndex,
        """
            sget-object v$scratchReg, $enumType->QUIC:$enumType
            if-ne v$reg, v$scratchReg, :$label
            sget-object v$scratchReg, $enumType->TLS:$enumType
            move-object v$reg, v$scratchReg
            :$label
            nop
        """.trimIndent(),
    )
}

private fun hookLiveConnection(method: MutableMethod) {
    val enumType = method.returnType
    if (!enumType.startsWith("L") || !enumType.endsWith(";")) {
        throw PatchException("Target 2 invalid return type: expected enum object, got $enumType")
    }
    val impl = method.implementation
        ?: throw PatchException("CronetFrontierConnection.covertTransportMode has no implementation")

    val returnPoints = findReturnObjectPoints(method)
    if (returnPoints.isEmpty()) {
        throw PatchException("Target 2 anchor failed: no return-object instructions found in covertTransportMode")
    }

    method.ensureRegisterCount(maxOf(impl.registerCount, 2))
    returnPoints.asReversed().forEach { (returnIndex, reg) ->
        injectTlsOverrideAtReturn(method, returnIndex, reg, enumType)
    }
    println("[Disable QUIC] Hooked CronetFrontierConnection.covertTransportMode() (${returnPoints.size} return point(s)) -> Forced TLS transport mode.")
}

private fun hookUploadUtil(method: MutableMethod) {
    if (method.implementation == null) {
        throw PatchException("BDUploadUtil.xquicAvalilable has no implementation")
    }
    method.replaceWithReturnBoolean(false)
    println("[Disable QUIC] Hooked BDUploadUtil.xquicAvalilable() -> Disabled upload QUIC transport.")
}
