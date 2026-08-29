package app.morphe.patches.brave

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.string
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

// ════════════════════════════════════════════════════════════════════════════════
// 1. CONFIGURATION & DECLARATIVE POLICY MODEL
// ════════════════════════════════════════════════════════════════════════════════

private const val PREF_PREFIX = "brave_origin_off_"

/**
 * Switch preference keys present in the Brave Origin settings UI.
 */
private val ORIGIN_SWITCH_KEYS = listOf(
    "rewards_switch",
    "privacy_preserving_analytics_switch",
    "email_aliases_switch",
    "leo_ai_switch",
    "news_switch",
    "statistics_reporting_switch",
    "vpn_switch",
    "wallet_switch",
    "web_discovery_project_switch",
)

/**
 * Declarative specification for feature policy gatekeepers.
 * Each entry maps an internal policy string to its corresponding local preference key.
 */
private data class GatekeeperPolicy(
    val name: String,
    val policyPrefString: String,
    val prefKey: String,
)

private val GATEKEEPER_POLICIES = listOf(
    GatekeeperPolicy(
        name = "Rewards",
        policyPrefString = "brave.rewards.disabled_by_policy",
        prefKey = "BraveRewardsDisabled",
    ),
    GatekeeperPolicy(
        name = "News",
        policyPrefString = "brave.news.disabled_by_policy",
        prefKey = "BraveNewsDisabled",
    ),
    GatekeeperPolicy(
        name = "Wallet",
        policyPrefString = "brave.wallet.disabled_by_policy",
        prefKey = "BraveWalletDisabled",
    ),
    GatekeeperPolicy(
        name = "VPN",
        policyPrefString = "brave.brave_vpn.disabled_by_policy",
        prefKey = "BraveVPNDisabled",
    ),
    GatekeeperPolicy(
        name = "Leo AI Policy",
        policyPrefString = "brave.ai_chat.enabled_by_policy",
        prefKey = "BraveAIChatEnabled",
    ),
)

// ════════════════════════════════════════════════════════════════════════════════
// 2. SHARED SMALI HOOK BUILDERS
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Builds the standard smali hook for gatekeeper methods.
 * Reads the local SharedPreferences flag via ActivityThread.currentApplication() Context.
 */
private fun buildGatekeeperHook(prefKey: String): String = """
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "$PREF_PREFIX$prefKey"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v0
    return v0
""".trimIndent()

/**
 * Builds the smali hook for Leo AI feature flag check (0 parameters, 2 registers).
 * Uses contains() check: key exists when disabled, removed when enabled.
 */
private fun buildLeoFeatureFlagHook(): String = """
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "${PREF_PREFIX}BraveAIChatEnabled"
    invoke-interface {v0, v1}, Landroid/content/SharedPreferences;->contains(Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :leo_disabled
    const/4 v0, 0x1
    return v0
    :leo_disabled
    const/4 v0, 0x0
    return v0
""".trimIndent()

// ════════════════════════════════════════════════════════════════════════════════
// 3. RESOURCE PATCH (XML DEFAULTS)
// ════════════════════════════════════════════════════════════════════════════════

private val braveOriginResourcePatch = resourcePatch(
    name = "Brave Origin Resource Defaults",
    description = "Sets Origin preference switches to enabled by default in layout XMLs.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val targetFile = get("res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .firstOrNull { file ->
                file.readText().contains("rewards_switch")
            } ?: return@execute

        var modifiedAttrs = 0
        document(targetFile.absolutePath).use { doc ->
            val elements = doc.getElementsByTagName("*")
            for (i in 0 until elements.length) {
                val node = elements.item(i) as? Element ?: continue
                val key = node.getAttribute("android:key").takeIf { it.isNotEmpty() }
                    ?: node.getAttribute("key")
                if (key in ORIGIN_SWITCH_KEYS) {
                    when {
                        node.hasAttribute("android:defaultValue") -> {
                            node.setAttribute("android:defaultValue", "true")
                            modifiedAttrs++
                        }
                        node.hasAttribute("defaultValue") -> {
                            node.setAttribute("defaultValue", "true")
                            modifiedAttrs++
                        }
                    }
                }
            }
        }

        println("[Brave Origin] Injected $modifiedAttrs Origin preference switches in ${targetFile.name}")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// 4. BYTECODE PATCH (ORIGIN UNLOCK & FEATURE TOGGLES)
// ════════════════════════════════════════════════════════════════════════════════

@Suppress("unused")
val braveOriginPatch = bytecodePatch(
    name = "Brave Origin",
    description = "Unlocks Brave Origin and enables local feature toggle controls.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    dependsOn(braveOriginResourcePatch)

    execute {

        // ── Phase A: Subscription Helper Mocking ───────────────────────────────────────

        // 1. isSubscriptionActive(Profile) -> true
        Fingerprint(
            returnType = "Z",
            parameters = listOf("Lorg/chromium/chrome/browser/profiles/Profile;"),
            strings = listOf(
                "getIsSubscriptionActive profile is null",
                "brave.origin.subscription_active_android",
            ),
        ).method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // 2. hasValidSubscriptionTokens(Profile) -> true
        val validSubscriptionFingerprint = Fingerprint(
            returnType = "Z",
            parameters = listOf("Lorg/chromium/chrome/browser/profiles/Profile;"),
            strings = listOf(
                "brave.origin.order_id_android",
                "brave.origin.purchase_token_android",
            ),
        )
        validSubscriptionFingerprint.method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // 3. isCredentialSummaryCached() -> true
        Fingerprint(
            returnType = "Z",
            parameters = emptyList(),
            strings = listOf("brave_origin_credential_summary_cached"),
        ).method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // 4. syncOriginPackageProduct(String, Profile) -> no-op
        Fingerprint(
            returnType = "V",
            parameters = listOf(
                "Ljava/lang/String;",
                "Lorg/chromium/chrome/browser/profiles/Profile;",
            ),
            strings = listOf(
                "brave.origin.package_name_android",
                "brave.origin.product_id_android",
            ),
        ).method.addInstructions(0, "return-void")

        // 5. showOriginSettingsForRestart() -> no-op
        Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/brave_origin/BraveOriginSettingsLauncherHelper;",
            name = "showOriginSettingsForRestart",
            returnType = "V",
            parameters = emptyList(),
        ).method.addInstructions(0, "return-void")

        // 6. requestCredentialSummary(Profile, Callback) -> fire Boolean.TRUE callback
        Fingerprint(
            returnType = "V",
            parameters = listOf(
                "Lorg/chromium/chrome/browser/profiles/Profile;",
                "Lorg/chromium/base/Callback;",
            ),
            strings = listOf(
                "requestCredentialSummary profile is null",
                "SkusService is null, cannot request credential summary",
            ),
        ).method.addInstructions(
            0,
            """
                sget-object p0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-interface {p1, p0}, Lorg/chromium/base/Callback;->onResult(Ljava/lang/Object;)V
                return-void
            """,
        )

        // ── Phase B: BraveOriginPreferences UI Fragment Controls ───────────────────────

        val validSubClass = validSubscriptionFingerprint.originalClassDef.type
        val validSubMethod = validSubscriptionFingerprint.originalMethod?.name
            ?: error("Failed to resolve Brave Origin subscription predicate")

        // 7. Neutralize subscription validation result register in onCreatePreferences
        val initFragmentFingerprint = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
            filters = listOf(
                methodCall(
                    definingClass = validSubClass,
                    name = validSubMethod,
                    parameters = listOf("Lorg/chromium/chrome/browser/profiles/Profile;"),
                    returnType = "Z",
                ),
            ),
        )
        val validCheckIndex = initFragmentFingerprint.instructionMatches.first().index
        val resultReg = initFragmentFingerprint.method
            .getInstruction<OneRegisterInstruction>(validCheckIndex + 1).registerA
        initFragmentFingerprint.method.addInstructions(validCheckIndex + 2, "const/4 v$resultReg, 0x0")

        // 8. Neutralize show_restart_prompt boolean flag in onCreatePreferences
        val restartPromptFingerprint = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
            filters = listOf(
                string("show_restart_prompt"),
                opcode(Opcode.IPUT_BOOLEAN),
            ),
        )
        val restartPutIndex = restartPromptFingerprint.instructionMatches[1].index
        val restartPutReg = restartPromptFingerprint.method
            .getInstruction<TwoRegisterInstruction>(restartPutIndex).registerA
        restartPromptFingerprint.method.addInstructions(restartPutIndex, "const/4 v$restartPutReg, 0x0")

        // 9. onPreferenceChange(Preference, Object): Route switch state to SharedPreferences
        val onPrefChangeFingerprint = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            returnType = "Z",
            parameters = listOf("Landroidx/preference/Preference;", "Ljava/lang/Object;"),
        )
        onPrefChangeFingerprint.method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructionsWithLabels(
                0,
                """
                    iget-boolean v0, p0, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->O0:Z
                    if-eqz v0, :not_locked
                    const/4 v0, 0x0
                    return v0
                    :not_locked
                    iget-object v0, p1, Landroidx/preference/Preference;->G:Ljava/lang/String;
                    invoke-static {v0}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->e5(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :no_key
                    check-cast p2, Ljava/lang/Boolean;
                    invoke-virtual {p2}, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v1
                    xor-int/lit8 v1, v1, 0x1
                    invoke-virtual {p0}, Lys7;->E4()Landroid/content/Context;
                    move-result-object v2
                    invoke-virtual {v2}, Landroid/content/Context;->getApplicationContext()Landroid/content/Context;
                    move-result-object v2
                    const-string v3, "$PREF_PREFIX"
                    invoke-virtual {v3, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v3
                    invoke-static {v2}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
                    move-result-object v2
                    invoke-interface {v2}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v2
                    if-nez v1, :write_disabled
                    invoke-interface {v2, v3}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v2
                    goto :write_done
                    :write_disabled
                    invoke-interface {v2, v3, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v2
                    :write_done
                    invoke-interface {v2}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
                    invoke-virtual {p0}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->d5()V
                    :no_key
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }

        // 10. setupPreference(String): Bind listener via reflection to avoid cross-DEX issues
        val setupPrefFingerprint = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
            returnType = "V",
            parameters = listOf("Ljava/lang/String;"),
        )
        setupPrefFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p0, p1}, Lbwe;->S4(Ljava/lang/CharSequence;)Landroidx/preference/Preference;
                move-result-object v0
                if-eqz v0, :setup_done
                move-object v3, p0
                const-string v1, "y"
                const-class v2, Landroidx/preference/Preference;
                invoke-virtual {v2, v1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
                move-result-object v1
                const/4 v2, 0x1
                invoke-virtual {v1, v2}, Ljava/lang/reflect/Field;->setAccessible(Z)V
                invoke-virtual {v1, v0, v3}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V
                :setup_done
                return-void
            """,
        )

        // ── Phase C: Feature Policy Gatekeepers ────────────────────────────────────────

        // Apply standard gatekeepers declaratively
        GATEKEEPER_POLICIES.forEach { policy ->
            val gatekeeperFingerprint = Fingerprint(
                strings = listOf(policy.policyPrefString),
            )
            gatekeeperFingerprint.method.apply {
                removeInstructions(0, implementation!!.instructions.count())
                addInstructions(0, buildGatekeeperHook(policy.prefKey))
            }
        }

        // Apply Leo AI feature flag gatekeeper
        val leoFeatureFlagFingerprint = Fingerprint(
            returnType = "Z",
            parameters = emptyList(),
            strings = listOf("AIChat"),
        )
        leoFeatureFlagFingerprint.method.apply {
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(0, buildLeoFeatureFlagHook())
        }

        val totalGatekeepers = GATEKEEPER_POLICIES.size + 1
        println("[Brave Origin] Hooked 10 core methods & configured $totalGatekeepers policy gatekeepers in Brave Origin UI")
    }
}
