package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val videoFitPatch = bytecodePatch(
    name = "Video Fit",
    description = "Adjusts video display aspect ratio across feeds and story cells: 'fit' ensures the entire video is visible without cropping, 'fill' expands the video to fill the screen, or 'off' preserves stock aspect ratio.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val fitMode by stringOption(
        key = "fitMode",
        title = "Video Fit Mode",
        description = "Aspect ratio mode for feed and story videos: 'fit' (entire video visible without crop), 'fill' (crop to fill screen), or 'off' (stock behavior).",
        default = "off",
        values = mapOf("Fit video" to "fit", "Fill screen" to "fill", "Off" to "off"),
        required = false,
    )

    execute {
        val mode = fitMode ?: "off"
        if (mode == "off") {
            println("[Video Fit] Skipped: Fit mode is off.")
            return@execute
        }

        var patched = 0

        // 1. Configure runtime fitMode on TikTokVideoFitHook
        try {
            val clinit = Fingerprint(
                definingClass = Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK,
                name = "<clinit>",
            ).method
            val instructions = clinit.implementation!!.instructions
            val returnIdx = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            val insertIdx = if (returnIdx != -1) returnIdx else 0
            clinit.addInstructions(
                insertIdx,
                """
                    const-string v0, "$mode"
                    sput-object v0, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitMode:Ljava/lang/String;
                """.trimIndent(),
            )
            println("[Video Fit] Configured fit mode: $mode")
        } catch (e: Exception) {
            println("[Video Fit] TikTokVideoFitHook clinit note: ${e.message}")
        }

        val resultClass = "Lcom/ss/android/ugc/aweme/videoadaption/adaptionparams/VideoAdaptionResult;"

        // 2. Validate VideoAdaptionResult class invariants
        val resultClassDef = classDefByOrNull(resultClass)
            ?: throw PatchException("VideoAdaptionResult class not found: $resultClass")

        val fieldMap = resultClassDef.fields.associate { it.name to it.type }
        if (fieldMap["width"] != "I") {
            throw PatchException("Expected field width:I in $resultClass, found: ${fieldMap["width"]}")
        }
        if (fieldMap["height"] != "I") {
            throw PatchException("Expected field height:I in $resultClass, found: ${fieldMap["height"]}")
        }
        if (fieldMap["translateX"] != "F") {
            throw PatchException("Expected field translateX:Float in $resultClass, found: ${fieldMap["translateX"]}")
        }
        if (fieldMap["translateY"] != "F") {
            throw PatchException("Expected field translateY:Float in $resultClass, found: ${fieldMap["translateY"]}")
        }

        val hasCopyMethod = resultClassDef.methods.any { method ->
            method.name == "copy" &&
                method.parameterTypes.size >= 4 &&
                method.parameterTypes[0] == "I" &&
                method.parameterTypes[1] == "I" &&
                method.parameterTypes[2] == "F" &&
                method.parameterTypes[3] == "F"
        }
        if (!hasCopyMethod) {
            throw PatchException("Method copy(I,I,Float,Float,...) not found in $resultClass")
        }
        println("[Video Fit] Validated VideoAdaptionResult structure (width, height, translateX, translateY, copy).")

        // 3. Hook VideoAdaptionResult.saveResultInner(View)V
        val saveInnerFp = Fingerprint(
            definingClass = resultClass,
            name = "saveResultInner",
            parameters = listOf("Landroid/view/View;"),
            returnType = "V",
        )
        val saveInnerMethod = saveInnerFp.method
        // In saveResultInner(View): instance method -> p0 is VideoAdaptionResult, p1 is View
        val saveIsStatic = AccessFlags.STATIC.isSet(saveInnerMethod.accessFlags)
        val saveThisReg = if (saveIsStatic) "p0" else "p0"
        val saveViewReg = if (saveIsStatic) "p1" else "p1"
        saveInnerMethod.addInstructions(
            0,
            """
                invoke-static/range {$saveThisReg .. $saveViewReg}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Ljava/lang/Object;Landroid/view/View;)Ljava/lang/Object;
                move-result-object $saveThisReg
                check-cast $saveThisReg, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit] Hooked VideoAdaptionResult.saveResultInner(View)V.")
        patched++

        // 4. Locate and hook static helper (View, VideoAdaptionResult, Function0)V
        val helper1Fp = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/view/View;", resultClass, "Lkotlin/jvm/functions/Function0;"),
            custom = { method, _ ->
                AccessFlags.STATIC.isSet(method.accessFlags) &&
                    method.implementation?.instructions?.any { ins ->
                        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        ref?.name == "getWidth"
                    } == true &&
                    method.implementation?.instructions?.any { ins ->
                        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        ref?.name == "getHeight"
                    } == true
            },
        )
        val helper1Methods = helper1Fp.matchAll().map { it.method }
        if (helper1Methods.size != 1) {
            throw PatchException("Expected exactly 1 static helper (View,Result,Function0)V, found ${helper1Methods.size}")
        }
        val helper1Method = helper1Methods.first()
        // In static helper1: p0=View, p1=VideoAdaptionResult, p2=Function0
        // Contiguous p0..p1 passes (View, Result) to fitted(View, Object)
        helper1Method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Landroid/view/View;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p1
                check-cast p1, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit] Hooked static helper (View,Result,Function0)V -> prepended fitted() swap.")
        patched++

        // 5. Locate and hook static helper (View, VideoAdaptionResult)Z
        val helper2Fp = Fingerprint(
            returnType = "Z",
            parameters = listOf("Landroid/view/View;", resultClass),
            custom = { method, _ ->
                AccessFlags.STATIC.isSet(method.accessFlags) &&
                    method.implementation?.instructions?.any { ins ->
                        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        ref?.name == "getWidth"
                    } == true &&
                    method.implementation?.instructions?.any { ins ->
                        val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                        ref?.name == "getHeight"
                    } == true
            },
        )
        val helper2Methods = helper2Fp.matchAll().map { it.method }
        if (helper2Methods.size != 1) {
            throw PatchException("Expected exactly 1 static helper (View,Result)Z, found ${helper2Methods.size}")
        }
        val helper2Method = helper2Methods.first()
        // In static helper2: p0=View, p1=VideoAdaptionResult
        helper2Method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Landroid/view/View;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p1
                check-cast p1, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit] Hooked static helper (View,Result)Z -> prepended fitted() swap.")
        patched++

        // 6. Story cell: adapt layout dimension writes with :leave label when negative
        try {
            val storyCellFp = Fingerprint(
                returnType = "V",
                custom = { method, classDef ->
                    classDef.type.contains("story") &&
                        method.implementation?.instructions?.any { ins ->
                            val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference
                            ref?.name in listOf("setLayoutParams", "setTranslationX", "setTranslationY")
                        } == true
                },
            )
            val storyMethod = storyCellFp.method
            storyMethod.ensureRegisterCount(4)
            // Guard register count <= 16 for non-range invoke
            val totalRegs = storyMethod.implementation!!.registerCount
            if (totalRegs <= 16) {
                storyMethod.addInstructionsWithLabels(
                    0,
                    """
                        const/4 v0, 0x0
                        const/4 v1, 0x0
                        const/4 v2, 0x0
                        invoke-static {v0, v1, v2}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitWidthFor(IILandroid/view/View;)I
                        move-result v3
                        if-gez v3, :leave
                        invoke-static {v0, v1, v2}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fittedHeightFor(IILandroid/view/View;)I
                        move-result v3
                        if-gez v3, :leave
                        :leave
                        nop
                    """.trimIndent(),
                )
            } else {
                storyMethod.addInstructionsWithLabels(
                    0,
                    """
                        const/4 v0, 0x0
                        const/4 v1, 0x0
                        const/4 v2, 0x0
                        invoke-static/range {v0 .. v2}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitWidthFor(IILandroid/view/View;)I
                        move-result v3
                        if-gez v3, :leave
                        invoke-static/range {v0 .. v2}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fittedHeightFor(IILandroid/view/View;)I
                        move-result v3
                        if-gez v3, :leave
                        :leave
                        nop
                    """.trimIndent(),
                )
            }
            println("[Video Fit] Hooked story cell dimensions calculation.")
            patched++
        } catch (e: Exception) {
            println("[Video Fit] Story cell note: ${e.message}")
        }

        println("[Video Fit] Applied $patched video fit hook(s) -> fitMode=$mode.")
    }
}
