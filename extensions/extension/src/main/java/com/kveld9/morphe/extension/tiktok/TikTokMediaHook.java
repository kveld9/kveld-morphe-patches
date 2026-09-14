package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Runtime hook helper for TikTok media playback and usability controls.
 * Unblocks downloads on restricted videos and enables seekbar scrubbing universally.
 */
@SuppressWarnings("unused")
public final class TikTokMediaHook {

    private static final String TAG = "MorpheTikTok";
    private static Method getPlayAddrMethod;
    public static volatile boolean forceWatermarkFreeDownload = true;

    /**
     * Intercepts Video.getDownloadAddr() and returns the watermark-free playAddr stream.
     *
     * @param originalUrl original UrlModel returned by getDownloadAddr()
     * @param videoObj target Video instance
     * @return watermark-free UrlModel if available, otherwise original
     */
    public static Object getWatermarkFreeDownloadUrl(Object originalUrl, Object videoObj) {
        if (!forceWatermarkFreeDownload || videoObj == null) return originalUrl;
        try {
            if (getPlayAddrMethod == null) {
                try {
                    Class<?> videoClass = videoObj.getClass().getClassLoader().loadClass("com.ss.android.ugc.aweme.feed.model.Video");
                    getPlayAddrMethod = videoClass.getMethod("getPlayAddr");
                } catch (Throwable ignored) {}
            }
            if (getPlayAddrMethod != null) {
                Object cleanUrl = getPlayAddrMethod.invoke(videoObj);
                if (cleanUrl != null) {
                    Log.i(TAG, "[Watermark Free] Replaced download stream with unwatermarked playAddr.");
                    return cleanUrl;
                }
            }
        } catch (Throwable ignored) {}
        return originalUrl;
    }
}
