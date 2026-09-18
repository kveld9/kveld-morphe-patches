package com.kveld9.morphe.extension.tiktok;

import android.util.Log;

public final class TikTokGhostModeHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokGhostModeHook() {}

    public static boolean shouldBlockStoryView() {
        Log.d(TAG, "[Ghost Mode] Outbound story view suppressed");
        return true;
    }

    public static boolean shouldBlockProfileView() {
        Log.d(TAG, "[Ghost Mode] Outbound profile view suppressed");
        return true;
    }

    public static boolean shouldBlockTypingStatus() {
        Log.d(TAG, "[Ghost Mode] Outbound typing status suppressed");
        return true;
    }
}
