package com.kveld9.morphe.extension.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Runtime controller for TikTok feed playback speed persistence across videos and app launches.
 */
@SuppressWarnings("unused")
public final class TikTokSpeedHook {

    private static final String TAG = "MorpheTikTok";
    private static final String PREFS_NAME = "morphe_tiktok_speed_prefs";
    private static final String KEY_SPEED = "playback_speed";

    private static volatile float cachedSpeed = -1.0f;
    private static volatile SharedPreferences prefs = null;

    private TikTokSpeedHook() {}

    private static SharedPreferences getPrefs() {
        if (prefs != null) return prefs;
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null);
            if (app != null) {
                prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {}
        return prefs;
    }

    /**
     * Resolves the active playback speed for a given Aweme video model.
     * Enforces normal 1.0x speed on live streams to protect stream sync.
     */
    public static float getPlaybackSpeed(Object aweme, float fallbackDefault) {
        if (aweme != null) {
            try {
                if (TikTokFeedAdFilter.isLiveStream(aweme)) {
                    return 1.0f;
                }
            } catch (Throwable ignored) {}
        }

        if (cachedSpeed > 0.0f) {
            return cachedSpeed;
        }

        SharedPreferences sp = getPrefs();
        if (sp != null) {
            float saved = sp.getFloat(KEY_SPEED, fallbackDefault);
            if (saved >= 0.25f && saved <= 4.0f) {
                cachedSpeed = saved;
                return cachedSpeed;
            }
            cachedSpeed = (fallbackDefault >= 0.25f && fallbackDefault <= 4.0f) ? fallbackDefault : 1.0f;
            return cachedSpeed;
        }

        return (fallbackDefault >= 0.25f && fallbackDefault <= 4.0f) ? fallbackDefault : 1.0f;
    }

    /**
     * Convenience overload for getPlaybackSpeed with default 1.0f.
     */
    public static float getPlaybackSpeed(Object aweme) {
        return getPlaybackSpeed(aweme, 1.0f);
    }

    /**
     * Intercepts user speed selection from native TikTok speed sheet / menu.
     */
    public static void onSpeedSelected(float speed) {
        if (speed < 0.25f || speed > 4.0f) return;
        cachedSpeed = speed;
        try {
            SharedPreferences sp = getPrefs();
            if (sp != null) {
                sp.edit().putFloat(KEY_SPEED, speed).apply();
            }
            Log.i(TAG, "[Playback Speed] Speed updated to " + speed + "x (persisted across feed videos)");
        } catch (Throwable t) {
            Log.w(TAG, "[Playback Speed] SharedPreferences write note: " + t.getMessage());
        }
    }
}
