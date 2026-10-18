package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val videoFitPatch = bytecodePatch(
    name = "Video Fit Mode",
    description = "Lets you choose between fitting the whole video or filling the screen.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

    val fitMode by stringOption(
        key = "fitMode",
        title = "Mode",
        description = "Choose 'fit' to see the whole video or 'fill' to crop and fill the screen.",
        default = "fit",
        values = mapOf("Fit video" to "fit", "Fill screen" to "fill"),
        required = false,
    )

    execute {
        val rawMode = fitMode?.trim()?.lowercase() ?: "fit"
        val mode = if (rawMode == "fill") "fill" else "fit"

        var patched = 0

        // 1. Configure runtime fitMode on TikTokVideoFitHook
        val clinitFp = Fingerprint(
            definingClass = Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK,
            name = "<clinit>",
        )
        val clinit = clinitFp.method
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
        println("[Video Fit Mode] Configured runtime fitMode: $mode.")
        patched++

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
        if (fieldMap["translateX"] != "Ljava/lang/Float;") {
            throw PatchException("Expected field translateX:Ljava/lang/Float; in $resultClass, found: ${fieldMap["translateX"]}")
        }
        if (fieldMap["translateY"] != "Ljava/lang/Float;") {
            throw PatchException("Expected field translateY:Ljava/lang/Float; in $resultClass, found: ${fieldMap["translateY"]}")
        }

        val hasCopyMethod = resultClassDef.methods.any { method ->
            method.name == "copy" &&
                method.parameterTypes.size >= 4 &&
                method.parameterTypes[0] == "I" &&
                method.parameterTypes[1] == "I" &&
                method.parameterTypes[2] == "Ljava/lang/Float;" &&
                method.parameterTypes[3] == "Ljava/lang/Float;"
        }
        if (!hasCopyMethod) {
            throw PatchException("Method copy(I,I,Ljava/lang/Float;,Ljava/lang/Float;,...) not found in $resultClass")
        }
        println("[Video Fit Mode] Validated VideoAdaptionResult structure (width, height, translateX, translateY, copy).")

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
        val saveThisReg = if (saveIsStatic) "p1" else "p0"
        val saveViewReg = if (saveIsStatic) "p0" else "p1"
        saveInnerMethod.addInstructions(
            0,
            """
                invoke-static/range {$saveThisReg .. $saveViewReg}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Ljava/lang/Object;Landroid/view/View;)Ljava/lang/Object;
                move-result-object $saveThisReg
                check-cast $saveThisReg, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit Mode] Hooked VideoAdaptionResult.saveResultInner(View)V.")
        patched++

        // 4 & 5. Locate both static helpers in a SINGLE scan.
        // matchAll() walks every class in the APK (TikTok: ~475k classes, ~2M methods), so the two
        // near-identical predicates are merged into one union scan and only partitioned afterwards.
        // The union predicate is the exact disjunction of the two previous ones, so the resulting
        // candidate set (and therefore the emitted DEX) is unchanged.
        val helper1Params = listOf("Landroid/view/View;", resultClass, "Lkotlin/jvm/functions/Function0;")
        val helper2Params = listOf("Landroid/view/View;", resultClass)
        val helperScanFp = Fingerprint(
            custom = { method, _ ->
                AccessFlags.STATIC.isSet(method.accessFlags) &&
                    ((method.returnType == "V" && method.parameterTypes == helper1Params) ||
                        (method.returnType == "Z" && method.parameterTypes == helper2Params)) &&
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
        val helperCandidates = helperScanFp.matchAll().map { it.method }
        val helper1Methods = helperCandidates.filter { it.returnType == "V" && it.parameterTypes == helper1Params }
        val helper2Methods = helperCandidates.filter { it.returnType == "Z" && it.parameterTypes == helper2Params }

        // 4. Hook static helper (View, VideoAdaptionResult, Function0)V
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
        println("[Video Fit Mode] Hooked static helper (View,Result,Function0)V -> prepended fitted() swap.")
        patched++

        // 5. Hook static helper (View, VideoAdaptionResult)Z
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
        println("[Video Fit Mode] Hooked static helper (View,Result)Z -> prepended fitted() swap.")
        patched++

        // 6. Hook CellAdaptionComponentV2.Kr(VideoAdaptionResult)V
        val cellAdaptionFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/feed/platform/cell/component/adaption/CellAdaptionComponentV2;",
            name = "Kr",
            parameters = listOf(resultClass),
            returnType = "V",
        )
        val cellAdaptionMethod = cellAdaptionFp.method
        cellAdaptionMethod.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p1
                check-cast p1, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit Mode] Hooked CellAdaptionComponentV2.Kr(Result)V -> prepended fitted() swap.")
        patched++

        // 7. Hook FeedCellAdaptionComponentV2.Kr(VideoAdaptionResult)V
        val feedCellAdaptionFp = Fingerprint(
            definingClass = "Lcom/ss/android/ugc/feed/platform/cell/component/adaption/FeedCellAdaptionComponentV2;",
            name = "Kr",
            parameters = listOf(resultClass),
            returnType = "V",
        )
        val feedCellAdaptionMethod = feedCellAdaptionFp.method
        feedCellAdaptionMethod.addInstructions(
            0,
            """
                invoke-static {p1}, ${Constants.TIKTOK_EXTENSION_VIDEO_FIT_HOOK}->fitted(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object p1
                check-cast p1, $resultClass
            """.trimIndent(),
        )
        println("[Video Fit Mode] Hooked FeedCellAdaptionComponentV2.Kr(Result)V -> prepended fitted() swap.")
        patched++

        if (patched == 0) {
            println("[Video Fit Mode] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Video Fit Mode] Applied $patched video fit hook(s) -> fitMode=$mode.")
        }
    }
}
