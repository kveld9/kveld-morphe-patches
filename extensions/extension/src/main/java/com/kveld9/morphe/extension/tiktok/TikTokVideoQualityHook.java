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
import java.util.LinkedHashMap;
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

    private static final Map<String, Object> videoIdToCappedPlayAddr =
        Collections.synchronizedMap(new LinkedHashMap<String, Object>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > 100;
            }
        });

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

    private static String getVideoId(Object videoObj) {
        if (videoObj == null) return null;
        try {
            if (videoGetVideoIdMethod != null) {
                Object id = videoGetVideoIdMethod.invoke(videoObj);
                if (id instanceof String && !((String) id).isEmpty()) return (String) id;
            } else {
                Method m = videoObj.getClass().getMethod("getVideoId");
                Object id = m.invoke(videoObj);
                if (id instanceof String && !((String) id).isEmpty()) return (String) id;
            }
        } catch (Throwable ignored) {}
        try {
            if (videoGetAidMethod != null) {
                Object aid = videoGetAidMethod.invoke(videoObj);
                if (aid instanceof String && !((String) aid).isEmpty()) return (String) aid;
            } else {
                Method m = videoObj.getClass().getMethod("getAid");
                Object aid = m.invoke(videoObj);
                if (aid instanceof String && !((String) aid).isEmpty()) return (String) aid;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static Object getBestDownloadPlayAddr(Object videoObj) {
        if (videoObj == null) return null;
        Object cached = uncappedDownloadAddrs.get(videoObj);
        if (cached != null) return cached;

        String vid = getVideoId(videoObj);
        if (vid != null) {
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
                            if (vid != null) videoIdToCappedPlayAddr.put(vid, playAddr);
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
                    return cur;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static Object enforceDownloadCap(Object currentUrl, Object videoObj) {
        if (videoObj == null) return currentUrl;
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

        if (filtered.size() != originalList.size()) {
            Log.i(TAG, "[Video Quality Governor] filterBitrates applied: " + originalList.size() + " -> " + filtered.size() + " streams (cap " + cap + "p)");
        }
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

        List h264Streams = new ArrayList();
        List bytevcStreams = new ArrayList();

        for (Object item : bitrates) {
            if (item == null) continue;
            if (isAudioBitrate(item)) continue;
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
     * Intercepts Video objects before playback to ensure both bitRateList and
     * the default play addresses (playAddrValue, playAddrBytevc1Value) obey the resolution cap,
     * while preserving the highest-quality stream matching download resolution ceiling for downloads.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void capVideoObject(Object videoObj) {
        if (videoObj == null || !isValidVideo(videoObj)) return;
        try {
            ensureReflection(videoObj.getClass().getClassLoader());
            String vid = getVideoId(videoObj);
            if (vid != null && videoIdToCappedPlayAddr.containsKey(vid)) {
                return;
            }

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

                    // Preserve H.264 stream matching download resolution ceiling for downloads
                    Object bestDownloadBitrate = resolveBestBitrate(originalList, getDownloadResolution());
                    if (bestDownloadBitrate != null) {
                        Object bestDownloadPlayAddr = extractPlayAddrFromBitrate(bestDownloadBitrate);
                        if (bestDownloadPlayAddr != null) {
                            uncappedDownloadAddrs.put(videoObj, bestDownloadPlayAddr);
                            if (vid != null) {
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

                    List cappedList = filterBitrates(originalList);
                    videoBitRateListField.set(videoObj, cappedList);
                    Log.i(TAG, "[Video Quality Governor] Capped video " + vid + ": playback " + originalList.size() + " -> " + cappedList.size() + " streams (cap " + cap + "p), download cap " + getDownloadResolution() + "p.");

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
