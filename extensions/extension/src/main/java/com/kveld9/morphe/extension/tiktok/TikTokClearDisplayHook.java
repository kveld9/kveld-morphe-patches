package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Runtime state for the Remember Clear Display patch.
 * Records the user's clear-display choice from clear-mode events and re-applies
 * it when new videos render their first frame.
 */
@SuppressWarnings("unused")
public final class TikTokClearDisplayHook {

    private static final String TAG = "MorpheTikTok";

    // Event types that must never update the remembered state (page switches and exits).
    private static final int EVENT_SWITCH_PAGE = 3;
    private static final int EVENT_NOTIFY_EXIT = 9;

    private static volatile boolean clearDisplayRemembered = false;
    private static volatile boolean wantsClear = false;

    private static volatile Class<?> cachedEventType = null;
    private static volatile Field cachedCleanField = null;
    private static volatile Field cachedTypeField = null;
    private static volatile boolean loggedResolutionPath = false;

    private TikTokClearDisplayHook() {}

    public static boolean getClearDisplayState() {
        return clearDisplayRemembered;
    }

    public static boolean getWantsClear() {
        return wantsClear;
    }

    public static void setWantsClear(boolean wants) {
        wantsClear = wants;
        Log.i(TAG, "[Clear Display Lite] wantsClear=" + wants);
    }

    public static boolean onClearModeCall(Object enumStatus, String reason) {
        if (enumStatus == null) return true;
        String status = enumStatus.toString();
        if ("OPEN".equals(status)) {
            clearDisplayRemembered = true;
            wantsClear = true;
            Log.i(TAG, "[Clear Display Lite] onClearModeCall OPEN (reason=" + reason + ") -> clearDisplayRemembered=true");
            return true;
        } else if ("CLOSE".equals(status)) {
            if ("switch_video".equals(reason) && clearDisplayRemembered) {
                Log.i(TAG, "[Clear Display Lite] Suppressing CLOSE for switch_video (clear display remembered)");
                return false;
            }
            clearDisplayRemembered = false;
            wantsClear = false;
            Log.i(TAG, "[Clear Display Lite] onClearModeCall CLOSE (reason=" + reason + ") -> clearDisplayRemembered=false");
            return true;
        }
        return true;
    }

    private static synchronized void resolveFields(Class<?> type) {
        if (cachedEventType == type) {
            return;
        }
        Field boolField = null;
        Field intField = null;
        int boolCount = 0;
        int intCount = 0;

        for (Field f : type.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isStatic(mods) || f.isSynthetic()) {
                continue;
            }
            if (f.getType() == boolean.class) {
                boolField = f;
                boolCount++;
            } else if (f.getType() == int.class) {
                intField = f;
                intCount++;
            }
        }

        if (boolCount == 1 && intCount == 1) {
            boolField.setAccessible(true);
            intField.setAccessible(true);
            cachedCleanField = boolField;
            cachedTypeField = intField;
            cachedEventType = type;
            if (!loggedResolutionPath) {
                loggedResolutionPath = true;
                Log.i(TAG, "[Clear Display] Resolved event fields structurally: " + boolField.getName() + " (boolean), " + intField.getName() + " (int)");
            }
            return;
        }

        try {
            Field legacyBool = type.getDeclaredField("LIZ");
            Field legacyInt = type.getDeclaredField("LIZIZ");
            legacyBool.setAccessible(true);
            legacyInt.setAccessible(true);
            cachedCleanField = legacyBool;
            cachedTypeField = legacyInt;
            cachedEventType = type;
            if (!loggedResolutionPath) {
                loggedResolutionPath = true;
                Log.i(TAG, "[Clear Display] Falling back to legacy event field names (LIZ, LIZIZ)");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Clear Display] Could not resolve legacy event fields: " + t.getMessage());
        }
    }

    public static void rememberClearDisplayEvent(Object event) {
        if (event == null) return;
        try {
            Class<?> type = event.getClass();
            if (cachedEventType != type) {
                resolveFields(type);
            }
            Field cleanField = cachedCleanField;
            Field typeField = cachedTypeField;
            if (cleanField == null || typeField == null) {
                return;
            }
            boolean isClean = cleanField.getBoolean(event);
            int eventType = typeField.getInt(event);
            if (eventType == EVENT_SWITCH_PAGE || eventType == EVENT_NOTIFY_EXIT) {
                return;
            }
            clearDisplayRemembered = isClean;
            Log.i(TAG, "[Clear Display] Remembered state=" + isClean + " (eventType=" + eventType + ")");
        } catch (Throwable t) {
            Log.w(TAG, "[Clear Display] Could not read clear-display event: " + t.getMessage());
        }
    }
}
