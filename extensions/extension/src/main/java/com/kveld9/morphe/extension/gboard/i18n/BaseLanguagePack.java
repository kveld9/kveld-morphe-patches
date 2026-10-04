package com.kveld9.morphe.extension.gboard.i18n;

import java.util.Map;
import static com.kveld9.morphe.extension.gboard.GboardExtension.*;

public abstract class BaseLanguagePack {
    public abstract String getLanguageCode();

    public abstract String getLanguageName();

    public abstract void populateTitles(Map<String, String> titles);

    public abstract void populateSummaries(Map<String, String> summaries);

    public String getRestartToast() {
        return "Restart Gboard to apply changes";
    }

    public String getRestartingToast() {
        return "Restarting Gboard...";
    }

    public String getRestartTitle(boolean pending) {
        if (pending) {
            return "Restart Gboard (Restart Pending)";
        }
        return "Restart Gboard Process";
    }

    public String getRestartSummary(boolean pending) {
        if (pending) {
            return "Changes pending! Tap here to restart Gboard and apply changes now.";
        }
        return "Tap to apply changes (required for most options to take effect)";
    }

    public String formatUnit(String prefKey, int value) {
        switch (prefKey) {
            case PREF_KEY_BOTTOM_PADDING:
                return value + " px";
            case PREF_KEY_TOOLBAR_ITEM_COUNT:
                return value + (value == 1 ? " icon" : " icons");
            case PREF_KEY_CLIPBOARD_RETENTION_HOURS:
                return value + " h";
            case PREF_KEY_CLIPBOARD_UNPINNED_LIMIT:
                return value + (value == 1 ? " clip" : " clips");
            case PREF_KEY_CLIPBOARD_GRID_COLUMNS:
                return value + (value == 1 ? " col" : " cols");
            case PREF_KEY_EMOJI_SCALE:
                return value + " %";
            default:
                return String.valueOf(value);
        }
    }
}
