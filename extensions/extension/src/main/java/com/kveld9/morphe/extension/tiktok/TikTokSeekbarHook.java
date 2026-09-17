package com.kveld9.morphe.extension.tiktok;

/**
 * Runtime hook helper for TikTok video seekbar display states.
 */
public final class TikTokSeekbarHook {
    private TikTokSeekbarHook() {}

    public static int sanitizeSeekbarShowType(int type) {
        // Types 3 and 4 are hidden/suppressed seekbar states; normalize them to visible scrubbing state 0
        if (type == 3 || type == 4) {
            return 0;
        }
        return type;
    }
}
