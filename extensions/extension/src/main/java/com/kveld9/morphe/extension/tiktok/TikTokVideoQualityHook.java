package com.kveld9.morphe.extension.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static volatile boolean isGovernorEnabled = false;
    public static volatile boolean avoidByteVC2 = false;
    public static volatile boolean dropUndecodableVideo = false;
    public static volatile int maxAllowedResolution = 480;
    public static volatile int downloadAllowedResolution = 1080;

    private static final Map<Object, Object> uncappedDownloadAddrs =
        Collections.synchronizedMap(new WeakHashMap<Object, Object>());

    private static final Map<String, Object> videoIdToCappedPlayAddr =
        Collections.synchronizedMap(new LinkedHashMap<String, Object>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > 100;
            }
        });

    private static final Map<Object, String> videoToAwemeId =
        Collections.synchronizedMap(new WeakHashMap<Object, String>());

    private static final Set<Object> cappedVideos =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Object, Boolean>()));

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
    private static Field videoDownloadNoWatermarkAddrField;
    private static Field videoDownloadAddrField;
    private static Field videoNewDownloadAddrField;
    private static Field videoUiAlikeAddrField;

    private static Method bitrateGetHeightMethod;
    private static Method bitrateGetGearNameMethod;
    private static Method bitrateGetPlayAddrMethod;
    private static Method videoGetVideoIdMethod;
    private static Method videoGetAidMethod;

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
        if (!isGovernorEnabled) return 0;
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
        // Retired: download quality is controlled by Media Usability (downloadQuality).
        // The patch injects downloadAllowedResolution = 0; ignore any legacy
        // SharedPreferences value so the old download ceiling cannot come back.
        if (!isGovernorEnabled) return 0;
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

    public static boolean isValidVideoId(String id) {
        if (id == null) return false;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return false;
        if ("null".equalsIgnoreCase(trimmed)) return false;
        if ("null_aweme_id".equalsIgnoreCase(trimmed)) return false;
        if ("null_video_id".equalsIgnoreCase(trimmed)) return false;
        if ("none".equalsIgnoreCase(trimmed)) return false;
        if ("undefined".equalsIgnoreCase(trimmed)) return false;
        if ("0".equals(trimmed)) return false;
        if ("-1".equals(trimmed)) return false;
        return true;
    }

    public static String getAwemeId(Object awemeObj) {
        if (awemeObj == null) return null;
        try {
            Method m = awemeObj.getClass().getMethod("getAid");
            Object aid = m.invoke(awemeObj);
            if (aid instanceof String && isValidVideoId((String) aid)) {
                return ((String) aid).trim();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String getVideoId(Object videoObj) {
        if (videoObj == null) return null;
        String mapped = videoToAwemeId.get(videoObj);
        if (isValidVideoId(mapped)) return mapped;

        try {
            if (videoGetVideoIdMethod != null) {
                Object id = videoGetVideoIdMethod.invoke(videoObj);
                if (id instanceof String && isValidVideoId((String) id)) {
                    return ((String) id).trim();
                }
            } else {
                Method m = videoObj.getClass().getMethod("getVideoId");
                Object id = m.invoke(videoObj);
                if (id instanceof String && isValidVideoId((String) id)) {
                    return ((String) id).trim();
                }
            }
        } catch (Throwable ignored) {}
        try {
            if (videoGetAidMethod != null) {
                Object aid = videoGetAidMethod.invoke(videoObj);
                if (aid instanceof String && isValidVideoId((String) aid)) {
                    return ((String) aid).trim();
                }
            } else {
                Method m = videoObj.getClass().getMethod("getAid");
                Object aid = m.invoke(videoObj);
                if (aid instanceof String && isValidVideoId((String) aid)) {
                    return ((String) aid).trim();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object getBestDownloadPlayAddr(Object videoObj) {
        if (!isGovernorEnabled || videoObj == null || getDownloadResolution() <= 0) return null;
        Object cached = uncappedDownloadAddrs.get(videoObj);
        if (cached != null) return cached;

        String vid = getVideoId(videoObj);
        if (isValidVideoId(vid)) {
            cached = videoIdToCappedPlayAddr.get(vid);
            if (cached != null) {
                uncappedDownloadAddrs.put(videoObj, cached);
                return cached;
            }
        }

        // Dynamic fallback: resolve directly from videoObj bitRateList if WeakHashMap lost reference
        try {
            ensureReflection(videoObj.getClass().getClassLoader());
            if (videoBitRateListField != null) {
                Object listObj = videoBitRateListField.get(videoObj);
                if (listObj instanceof List) {
                    Object bestBitrate = resolveBestBitrate((List) listObj, getDownloadResolution());
                    if (bestBitrate != null) {
                        Object playAddr = extractPlayAddrFromBitrate(bestBitrate);
                        if (playAddr != null) {
                            uncappedDownloadAddrs.put(videoObj, playAddr);
                            if (isValidVideoId(vid)) videoIdToCappedPlayAddr.put(vid, playAddr);
                            return playAddr;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Fallback: check if videoDownloadNoWatermarkAddrField was already populated with capped stream
        try {
            ensureReflection(videoObj.getClass().getClassLoader());
            if (videoDownloadNoWatermarkAddrField != null) {
                Object cur = videoDownloadNoWatermarkAddrField.get(videoObj);
                if (cur != null) {
                    uncappedDownloadAddrs.put(videoObj, cur);
                    return cur;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static Object enforceDownloadCap(Object currentUrl, Object videoObj) {
        if (!isGovernorEnabled || videoObj == null || getDownloadResolution() <= 0) return currentUrl;
        try {
            Object capped = getBestDownloadPlayAddr(videoObj);
            if (capped != null) {
                Log.i(TAG, "[Video Quality Governor] enforceDownloadCap applied capped stream (" + getDownloadResolution() + "p cap).");
                return capped;
            }
        } catch (Throwable ignored) {}
        return currentUrl;
    }

    public static boolean isValidResolution(int res) {
        return res == 0 || res == 360 || res == 480 || res == 540 || res == 720 || res == 1080;
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

                try {
                    videoDownloadNoWatermarkAddrField = videoClass.getDeclaredField("downloadNoWatermarkAddr");
                    videoDownloadNoWatermarkAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoDownloadAddrField = videoClass.getDeclaredField("downloadAddr");
                    videoDownloadAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoNewDownloadAddrField = videoClass.getDeclaredField("newDownloadAddr");
                    videoNewDownloadAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoUiAlikeAddrField = videoClass.getDeclaredField("uiAlikeAddr");
                    videoUiAlikeAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoGetVideoIdMethod = videoClass.getMethod("getVideoId");
                    videoGetVideoIdMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    videoGetAidMethod = videoClass.getMethod("getAid");
                    videoGetAidMethod.setAccessible(true);
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
        return videoObj.getClass().getName().contains("Video");
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

    /**
     * DASH renditions are video-only fragmented MP4; their audio is served separately
     * through Video.bitRateAudio, so saving one directly yields a silent file.
     */
    public static boolean isDashBitrate(Object bitrateObj) {
        if (bitrateObj == null) return false;
        try {
            Object dash = bitrateObj.getClass().getMethod("isDash").invoke(bitrateObj);
            if (Boolean.TRUE.equals(dash)) return true;
        } catch (Throwable ignored) {}
        try {
            Object fmt = bitrateObj.getClass().getMethod("getFormat").invoke(bitrateObj);
            if (fmt instanceof String && ((String) fmt).toLowerCase().contains("dash")) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * BitRate.isBytevc1() is a codec id (0 = H.264, 1 = ByteVC1/HEVC, 2 = ByteVC2), not a flag.
     * ByteVC2 is proprietary and cannot be decoded by gallery players, so it is never a download target.
     */
    public static boolean isBytevc2(Object bitrateObj) {
        if (bitrateObj == null) return false;
        try {
            Object res = bitrateObj.getClass().getMethod("isBytevc1").invoke(bitrateObj);
            if (res instanceof Integer && ((Integer) res) >= 2) return true;
        } catch (Throwable ignored) {}
        for (String getter : new String[]{"getFormat", "getGearName"}) {
            try {
                Object val = bitrateObj.getClass().getMethod(getter).invoke(bitrateObj);
                if (val instanceof String) {
                    String s = ((String) val).toLowerCase();
                    if (s.contains("bytevc2") || s.contains("bvc2")) return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /** Progressive (muxed audio + video) rendition in a codec that standard players can decode. */
    public static boolean isDownloadableBitrate(Object bitrateObj) {
        return !isDashBitrate(bitrateObj) && !isBytevc2(bitrateObj);
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
        String s = text.toLowerCase();
        if (s.contains("1080") || s.contains("extreme") || s.contains("hdr")) return 1080;
        if (s.contains("720") || s.contains("super")) return 720;
        if (s.contains("540") || s.contains("h_high")) return 540;
        if (s.contains("480")) return 480;
        if (s.contains("360") || s.contains("standard") || s.contains("lower")) return 360;
        if (s.contains("240") || s.contains("lowest")) return 240;
        return 0;
    }

    private static int normalizeResolution(int dim) {
        if (dim >= 1000) return 1080;
        if (dim >= 680) return 720;
        if (dim >= 500) return 540;
        if (dim >= 440) return 480;
        if (dim >= 300) return 360;
        if (dim >= 200) return 240;
        return dim;
    }

    public static int deriveBitrateFromBps(Object bitrateObj) {
        if (bitrateObj == null) return 0;
        try {
            Method mBitRate = bitrateObj.getClass().getMethod("getBitRate");
            Object brObj = mBitRate.invoke(bitrateObj);
            if (brObj instanceof Integer) {
                int br = (Integer) brObj;
                if (br >= 2000000) return 1080;
                if (br >= 1100000) return 720;
                if (br >= 700000) return 540;
                if (br >= 400000) return 480;
                if (br > 0) return 360;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int resolveBitrateHeight(Object bitrateObj) {
        if (bitrateObj == null) return 0;

        // 1. Try gearName string (authoritative tier label in TikTok API)
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
                    int res = parseResolutionFromString((String) gearObj);
                    if (res > 0) return res;
                }
            }
        } catch (Throwable ignored) {}

        // 2. Try playAddr dimensions and URI/URLs (UrlModel / SimUrlModel)
        try {
            Object playAddr = extractPlayAddrFromBitrate(bitrateObj);
            if (playAddr != null) {
                int w = -1, h = -1;
                try {
                    Method mW = playAddr.getClass().getMethod("getWidth");
                    Object resW = mW.invoke(playAddr);
                    if (resW instanceof Integer) w = (Integer) resW;
                } catch (Throwable ignored) {}
                try {
                    Method mH = playAddr.getClass().getMethod("getHeight");
                    Object resH = mH.invoke(playAddr);
                    if (resH instanceof Integer) h = (Integer) resH;
                } catch (Throwable ignored) {}

                if (w > 0 && h > 0) {
                    return normalizeResolution(Math.min(w, h));
                } else if (w > 0) {
                    return normalizeResolution(w);
                } else if (h > 0) {
                    return normalizeResolution(h);
                }

                try {
                    Method mUri = playAddr.getClass().getMethod("getUri");
                    Object uriObj = mUri.invoke(playAddr);
                    if (uriObj instanceof String) {
                        int res = parseResolutionFromString((String) uriObj);
                        if (res > 0) return res;
                    }
                } catch (Throwable ignored) {}

                try {
                    Method mUrls = playAddr.getClass().getMethod("getUrlList");
                    Object urlsObj = mUrls.invoke(playAddr);
                    if (urlsObj instanceof List) {
                        for (Object u : (List) urlsObj) {
                            if (u instanceof String) {
                                int res = parseResolutionFromString((String) u);
                                if (res > 0) return res;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // 3. Try quality string (e.g. from SimBitRate)
        try {
            Method mQuality = bitrateObj.getClass().getMethod("getQuality");
            Object qObj = mQuality.invoke(bitrateObj);
            if (qObj instanceof String) {
                int h = parseResolutionFromString((String) qObj);
                if (h > 0) return h;
            }
        } catch (Throwable ignored) {}

        // 4. Try direct videoWidth and videoHeight from bitrateObj
        try {
            int w = -1, h = -1;
            try {
                Method mW = bitrateObj.getClass().getMethod("getVideoWidth");
                Object resW = mW.invoke(bitrateObj);
                if (resW instanceof Integer) w = (Integer) resW;
            } catch (Throwable ignored) {}
            try {
                Method mH = null;
                try {
                    mH = bitrateObj.getClass().getMethod("getVideoHeight");
                } catch (Throwable ignored) {
                    mH = bitrateGetHeightMethod;
                }
                if (mH != null) {
                    Object resH = mH.invoke(bitrateObj);
                    if (resH instanceof Integer) h = (Integer) resH;
                }
            } catch (Throwable ignored) {}

            if (w > 0 && h > 0) {
                return normalizeResolution(Math.min(w, h));
            } else if (w > 0) {
                return normalizeResolution(w);
            } else if (h > 0) {
                return normalizeResolution(h);
            }
        } catch (Throwable ignored) {}

        // 5. Fallback: derive from bitrate bps
        return deriveBitrateFromBps(bitrateObj);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List dropByteVC2(List list) {
        if (!avoidByteVC2 || list == null || list.isEmpty()) {
            return list;
        }

        List filtered = new ArrayList();
        boolean hasVideo = false;
        int dropped = 0;

        for (Object item : list) {
            if (item == null) continue;
            if (isAudioBitrate(item)) {
                filtered.add(item);
            } else if (isBytevc2(item)) {
                dropped++;
            } else {
                filtered.add(item);
                hasVideo = true;
            }
        }

        if (!hasVideo || dropped == 0) {
            return list;
        }

        Log.i(TAG, "[Video Quality Governor] Dropped " + dropped + " ByteVC2 rendition(s) -> hardware decoder path");
        return filtered;
    }

    /**
     * Resolves stream pixel dimensions (width, height) from bitrate metadata
     * or its play address model. Unknown sides are -1.
     * Unlike resolveBitrateHeight, no ladder-tier normalization is applied.
     */
    private static int[] resolveBitrateDims(Object bitrateObj) {
        if (bitrateObj == null) return new int[]{-1, -1};
        int w = readIntGetter(bitrateObj, "getVideoWidth");
        int h = readIntGetter(bitrateObj, "getVideoHeight");
        if (w > 0 && h > 0) return new int[]{w, h};
        try {
            Object playAddr = extractPlayAddrFromBitrate(bitrateObj);
            if (playAddr != null) {
                int uw = readIntGetter(playAddr, "getWidth");
                int uh = readIntGetter(playAddr, "getHeight");
                if (uw > 0 || uh > 0) return new int[]{uw, uh};
            }
        } catch (Throwable ignored) {}
        if (w > 0 || h > 0) return new int[]{w, h};
        return new int[]{-1, -1};
    }

    /** Parent Video dimensions (authoritative server metadata), -1 when unknown. */
    private static int[] videoSize(Object videoObj) {
        if (videoObj == null) return new int[]{-1, -1};
        return new int[]{readIntGetter(videoObj, "getWidth"), readIntGetter(videoObj, "getHeight")};
    }

    private static int readIntGetter(Object target, String getter) {
        if (target == null) return -1;
        try {
            Object res = target.getClass().getMethod(getter).invoke(target);
            if (res instanceof Integer) return (Integer) res;
        } catch (Throwable ignored) {}
        return -1;
    }

    private static boolean supportsMime(MediaCodecInfo info, String mime) {
        try {
            for (String t : info.getSupportedTypes()) {
                if (mime.equalsIgnoreCase(t)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static int codecMaxSide(MediaCodecInfo info, String mime) {
        if (info == null || info.isEncoder() || !supportsMime(info, mime)) return -1;
        try {
            MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
            if (caps == null || caps.getVideoCapabilities() == null) return -1;
            MediaCodecInfo.VideoCapabilities vc = caps.getVideoCapabilities();
            return Math.max(vc.getSupportedWidths().getUpper(), vc.getSupportedHeights().getUpper());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static final Map<String, Integer> hwMaxLongSideCache =
        Collections.synchronizedMap(new HashMap<String, Integer>());

    /**
     * Queries MediaCodecList once per mime for the largest supported long side
     * across all device decoders. Returns -1 when unknown (fail-open: never drop).
     */
    private static int getHwMaxLongSide(String mime) {
        Integer cached = hwMaxLongSideCache.get(mime);
        if (cached != null) return cached;
        int result = -1;
        try {
            MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
            for (MediaCodecInfo info : list.getCodecInfos()) {
                int side = codecMaxSide(info, mime);
                if (side > result) result = side;
            }
        } catch (Throwable ignored) {}
        hwMaxLongSideCache.put(mime, result);
        return result;
    }

    /**
     * True when a known stream side exceeds every device decoder for its codec
     * family, meaning the hardware rejects it (observed as C2MtkVdec BAD VALUE loops
     * on 2160x3840 content). No orientation is assumed: the long side is always
     * >= any single known side. ByteVC2 is excluded: it uses ByteDance's CPU decoder.
     * Unknown dimensions or unknown hardware fail open (false).
     */
    private static boolean isUndecodableSize(Object item, int[] parentSize) {
        if (item == null || isBytevc2(item)) return false;
        int[] size = resolveBitrateDims(item);
        if (size[0] <= 0 && size[1] <= 0 && parentSize != null) {
            size = parentSize;
        }
        String mime = isBytevc1(item) ? "video/hevc" : "video/avc";
        int hwMax = getHwMaxLongSide(mime);
        if (hwMax <= 0) return false;
        return (size[0] > hwMax) || (size[1] > hwMax);
    }

    /**
     * When the ladder floor itself exceeds hardware capability, no playable video
     * rendition exists: returns an audio-only list so the player fails fast instead
     * of entering decoder-reject retry loops. Returns null to proceed normally.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List maybeDropUndecodable(List originalList, Object lowestVideoStream, int[] parentSize, boolean failsafeKept) {
        if (!dropUndecodableVideo || lowestVideoStream == null) return null;
        if (!isUndecodableSize(lowestVideoStream, parentSize)) {
            if (failsafeKept) {
                int[] size = resolveBitrateDims(lowestVideoStream);
                String dims = (size[0] > 0 || size[1] > 0) ? (size[0] + "x" + size[1]) : "unknown-size";
                String mime = isBytevc2(lowestVideoStream) ? "bytevc2" : (isBytevc1(lowestVideoStream) ? "video/hevc" : "video/avc");
                Log.i(TAG, "[Video Quality Governor] Ladder floor kept (failsafe): dims=" + dims + " hwMax=" + getHwMaxLongSide(mime) + "px mime=" + mime + ".");
            }
            return null;
        }
        List audioOnly = new ArrayList();
        int dropped = 0;
        for (Object item : originalList) {
            if (item == null) continue;
            if (isAudioBitrate(item)) {
                audioOnly.add(item);
            } else {
                dropped++;
            }
        }
        if (audioOnly.isEmpty()) return null;
        int[] size = resolveBitrateDims(lowestVideoStream);
        String dims = (size[0] > 0 || size[1] > 0) ? (size[0] + "x" + size[1]) : "unknown-size";
        Log.i(TAG, "[Video Quality Governor] Undecodable " + dims + " stream exceeds HW decoder -> dropped " + dropped + " video stream(s), audio-only fallback.");
        return audioOnly;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterBitrates(List originalList) {
        return filterBitratesEx(originalList, null);
    }

    /**
     * Filters a list of BitRate or SimBitRate objects, discarding any streams whose
     * resolution height exceeds maxAllowedResolution. videoObj (when available) lends
     * authoritative parent dimensions for ladder entries that hide their own size.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterBitratesEx(List originalList, Object videoObj) {
        if (originalList == null || originalList.isEmpty()) {
            return originalList;
        }

        originalList = dropByteVC2(originalList);

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

            if (!isAudio) {
                int h = resolveBitrateHeight(item);
                if (h <= 0) {
                    h = deriveBitrateFromBps(item);
                }
                if (h > 0) {
                    if (h < lowestVideoHeight) {
                        lowestVideoHeight = h;
                        lowestVideoStream = item;
                    }
                    if (h <= cap) {
                        filtered.add(item);
                        hasVideoInFiltered = true;
                    }
                }
            } else {
                // Preserve audio stream for DASH playback engine
                filtered.add(item);
            }
        }

        // Failsafe: if all video renditions exceeded the cap, keep the lowest available video stream
        boolean failsafeKept = !hasVideoInFiltered && lowestVideoStream != null;
        if (failsafeKept) {
            filtered.add(lowestVideoStream);
            hasVideoInFiltered = true;
        }

        // Undecodable guard: ladder floor exceeds hardware -> audio-only instead of retry loops
        List undecodableFallback = maybeDropUndecodable(originalList, lowestVideoStream, videoSize(videoObj), failsafeKept);
        if (undecodableFallback != null) {
            return undecodableFallback;
        }

        if (filtered.isEmpty() || !hasVideoInFiltered) {
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

        if (filtered.size() != originalList.size()) {
            Log.i(TAG, "[Video Quality Governor] filterBitrates applied: " + originalList.size() + " -> " + filtered.size() + " streams (cap " + cap + "p)");
        }
        return filtered;
    }

    private static void syncUrlModel(Object targetModel, Object sourceModel) {
        if (targetModel == null || sourceModel == null || targetModel == sourceModel) return;
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

            try {
                Method getUrlKey = sourceModel.getClass().getMethod("getUrlKey");
                Method setUrlKey = targetModel.getClass().getMethod("setUrlKey", String.class);
                Object key = getUrlKey.invoke(sourceModel);
                if (key instanceof String) {
                    setUrlKey.invoke(targetModel, key);
                }
            } catch (Throwable ignored) {}

            try {
                Method getFileHash = sourceModel.getClass().getMethod("getFileHash");
                Method setFileHash = targetModel.getClass().getMethod("setFileHash", String.class);
                Object hash = getFileHash.invoke(sourceModel);
                if (hash instanceof String) {
                    setFileHash.invoke(targetModel, hash);
                }
            } catch (Throwable ignored) {}
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
    private static Object findBestStreamBelowCap(List streams, int cap) {
        Object bestMatch = null;
        int bestHeight = 0;
        for (Object item : streams) {
            int h = resolveBitrateHeight(item);
            if (h <= 0) h = deriveBitrateFromBps(item);
            if (h <= cap && h > bestHeight) {
                bestHeight = h;
                bestMatch = item;
            }
        }
        return bestMatch;
    }

    @SuppressWarnings("rawtypes")
    private static Object findLowestStreamAboveCap(List streams, int cap) {
        Object lowestMatch = null;
        int lowestHeight = Integer.MAX_VALUE;
        for (Object item : streams) {
            int h = resolveBitrateHeight(item);
            if (h <= 0) h = deriveBitrateFromBps(item);
            if (h > cap && h < lowestHeight) {
                lowestHeight = h;
                lowestMatch = item;
            }
        }
        return lowestMatch;
    }

    @SuppressWarnings("rawtypes")
    private static Object resolveBestBitrate(List bitrates, int cap) {
        if (bitrates == null || bitrates.isEmpty()) return null;
        if (cap <= 0) cap = Integer.MAX_VALUE;

        List h264Streams = new ArrayList();
        List bytevcStreams = new ArrayList();

        for (Object item : bitrates) {
            if (item == null) continue;
            if (isAudioBitrate(item) || !isDownloadableBitrate(item)) continue;
            int h = resolveBitrateHeight(item);
            if (h <= 0) {
                h = deriveBitrateFromBps(item);
            }
            if (h > 0) {
                if (isBytevc1(item)) {
                    bytevcStreams.add(item);
                } else {
                    h264Streams.add(item);
                }
            }
        }

        // 1. Highest H.264 stream <= cap
        Object bestMatch = findBestStreamBelowCap(h264Streams, cap);
        if (bestMatch != null) return bestMatch;

        // 2. Highest ByteVC1 stream <= cap
        bestMatch = findBestStreamBelowCap(bytevcStreams, cap);
        if (bestMatch != null) return bestMatch;

        // 3. Failsafe if all streams exceed cap: find lowest resolution stream above cap
        Object lowestAbove = findLowestStreamAboveCap(h264Streams, cap);
        if (lowestAbove != null) return lowestAbove;

        lowestAbove = findLowestStreamAboveCap(bytevcStreams, cap);
        if (lowestAbove != null) return lowestAbove;

        if (!h264Streams.isEmpty()) return h264Streams.get(0);
        if (!bytevcStreams.isEmpty()) return bytevcStreams.get(0);
        return null;
    }

    /**
     * Resolves the capped playback address for a Video object without calling any
     * hooked getter (field reflection only), so it is safe to invoke from
     * Video.getPlayAddr()/getProperPlayAddr() return hooks without recursion.
     * Returns null when no capped stream applies.
     */
    @SuppressWarnings("rawtypes")
    private static Object findCappedPlayAddr(Object videoObj, int cap) {
        if (videoObj == null || cap <= 0 || videoBitRateListField == null) return null;
        try {
            Object listObj = videoBitRateListField.get(videoObj);
            if (!(listObj instanceof List)) return null;
            List originalList = (List) listObj;
            if (originalList.isEmpty()) return null;
            Object best = resolveBestBitrate(dropByteVC2(originalList), cap);
            if (best == null) return null;
            return extractPlayAddrFromBitrate(best);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String urlModelUri(Object urlModel) {
        if (urlModel == null) return null;
        try {
            Object uri = urlModel.getClass().getMethod("getUri").invoke(urlModel);
            if (uri instanceof String) return (String) uri;
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Enforces the playback resolution cap on a directly-read play address.
     * Covers every path that reads Video.getPlayAddr()/getProperPlayAddr(), including
     * the detail page, which bypasses Aweme.getVideo(). Mutates the returned
     * UrlModel in place to the best stream at or below the cap. Void return keeps
     * Dalvik verifier types intact at the hooked return points.
     */
    public static void enforcePlaybackCap(Object playAddr, Object videoObj) {
        if (!isGovernorEnabled || playAddr == null || videoObj == null) return;
        try {
            ensureReflection(videoObj.getClass().getClassLoader());
            int cap = getMaxResolution();
            Object bestAddr = findCappedPlayAddr(videoObj, cap);
            if (bestAddr == null || bestAddr == playAddr) return;
            String currentUri = urlModelUri(playAddr);
            if (currentUri != null && currentUri.equals(urlModelUri(bestAddr))) return;
            syncUrlModel(playAddr, bestAddr);
            Log.i(TAG, "[Video Quality Governor] Detail-path playAddr enforced to capped stream (cap " + cap + "p).");
        } catch (Throwable t) {
            Log.w(TAG, "[Video Quality Governor] enforcePlaybackCap note: " + t.getMessage());
        }
    }

    /**
     * Intercepts Video objects before playback to ensure both bitRateList and
     * the default play addresses (playAddrValue, playAddrBytevc1Value) obey the resolution cap,
     * while preserving the highest-quality stream matching download resolution ceiling for downloads.
     */
    public static void capVideoObject(Object videoObj) {
        capVideoObject(videoObj, null);
    }

    /**
     * Intercepts Video objects before playback to ensure both bitRateList and
     * the default play addresses (playAddrValue, playAddrBytevc1Value) obey the resolution cap,
     * while preserving the highest-quality stream matching download resolution ceiling for downloads.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void capVideoObject(Object videoObj, Object awemeObj) {
        if (videoObj == null || !isValidVideo(videoObj)) return;
        try {
            if (awemeObj != null) {
                String aid = getAwemeId(awemeObj);
                if (isValidVideoId(aid)) {
                    videoToAwemeId.put(videoObj, aid);
                    Object existingPlayAddr = uncappedDownloadAddrs.get(videoObj);
                    if (existingPlayAddr != null) {
                        videoIdToCappedPlayAddr.put(aid, existingPlayAddr);
                    }
                }
            }

            if (!isGovernorEnabled) return;

            if (cappedVideos.contains(videoObj)) return;
            ensureReflection(videoObj.getClass().getClassLoader());

            int cap = getMaxResolution();
            int downloadCap = getDownloadResolution();
            if (cap <= 0 && downloadCap <= 0) return;

            String vid = getVideoId(videoObj);

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

                    // Preserve H.264 stream matching download resolution ceiling for downloads
                    if (downloadCap > 0) {
                        Object bestDownloadBitrate = resolveBestBitrate(originalList, downloadCap);
                        if (bestDownloadBitrate != null) {
                            Object bestDownloadPlayAddr = extractPlayAddrFromBitrate(bestDownloadBitrate);
                            if (bestDownloadPlayAddr != null) {
                                uncappedDownloadAddrs.put(videoObj, bestDownloadPlayAddr);
                                if (isValidVideoId(vid)) {
                                    videoIdToCappedPlayAddr.put(vid, bestDownloadPlayAddr);
                                }

                                // Directly sync Video's download stream fields so all downloaders receive the capped stream
                                Field[] downloadFields = {
                                    videoDownloadNoWatermarkAddrField,
                                    videoDownloadAddrField,
                                    videoNewDownloadAddrField,
                                    videoUiAlikeAddrField
                                };
                                for (Field f : downloadFields) {
                                    if (f != null) {
                                        try {
                                            Object cur = f.get(videoObj);
                                            if (cur != null) {
                                                syncUrlModel(cur, bestDownloadPlayAddr);
                                            } else if (f.getType().isInstance(bestDownloadPlayAddr)) {
                                                f.set(videoObj, bestDownloadPlayAddr);
                                            }
                                        } catch (Throwable ignored) {}
                                    }
                                }

                                Log.i(TAG, "[Video Quality Governor] Saved capped download stream (" + resolveBitrateHeight(bestDownloadBitrate) + "p) for video " + vid + ".");
                            }
                        }
                    }

                    if (cap > 0) {
                        List cappedList = filterBitrates(originalList);
                        videoBitRateListField.set(videoObj, cappedList);
                        cappedVideos.add(videoObj);
                        Log.i(TAG, "[Video Quality Governor] Capped video " + vid + ": playback " + originalList.size() + " -> " + cappedList.size() + " streams (cap " + cap + "p), download cap " + downloadCap + "p.");

                        // Sync default play addresses strictly to the capped video streams without cross-codec contamination
                        if (!cappedList.isEmpty()) {
                            Object bestAllowedH264 = null;
                            Object bestAllowedBytevc1 = null;

                            for (Object item : cappedList) {
                                // Progressive play addresses must never receive video-only DASH or ByteVC2 URLs
                                if (isAudioBitrate(item) || !isDownloadableBitrate(item) || resolveBitrateHeight(item) <= 0) continue;
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
                    } else {
                        cappedVideos.add(videoObj);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Video Quality Governor] capVideoObject note: " + t.getMessage());
        }
    }
}
