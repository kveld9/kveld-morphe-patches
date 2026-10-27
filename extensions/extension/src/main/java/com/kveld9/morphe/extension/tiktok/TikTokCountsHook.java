package com.kveld9.morphe.extension.tiktok;

import java.text.NumberFormat;

/**
 * Runtime hook helper for TikTok exact count formatting.
 */
public final class TikTokCountsHook {
    private TikTokCountsHook() {}

    public static String exact(long count) {
        if (count < 0) {
            return "0";
        }
        return NumberFormat.getIntegerInstance().format(count);
    }
}
