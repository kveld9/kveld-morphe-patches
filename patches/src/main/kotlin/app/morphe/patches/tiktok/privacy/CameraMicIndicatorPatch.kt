package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
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

val cameraMicIndicatorPatch = bytecodePatch(
    name = "Camera & Microphone Indicator",
    description = "Shows a corner mark while TikTok holds camera/mic open.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val enabled by booleanOption(
        key = "enabled",
        default = true,
        title = "Enable Indicator",
        description = "Shows an on-screen corner indicator while TikTok holds camera or microphone open.",
        required = false,
    )

    execute {
        if (enabled == false) {
            println("[Camera Mic Indicator] Skipped: Indicator option is disabled.")
            return@execute
        }

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
        var cameraOpens = 0
        var cameraReleases = 0
        var micStarts = 0
        var micStops = 0

        // Target API 1: Camera.open() and Camera.open(int)
        val cameraOpenFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.definingClass == "Landroid/hardware/Camera;" && ref.name == "open" && ref.returnType == "Landroid/hardware/Camera;"
                } == true
            },
        )
        cameraOpenFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/hardware/Camera;" && ref.name == "open" && ref.returnType == "Landroid/hardware/Camera;") {
                    val nextInsn = instructions.getOrNull(index + 1)
                    if (nextInsn?.opcode == Opcode.MOVE_RESULT_OBJECT) {
                        val cameraReg = (nextInsn as OneRegisterInstruction).registerA
                        edits.add(
                            SiteEdit(
                                insertIndex = index + 2,
                                smali = "invoke-static/range {v$cameraReg .. v$cameraReg}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->onCameraOpened(Landroid/hardware/Camera;)V",
                                type = SiteType.CAMERA_OPEN,
                            )
                        )
                    }
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    cameraOpens++
                }
            }
        }

        // Target API 2: CameraManager.openCamera(...)
        val cameraManagerOpenFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.definingClass == "Landroid/hardware/camera2/CameraManager;" && ref.name == "openCamera" && ref.returnType == "V"
                } == true
            },
        )
        cameraManagerOpenFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/hardware/camera2/CameraManager;" && ref.name == "openCamera" && ref.returnType == "V") {
                    edits.add(
                        SiteEdit(
                            insertIndex = index + 1,
                            smali = "invoke-static {}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->onCameraStart()V",
                            type = SiteType.CAMERA_OPEN,
                        )
                    )
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    cameraOpens++
                }
            }
        }

        // Target API 3: Camera.release() and CameraDevice.close()
        val cameraReleaseFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    val defClass = ref.definingClass
                    val name = ref.name
                    val params = ref.parameterTypes
                    val returnType = ref.returnType
                    ((defClass == "Landroid/hardware/Camera;" && name == "release" && params.isEmpty() && returnType == "V") ||
                        (defClass == "Landroid/hardware/camera2/CameraDevice;" && name == "close" && params.isEmpty() && returnType == "V"))
                } == true
            },
        )
        cameraReleaseFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                val defClass = ref.definingClass
                val name = ref.name
                val params = ref.parameterTypes
                val returnType = ref.returnType
                if ((defClass == "Landroid/hardware/Camera;" && name == "release" && params.isEmpty() && returnType == "V") ||
                    (defClass == "Landroid/hardware/camera2/CameraDevice;" && name == "close" && params.isEmpty() && returnType == "V")) {
                    edits.add(
                        SiteEdit(
                            insertIndex = index + 1,
                            smali = "invoke-static {}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->onCameraStop()V",
                            type = SiteType.CAMERA_RELEASE,
                        )
                    )
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    cameraReleases++
                }
            }
        }

        // Target API 4: AudioRecord.startRecording and MediaRecorder.start
        val micStartFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    val defClass = ref.definingClass
                    val name = ref.name
                    val params = ref.parameterTypes
                    val returnType = ref.returnType
                    ((defClass == "Landroid/media/AudioRecord;" && name == "startRecording" && returnType == "V") ||
                        (defClass == "Landroid/media/MediaRecorder;" && name == "start" && params.isEmpty() && returnType == "V"))
                } == true
            },
        )
        micStartFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                val defClass = ref.definingClass
                val name = ref.name
                val params = ref.parameterTypes
                val returnType = ref.returnType
                if ((defClass == "Landroid/media/AudioRecord;" && name == "startRecording" && returnType == "V") ||
                    (defClass == "Landroid/media/MediaRecorder;" && name == "start" && params.isEmpty() && returnType == "V")) {
                    edits.add(
                        SiteEdit(
                            insertIndex = index + 1,
                            smali = "invoke-static {}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->onMicStart()V",
                            type = SiteType.MIC_START,
                        )
                    )
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    micStarts++
                }
            }
        }

        // Target API 5: AudioRecord.stop/release and MediaRecorder.stop/release
        val micStopFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    val defClass = ref.definingClass
                    val name = ref.name
                    val params = ref.parameterTypes
                    val returnType = ref.returnType
                    ((defClass == "Landroid/media/AudioRecord;" && (name == "stop" || name == "release") && params.isEmpty() && returnType == "V") ||
                        (defClass == "Landroid/media/MediaRecorder;" && (name == "stop" || name == "release") && params.isEmpty() && returnType == "V"))
                } == true
            },
        )
        micStopFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<SiteEdit>()
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                val defClass = ref.definingClass
                val name = ref.name
                val params = ref.parameterTypes
                val returnType = ref.returnType
                if ((defClass == "Landroid/media/AudioRecord;" && (name == "stop" || name == "release") && params.isEmpty() && returnType == "V") ||
                    (defClass == "Landroid/media/MediaRecorder;" && (name == "stop" || name == "release") && params.isEmpty() && returnType == "V")) {
                    edits.add(
                        SiteEdit(
                            insertIndex = index + 1,
                            smali = "invoke-static {}, ${Constants.TIKTOK_EXTENSION_CAMERA_MIC_HOOK}->onMicStop()V",
                            type = SiteType.MIC_STOP,
                        )
                    )
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.insertIndex }
                edits.forEach { edit ->
                    method.addInstructions(edit.insertIndex, edit.smali)
                    micStops++
                }
            }
        }

        val totalSites = cameraOpens + cameraReleases + micStarts + micStops
        if (totalSites == 0) {
            throw PatchException("Zero camera and microphone call sites found to instrument.")
        }

        println("[Camera Mic Indicator] Instrumented $cameraOpens camera opens, $cameraReleases camera releases, $micStarts mic starts, $micStops mic stops across $totalSites call site(s).")
    }
}
