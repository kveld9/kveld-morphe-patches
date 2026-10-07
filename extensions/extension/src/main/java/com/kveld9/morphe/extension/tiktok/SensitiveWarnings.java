package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime cleaner for TikTok content warnings and sensitive filter interstitials.
 * Called at the start of VideoAuthorInfoVM.paramSync2StateAccept to clear warning/mask metadata
 * from in-flight VideoItemParams and underlying Aweme instances.
 */
@SuppressWarnings("unused")
public final class SensitiveWarnings {

    private static final String TAG = "MorpheTikTok";

    private SensitiveWarnings() {}

    /**
     * Clears content warnings, age gates, graphic media flags, and blur masks from the video parameters.
     *
     * @param params VideoItemParams instance passed into VideoAuthorInfoVM.
     */
    public static void clear(Object params) {
        if (params == null) return;
        try {
            Object aweme = extractAweme(params);
            if (aweme == null) return;

            clearAwemeWarnings(aweme);
        } catch (Throwable t) {
            Log.w(TAG, "[Always Show Publish Date] Failed to clear sensitive warnings: " + t.getMessage());
        }
    }

    private static Object extractAweme(Object params) {
        try {
            Method m = params.getClass().getMethod("getAweme");
            Object aweme = m.invoke(params);
            if (aweme != null) return aweme;
        } catch (Throwable ignored) {}

        try {
            Field f = params.getClass().getDeclaredField("aweme");
            f.setAccessible(true);
            return f.get(params);
        } catch (Throwable ignored) {}
        return null;
    }

    private static void clearAwemeWarnings(Object aweme) {
        Class<?> clazz = aweme.getClass();

        // 1. Clear warning info
        try {
            Method setWarnInfo = clazz.getMethod("setWarnInfo", Object.class);
            setWarnInfo.invoke(aweme, (Object) null);
        } catch (Throwable ignored) {
            clearField(clazz, aweme, "warnInfo");
        }

        // 2. Clear mask / blur info
        try {
            Method setMaskInfo = clazz.getMethod("setMaskInfo", Object.class);
            setMaskInfo.invoke(aweme, (Object) null);
        } catch (Throwable ignored) {
            clearField(clazz, aweme, "maskInfo");
        }

        // 3. Clear content classification
        clearField(clazz, aweme, "contentClassification");
        clearField(clazz, aweme, "warningInfo");

        // 4. Reset sensitive flags
        try {
            Method setSensitive = clazz.getMethod("setIsSensitive", boolean.class);
            setSensitive.invoke(aweme, false);
        } catch (Throwable ignored) {
            setBooleanField(clazz, aweme, "isSensitive", false);
        }

        Log.d(TAG, "[Always Show Publish Date] Neutralized content warning and mask flags.");
    }

    private static void clearField(Class<?> clazz, Object target, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, null);
        } catch (Throwable ignored) {}
    }

    private static void setBooleanField(Class<?> clazz, Object target, String fieldName, boolean value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.setBoolean(target, value);
        } catch (Throwable ignored) {}
    }
}
