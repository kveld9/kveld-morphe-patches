package com.kveld9.morphe.extension.tiktok;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runtime hook helper for TikTok custom offline videos download limit.
 * Injects user-configured video limits into offline mode sheet selection,
 * formats pluralized titles, and computes duration and storage size estimations.
 */
@SuppressWarnings("unused")
@SuppressLint("ResourceType")
public final class TikTokOfflineVideosHook {

    public static int targetLimit = 200;

    static {
        targetLimit = 200;
    }

    private TikTokOfflineVideosHook() {}

    /**
     * Injects targetLimit into the allowed limits list while preserving
     * the auto-adjust marker (-1) if present, returning sorted limits.
     */
    public static List<Integer> getOfflineLimits(List<Integer> originalList) {
        boolean hasAutoAdjust = false;
        Set<Integer> sortedLimits = new TreeSet<>();

        if (originalList != null) {
            for (Integer limit : originalList) {
                if (limit == null) continue;
                if (limit == -1) {
                    hasAutoAdjust = true;
                } else if (limit > 0) {
                    sortedLimits.add(limit);
                }
            }
        }

        if (targetLimit > 0) {
            sortedLimits.add(targetLimit);
        }

        List<Integer> result = new ArrayList<>();
        if (hasAutoAdjust) {
            result.add(-1);
        }
        result.addAll(sortedLimits);
        return result;
    }

    /**
     * Formats the sheet header title using native plural resource (2131755487)
     * when available, falling back to english pluralization if unresolvable.
     */
    public static String formatTitle(int count, Activity activity, String fallback) {
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }

        int effectiveCount = count > 0 ? count : targetLimit;
        if (activity != null) {
            try {
                Resources res = activity.getResources();
                if (res != null) {
                    return res.getQuantityString(2131755487, effectiveCount, effectiveCount);
                }
            } catch (Throwable ignored) {}
        }

        return effectiveCount == 1 ? "1 video" : effectiveCount + " videos";
    }

    /**
     * Formats the radio item subtitle in the offline videos sheet.
     * Extracts video count directly from title string, avoiding reflection.
     */
    public static String formatRadioSubtitle(String title, String fallback) {
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }

        int count = parseCountFromTitle(title);
        if (count <= 0) {
            return fallback != null ? fallback : "";
        }
        return computeDurationAndSize(count);
    }

    /**
     * Formats the download progress subtitle when enum matching fails for custom numbers.
     */
    public static String formatProgressSubtitle(Context context, int count, String fallback) {
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }

        int effectiveCount = count > 0 ? count : targetLimit;
        return computeDurationAndSize(effectiveCount);
    }

    private static int parseCountFromTitle(String title) {
        if (title == null || title.isEmpty()) {
            return targetLimit;
        }
        try {
            int i = 0;
            while (i < title.length() && Character.isDigit(title.charAt(i))) {
                i++;
            }
            if (i > 0) {
                return Integer.parseInt(title.substring(0, i));
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static String computeDurationAndSize(int count) {
        int effectiveCount = count > 0 ? count : targetLimit;
        long minutes = Math.max(1L, Math.round(effectiveCount * 0.5));
        long sizeMb = Math.max(1L, (long) effectiveCount * 2L);

        String sizeStr = sizeMb >= 1000
            ? String.format(Locale.US, "%.1f GB", sizeMb / 1000.0)
            : (sizeMb + " MB");

        String minsStr = "~" + minutes + (minutes == 1 ? " min" : " mins");
        return minsStr + ", " + sizeStr;
    }
}
