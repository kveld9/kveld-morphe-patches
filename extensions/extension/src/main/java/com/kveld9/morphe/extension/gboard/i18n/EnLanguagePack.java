package com.kveld9.morphe.extension.gboard.i18n;

import java.util.Map;
import static com.kveld9.morphe.extension.gboard.GboardExtension.*;

public class EnLanguagePack extends BaseLanguagePack {
    @Override
    public String getLanguageCode() {
        return "en";
    }

    @Override
    public String getLanguageName() {
        return "English";
    }

    @Override
    public void populateTitles(Map<String, String> titles) {
        titles.put(PREF_KEY_HEADER, "Morphe Patches");
        titles.put(PREF_KEY_SCREEN, "Morphe Patches");
        titles.put(PREF_KEY_CAT_ACTIONS, "Actions & Status");
        titles.put(PREF_KEY_ENABLE_IME, "Enable Gboard in System Settings");
        titles.put(PREF_KEY_SELECT_IME, "Select Gboard Input Method");
        titles.put(PREF_KEY_RESTART_GBOARD, "Restart Gboard Process");
        titles.put(PREF_KEY_CAT_APPEARANCE, "Appearance & Theme");
        titles.put(PREF_KEY_AMOLED, "Pure AMOLED Theme");
        titles.put(PREF_KEY_ZERO_BOTTOM_INSET, "Zero Bottom Inset");
        titles.put(PREF_KEY_BOTTOM_PADDING, "Bottom Padding (px)");
        titles.put(PREF_KEY_KEY_SHAPE_SELECTION, "Key Border Shapes");
        titles.put(PREF_KEY_EMOJI_SCALE, "Emoji Size Scaling");
        titles.put(PREF_KEY_CAT_TOOLBAR, "Toolbar & Navigation");
        titles.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Access Points Redesign");
        titles.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Toolbar Item Count");
        titles.put(PREF_KEY_DISMISS_SUGGESTIONS, "Dismiss Suggestions Button");
        titles.put(PREF_KEY_CURSOR_TRACKPAD, "Cursor Trackpad Mode");
        titles.put(PREF_KEY_HIDE_NUMBER_HINTS, "Hide Number Hints");
        titles.put(PREF_KEY_CAT_CLIPBOARD, "Clipboard");
        titles.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Extended History Retention");
        titles.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Retention Time Limit (Hours)");
        titles.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Raise Unpinned Clips Limit");
        titles.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Unpinned Clips Limit");
        titles.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Clipboard Grid Layout");
        titles.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Clipboard Grid Columns");
        titles.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Clip Character Limit");
        titles.put(PREF_KEY_CAT_HAPTICS, "Haptics & Vibration");
        titles.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Independent Keyboard Vibration");
        titles.put(PREF_KEY_MODERN_HAPTICS, "Modern Keypress Haptics");
        titles.put(PREF_KEY_CAT_SMART, "Smart Features & Voice");
        titles.put(PREF_KEY_GRAMMAR_CHECKER, "Grammar Checker & Smart Compose");
        titles.put(PREF_KEY_BLUETOOTH_MIC, "Bluetooth Microphone");
        titles.put(PREF_KEY_CAT_PRIVACY, "Privacy & Security");
        titles.put(PREF_KEY_FORCE_INCOGNITO, "Force Incognito Mode");
        titles.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Hide Incognito Icon");
        titles.put(PREF_KEY_VOICE_INCOGNITO, "Voice Typing in Incognito");
        titles.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Clipboard in Incognito");
    }

    @Override
    public void populateSummaries(Map<String, String> summaries) {
        summaries.put(PREF_KEY_HEADER, "Customization and patch toggles");
        summaries.put(PREF_KEY_ENABLE_IME, "Gboard is disabled in Android. Tap to enable it in Manage Keyboards.");
        summaries.put(PREF_KEY_SELECT_IME, "Gboard is enabled but not active. Tap to choose Gboard as your keyboard.");
        summaries.put(PREF_KEY_RESTART_GBOARD, "Tap to apply changes (required for most options to take effect)");
        summaries.put(PREF_KEY_AMOLED, "Force pure black (#000000) background on dark themes");
        summaries.put(PREF_KEY_ZERO_BOTTOM_INSET, "Eliminate bottom margin chin under keyboard in gesture navigation");
        summaries.put(PREF_KEY_BOTTOM_PADDING, "Forced bottom margin padding in pixels (0 for completely flush, default: 0)");
        summaries.put(PREF_KEY_KEY_SHAPE_SELECTION, "Enable rounded and borderless key styles in themes");
        summaries.put(PREF_KEY_EMOJI_SCALE, "Adjust emoji visual size on the keyboard (50% - 150%)");
        summaries.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Enable redesigned access points menu bar and panel (Panel V2)");
        summaries.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Maximum number of access point icons displayed on top toolbar (default: 5)");
        summaries.put(PREF_KEY_DISMISS_SUGGESTIONS, "Show close button (X) on proactive suggestions bar");
        summaries.put(PREF_KEY_CURSOR_TRACKPAD, "2D spacebar trackpad cursor navigation and cursor lock mode");
        summaries.put(PREF_KEY_HIDE_NUMBER_HINTS, "Hide the small number hints above the letter row. Long-press symbols keep working");
        summaries.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Enable custom retention time limit for unpinned clips");
        summaries.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Hours to retain unpinned clips in history before cleanup (default: 24h)");
        summaries.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Enable custom limit for unpinned clipboard history items");
        summaries.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Maximum number of unpinned items displayed in clipboard (default: 50)");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Enable custom multi-column layout for clipboard clips");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Number of columns in clipboard layout (1, 2, or 3. Default: 2)");
        summaries.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Maximum characters stored per text clip, in thousands (default: 20k). Restart Gboard to apply");
        summaries.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Keep keyboard vibration active even when Android's system Touch feedback and gesture haptics are disabled");
        summaries.put(PREF_KEY_MODERN_HAPTICS, "Use Android haptic primitives (crisp tick) for keypresses instead of a plain buzz. Strength slider becomes intensity. Restart Gboard to apply");
        summaries.put(PREF_KEY_GRAMMAR_CHECKER, "Inline grammar review and Smart Compose predictions");
        summaries.put(PREF_KEY_BLUETOOTH_MIC, "Enable Bluetooth microphone audio input for voice typing");
        summaries.put(PREF_KEY_FORCE_INCOGNITO, "Always operate in incognito mode (disables input history and learning)");
        summaries.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Hide the incognito mask icon on the toolbar");
        summaries.put(PREF_KEY_VOICE_INCOGNITO, "Enable voice typing and microphone dictation in private fields and incognito mode");
        summaries.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Enable clipboard history and paste in private fields and incognito mode");
    }
}
