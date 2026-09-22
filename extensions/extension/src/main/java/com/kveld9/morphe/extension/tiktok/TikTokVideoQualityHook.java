package com.kveld9.morphe.extension.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime hook helper for TikTok video playback and download resolution capping.
 * Intercepts video bitrate lists and play address models to enforce user-defined
 * resolution ceilings (1080p, 720p, 540p, 480p, 360p), conserving device thermals,
 * GPU/MediaCodec load, RAM GraphicBuffers, and mobile data, while allowing downloads
 * to independently preserve full/high resolution.
 */
@SuppressWarnings("unused")
public final class TikTokVideoQualityHook {

    private static final String TAG = "MorpheTikTok";
    private static final String PREFS_NAME = "morphe_tiktok_quality_prefs";
    private static final String KEY_MAX_QUALITY = "max_video_quality";
    private static final String KEY_DOWNLOAD_QUALITY = "download_video_quality";

    public static volatile int maxAllowedResolution = 480;
    public static volatile int downloadAllowedResolution = 1080;

    private static final Map<Object, Object> uncappedDownloadAddrs =
        Collections.synchronizedMap(new WeakHashMap<Object, Object>());

    private static final Map<Object, Boolean> playAddrIsBytevc =
        Collections.synchronizedMap(new WeakHashMap<Object, Boolean>());

    public static boolean isBytevcPlayAddr(Object playAddr) {
        if (playAddr == null) return false;
        Boolean isBvc = playAddrIsBytevc.get(playAddr);
        return Boolean.TRUE.equals(isBvc);
    }

    private static volatile boolean reflectionInitialized = false;
    private static Field videoBitRateListField;
    private static Field videoPlayAddrValueField;
    private static Field videoPlayAddrBytevc1ValueField;
    private static Field videoH264PlayAddrValueField;

    private static Method bitrateGetHeightMethod;
    private static Method bitrateGetGearNameMethod;
    private static Method bitrateGetPlayAddrMethod;

    private static volatile SharedPreferences prefs = null;

    private TikTokVideoQualityHook() {}

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

    public static int getMaxResolution() {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            int saved = sp.getInt(KEY_MAX_QUALITY, maxAllowedResolution);
            if (isValidResolution(saved)) {
                maxAllowedResolution = saved;
                return saved;
            }
        }
        return maxAllowedResolution;
    }

    public static void setMaxResolution(int resolution) {
        if (!isValidResolution(resolution)) return;
        maxAllowedResolution = resolution;
        try {
            SharedPreferences sp = getPrefs();
            if (sp != null) {
                sp.edit().putInt(KEY_MAX_QUALITY, resolution).apply();
            }
            Log.i(TAG, "[Video Quality Governor] Active resolution cap set to: " + resolution + "p");
        } catch (Throwable t) {
            Log.w(TAG, "[Video Quality Governor] SharedPreferences write note: " + t.getMessage());
        }
    }

    public static int getDownloadResolution() {
        SharedPreferences sp = getPrefs();
        if (sp != null) {
            int saved = sp.getInt(KEY_DOWNLOAD_QUALITY, downloadAllowedResolution);
            if (isValidResolution(saved)) {
                downloadAllowedResolution = saved;
                return saved;
            }
        }
        return downloadAllowedResolution;
    }

    public static void setDownloadResolution(int resolution) {
        if (!isValidResolution(resolution)) return;
        downloadAllowedResolution = resolution;
        try {
            SharedPreferences sp = getPrefs();
            if (sp != null) {
                sp.edit().putInt(KEY_DOWNLOAD_QUALITY, resolution).apply();
            }
            Log.i(TAG, "[Video Quality Governor] Active download resolution cap set to: " + resolution + "p");
        } catch (Throwable t) {
            Log.w(TAG, "[Video Quality Governor] Download SharedPreferences write note: " + t.getMessage());
        }
    }

    public static Object getBestDownloadPlayAddr(Object videoObj) {
        if (videoObj == null) return null;
        return uncappedDownloadAddrs.get(videoObj);
    }

    public static boolean isValidResolution(int res) {
        return res == 360 || res == 480 || res == 540 || res == 720 || res == 1080;
    }

    private static void ensureReflection(ClassLoader classLoader) {
        if (reflectionInitialized) return;
        synchronized (TikTokVideoQualityHook.class) {
            if (reflectionInitialized) return;
            if (classLoader == null) {
                classLoader = TikTokVideoQualityHook.class.getClassLoader();
            }
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) return;

            try {
                Class<?> videoClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.Video");
                try {
                    videoBitRateListField = videoClass.getDeclaredField("bitRateList");
                    videoBitRateListField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoPlayAddrValueField = videoClass.getDeclaredField("playAddrValue");
                    videoPlayAddrValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoPlayAddrBytevc1ValueField = videoClass.getDeclaredField("playAddrBytevc1Value");
                    videoPlayAddrBytevc1ValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoH264PlayAddrValueField = videoClass.getDeclaredField("h264PlayAddrValue");
                    videoH264PlayAddrValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                Class<?> bitRateClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.BitRate");
                try {
                    bitrateGetHeightMethod = bitRateClass.getMethod("getVideoHeight");
                    bitrateGetHeightMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    bitrateGetGearNameMethod = bitRateClass.getMethod("getGearName");
                    bitrateGetGearNameMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    bitrateGetPlayAddrMethod = bitRateClass.getMethod("getPlayAddr");
                    bitrateGetPlayAddrMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                reflectionInitialized = true;
                Log.i(TAG, "[Video Quality Governor] Reflection initialized successfully.");
            } catch (Throwable t) {
                Log.w(TAG, "[Video Quality Governor] Reflection initialization note: " + t.getMessage());
            }
        }
    }

    public static boolean isValidVideo(Object videoObj) {
        if (videoObj == null) return false;
        try {
            int h = -1;
            int w = -1;
            try {
                Method mH = videoObj.getClass().getMethod("getHeight");
                Object resH = mH.invoke(videoObj);
                if (resH instanceof Integer) h = (Integer) resH;
            } catch (Throwable ignored) {}
            try {
                Method mW = videoObj.getClass().getMethod("getWidth");
                Object resW = mW.invoke(videoObj);
                if (resW instanceof Integer) w = (Integer) resW;
            } catch (Throwable ignored) {}

            if (h == 0 || w == 0) return false;
        } catch (Throwable ignored) {}
        return true;
    }

    public static boolean isAudioBitrate(Object bitrateObj) {
        if (bitrateObj == null) return false;
        try {
            Method mAudioId = bitrateObj.getClass().getMethod("getAudioFileId");
            Object audioId = mAudioId.invoke(bitrateObj);
            if (audioId instanceof String && !((String) audioId).trim().isEmpty()) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Method mFormat = bitrateObj.getClass().getMethod("getFormat");
            Object fmt = mFormat.invoke(bitrateObj);
            if (fmt instanceof String) {
                String sFmt = ((String) fmt).toLowerCase();
                if (sFmt.contains("audio") || sFmt.contains("m4a") || sFmt.contains("mp3")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Method mDash = bitrateObj.getClass().getMethod("isDash");
            Object dashObj = mDash.invoke(bitrateObj);
            if (Boolean.TRUE.equals(dashObj) && resolveBitrateHeight(bitrateObj) <= 0) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object playAddr = extractPlayAddrFromBitrate(bitrateObj);
            if (playAddr != null) {
                Method mUri = playAddr.getClass().getMethod("getUri");
                Object uriObj = mUri.invoke(playAddr);
                if (uriObj instanceof String) {
                    String uri = ((String) uriObj).toLowerCase();
                    if (uri.contains("dash_audio") || uri.contains("audio_id=") || uri.endsWith(".m4a") || uri.endsWith(".mp3")) {
                        return true;
                    }
                }
                Method mUrls = playAddr.getClass().getMethod("getUrlList");
                Object urlsObj = mUrls.invoke(playAddr);
                if (urlsObj instanceof List) {
                    for (Object u : (List) urlsObj) {
                        if (u instanceof String) {
                            String url = ((String) u).toLowerCase();
                            if (url.contains("mime_type=audio") || url.contains("dash_audio") || url.endsWith(".m4a") || url.endsWith(".mp3")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean isBytevc1(Object bitrateObj) {
        if (bitrateObj == null) return false;
        try {
            Method m = null;
            try {
                m = bitrateObj.getClass().getMethod("isBytevc1");
            } catch (Throwable ignored) {
                m = bitrateObj.getClass().getMethod("getIsBytevc1");
            }
            if (m != null) {
                Object res = m.invoke(bitrateObj);
                if (res instanceof Integer) return ((Integer) res) != 0;
                if (res instanceof Boolean) return (Boolean) res;
            }
        } catch (Throwable ignored) {}

        try {
            Field f = bitrateObj.getClass().getDeclaredField("isBytevc1");
            f.setAccessible(true);
            Object res = f.get(bitrateObj);
            if (res instanceof Integer) return ((Integer) res) != 0;
        } catch (Throwable ignored) {}

        try {
            Method mCodec = bitrateObj.getClass().getMethod("getCodecType");
            Object codecObj = mCodec.invoke(bitrateObj);
            if (codecObj instanceof Integer) {
                int c = ((Integer) codecObj);
                if (c != 0) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Method mFormat = bitrateObj.getClass().getMethod("getFormat");
            Object fmt = mFormat.invoke(bitrateObj);
            if (fmt instanceof String) {
                String sFmt = ((String) fmt).toLowerCase();
                if (sFmt.contains("bytevc1") || sFmt.contains("bytevc2") || sFmt.contains("bvc2") || sFmt.contains("hevc") || sFmt.contains("h265") || sFmt.contains("vvc") || sFmt.contains("h266")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Method mGear = bitrateObj.getClass().getMethod("getGearName");
            Object gear = mGear.invoke(bitrateObj);
            if (gear instanceof String) {
                String sGear = ((String) gear).toLowerCase();
                if (sGear.contains("bytevc1") || sGear.contains("bytevc2") || sGear.contains("bvc2") || sGear.contains("hevc") || sGear.contains("h265")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object playAddr = extractPlayAddrFromBitrate(bitrateObj);
            if (playAddr != null) {
                Method mUri = playAddr.getClass().getMethod("getUri");
                Object uriObj = mUri.invoke(playAddr);
                if (uriObj instanceof String) {
                    String uri = ((String) uriObj).toLowerCase();
                    if (uri.contains("bytevc1") || uri.contains("bytevc2") || uri.contains("bvc2") || uri.contains("hevc") || uri.contains("h265") || uri.contains("codec_type=bytevc1") || uri.contains("codec_type=bytevc2") || uri.contains("codec_type=bvc2")) {
                        return true;
                    }
                }
                Method mUrls = playAddr.getClass().getMethod("getUrlList");
                Object urlsObj = mUrls.invoke(playAddr);
                if (urlsObj instanceof List) {
                    for (Object u : (List) urlsObj) {
                        if (u instanceof String) {
                            String url = ((String) u).toLowerCase();
                            if (url.contains("bytevc1") || url.contains("bytevc2") || url.contains("bvc2") || url.contains("hevc") || url.contains("h265") || url.contains("codec_type=bytevc1") || url.contains("codec_type=bytevc2") || url.contains("codec_type=bvc2")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static int parseResolutionFromString(String text) {
        if (text == null) return 0;
        if (text.contains("1080")) return 1080;
        if (text.contains("720")) return 720;
        if (text.contains("540")) return 540;
        if (text.contains("480")) return 480;
        if (text.contains("360")) return 360;
        if (text.contains("240")) return 240;
        return 0;
    }

    private static int resolveBitrateHeight(Object bitrateObj) {
        if (bitrateObj == null) return 0;
        try {
            Method mHeight = null;
            try {
                mHeight = bitrateObj.getClass().getMethod("getVideoHeight");
            } catch (Throwable ignored) {
                mHeight = bitrateGetHeightMethod;
            }
            if (mHeight != null) {
                Object hObj = mHeight.invoke(bitrateObj);
                if (hObj instanceof Integer) {
                    int h = (Integer) hObj;
                    if (h > 0) return h;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback: parse resolution string from gearName
        try {
            Method mGear = null;
            try {
                mGear = bitrateObj.getClass().getMethod("getGearName");
            } catch (Throwable ignored) {
                mGear = bitrateGetGearNameMethod;
            }
            if (mGear != null) {
                Object gearObj = mGear.invoke(bitrateObj);
                if (gearObj instanceof String) {
                    int h = parseResolutionFromString((String) gearObj);
                    if (h > 0) return h;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback: parse resolution string from quality
        try {
            Method mQuality = bitrateObj.getClass().getMethod("getQuality");
            Object qObj = mQuality.invoke(bitrateObj);
            if (qObj instanceof String) {
                int h = parseResolutionFromString((String) qObj);
                if (h > 0) return h;
            }
        } catch (Throwable ignored) {}

        // Fallback: derive from width
        try {
            Method mWidth = bitrateObj.getClass().getMethod("getVideoWidth");
            Object wObj = mWidth.invoke(bitrateObj);
            if (wObj instanceof Integer) {
                int w = (Integer) wObj;
                if (w >= 1080) return 1080;
                if (w >= 720) return 720;
                if (w >= 540) return 540;
                if (w >= 480) return 480;
                if (w >= 360) return 360;
                if (w > 0) return w;
            }
        } catch (Throwable ignored) {}

        return 0;
    }

    /**
     * Filters a list of BitRate or SimBitRate objects, discarding any streams whose
     * resolution height exceeds maxAllowedResolution.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterBitrates(List originalList) {
        if (originalList == null || originalList.isEmpty()) {
            return originalList;
        }

        int cap = getMaxResolution();
        if (cap <= 0) {
            return originalList;
        }

        List filtered = new ArrayList();
        Object lowestVideoStream = null;
        int lowestVideoHeight = Integer.MAX_VALUE;
        boolean hasVideoInFiltered = false;

        for (Object item : originalList) {
            if (item == null) continue;
            boolean isAudio = isAudioBitrate(item);
            int h = resolveBitrateHeight(item);

            if (!isAudio && h > 0) {
                if (h < lowestVideoHeight) {
                    lowestVideoHeight = h;
                    lowestVideoStream = item;
                }
                if (h <= cap) {
                    filtered.add(item);
                    hasVideoInFiltered = true;
                }
            } else {
                // Preserve audio stream for DASH playback engine
                filtered.add(item);
            }
        }

        // Failsafe: if all video renditions exceeded the cap, keep the lowest available video stream
        if (!hasVideoInFiltered && lowestVideoStream != null) {
            filtered.add(lowestVideoStream);
        }

        if (filtered.isEmpty()) {
            return originalList;
        }

        // Sort surviving streams: video streams with positive height first (descending by height, H.264 over ByteVC1),
        // audio streams (h <= 0) at the end.
        Collections.sort(filtered, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int h1 = resolveBitrateHeight(o1);
                int h2 = resolveBitrateHeight(o2);
                boolean a1 = isAudioBitrate(o1) || h1 <= 0;
                boolean a2 = isAudioBitrate(o2) || h2 <= 0;
                if (!a1 && a2) return -1;
                if (a1 && !a2) return 1;
                if (!a1 && !a2) {
                    int cmp = Integer.compare(h2, h1);
                    if (cmp != 0) return cmp;
                    boolean b1 = isBytevc1(o1);
                    boolean b2 = isBytevc1(o2);
                    if (!b1 && b2) return -1;
                    if (b1 && !b2) return 1;
                    return 0;
                }
                return Integer.compare(h2, h1);
            }
        });

        return filtered;
    }

    private static void syncUrlModel(Object targetModel, Object sourceModel) {
        if (targetModel == null || sourceModel == null) return;
        try {
            Method getUrlList = sourceModel.getClass().getMethod("getUrlList");
            Method setUrlList = targetModel.getClass().getMethod("setUrlList", List.class);
            Object urls = getUrlList.invoke(sourceModel);
            if (urls instanceof List) {
                setUrlList.invoke(targetModel, urls);
            }

            Method getUri = sourceModel.getClass().getMethod("getUri");
            Method setUri = targetModel.getClass().getMethod("setUri", String.class);
            Object uri = getUri.invoke(sourceModel);
            if (uri instanceof String) {
                setUri.invoke(targetModel, uri);
            }
        } catch (Throwable ignored) {}
    }

    private static Object extractPlayAddrFromBitrate(Object bitrateObj) {
        if (bitrateObj == null) return null;
        try {
            Method m = (bitrateGetPlayAddrMethod != null) ?
                bitrateGetPlayAddrMethod : bitrateObj.getClass().getMethod("getPlayAddr");
            return m.invoke(bitrateObj);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static Object resolveBestBitrate(List bitrates, int cap) {
        if (bitrates == null || bitrates.isEmpty()) return null;

        // 1. Collect only valid H.264 video streams (exclude audio-only, DASH audio, and ByteVC1)
        List h264Streams = new ArrayList();
        for (Object item : bitrates) {
            if (item == null) continue;
            if (isAudioBitrate(item)) continue;
            if (isBytevc1(item)) continue;
            int h = resolveBitrateHeight(item);
            if (h > 0) {
                h264Streams.add(item);
            }
        }

        // Never fallback to ByteVC1 or audio for downloads
        if (h264Streams.isEmpty()) {
            return null;
        }

        // 2. Find the best H.264 stream adhering to the cap (highest resolution <= cap)
        Object bestMatch = null;
        int bestHeight = 0;
        Object lowestAboveCap = null;
        int lowestHeightAboveCap = Integer.MAX_VALUE;

        for (Object item : h264Streams) {
            int h = resolveBitrateHeight(item);
            if (h <= cap && h > bestHeight) {
                bestHeight = h;
                bestMatch = item;
            }
            if (h > cap && h < lowestHeightAboveCap) {
                lowestHeightAboveCap = h;
                lowestAboveCap = item;
            }
        }

        if (bestMatch != null) return bestMatch;
        if (lowestAboveCap != null) return lowestAboveCap;
        return h264Streams.get(0);
    }

    /**
     * Intercepts Video objects before playback to ensure both bitRateList and
     * the default play addresses (playAddrValue, playAddrBytevc1Value) obey the resolution cap,
     * while preserving the highest-quality stream matching download resolution ceiling for downloads.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void capVideoObject(Object videoObj) {
        if (videoObj == null || !isValidVideo(videoObj)) return;
        try {
            ensureReflection(videoObj.getClass().getClassLoader());

            int cap = getMaxResolution();
            if (cap <= 0) return;

            if (videoBitRateListField != null) {
                Object listObj = videoBitRateListField.get(videoObj);
                if (listObj instanceof List) {
                    List originalList = (List) listObj;
                    if (originalList.isEmpty()) return;

                    // Track known codec classification for playAddr objects
                    for (Object item : originalList) {
                        if (item == null) continue;
                        Object playAddr = extractPlayAddrFromBitrate(item);
                        if (playAddr != null) {
                            if (isBytevc1(item)) {
                                playAddrIsBytevc.put(playAddr, Boolean.TRUE);
                            } else if (!isAudioBitrate(item) && resolveBitrateHeight(item) > 0) {
                                playAddrIsBytevc.put(playAddr, Boolean.FALSE);
                            }
                        }
                    }
                    if (videoPlayAddrBytevc1ValueField != null) {
                        try {
                            Object bvc = videoPlayAddrBytevc1ValueField.get(videoObj);
                            if (bvc != null) playAddrIsBytevc.put(bvc, Boolean.TRUE);
                        } catch (Throwable ignored) {}
                    }
                    if (videoH264PlayAddrValueField != null) {
                        try {
                            Object h264 = videoH264PlayAddrValueField.get(videoObj);
                            if (h264 != null) playAddrIsBytevc.put(h264, Boolean.FALSE);
                        } catch (Throwable ignored) {}
                    }

                    // Preserve uncapped H.264 stream matching download resolution ceiling for downloads
                    Object bestDownloadBitrate = resolveBestBitrate(originalList, getDownloadResolution());
                    if (bestDownloadBitrate != null) {
                        Object bestDownloadPlayAddr = extractPlayAddrFromBitrate(bestDownloadBitrate);
                        if (bestDownloadPlayAddr != null) {
                            uncappedDownloadAddrs.put(videoObj, bestDownloadPlayAddr);
                            Log.i(TAG, "[Video Quality Governor] Saved uncapped H.264 download stream (" + resolveBitrateHeight(bestDownloadBitrate) + "p).");
                        }
                    }

                    List cappedList = filterBitrates(originalList);
                    videoBitRateListField.set(videoObj, cappedList);

                    // Sync default play addresses strictly to the capped video streams without cross-codec contamination
                    if (!cappedList.isEmpty()) {
                        Object bestAllowedH264 = null;
                        Object bestAllowedBytevc1 = null;

                        for (Object item : cappedList) {
                            if (isAudioBitrate(item) || resolveBitrateHeight(item) <= 0) continue;
                            if (isBytevc1(item)) {
                                if (bestAllowedBytevc1 == null) {
                                    bestAllowedBytevc1 = item;
                                }
                            } else {
                                if (bestAllowedH264 == null) {
                                    bestAllowedH264 = item;
                                }
                            }
                            if (bestAllowedH264 != null && bestAllowedBytevc1 != null) break;
                        }

                        // Sync H.264 stream strictly to playAddrValue and h264PlayAddrValue
                        if (bestAllowedH264 != null) {
                            Object cappedH264PlayAddr = extractPlayAddrFromBitrate(bestAllowedH264);
                            if (cappedH264PlayAddr != null) {
                                if (videoPlayAddrValueField != null) {
                                    Object currentPlay = videoPlayAddrValueField.get(videoObj);
                                    if (currentPlay != null) {
                                        syncUrlModel(currentPlay, cappedH264PlayAddr);
                                    } else if (videoPlayAddrValueField.getType().isInstance(cappedH264PlayAddr)) {
                                        videoPlayAddrValueField.set(videoObj, cappedH264PlayAddr);
                                    }
                                }
                                if (videoH264PlayAddrValueField != null) {
                                    Object currentH264 = videoH264PlayAddrValueField.get(videoObj);
                                    if (currentH264 != null) {
                                        syncUrlModel(currentH264, cappedH264PlayAddr);
                                    } else if (videoH264PlayAddrValueField.getType().isInstance(cappedH264PlayAddr)) {
                                        videoH264PlayAddrValueField.set(videoObj, cappedH264PlayAddr);
                                    }
                                }
                            }
                        }

                        // Sync ByteVC1 stream to playAddrBytevc1Value
                        if (bestAllowedBytevc1 != null) {
                            Object cappedBytevc1PlayAddr = extractPlayAddrFromBitrate(bestAllowedBytevc1);
                            if (cappedBytevc1PlayAddr != null && videoPlayAddrBytevc1ValueField != null) {
                                Object currentBytevc1 = videoPlayAddrBytevc1ValueField.get(videoObj);
                                if (currentBytevc1 != null) {
                                    syncUrlModel(currentBytevc1, cappedBytevc1PlayAddr);
                                } else if (videoPlayAddrBytevc1ValueField.getType().isInstance(cappedBytevc1PlayAddr)) {
                                    videoPlayAddrBytevc1ValueField.set(videoObj, cappedBytevc1PlayAddr);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Video Quality Governor] capVideoObject note: " + t.getMessage());
        }
    }
}
