package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val xiaomiEarbudsDisablePromosAndNagsPatch = bytecodePatch(
    name = "Xiaomi Earbuds Disable Promos & Nags",
    description = "Bypasses startup privacy agreements, onboarding guides, region selector prompts, and Bluetooth permission nags, and disables in-app promotional banners, marketing activities, and store review nag dialogs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. DeviceSettingsFragment.loadBanner()V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "loadBanner",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("DeviceSettingsFragment.loadBanner")
        }

        // 2. DeviceSettingsFragment.loadActivities()V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "loadActivities",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("DeviceSettingsFragment.loadActivities")
        }

        // 3. InviteReviewManager.isShowDialog()Z
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/cloudconfig/InviteReviewManager;",
            name = "isShowDialog",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("InviteReviewManager.isShowDialog")
        }

        // 4. RegionPreference.getPRIVACY_AGREE()Z -> true
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/account/region/RegionPreference;",
            name = "getPRIVACY_AGREE",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("RegionPreference.getPRIVACY_AGREE")
        }

        // 5. RegionPreference.getPREF_PRIVACY_EXPERIENCE_TAG()Z -> false (disable User Experience Program telemetry)
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/account/region/RegionPreference;",
            name = "getPREF_PRIVACY_EXPERIENCE_TAG",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("RegionPreference.getPREF_PRIVACY_EXPERIENCE_TAG")
        }

        // 6. PrivacyPreference.getPrivacyAgreeTime()J -> valid non-zero timestamp
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/preference/PrivacyPreference;",
            name = "getPrivacyAgreeTime",
            parameters = listOf(),
            returnType = "J",
        ).method.apply {
            addInstructions(
                0,
                """
                    const-wide v0, 0x18bdc6d2000L
                    return-wide v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyPreference.getPrivacyAgreeTime")
        }

        // 7. PrivacyPreference.getPrivacyDialogFinished()Z -> true
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/preference/PrivacyPreference;",
            name = "getPrivacyDialogFinished",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyPreference.getPrivacyDialogFinished")
        }

        // 8. ModePreference.getWelcomeFinish()Z -> true (bypass GuideActivity tutorial carousel)
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/preference/ModePreference;",
            name = "getWelcomeFinish",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 1
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("ModePreference.getWelcomeFinish")
        }

        // 9. RegionSelectPreference.getCurrentRegion()Ljava/lang/String; -> defaults to "us" if empty
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/preference/RegionSelectPreference;",
            name = "getCurrentRegion",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-virtual {p0}, Lcom/xiaomi/fitness/login/preference/RegionSelectPreference;->getKEY_CURRENT_REGION()Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :cond_default
                    invoke-virtual {v0}, Ljava/lang/String;->length()I
                    move-result v1
                    if-eqz v1, :cond_default
                    sget-object v1, Ljava/util/Locale;->US:Ljava/util/Locale;
                    invoke-virtual {v0, v1}, Ljava/lang/String;->toLowerCase(Ljava/util/Locale;)Ljava/lang/String;
                    move-result-object v0
                    return-object v0
                    :cond_default
                    const-string v0, "us"
                    return-object v0
                """.trimIndent(),
            )
            hookedMethods.add("RegionSelectPreference.getCurrentRegion")
        }

        // 10. PrivacyActivity.onCreate(Bundle)V -> auto-agree and finish immediately if ever entered
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/privacy/PrivacyActivity;",
            name = "onCreate",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/xiaomi/fitness/login/privacy/Hilt_PrivacyActivity;->onCreate(Landroid/os/Bundle;)V
                    invoke-direct {p0}, Lcom/xiaomi/fitness/login/privacy/PrivacyActivity;->handlePrivacyAgree()V
                    return-void
                """.trimIndent(),
            )
            hookedMethods.add("PrivacyActivity.onCreate")
        }

        // 11. CheckerPrivacyVersion.checkPrivacyChange(FragmentActivity)V -> return-void
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/privacy/CheckerPrivacyVersion;",
            name = "checkPrivacyChange",
            parameters = listOf("Landroidx/fragment/app/FragmentActivity;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CheckerPrivacyVersion.checkPrivacyChange")
        }

        // 12. CheckerPrivacyVersion.reportAgreeTime()V -> return-void
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/privacy/CheckerPrivacyVersion;",
            name = "reportAgreeTime",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CheckerPrivacyVersion.reportAgreeTime")
        }

        // 13. CheckerPrivacyVersion.reportRemovePrivacyAgreeTime()V -> return-void
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/privacy/CheckerPrivacyVersion;",
            name = "reportRemovePrivacyAgreeTime",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CheckerPrivacyVersion.reportRemovePrivacyAgreeTime")
        }

        // 14. CheckerPrivacyVersion.showPrivacyChangeDialog(...)V -> return-void
        Fingerprint(
            definingClass = "Lcom/xiaomi/fitness/login/privacy/CheckerPrivacyVersion;",
            name = "showPrivacyChangeDialog",
            parameters = listOf(
                "Landroidx/fragment/app/FragmentActivity;",
                "I",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
            ),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("CheckerPrivacyVersion.showPrivacyChangeDialog")
        }

        // 15. DeviceSettingsFragment.requestBluetoothPermissionForS()V -> bypass custom dialog and request system permission directly
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;",
            name = "requestBluetoothPermissionForS",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, Lcom/mi/earphone/bluetoothsdk/util/CheckPermissionUtilKt;->isBluetoothPermissionGrantedForS()Z
                    move-result v0
                    if-nez v0, :cond_skip
                    invoke-static {p0}, Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;->access${'$'}getMPermissionRequestLauncher${'$'}p(Lcom/mi/earphone/settings/ui/DeviceSettingsFragment;)Landroidx/activity/result/ActivityResultLauncher;
                    move-result-object v0
                    if-eqz v0, :cond_skip
                    invoke-static {}, Lcom/mi/earphone/bluetoothsdk/util/CheckPermissionUtilKt;->getBluetoothPermissionsForAndroidS()[Ljava/lang/String;
                    move-result-object v1
                    invoke-virtual {v0, v1}, Landroidx/activity/result/ActivityResultLauncher;->launch(Ljava/lang/Object;)V
                    :cond_skip
                    return-void
                """.trimIndent(),
            )
            hookedMethods.add("DeviceSettingsFragment.requestBluetoothPermissionForS")
        }

        // 16. DeviceListFragment.requestBluetoothPermissionForS()V -> return-void (suppress Bluetooth permission nag dialog)
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/ui/devicelist/DeviceListFragment;",
            name = "requestBluetoothPermissionForS",
            parameters = listOf(),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("DeviceListFragment.requestBluetoothPermissionForS")
        }

        println("[Xiaomi Earbuds Disable Promos & Nags] Disabled startup privacy agreements, onboarding guides, region selector prompts, promotional banners, Bluetooth permission nags, and review nags across ${hookedMethods.size} targets.")
    }
}

