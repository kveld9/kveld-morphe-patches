package app.morphe.patches.tiktok.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun buildStaticInvokeSmali(
    instruction: Instruction,
    targetMethodSignature: String,
): String {
    if (instruction is RegisterRangeInstruction) {
        val start = instruction.startRegister
        val count = instruction.registerCount
        val end = start + count - 1
        return if (count == 0) {
            "invoke-static {}, $targetMethodSignature"
        } else {
            "invoke-static/range {v$start .. v$end}, $targetMethodSignature"
        }
    }
    if (instruction is FiveRegisterInstruction) {
        val count = instruction.registerCount
        val regs = when (count) {
            0 -> emptyList()
            1 -> listOf(instruction.registerC)
            2 -> listOf(instruction.registerC, instruction.registerD)
            3 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE)
            4 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF)
            5 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF, instruction.registerG)
            else -> emptyList()
        }
        if (regs.isEmpty()) {
            return "invoke-static {}, $targetMethodSignature"
        }
        if (regs.size == 1 && regs[0] > 15) {
            return "invoke-static/range {v${regs[0]} .. v${regs[0]}}, $targetMethodSignature"
        }
        val isContiguous = regs.size > 1 && regs.zipWithNext().all { (a, b) -> b == a + 1 }
        return if (regs.all { it <= 15 }) {
            "invoke-static {${regs.joinToString(", ") { "v$it" }}}, $targetMethodSignature"
        } else if (isContiguous) {
            "invoke-static/range {v${regs.first()} .. v${regs.last()}}, $targetMethodSignature"
        } else {
            "invoke-static {${regs.joinToString(", ") { "v$it" }}}, $targetMethodSignature"
        }
    }
    error("Unsupported instruction format for static invoke rewriting: ${instruction.javaClass.name}")
}

val devicePrivacyGuardPatch = bytecodePatch(
    name = "Device Privacy Guard",
    description = "Neutralizes invasive runtime permissions, contacts queries, package inventory inspection, location hardware tracking, advertising ID profiling, background clipboard snooping routines, and motion sensor profiling to protect user data.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)
    dependsOn(sharedExtensionPatch)

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

        // 1.3 Permission denial cache checker (LX/04CR;->LIZ in v47.1.4, was LX/04CN;)
        Fingerprint(
            definingClass = "LX/04CR;",
            name = "LIZ",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                invoke-static/range {p0 .. p0}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->isPermissionBlocked(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :cond_check
                const/4 v0, 0
                return v0
                :cond_check
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Intercepted LX/04CR.LIZ() -> suppressed denial flag for blocked permissions.")
        patched++

        // 1.4 Suppress permanently denied / open settings prompt redirector (LX/06WZ;->LJI in v47.1.4, was LX/06WV;)
        Fingerprint(
            definingClass = "LX/06WZ;",
            name = "LJI",
            parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Z"),
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, ${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->isPermissionBlocked(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :cond_proceed
                const/4 v0, 0
                return v0
                :cond_proceed
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/06WZ.LJI() -> permanently denied settings redirects suppressed for blocked permissions.")
        patched++

        // ==========================================
        // 2. LOCATION TRACKING & POPUP NEUTRALIZATION
        // ==========================================

        // 2.1 Disable all scene permission apply (LX/0AwX;->LJI -> false in v47.1.4, was LX/0AwT;)
        Fingerprint(
            definingClass = "LX/0AwX;",
            name = "LJI",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwX.LJI() -> location scene permission application disabled.")
        patched++

        // 2.2 Disable pre-instruction location popups (LX/0AwX;->LJII -> false in v47.1.4, was LX/0AwT;)
        Fingerprint(
            definingClass = "LX/0AwX;",
            name = "LJII",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwX.LJII() -> pre-instruction location popup disabled.")
        patched++

        // 2.3 Disable popup scenes (LX/0AwX;->LJIIIIZZ -> false in v47.1.4, was LX/0AwT;)
        Fingerprint(
            definingClass = "LX/0AwX;",
            name = "LJIIIIZZ",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwX.LJIIIIZZ() -> location popup scenes disabled.")
        patched++

        // 2.4 Force location scenes empty (LX/0AwX;->LJIIL -> true in v47.1.4, was LX/0AwT;)
        Fingerprint(
            definingClass = "LX/0AwX;",
            name = "LJIIL",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0AwX.LJIIL() -> location scenes declared empty.")
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

        // 3.4 Neutralize relation onboarding and popup triggers (LX/0v61, LX/0v60, LX/0v5z in v47.1.4, was LX/16rQ, LX/16rP, LX/16rO)
        Fingerprint(
            definingClass = "LX/0v61;",
            name = "LIZJ",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0v61.LIZJ() -> contacts relation auth trigger suppressed.")
        patched++

        Fingerprint(
            definingClass = "LX/0v60;",
            name = "LIZJ",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0v60.LIZJ() -> Facebook relation auth trigger suppressed.")
        patched++

        Fingerprint(
            definingClass = "LX/0v5z;",
            name = "LIZIZ",
            returnType = "Z",
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
        println("[Device Privacy Guard] Neutralized LX/0v5z.LIZIZ() -> enablePermissionPopup forced false.")
        patched++

        // ==========================================
        // 4. ADVERTISING ID (AD_ID) PROFILING BLOCK
        // ==========================================

        Fingerprint(
            definingClass = "LX/02zD;",
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
            definingClass = "LX/02zD;",
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

        // 5.2 Intercept BPEA clipboard reading (LX/1Gmz;->LIZIZ in v47.1.4, was LX/1PwP;)
        Fingerprint(
            definingClass = "LX/1Gmz;",
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
        println("[Device Privacy Guard] Neutralized LX/1Gmz.LIZIZ() (BPEA clipboard read) -> forced null.")
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

        // ==========================================
        // 7. CONTENTRESOLVER CONTACTS QUERY ISOLATION
        // ==========================================

        var querySites = 0

        // 7.1 query(Uri, String[], String, String[], String) -> 5 params
        val query5ParamFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/content/ContentResolver;" &&
                        ref.name == "query" &&
                        ref.returnType == "Landroid/database/Cursor;" &&
                        ref.parameterTypes.map { it.toString() } == listOf(
                            "Landroid/net/Uri;",
                            "[Ljava/lang/String;",
                            "Ljava/lang/String;",
                            "[Ljava/lang/String;",
                            "Ljava/lang/String;",
                        )
                } == true
            },
        )
        query5ParamFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptQuery(Landroid/content/ContentResolver;Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/content/ContentResolver;" &&
                    ref.name == "query" &&
                    ref.returnType == "Landroid/database/Cursor;" &&
                    ref.parameterTypes.map { it.toString() } == listOf(
                        "Landroid/net/Uri;",
                        "[Ljava/lang/String;",
                        "Ljava/lang/String;",
                        "[Ljava/lang/String;",
                        "Ljava/lang/String;",
                    )
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    querySites++
                }
            }
        }

        // 7.2 query(Uri, String[], String, String[], String, CancellationSignal) -> 6 params
        val query6ParamFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/content/ContentResolver;" &&
                        ref.name == "query" &&
                        ref.returnType == "Landroid/database/Cursor;" &&
                        ref.parameterTypes.map { it.toString() } == listOf(
                            "Landroid/net/Uri;",
                            "[Ljava/lang/String;",
                            "Ljava/lang/String;",
                            "[Ljava/lang/String;",
                            "Ljava/lang/String;",
                            "Landroid/os/CancellationSignal;",
                        )
                } == true
            },
        )
        query6ParamFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptQuery(Landroid/content/ContentResolver;Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Landroid/os/CancellationSignal;)Landroid/database/Cursor;"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/content/ContentResolver;" &&
                    ref.name == "query" &&
                    ref.returnType == "Landroid/database/Cursor;" &&
                    ref.parameterTypes.map { it.toString() } == listOf(
                        "Landroid/net/Uri;",
                        "[Ljava/lang/String;",
                        "Ljava/lang/String;",
                        "[Ljava/lang/String;",
                        "Ljava/lang/String;",
                        "Landroid/os/CancellationSignal;",
                    )
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    querySites++
                }
            }
        }

        // 7.3 query(Uri, String[], Bundle, CancellationSignal) -> 4 params (API 26)
        val query4ParamFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/content/ContentResolver;" &&
                        ref.name == "query" &&
                        ref.returnType == "Landroid/database/Cursor;" &&
                        ref.parameterTypes.map { it.toString() } == listOf(
                            "Landroid/net/Uri;",
                            "[Ljava/lang/String;",
                            "Landroid/os/Bundle;",
                            "Landroid/os/CancellationSignal;",
                        )
                } == true
            },
        )
        query4ParamFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptQuery(Landroid/content/ContentResolver;Landroid/net/Uri;[Ljava/lang/String;Landroid/os/Bundle;Landroid/os/CancellationSignal;)Landroid/database/Cursor;"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/content/ContentResolver;" &&
                    ref.name == "query" &&
                    ref.returnType == "Landroid/database/Cursor;" &&
                    ref.parameterTypes.map { it.toString() } == listOf(
                        "Landroid/net/Uri;",
                        "[Ljava/lang/String;",
                        "Landroid/os/Bundle;",
                        "Landroid/os/CancellationSignal;",
                    )
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    querySites++
                }
            }
        }

        if (querySites == 0) {
            throw PatchException("Zero ContentResolver.query call sites found to intercept.")
        }
        println("[Device Privacy Guard] Intercepted $querySites ContentResolver.query call site(s) -> contacts queries suppressed.")
        patched++

        // ==========================================
        // 8. PACKAGEMANAGER INVENTORY READING ISOLATION
        // ==========================================

        var packageSites = 0

        // 8.1 queryIntentActivities(Intent, int)
        val pkgQueryIntentFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/content/pm/PackageManager;" &&
                        ref.name == "queryIntentActivities" &&
                        ref.returnType == "Ljava/util/List;" &&
                        ref.parameterTypes.map { it.toString() } == listOf("Landroid/content/Intent;", "I")
                } == true
            },
        )
        pkgQueryIntentFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptQueryIntentActivities(Landroid/content/pm/PackageManager;Landroid/content/Intent;I)Ljava/util/List;"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/content/pm/PackageManager;" &&
                    ref.name == "queryIntentActivities" &&
                    ref.returnType == "Ljava/util/List;" &&
                    ref.parameterTypes.map { it.toString() } == listOf("Landroid/content/Intent;", "I")
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    packageSites++
                }
            }
        }

        if (packageSites == 0) {
            throw PatchException("Zero PackageManager inventory call sites found to intercept.")
        }
        println("[Device Privacy Guard] Intercepted $packageSites PackageManager inventory call site(s) -> package scanning isolated.")
        patched++

        // ==========================================
        // 9. LOCATIONMANAGER HARDWARE QUERY ISOLATION
        // ==========================================

        var locationSites = 0

        // 9.1 getLastKnownLocation(String) -> Location
        val locGetLastKnownFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/location/LocationManager;" &&
                        ref.name == "getLastKnownLocation" &&
                        ref.returnType == "Landroid/location/Location;" &&
                        ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")
                } == true
            },
        )
        locGetLastKnownFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptGetLastKnownLocation(Landroid/location/LocationManager;Ljava/lang/String;)Landroid/location/Location;"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/location/LocationManager;" &&
                    ref.name == "getLastKnownLocation" &&
                    ref.returnType == "Landroid/location/Location;" &&
                    ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;")
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    locationSites++
                }
            }
        }

        // 9.2 requestSingleUpdate(String, LocationListener, Looper) -> void
        val locSingleUpdateStrFp = Fingerprint(
            custom = { method, _ ->
                method.implementation?.instructions?.any { ins ->
                    val ref = (ins as? ReferenceInstruction)?.reference as? MethodReference ?: return@any false
                    ref.definingClass == "Landroid/location/LocationManager;" &&
                        ref.name == "requestSingleUpdate" &&
                        ref.returnType == "V" &&
                        ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;", "Landroid/location/LocationListener;", "Landroid/os/Looper;")
                } == true
            },
        )
        locSingleUpdateStrFp.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val edits = mutableListOf<Pair<Int, String>>()
            val targetSig = "${Constants.TIKTOK_EXTENSION_PRIVACY_HOOK}->interceptRequestSingleUpdate(Landroid/location/LocationManager;Ljava/lang/String;Landroid/location/LocationListener;Landroid/os/Looper;)V"
            instructions.forEachIndexed { index, instruction ->
                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (ref.definingClass == "Landroid/location/LocationManager;" &&
                    ref.name == "requestSingleUpdate" &&
                    ref.returnType == "V" &&
                    ref.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;", "Landroid/location/LocationListener;", "Landroid/os/Looper;")
                ) {
                    edits.add(index to buildStaticInvokeSmali(instruction, targetSig))
                }
            }
            if (edits.isNotEmpty()) {
                edits.sortByDescending { it.first }
                edits.forEach { (index, replacementSmali) ->
                    method.removeInstructions(index, 1)
                    method.addInstructions(index, replacementSmali)
                    locationSites++
                }
            }
        }

        if (locationSites == 0) {
            throw PatchException("Zero LocationManager call sites found to intercept.")
        }
        println("[Device Privacy Guard] Intercepted $locationSites LocationManager call site(s) -> hardware location queries suppressed.")
        patched++

        println("[Device Privacy Guard] Applied $patched device privacy protection hook(s).")
    }
}
