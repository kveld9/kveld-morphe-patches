package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes invasive runtime permissions (contacts sync, location tracking, nearby devices), advertising ID profiling, background clipboard snooping routines, and motion sensor profiling to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // ==========================================
        // 1. RUNTIME PERMISSION DISPATCH & DEFENSE
        // ==========================================


        // 1.2 PowerPermissions FakeFragment dispatcher (FakeFragment;->cY/jT)
        val fakeFragmentFp = Fingerprint(
            definingClass = "Lcom/bytedance/ies/powerpermissions/FakeFragment;",
            parameters = listOf("Ljava/util/HashSet;"),
            returnType = "V",
        )
        fakeFragmentFp.method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p1}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptPowerPermissions(Ljava/lang/Object;Ljava/util/Set;)Z
                move-result v0
                if-eqz v0, :cond_proceed
                return-void
                :cond_proceed
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized FakeFragment.${fakeFragmentFp.method.name}() (PowerPermissions request dispatcher).")
        patched++

        // 1.3 Permission denial cache checker (LX/04CN;->LIZ)
        Fingerprint(
            definingClass = "LX/04CN;",
            name = "LIZ",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->isPermissionBlocked(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :cond_check
                const/4 v0, 0x1
                return v0
                :cond_check
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Intercepted LX/04CN.LIZ() -> permanently denied for blocked permissions.")
        patched++

        // ==========================================
        // 2. LOCATION TRACKING & POPUP NEUTRALIZATION
        // ==========================================

        // 2.1 Disable all scene permission apply (LX/0AwT;->LJI -> false)
        Fingerprint(
            definingClass = "LX/0AwT;",
            name = "LJI",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwT.LJI() -> location scene permission application disabled.")
        patched++

        // 2.2 Disable pre-instruction location popups (LX/0AwT;->LJII -> false)
        Fingerprint(
            definingClass = "LX/0AwT;",
            name = "LJII",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwT.LJII() -> pre-instruction location popup disabled.")
        patched++

        // 2.3 Disable popup scenes (LX/0AwT;->LJIIIIZZ -> false)
        Fingerprint(
            definingClass = "LX/0AwT;",
            name = "LJIIIIZZ",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwT.LJIIIIZZ() -> location popup scenes disabled.")
        patched++

        // 2.4 Force location scenes empty (LX/0AwT;->LJIIL -> true)
        Fingerprint(
            definingClass = "LX/0AwT;",
            name = "LJIIL",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwT.LJIIL() -> location scenes declared empty.")
        patched++

        // 2.5 Neutralize LocationServiceImpl precise and coarse optimization flags
        listOf("LJIIZILJ", "LJIJ").forEach { methodName ->
            Fingerprint(
                definingClass = "Lcom/ss/android/ugc/tiktok/location/serviceimpl/LocationServiceImpl;",
                name = methodName,
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized LocationServiceImpl.$methodName() -> false.")
            patched++
        }

        // 2.6 Neutralize location startup Lego tasks
        val locationTasks = listOf(
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTask;",
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTaskHolder\$Background;",
            "Lcom/ss/android/ugc/tiktok/location/task/InitLocationTaskHolder\$Main;",
        )
        locationTasks.forEach { taskClass ->
            Fingerprint(
                definingClass = taskClass,
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized $taskClass.run(Context).")
            patched++
        }

        // ==========================================
        // 3. CONTACTS SYNC & RELATION PROMPTS NEUTRALIZATION
        // ==========================================

        // 3.1 Neutralize RelationAuthDialogControl.LJIIIIZZ (in-app Contacts sync popup dialog)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/relation/auth/pipeline/common/RelationAuthDialogControl;",
            name = "LJIIIIZZ",
        ).method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized RelationAuthDialogControl.LJIIIIZZ() -> suppressed Contacts sync dialog.")
        patched++

        // 3.2 Neutralize RelationAuthDialogControl.LJI (in-app Facebook relation auth dialog)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/relation/auth/pipeline/common/RelationAuthDialogControl;",
            name = "LJI",
        ).method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized RelationAuthDialogControl.LJI() -> suppressed Facebook sync dialog.")
        patched++

        // 3.3 Neutralize contacts upload and background sync Lego tasks
        val contactTasks = listOf(
            "Lcom/ss/android/ugc/aweme/friends/lego/ContactsUploadRequest;",
            "Lcom/ss/android/ugc/aweme/relation/auth/lego/PermissionRequestAndUploadLegoTask;",
            "Lcom/ss/android/ugc/aweme/im/contacts/impl/bytesync/IMContactInitTask;",
            "Lcom/ss/android/ugc/aweme/friends/lego/MafFollowBackBootRequest;",
        )
        contactTasks.forEach { taskClass ->
            Fingerprint(
                definingClass = taskClass,
                name = "run",
                parameters = listOf("Landroid/content/Context;"),
                returnType = "V",
            ).method.addInstructions(
                0,
                """
                    return-void
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized $taskClass.run(Context).")
            patched++

            Fingerprint(
                definingClass = taskClass,
                name = "meetTrigger",
                returnType = "Z",
            ).method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
            println("[Device Privacy Guard] Neutralized $taskClass.meetTrigger() -> false.")
            patched++
        }

        // ==========================================
        // 4. ADVERTISING ID (AD_ID) PROFILING BLOCK
        // ==========================================

        Fingerprint(
            definingClass = "LX/02z9;",
            name = "LLLLIILL",
            returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient${'$'}Info;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized AdvertisingIdClient.getInfo() -> null.")
        patched++

        Fingerprint(
            definingClass = "LX/02z9;",
            name = "LLLLIIL",
            returnType = "Ljava/lang/String;",
        ).method.addInstructions(
            0,
            """
                const-string v0, "00000000-0000-0000-0000-000000000000"
                return-object v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized AdvertisingIdClient.getId() -> zeroed UUID.")
        patched++

        // ==========================================
        // 5. CLIPBOARD PRIVACY PROTECTION
        // ==========================================

        // 5.1 Hook IMMessageListClipboardServiceImpl (messenger clipboard integration)
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/im/messagelist/impl/IMMessageListClipboardServiceImpl;",
            name = "LIZ",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized IMMessageListClipboardServiceImpl.LIZ().")
        patched++

        // 5.2 Intercept BPEA clipboard reading (LX/1PwP;->LIZIZ)
        Fingerprint(
            definingClass = "LX/1PwP;",
            name = "LIZIZ",
            parameters = listOf("Landroid/content/ClipboardManager;", "Lcom/bytedance/bpea/basics/Cert;"),
            returnType = "Landroid/content/ClipData;",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/1PwP.LIZIZ() (BPEA clipboard read) -> forced null.")
        patched++

        // ==========================================
        // 6. SENSOR HAR (HUMAN ACTIVITY RECOGNITION) ISOLATION
        // ==========================================

        // 6.1 Intercept SmartHARServiceImpl.enable() -> false
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/ml/impl/har/SmartHARServiceImpl;",
            name = "enable",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized SmartHARServiceImpl.enable() -> forced false.")
        patched++

        // 6.2 Intercept SmartHARServiceImpl.checkAndInit() -> return-void
        Fingerprint(
            definingClass = "Lcom/ss/android/ugc/aweme/ml/impl/har/SmartHARServiceImpl;",
            name = "checkAndInit",
            returnType = "V",
        ).method.addInstructions(
            0,
            """
                return-void
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized SmartHARServiceImpl.checkAndInit() -> return-void.")
        patched++

        println("[Device Privacy Guard] Applied $patched device privacy protection hook(s).")
    }
}
