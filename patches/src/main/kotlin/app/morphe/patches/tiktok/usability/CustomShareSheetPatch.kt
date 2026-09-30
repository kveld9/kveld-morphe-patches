package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.ensureRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val customShareSheetPatch = bytecodePatch(
    name = "Custom Share Sheet",
    description = "Customizes and cleans the native TikTok share sheet via individual toggle switches for third-party apps, essential sharing features, and secondary utility actions.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    extendWith("extensions/extension.mpe")

    // 1. Third-party social apps
    val hideWhatsApp by booleanOption(
        key = "hideWhatsApp",
        default = true,
        title = "Hide WhatsApp",
        description = "Hides WhatsApp and WhatsApp Status from the share sheet.",
        required = false,
    )

    val hideInstagram by booleanOption(
        key = "hideInstagram",
        default = true,
        title = "Hide Instagram",
        description = "Hides Instagram and Instagram Stories from the share sheet.",
        required = false,
    )

    val hideFacebook by booleanOption(
        key = "hideFacebook",
        default = true,
        title = "Hide Facebook & Messenger",
        description = "Hides Facebook, Facebook Stories, and Messenger from the share sheet.",
        required = false,
    )

    val hideTelegram by booleanOption(
        key = "hideTelegram",
        default = true,
        title = "Hide Telegram",
        description = "Hides Telegram from the share sheet.",
        required = false,
    )

    val hideTwitter by booleanOption(
        key = "hideTwitter",
        default = true,
        title = "Hide X / Twitter",
        description = "Hides X (Twitter) from the share sheet.",
        required = false,
    )

    val hideSnapchat by booleanOption(
        key = "hideSnapchat",
        default = true,
        title = "Hide Snapchat",
        description = "Hides Snapchat from the share sheet.",
        required = false,
    )

    val hideReddit by booleanOption(
        key = "hideReddit",
        default = true,
        title = "Hide Reddit & Discord",
        description = "Hides Reddit and Discord from the share sheet.",
        required = false,
    )

    val hideSms by booleanOption(
        key = "hideSms",
        default = true,
        title = "Hide SMS & Messages",
        description = "Hides SMS and Google Messages from the share sheet.",
        required = false,
    )

    val hideSecondaryApps by booleanOption(
        key = "hideSecondaryApps",
        default = true,
        title = "Hide Secondary Networks",
        description = "Hides regional and secondary third-party social apps (Line, Kakao, Viber, VK, Lemon8, etc.).",
        required = false,
    )

    // 2. Native sharing & links
    val hideRepost by booleanOption(
        key = "hideRepost",
        default = false,
        title = "Hide 'Repost' Button",
        description = "Hides the native Repost button from the share sheet. Note: disabling this also disables the long-press to repost gesture.",
        required = false,
    )

    val hideQrCode by booleanOption(
        key = "hideQrCode",
        default = false,
        title = "Hide QR Code",
        description = "Hides the QR code sharing button from the share sheet.",
        required = false,
    )

    val hideCopyLink by booleanOption(
        key = "hideCopyLink",
        default = false,
        title = "Hide 'Copy Link'",
        description = "Hides the Copy link button from the share sheet.",
        required = false,
    )

    val hideSystemShare by booleanOption(
        key = "hideSystemShare",
        default = false,
        title = "Hide System Share ('More')",
        description = "Hides the system share dialog ('More') button from the share sheet.",
        required = false,
    )

    // 3. Contacts / DM row
    val hideFriendsRow by booleanOption(
        key = "hideFriendsRow",
        default = false,
        title = "Hide Friends / Direct Messages Row",
        description = "Hides the top row of direct message friend and contact avatars ('Send to') in the share sheet.",
        required = false,
    )

    // 4. Utility actions
    val hidePromote by booleanOption(
        key = "hidePromote",
        default = true,
        title = "Hide 'Promote' Action",
        description = "Hides the commercial Promote action from the bottom utilities row.",
        required = false,
    )

    val hideWhyThisVideo by booleanOption(
        key = "hideWhyThisVideo",
        default = true,
        title = "Hide 'Why This Video'",
        description = "Hides the recommendation explanation action from the bottom utilities row.",
        required = false,
    )

    val hideCreateGroup by booleanOption(
        key = "hideCreateGroup",
        default = false,
        title = "Hide 'Create Group' Action",
        description = "Hides the Create group action from the bottom utilities row.",
        required = false,
    )

    val hideAddToStory by booleanOption(
        key = "hideAddToStory",
        default = false,
        title = "Hide 'Add to Story'",
        description = "Hides the Add to Story action from the bottom utilities row.",
        required = false,
    )

    val hideCreateSticker by booleanOption(
        key = "hideCreateSticker",
        default = false,
        title = "Hide 'Create Sticker'",
        description = "Hides the sticker creation tool from the bottom utilities row.",
        required = false,
    )

    val hideDuet by booleanOption(
        key = "hideDuet",
        default = false,
        title = "Hide 'Duet' Action",
        description = "Hides the Duet action from the bottom utilities row.",
        required = false,
    )

    val hideStitch by booleanOption(
        key = "hideStitch",
        default = false,
        title = "Hide 'Stitch' Action",
        description = "Hides the Stitch action from the bottom utilities row.",
        required = false,
    )

    val hidePip by booleanOption(
        key = "hidePip",
        default = false,
        title = "Hide 'Picture-in-Picture' (PiP)",
        description = "Hides the Picture-in-Picture floating player action from the bottom utilities row.",
        required = false,
    )

    val hideClearDisplay by booleanOption(
        key = "hideClearDisplay",
        default = false,
        title = "Hide 'Clear Display'",
        description = "Hides the Clear display mode action from the bottom utilities row.",
        required = false,
    )

    val hideListenAudio by booleanOption(
        key = "hideListenAudio",
        default = false,
        title = "Hide 'Background Audio'",
        description = "Hides the background audio playback action from the bottom utilities row.",
        required = false,
    )

    val hideWallpaperAndGif by booleanOption(
        key = "hideWallpaperAndGif",
        default = false,
        title = "Hide Live Wallpaper & GIF",
        description = "Hides Live wallpaper and GIF creation actions from the bottom utilities row.",
        required = false,
    )

    val hideNotInterested by booleanOption(
        key = "hideNotInterested",
        default = false,
        title = "Hide 'Not Interested'",
        description = "Hides the 'Not interested' action from the bottom utilities row.",
        required = false,
    )

    val hideReport by booleanOption(
        key = "hideReport",
        default = false,
        title = "Hide 'Report'",
        description = "Hides the Report action from the bottom utilities row.",
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

        val booleanSettings = listOf(
            "hideWhatsApp" to (hideWhatsApp == true),
            "hideInstagram" to (hideInstagram == true),
            "hideFacebook" to (hideFacebook == true),
            "hideTelegram" to (hideTelegram == true),
            "hideTwitter" to (hideTwitter == true),
            "hideSnapchat" to (hideSnapchat == true),
            "hideReddit" to (hideReddit == true),
            "hideSms" to (hideSms == true),
            "hideSecondaryApps" to (hideSecondaryApps == true),
            "hideFriendsRow" to (hideFriendsRow == true),
            "hideRepost" to (hideRepost == true),
            "hideQrCode" to (hideQrCode == true),
            "hideCopyLink" to (hideCopyLink == true),
            "hideSystemShare" to (hideSystemShare == true),
            "hidePromote" to (hidePromote == true),
            "hideWhyThisVideo" to (hideWhyThisVideo == true),
            "hideCreateGroup" to (hideCreateGroup == true),
            "hideAddToStory" to (hideAddToStory == true),
            "hideCreateSticker" to (hideCreateSticker == true),
            "hideDuet" to (hideDuet == true),
            "hideStitch" to (hideStitch == true),
            "hidePip" to (hidePip == true),
            "hideClearDisplay" to (hideClearDisplay == true),
            "hideListenAudio" to (hideListenAudio == true),
            "hideWallpaperAndGif" to (hideWallpaperAndGif == true),
            "hideNotInterested" to (hideNotInterested == true),
            "hideReport" to (hideReport == true),
        )

        val smaliBuilder = StringBuilder()
        for ((field, value) in booleanSettings) {
            val v = if (value) 1 else 0
            smaliBuilder.append("const v0, $v\n")
            smaliBuilder.append("sput-boolean v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->$field:Z\n")
        }

        smaliBuilder.append("const-string v0, \"$isImOffField\"\n")
        smaliBuilder.append("sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->isImFunctionOffFieldName:Ljava/lang/String;\n")
        smaliBuilder.append("const-string v0, \"$supportImField\"\n")
        smaliBuilder.append("sput-object v0, ${Constants.TIKTOK_EXTENSION_SHARE_HOOK}->supportIMFieldName:Ljava/lang/String;\n")

        hookClinit.addInstructions(insertIdx, smaliBuilder.toString().trimIndent())
        println("[Custom Share Sheet] Configured runtime hook settings via ${booleanSettings.size} individual toggles.")
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
