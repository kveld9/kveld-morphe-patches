package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import java.io.File
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val DEVELOPER_SETTINGS_FRAGMENT = "com.google.android.apps.inputmethod.latin.preference.DeveloperSettingsFragment"

private val gboardSeekBarEnhancementsPatch = bytecodePatch(
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)
    extendWith("extensions/extension.mpe")

    execute {
        var patched = 0

        // 1. Hook SeekBarPreference.a(PreferenceViewHolder) to initialize min, showValue and continuous updates
        val fpBind = Fingerprint(
            definingClass = "Landroidx/preference/SeekBarPreference;",
            name = "a",
            returnType = "V",
        )
        fpBind.method.addInstructions(
            0,
            """
                invoke-static {p0}, ${Constants.GBOARD_EXTENSION_CLASS}->onBindSeekBar(Ljava/lang/Object;)V
            """.trimIndent(),
        )
        patched++

        // 2. Hook SeekBarPreference.l(int) to format value with units dynamically during slider drag
        val fpFormat = Fingerprint(
            definingClass = "Landroidx/preference/SeekBarPreference;",
            name = "l",
            parameters = listOf("I"),
            returnType = "V",
        )
        fpFormat.method.apply {
            clearTryBlocks()
            removeInstructions(0, implementation!!.instructions.count())
            addInstructions(
                0,
                """
                    invoke-static {p0, p1}, ${Constants.GBOARD_EXTENSION_CLASS}->updateSeekBarLabel(Ljava/lang/Object;I)V
                    return-void
                """.trimIndent(),
            )
        }
        patched++

        // 3. Hook Preference.a(PreferenceViewHolder) to customize action cards and restart status
        val fpPrefBind = Fingerprint(
            definingClass = "Landroidx/preference/Preference;",
            name = "a",
            parameters = listOf("Lbjo;"),
            returnType = "V",
        )
        fpPrefBind.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, ${Constants.GBOARD_EXTENSION_CLASS}->onBindPreference(Ljava/lang/Object;Ljava/lang/Object;)V
            """.trimIndent(),
        )
        patched++

        println("[Preference Enhancements] Injected $patched live value indicator and status card hook(s) into Preference classes.")
    }
}

val gboardSettingsMenuPatch = resourcePatch(
    name = "Gboard Enhancements",
    description = "Master customization suite bundling in-app toggleable features (AMOLED Pure Black theme, zero bottom inset, independent keyboard vibration, force incognito, voice typing in incognito, clipboard retention, top toolbar icons count, cursor trackpad, and smart flags) managed directly from a top-level Morphe Patches category in Gboard Settings.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    dependsOn(
        gboardAmoledPatch,
        gboardZeroBottomInsetPatch,
        gboardForceIncognitoPatch,
        gboardVoiceTypingIncognitoPatch,
        gboardClipboardEnhancementsPatch,
        gboardFeatureFlagsPatch,
        gboardTopToolbarItemCountPatch,
        gboardSeekBarEnhancementsPatch,
        gboardCoreIntegrityPatch,
        gboardDecoupleHapticsPatch,
        gboardAaptWorkaroundPatch,
    )

    execute {
        val resDir = get("res")
        if (!resDir.exists() || !resDir.isDirectory) {
            println("[Gboard Enhancements] Skipped: res directory not found.")
            return@execute
        }

        val targetScreens = findTargetPreferenceScreens(resDir)
        var injectedHeaders = 0
        var strippedHeaders = 0

        for (file in targetScreens) {
            document(file.absolutePath).use { doc ->
                if (injectMorpheHeader(doc)) {
                    injectedHeaders++
                }
                strippedHeaders += stripUnwantedSettingsHeaders(doc)
            }
        }

        var populatedScreen = false
        val devScreen = findDeveloperSettingsScreen(resDir)
        if (devScreen != null) {
            document(devScreen.absolutePath).use { doc ->
                populatedScreen = populateMorpheSettingsScreen(doc)
            }
        }

        var strippedEmojiSliders = 0
        val emojiSeq = "EmojiScaleSliderPreference".toByteArray()
        resDir.walkTopDown().filter { it.isFile && it.extension == "xml" && it != devScreen }.forEach { file ->
            val content = file.readBytes()
            if (content.containsSequence(emojiSeq)) {
                document(file.absolutePath).use { doc ->
                    val root = doc.documentElement ?: return@use
                    val nodes = root.getElementsByTagName("com.google.android.libraries.inputmethod.preferencewidgets.EmojiScaleSliderPreference")
                    if (nodes.length > 0) {
                        for (i in (nodes.length - 1) downTo 0) {
                            val node = nodes.item(i)
                            node.parentNode?.removeChild(node)
                            strippedEmojiSliders++
                        }
                    }
                }
            }
        }
        if (strippedEmojiSliders > 0) {
            println("[Gboard Enhancements] Stripped $strippedEmojiSliders native EmojiScaleSliderPreference element(s) from other preference screens.")
        }
        if (strippedHeaders > 0) {
            println("[Gboard Enhancements] Pruned $strippedHeaders obsolete telemetry/support header(s) from root settings.")
        }

        println("[Gboard Enhancements] Injected top-level Morphe Patches header into $injectedHeaders root screen(s), populated dedicated settings screen: $populatedScreen.")
    }
}

private fun findTargetPreferenceScreens(resDir: File): List<File> {
    val results = mutableListOf<File>()
    val headerSeq = "HeaderPreference".toByteArray()
    val langSeq = "LanguageSettingFragment".toByteArray()

    resDir.walkTopDown().filter { it.isFile && it.extension == "xml" }.forEach { file ->
        val raw = file.readBytes()
        if (raw.containsSequence(headerSeq) && raw.containsSequence(langSeq)) {
            results.add(file)
        }
    }
    return results
}

private fun findDeveloperSettingsScreen(resDir: File): File? {
    val flagEditorSeq = "FlagEditorFragment".toByteArray()
    return resDir.walkTopDown().filter { it.isFile && it.extension == "xml" }.firstOrNull { file ->
        file.readBytes().containsSequence(flagEditorSeq)
    }
}

private fun injectMorpheHeader(doc: Document): Boolean {
    val root = doc.documentElement ?: return false
    if (root.tagName != "PreferenceScreen") return false

    if (hasExistingHeader(root)) return false

    val header = doc.createElement("com.google.android.libraries.inputmethod.settings.widget.HeaderPreference")
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Morphe Patches")
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:summary", "Customization and patch toggles")
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_HEADER)
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:icon", "@android:drawable/ic_menu_preferences")
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:persistent", "false")
    header.setAttributeNS(ANDROID_XML_NAMESPACE, "android:fragment", DEVELOPER_SETTINGS_FRAGMENT)

    val firstElement = (0 until root.childNodes.length)
        .mapNotNull { root.childNodes.item(it) as? Element }
        .firstOrNull()

    if (firstElement != null && firstElement.tagName == "androidx.preference.PreferenceCategory") {
        val firstInner = (0 until firstElement.childNodes.length)
            .mapNotNull { firstElement.childNodes.item(it) as? Element }
            .firstOrNull()
        if (firstInner != null) {
            firstElement.insertBefore(header, firstInner)
        } else {
            firstElement.appendChild(header)
        }
    } else if (firstElement != null) {
        root.insertBefore(header, firstElement)
    } else {
        root.appendChild(header)
    }
    return true
}

private fun hasExistingHeader(root: Element): Boolean {
    val headers = root.getElementsByTagName("com.google.android.libraries.inputmethod.settings.widget.HeaderPreference")
    for (i in 0 until headers.length) {
        val h = headers.item(i) as? Element ?: continue
        val key = h.getAttributeNS(ANDROID_XML_NAMESPACE, "key").ifEmpty {
            h.getAttribute("android:key")
        }
        if (key == Constants.GboardPrefs.KEY_HEADER) return true
    }
    return false
}

private fun stripUnwantedSettingsHeaders(doc: Document): Int {
    val root = doc.documentElement ?: return 0
    if (root.tagName != "PreferenceScreen") return 0
    var stripped = 0

    val nodesToRemove = mutableListOf<Element>()
    val allElements = root.getElementsByTagName("*")

    for (i in 0 until allElements.length) {
        val elem = allElements.item(i) as? Element ?: continue
        val tagName = elem.tagName
        val frag = elem.getAttributeNS(ANDROID_XML_NAMESPACE, "fragment").ifEmpty {
            elem.getAttribute("android:fragment")
        }
        val key = elem.getAttributeNS(ANDROID_XML_NAMESPACE, "key").ifEmpty {
            elem.getAttribute("android:key")
        }

        val isPrivacy = frag.endsWith("PrivacySettingsFragment") || key.contains("0x7f1409a4") || key.contains("setting_privacy")
        val isAbout = frag.endsWith("AboutSettingsFragment") || key.contains("0x7f140994") || key.contains("setting_about")
        val isRateUs = tagName.endsWith("RateUsPreference") || key.contains("0x7f1409a5") || key.contains("rate_us")
        val isFooter = tagName.endsWith("FooterPreference") || key.contains("0x7f1409ac") || key.contains("work_profile_footer")
        val isHelpOrShare = key.contains("0x7f14099d") || key.contains("0x7f1409a6") ||
            key.contains("help_and_feedback") || key.contains("sharing")

        if (isPrivacy || isAbout || isRateUs || isFooter || isHelpOrShare) {
            nodesToRemove.add(elem)
        }
    }

    for (elem in nodesToRemove) {
        elem.parentNode?.removeChild(elem)
        stripped++
    }

    val categories = root.getElementsByTagName("androidx.preference.PreferenceCategory")
    for (i in (categories.length - 1) downTo 0) {
        val cat = categories.item(i) as? Element ?: continue
        val hasElements = (0 until cat.childNodes.length).any { idx ->
            cat.childNodes.item(idx) is Element
        }
        if (!hasElements) {
            cat.parentNode?.removeChild(cat)
        }
    }

    return stripped
}

private fun populateMorpheSettingsScreen(doc: Document): Boolean {
    val root = doc.documentElement ?: return false
    if (root.tagName != "PreferenceScreen") return false

    while (root.hasChildNodes()) {
        root.removeChild(root.firstChild)
    }

    root.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Morphe Patches")
    root.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_SCREEN)

    // 0. Quick Actions & Status
    val actionsCategory = doc.createElement("androidx.preference.PreferenceCategory")
    actionsCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_ACTIONS)
    actionsCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Actions & Status")
    actionsCategory.appendChild(
        createActionPreference(
            doc = doc,
            key = Constants.GboardPrefs.KEY_ENABLE_IME,
            title = "Enable Gboard in System Settings",
            summary = "Gboard is disabled in Android. Tap to enable it in Manage Keyboards.",
            icon = "@android:drawable/ic_dialog_alert",
        )
    )
    actionsCategory.appendChild(
        createActionPreference(
            doc = doc,
            key = Constants.GboardPrefs.KEY_SELECT_IME,
            title = "Select Gboard Input Method",
            summary = "Gboard is enabled but not active. Tap to choose Gboard as your keyboard.",
            icon = "@android:drawable/ic_input_add",
        )
    )
    actionsCategory.appendChild(
        createActionPreference(
            doc = doc,
            key = Constants.GboardPrefs.KEY_RESTART_GBOARD,
            title = "Restart Gboard Process",
            summary = "Tap to apply changes (required for most options to take effect)",
            icon = "@android:drawable/ic_menu_rotate",
        )
    )
    root.appendChild(actionsCategory)

    // 1. Appearance & Theme
    val appearanceCategory = doc.createElement("androidx.preference.PreferenceCategory")
    appearanceCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_APPEARANCE)
    appearanceCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Appearance & Theme")

    appearanceCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_AMOLED,
            title = "Pure AMOLED Theme",
            summary = "Force pure black (#000000) background on dark themes",
            defaultValue = "true",
        )
    )
    appearanceCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_ZERO_BOTTOM_INSET,
            title = "Zero Bottom Inset",
            summary = "Eliminate bottom margin chin under keyboard in gesture navigation",
            defaultValue = "true",
        )
    )
    appearanceCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_BOTTOM_PADDING,
            title = "Bottom Padding (px)",
            summary = "Forced bottom margin padding in pixels (0 for completely flush, default: 0)",
            defaultValue = Constants.GboardPrefs.DEFAULT_BOTTOM_PADDING,
            max = Constants.GboardPrefs.MAX_BOTTOM_PADDING,
            dependency = Constants.GboardPrefs.KEY_ZERO_BOTTOM_INSET,
        )
    )
    appearanceCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_KEY_SHAPE_SELECTION,
            title = "Key Border Shapes",
            summary = "Enable rounded and borderless key styles in themes",
            defaultValue = "true",
        )
    )
    appearanceCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_EMOJI_SCALE,
            title = "Emoji Size Scaling",
            summary = "Adjust emoji visual size on the keyboard (50% - 150%)",
            defaultValue = Constants.GboardPrefs.DEFAULT_EMOJI_SCALE,
            max = Constants.GboardPrefs.MAX_EMOJI_SCALE,
        )
    )
    root.appendChild(appearanceCategory)

    // 2. Toolbar & Navigation
    val toolbarCategory = doc.createElement("androidx.preference.PreferenceCategory")
    toolbarCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_TOOLBAR)
    toolbarCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Toolbar & Navigation")

    toolbarCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_ACCESS_POINTS_REDESIGN,
            title = "Access Points Redesign",
            summary = "Enable redesigned access points menu bar and panel (Panel V2)",
            defaultValue = "true",
        )
    )
    toolbarCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_TOOLBAR_ITEM_COUNT,
            title = "Toolbar Item Count",
            summary = "Maximum number of access point icons displayed on top toolbar (default: 5)",
            defaultValue = Constants.GboardPrefs.DEFAULT_TOOLBAR_ITEM_COUNT,
            max = Constants.GboardPrefs.MAX_TOOLBAR_ITEM_COUNT,
        )
    )
    toolbarCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_DISMISS_SUGGESTIONS,
            title = "Dismiss Suggestions Button",
            summary = "Show close button (X) on proactive suggestions bar",
            defaultValue = "true",
        )
    )
    toolbarCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CURSOR_TRACKPAD,
            title = "Cursor Trackpad Mode",
            summary = "2D spacebar trackpad cursor navigation and cursor lock mode",
            defaultValue = "false",
        )
    )
    root.appendChild(toolbarCategory)

    // 3. Clipboard Enhancements
    val clipboardCategory = doc.createElement("androidx.preference.PreferenceCategory")
    clipboardCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_CLIPBOARD)
    clipboardCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Clipboard")

    clipboardCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_EXTENDED_RETENTION,
            title = "Extended History Retention",
            summary = "Enable custom retention time limit for unpinned clips",
            defaultValue = "true",
        )
    )
    clipboardCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_RETENTION_HOURS,
            title = "Retention Time Limit (Hours)",
            summary = "Hours to retain unpinned clips in history before cleanup (default: 24h)",
            defaultValue = Constants.GboardPrefs.DEFAULT_CLIPBOARD_RETENTION_HOURS,
            max = Constants.GboardPrefs.MAX_CLIPBOARD_RETENTION_HOURS,
            dependency = Constants.GboardPrefs.KEY_CLIPBOARD_EXTENDED_RETENTION,
        )
    )
    clipboardCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_RAISE_LIMIT,
            title = "Raise Unpinned Clips Limit",
            summary = "Enable custom limit for unpinned clipboard history items",
            defaultValue = "true",
        )
    )
    clipboardCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_UNPINNED_LIMIT,
            title = "Unpinned Clips Limit",
            summary = "Maximum number of unpinned items displayed in clipboard (default: 50)",
            defaultValue = Constants.GboardPrefs.DEFAULT_CLIPBOARD_UNPINNED_LIMIT,
            max = Constants.GboardPrefs.MAX_CLIPBOARD_UNPINNED_LIMIT,
            dependency = Constants.GboardPrefs.KEY_CLIPBOARD_RAISE_LIMIT,
        )
    )
    clipboardCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_GRID_LAYOUT,
            title = "Clipboard Grid Layout",
            summary = "Enable custom multi-column layout for clipboard clips",
            defaultValue = "true",
        )
    )
    clipboardCategory.appendChild(
        createSeekBar(
            doc = doc,
            key = Constants.GboardPrefs.KEY_CLIPBOARD_GRID_COLUMNS,
            title = "Clipboard Grid Columns",
            summary = "Number of columns in clipboard layout (1, 2, or 3. Default: 2)",
            defaultValue = Constants.GboardPrefs.DEFAULT_CLIPBOARD_GRID_COLUMNS,
            max = Constants.GboardPrefs.MAX_CLIPBOARD_GRID_COLUMNS,
            dependency = Constants.GboardPrefs.KEY_CLIPBOARD_GRID_LAYOUT,
        )
    )
    root.appendChild(clipboardCategory)

    // 4. Haptics & Vibration
    val hapticsCategory = doc.createElement("androidx.preference.PreferenceCategory")
    hapticsCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_HAPTICS)
    hapticsCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Haptics & Vibration")

    hapticsCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_DECOUPLE_TOUCH_FEEDBACK,
            title = "Independent Keyboard Vibration",
            summary = "Keep keyboard vibration active even when Android's system Touch feedback and gesture haptics are disabled",
            defaultValue = "true",
        )
    )
    root.appendChild(hapticsCategory)

    // 5. Smart Features & Voice
    val smartCategory = doc.createElement("androidx.preference.PreferenceCategory")
    smartCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_SMART)
    smartCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Smart Features & Voice")

    smartCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_GRAMMAR_CHECKER,
            title = "Grammar Checker & Smart Compose",
            summary = "Inline grammar review and Smart Compose predictions",
            defaultValue = "true",
        )
    )
    smartCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_BLUETOOTH_MIC,
            title = "Bluetooth Microphone",
            summary = "Enable Bluetooth microphone audio input for voice typing",
            defaultValue = "true",
        )
    )
    root.appendChild(smartCategory)

    // 6. Privacy & Security
    val privacyCategory = doc.createElement("androidx.preference.PreferenceCategory")
    privacyCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", Constants.GboardPrefs.KEY_CAT_PRIVACY)
    privacyCategory.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", "Privacy & Security")

    privacyCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_FORCE_INCOGNITO,
            title = "Force Incognito Mode",
            summary = "Always operate in incognito mode (disables input history and learning)",
            defaultValue = "false",
        )
    )
    privacyCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_HIDE_INCOGNITO_ICON,
            title = "Hide Incognito Icon",
            summary = "Hide the incognito mask icon on the toolbar",
            defaultValue = "false",
            dependency = Constants.GboardPrefs.KEY_FORCE_INCOGNITO,
        )
    )
    privacyCategory.appendChild(
        createSwitch(
            doc = doc,
            key = Constants.GboardPrefs.KEY_VOICE_INCOGNITO,
            title = "Voice Typing in Incognito",
            summary = "Enable voice typing and microphone dictation in private fields and incognito mode",
            defaultValue = "true",
        )
    )
    root.appendChild(privacyCategory)

    return true
}

private fun createSwitch(
    doc: Document,
    key: String,
    title: String,
    summary: String,
    defaultValue: String,
    dependency: String? = null,
): Element {
    val pref = doc.createElement("SwitchPreferenceCompat")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:persistent", "true")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", key)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", title)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:summary", summary)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:defaultValue", defaultValue)
    if (dependency != null) {
        pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:dependency", dependency)
    }
    return pref
}

private fun createSeekBar(
    doc: Document,
    key: String,
    title: String,
    summary: String,
    defaultValue: Int,
    max: Int,
    dependency: String? = null,
): Element {
    val pref = doc.createElement("androidx.preference.SeekBarPreference")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:persistent", "true")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", key)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", title)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:summary", summary)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:defaultValue", defaultValue.toString())
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:max", max.toString())
    if (dependency != null) {
        pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:dependency", dependency)
    }
    return pref
}

private fun createActionPreference(
    doc: Document,
    key: String,
    title: String,
    summary: String,
    icon: String? = null,
): Element {
    val pref = doc.createElement("androidx.preference.Preference")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:persistent", "false")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:selectable", "true")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:enabled", "true")
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:key", key)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:title", title)
    pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:summary", summary)
    if (icon != null) {
        pref.setAttributeNS(ANDROID_XML_NAMESPACE, "android:icon", icon)
    }
    return pref
}
