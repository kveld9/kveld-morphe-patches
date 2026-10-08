package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private enum class SiteType {
    CAMERA_OPEN,
    CAMERA_RELEASE,
    MIC_START,
    MIC_STOP,
}

private data class SiteEdit(
    val insertIndex: Int,
    val smali: String,
    val type: SiteType,
)

private const val HOOK = Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK

private data class Target(
    val definingClass: String,
    val name: String,
    val returnType: String,
    val noParameters: Boolean,
    val type: SiteType,
    val isAfterMoveResult: Boolean = false,
    val smali: String = "",
)

private val TARGET_TABLE = listOf(
    Target(
        definingClass = "Landroid/hardware/Camera;",
        name = "open",
        returnType = "Landroid/hardware/Camera;",
        noParameters = false,
        type = SiteType.CAMERA_OPEN,
        isAfterMoveResult = true,
    ),
    Target(
        definingClass = "Landroid/hardware/camera2/CameraManager;",
        name = "openCamera",
        returnType = "V",
        noParameters = false,
        type = SiteType.CAMERA_OPEN,
        smali = "invoke-static {}, $HOOK->onCameraStart()V",
    ),
    Target(
        definingClass = "Landroid/hardware/Camera;",
        name = "release",
        returnType = "V",
        noParameters = true,
        type = SiteType.CAMERA_RELEASE,
        smali = "invoke-static {}, $HOOK->onCameraStop()V",
    ),
    Target(
        definingClass = "Landroid/hardware/camera2/CameraDevice;",
        name = "close",
        returnType = "V",
        noParameters = true,
        type = SiteType.CAMERA_RELEASE,
        smali = "invoke-static {}, $HOOK->onCameraStop()V",
    ),
    Target(
        definingClass = "Landroid/media/AudioRecord;",
        name = "startRecording",
        returnType = "V",
        noParameters = false,
        type = SiteType.MIC_START,
        smali = "invoke-static {}, $HOOK->onMicStart()V",
    ),
    Target(
        definingClass = "Landroid/media/MediaRecorder;",
        name = "start",
        returnType = "V",
        noParameters = true,
        type = SiteType.MIC_START,
        smali = "invoke-static {}, $HOOK->onMicStart()V",
    ),
    Target(
        definingClass = "Landroid/media/AudioRecord;",
        name = "stop",
        returnType = "V",
        noParameters = true,
        type = SiteType.MIC_STOP,
        smali = "invoke-static {}, $HOOK->onMicStop()V",
    ),
    Target(
        definingClass = "Landroid/media/AudioRecord;",
        name = "release",
        returnType = "V",
        noParameters = true,
        type = SiteType.MIC_STOP,
        smali = "invoke-static {}, $HOOK->onMicStop()V",
    ),
    Target(
        definingClass = "Landroid/media/MediaRecorder;",
        name = "stop",
        returnType = "V",
        noParameters = true,
        type = SiteType.MIC_STOP,
        smali = "invoke-static {}, $HOOK->onMicStop()V",
    ),
    Target(
        definingClass = "Landroid/media/MediaRecorder;",
        name = "release",
        returnType = "V",
        noParameters = true,
        type = SiteType.MIC_STOP,
        smali = "invoke-static {}, $HOOK->onMicStop()V",
    ),
)

private val TARGET_NAMES = TARGET_TABLE.map { it.name }.toSet()

private fun Target.matches(ref: MethodReference): Boolean {
    if (ref.definingClass != definingClass) return false
    if (ref.returnType != returnType) return false
    if (noParameters && !ref.parameterTypes.isEmpty()) return false
    return true
}

private fun matchTarget(instruction: Instruction): Target? {
    if (instruction.opcode.referenceType != ReferenceType.METHOD) return null
    val ref = (instruction as ReferenceInstruction).reference as MethodReference
    if (ref.name !in TARGET_NAMES) return null
    return TARGET_TABLE.firstOrNull { target ->
        target.name == ref.name && target.matches(ref)
    }
}

private fun computeEdit(
    target: Target,
    index: Int,
    instructions: List<Instruction>,
): SiteEdit? {
    if (target.isAfterMoveResult) {
        val nextInsn = instructions.getOrNull(index + 1) ?: return null
        if (nextInsn.opcode != Opcode.MOVE_RESULT_OBJECT) return null
        val cameraReg = (nextInsn as OneRegisterInstruction).registerA
        return SiteEdit(
            insertIndex = index + 2,
            smali = "invoke-static/range {v$cameraReg .. v$cameraReg}, $HOOK->onCameraOpened(Landroid/hardware/Camera;)V",
            type = target.type,
        )
    }
    return SiteEdit(
        insertIndex = index + 1,
        smali = target.smali,
        type = target.type,
    )
}

val cameraMicIndicatorPatch = bytecodePatch(
    name = "Camera & Microphone Indicator",
    description = "Shows a corner mark while TikTok holds camera/mic open.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    execute {
        // (a) Hook MainActivity.onCreate at index 0 to install the indicator overlay view.
        val mainActivityFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/main/MainActivity;",
            name = "onCreate",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        )
        mainActivityFp.method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->install(Landroid/app/Activity;)V
            """.trimIndent(),
        )

        // (b) Instrument invoke sites across target methods using Fingerprint + matchAll.
        // Single scan over all methods instead of five; name check before class check avoids decoding long class strings.
        var cameraOpens = 0
        var cameraReleases = 0
        var micStarts = 0
        var micStops = 0

        val scanFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { matchTarget(it) != null } == true
            },
        )
        scanFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val target = matchTarget(instruction) ?: return@forEachIndexed
                val edit = computeEdit(target, index, instructions) ?: return@forEachIndexed
                edits.add(edit)
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    when (edit.type) {
                        SiteType.CAMERA_OPEN -> cameraOpens++
                        SiteType.CAMERA_RELEASE -> cameraReleases++
                        SiteType.MIC_START -> micStarts++
                        SiteType.MIC_STOP -> micStops++
                    }
                }
            }
        }

        val totalSites = cameraOpens + cameraReleases + micStarts + micStops
        if (totalSites == 0) {
            throw PatchException("Zero camera and microphone call sites found to instrument.")
        }
        var patched = totalSites

        println("[Camera Mic Indicator] Instrumented $cameraOpens camera opens, $cameraReleases camera releases, $micStarts mic starts, $micStops mic stops across $totalSites call site(s) -> $patched indicator hook(s) active.")
    }
}
