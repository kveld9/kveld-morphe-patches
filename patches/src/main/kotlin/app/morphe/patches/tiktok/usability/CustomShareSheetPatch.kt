package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val customShareSheetPatch = bytecodePatch(
    name = "Custom Share Sheet",
    description = "Customizes and simplifies the native TikTok share sheet by removing third-party app bloat, hiding specified apps or actions, and optionally suppressing the friends/contacts direct message row.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    val simplifyShareSheet by booleanOption(
        key = "simplifyShareSheet",
        default = true,
        title = "Simplify Share Sheet",
        description = "Trims third-party app channels down to essentials (Copy link and System share / More).",
        required = false,
    )

    val hideFriendsRow by booleanOption(
        key = "hideFriendsRow",
        default = false,
        title = "Hide Friends Suggestion Row",
        description = "Hides the top row of direct message friend/contact avatars ('Send to') in the share sheet.",
        required = false,
    )

    val hiddenApps by stringOption(
        key = "hiddenApps",
        default = "",
        title = "Hide Specific Apps",
        description = "Comma-separated list of app channel keys to hide (e.g. facebook,instagram,snapchat,messenger,twitter). Active when Simplify Share Sheet is disabled.",
        required = false,
    )

    val hiddenActions by stringOption(
        key = "hiddenActions",
        default = "",
        title = "Hide Specific Actions",
        description = "Comma-separated list of share sheet action keys to hide (e.g. promote,qr_code,pip_switch,why_this_video).",
        required = false,
    )

    execute {
        var patched = 0

        // 1. Discover panel class and constructor
        val panelInitFp = Fingerprint(
            name = "<init>",
            returnType = "V",
            strings = listOf("config_duration", "click_to_respond_duration"),
        )
        val panelInitMethod = panelInitFp.method
        val panelClass = panelInitMethod.definingClass

        // 2. Discover IM toggle field names on panelClass from ShareDependServiceDMTImpl
        var isImOffField = "LJJIJIL"
        var supportImField = "LJIJJLI"
        try {
            val imMethodFp = Fingerprint(
                strings = listOf("isImFunctionOff = ", ", supportIM = "),
            )
            val foundFields = mutableListOf<String>()
            for (ins in imMethodFp.method.implementation!!.instructions) {
                if (ins is ReferenceInstruction) {
                    val ref = ins.reference as? FieldReference
                    if (ref != null && ref.definingClass == panelClass && ref.type == "Z") {
                        if (!foundFields.contains(ref.name)) {
                            foundFields.add(ref.name)
                        }
                    }
                }
            }
            if (foundFields.size >= 2) {
                isImOffField = foundFields[0]
                supportImField = foundFields[1]
            }
        } catch (e: Exception) {
            println("[Custom Share Sheet] Note extracting IM fields: ${e.message}")
        }

        // 3. Configure TikTokShareHook.<clinit>
        val hookClinitFp = Fingerprint(
            definingClass = Constants.TIKTOK_EXTENSION_SHARE_HOOK,
            name = "<clinit>",
        )
        val hookClinit = hookClinitFp.method
        hookClinit.ensureRegisterCount(2)
        val clinitInstructions = hookClinit.implementation!!.instructions
        val returnIdx = clinitInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        val insertIdx = if (returnIdx != -1) returnIdx else 0

        val simplifyVal = if (simplifyShareSheet != false) 1 else 0
        val hideFriendsVal = if (hideFriendsRow == true) 1 else 0
        val appsVal = hiddenApps?.trim() ?: ""
        val actionsVal = hiddenActions?.trim() ?: ""

        hookClinit.addInstructions(
            insertIdx,
            """
                const v0, $simplifyVal
                sput-boolean v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->simplifyShareSheet:Z
                const v0, $hideFriendsVal
                sput-boolean v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->hideFriendsRow:Z
                const-string v0, "$appsVal"
                sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->hiddenApps:Ljava/lang/String;
                const-string v0, "$actionsVal"
                sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->hiddenActions:Ljava/lang/String;
                const-string v0, "$isImOffField"
                sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->isImFunctionOffFieldName:Ljava/lang/String;
                const-string v0, "$supportImField"
                sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->supportIMFieldName:Ljava/lang/String;
            """.trimIndent(),
        )
        println("[Custom Share Sheet] Configured runtime hook settings (simplify=$simplifyShareSheet, hideFriends=$hideFriendsRow).")
        patched++

        // 4. Inject hook call into panel constructor right before RETURN_VOID
        val panelInstructions = panelInitMethod.implementation!!.instructions
        val panelReturnIdx = panelInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        if (panelReturnIdx != -1) {
            panelInitMethod.addInstructions(
                panelReturnIdx,
                """
                    invoke-static {p0}, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->filterSharePanel(Ljava/lang/Object;)V
                """.trimIndent(),
            )
            println("[Custom Share Sheet] Hooked $panelClass.<init> -> filterSharePanel.")
            patched++
        }

        println("[Custom Share Sheet] Applied $patched hooks -> share sheet customized.")
    }
}
