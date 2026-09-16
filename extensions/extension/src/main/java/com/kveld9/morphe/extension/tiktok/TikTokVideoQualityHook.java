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

/**
 * Runtime hook helper for TikTok video playback resolution capping.
 * Intercepts video bitrate lists and play address models to enforce user-defined
 * resolution ceilings (1080p, 720p, 540p, 480p, 360p), conserving device thermals,
 * GPU/MediaCodec load, RAM GraphicBuffers, and mobile data.
 */
@SuppressWarnings("unused")
public final class TikTokVideoQualityHook {

    private static final String TAG = "MorpheTikTok";
    private static final String PREFS_NAME = "morphe_tiktok_quality_prefs";
    private static final String KEY_MAX_QUALITY = "max_video_quality";

    public static volatile int maxAllowedResolution = 480;

    private static volatile boolean reflectionInitialized = false;
    private static Field videoBitRateListField;
    private static Field videoPlayAddrValueField;
    private static Field videoPlayAddrBytevc1ValueField;

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

        // Fallback: parse resolution string from gearName or quality
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
                    String gear = (String) gearObj;
                    if (gear.contains("1080")) return 1080;
                    if (gear.contains("720")) return 720;
                    if (gear.contains("540")) return 540;
                    if (gear.contains("480")) return 480;
                    if (gear.contains("360")) return 360;
                    if (gear.contains("240")) return 240;
                }
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
        Object lowestStream = null;
        int lowestHeight = Integer.MAX_VALUE;

        for (Object item : originalList) {
            int h = resolveBitrateHeight(item);
            if (h > 0 && h < lowestHeight) {
                lowestHeight = h;
                lowestStream = item;
            }

            if (h <= 0 || h <= cap) {
                filtered.add(item);
            }
        }

        // Failsafe: if all renditions exceeded the cap, keep the lowest available stream
        if (filtered.isEmpty()) {
            if (lowestStream != null) {
                filtered.add(lowestStream);
            } else {
                return originalList;
            }
        }

        // Sort surviving streams descending by resolution height so the best allowed stream is first
        Collections.sort(filtered, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int h1 = resolveBitrateHeight(o1);
                int h2 = resolveBitrateHeight(o2);
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

    /**
     * Intercepts Video objects before playback to ensure both bitRateList and
     * the default play addresses (playAddrValue, playAddrBytevc1Value) obey the resolution cap.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void capVideoObject(Object videoObj) {
        if (videoObj == null) return;
        try {
            ensureReflection(videoObj.getClass().getClassLoader());

            int cap = getMaxResolution();
            if (cap <= 0) return;

            if (videoBitRateListField != null) {
                Object listObj = videoBitRateListField.get(videoObj);
                if (listObj instanceof List) {
                    List originalList = (List) listObj;
                    List cappedList = filterBitrates(originalList);
                    videoBitRateListField.set(videoObj, cappedList);

                    // Sync default play addresses to the capped stream
                    if (!cappedList.isEmpty()) {
                        Object bestAllowedBitrate = cappedList.get(0);
                        Method getPlayAddrMethod = null;
                        try {
                            getPlayAddrMethod = bestAllowedBitrate.getClass().getMethod("getPlayAddr");
                        } catch (Throwable ignored) {
                            getPlayAddrMethod = bitrateGetPlayAddrMethod;
                        }

                        if (getPlayAddrMethod != null) {
                            Object cappedPlayAddr = getPlayAddrMethod.invoke(bestAllowedBitrate);
                            if (cappedPlayAddr != null) {
                                if (videoPlayAddrValueField != null) {
                                    Object currentPlay = videoPlayAddrValueField.get(videoObj);
                                    if (currentPlay != null) {
                                        syncUrlModel(currentPlay, cappedPlayAddr);
                                    } else if (videoPlayAddrValueField.getType().isInstance(cappedPlayAddr)) {
                                        videoPlayAddrValueField.set(videoObj, cappedPlayAddr);
                                    }
                                }
                                if (videoPlayAddrBytevc1ValueField != null) {
                                    Object currentBytevc1 = videoPlayAddrBytevc1ValueField.get(videoObj);
                                    if (currentBytevc1 != null) {
                                        syncUrlModel(currentBytevc1, cappedPlayAddr);
                                    } else if (videoPlayAddrBytevc1ValueField.getType().isInstance(cappedPlayAddr)) {
                                        videoPlayAddrBytevc1ValueField.set(videoObj, cappedPlayAddr);
                                    }
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
