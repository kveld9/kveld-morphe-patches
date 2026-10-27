package com.kveld9.morphe.extension.tiktok;

import android.util.Log;

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

    public static void onClearModeCall(Object enumStatus, String reason) {
        if (enumStatus == null) return;
        String status = enumStatus.toString();
        if ("OPEN".equals(status)) {
            wantsClear = true;
            Log.i(TAG, "[Clear Display Lite] onClearModeCall OPEN (reason=" + reason + ") -> wantsClear=true");
        } else if ("CLOSE".equals(status)) {
            if (!"switch_video".equals(reason)) {
                wantsClear = false;
                Log.i(TAG, "[Clear Display Lite] onClearModeCall CLOSE (reason=" + reason + ") -> wantsClear=false");
            } else {
                Log.i(TAG, "[Clear Display Lite] onClearModeCall CLOSE (switch_video ignored for wantsClear)");
            }
        }
    }

    public static void rememberClearDisplayEvent(Object event) {
        if (event == null) return;
        try {
            Class<?> type = event.getClass();
            boolean isClean = type.getDeclaredField("LIZ").getBoolean(event);
            int eventType = type.getDeclaredField("LIZIZ").getInt(event);
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
