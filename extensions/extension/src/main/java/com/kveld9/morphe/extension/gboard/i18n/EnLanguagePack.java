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
        titles.put(PREF_KEY_ENABLE_IME, "Turn on Gboard");
        titles.put(PREF_KEY_SELECT_IME, "Choose Gboard");
        titles.put(PREF_KEY_RESTART_GBOARD, "Restart Gboard");
        titles.put(PREF_KEY_CAT_APPEARANCE, "Appearance & Theme");
        titles.put(PREF_KEY_AMOLED, "Pure AMOLED Theme");
        titles.put(PREF_KEY_ZERO_BOTTOM_INSET, "No bottom gap");
        titles.put(PREF_KEY_BOTTOM_PADDING, "Bottom space (px)");
        titles.put(PREF_KEY_HIDE_IME_NAV_BAR, "Hide system bar");
        titles.put(PREF_KEY_KEY_SHAPE_SELECTION, "Key shapes");
        titles.put(PREF_KEY_CAT_TOOLBAR, "Toolbar & Navigation");
        titles.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "New toolbar menu");
        titles.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Toolbar icons");
        titles.put(PREF_KEY_DISMISS_SUGGESTIONS, "Close suggestions button");
        titles.put(PREF_KEY_CURSOR_TRACKPAD, "Cursor trackpad");
        titles.put(PREF_KEY_CAT_CLIPBOARD, "Clipboard");
        titles.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Longer history");
        titles.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Keep for (hours)");
        titles.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "More unpinned clips");
        titles.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Unpinned limit");
        titles.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Grid view");
        titles.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Grid columns");
        titles.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Text length limit");
        titles.put(PREF_KEY_CAT_HAPTICS, "Vibration");
        titles.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Own vibration");
        titles.put(PREF_KEY_MODERN_HAPTICS, "Modern vibration");
        titles.put(PREF_KEY_CAT_SMART, "Smart & voice");
        titles.put(PREF_KEY_GRAMMAR_CHECKER, "Grammar check");
        titles.put(PREF_KEY_BLUETOOTH_MIC, "Bluetooth mic");
        titles.put(PREF_KEY_CAT_PRIVACY, "Privacy");
        titles.put(PREF_KEY_FORCE_INCOGNITO, "Always incognito");
        titles.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Hide Incognito Icon");
        titles.put(PREF_KEY_VOICE_INCOGNITO, "Voice typing in private");
        titles.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Clipboard in private");
    }

    @Override
    public void populateSummaries(Map<String, String> summaries) {
        summaries.put(PREF_KEY_HEADER, "Patch settings");
        summaries.put(PREF_KEY_ENABLE_IME, "Gboard is off. Tap to turn it on in system settings.");
        summaries.put(PREF_KEY_SELECT_IME, "Gboard is on but not active. Tap to choose it.");
        summaries.put(PREF_KEY_RESTART_GBOARD, "Tap to apply changes.");
        summaries.put(PREF_KEY_AMOLED, "Pure black background on dark themes.");
        summaries.put(PREF_KEY_ZERO_BOTTOM_INSET, "Removes the gap below the keyboard.");
        summaries.put(PREF_KEY_BOTTOM_PADDING, "Extra space below, in pixels. 0 = none.");
        summaries.put(PREF_KEY_HIDE_IME_NAV_BAR, "Hides the system bar under the keyboard (Android 13+).");
        summaries.put(PREF_KEY_KEY_SHAPE_SELECTION, "Round and borderless key styles.");
        summaries.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "New toolbar menu and panel.");
        summaries.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Max icons on the top bar (default 5).");
        summaries.put(PREF_KEY_DISMISS_SUGGESTIONS, "Shows an X to close suggestions.");
        summaries.put(PREF_KEY_CURSOR_TRACKPAD, "Move cursor by sliding on spacebar.");
        summaries.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Custom time limit for unpinned clips.");
        summaries.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Hours before unpinned clips are deleted (default 24).");
        summaries.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Custom limit for unpinned clips.");
        summaries.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Max unpinned clips (default 50).");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Multi-column clipboard.");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Columns: 1, 2 or 3 (default 2).");
        summaries.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Max characters per clip, in thousands. Restart to apply.");
        summaries.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibrate even when system touch feedback is off.");
        summaries.put(PREF_KEY_MODERN_HAPTICS, "Crisp tick instead of buzz. Restart to apply.");
        summaries.put(PREF_KEY_GRAMMAR_CHECKER, "Grammar check and smart predictions.");
        summaries.put(PREF_KEY_BLUETOOTH_MIC, "Dictate with a Bluetooth mic.");
        summaries.put(PREF_KEY_FORCE_INCOGNITO, "Always private: no history, no learning.");
        summaries.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Hides the private-mode mask icon.");
        summaries.put(PREF_KEY_VOICE_INCOGNITO, "Voice typing in private fields.");
        summaries.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Clipboard in private fields.");
    }
}
