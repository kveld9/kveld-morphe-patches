package com.kveld9.morphe.extension.tiktok;

import java.text.NumberFormat;

/**
 * Runtime hook helper for TikTok exact count formatting.
 */
public final class TikTokCountsHook {
    private TikTokCountsHook() {}

    private static final ThreadLocal<NumberFormat> NUMBER_FORMAT = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return NumberFormat.getIntegerInstance();
        }
    };

    public static String exact(long count) {
        if (count < 0) {
            return "0";
        }
        return NUMBER_FORMAT.get().format(count);
    }
}
