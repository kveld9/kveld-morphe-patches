package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Runtime hook helper for TikTok media playback and usability controls.
 * Unblocks downloads on restricted videos, guarantees clean unwatermarked media streams,
 * and neutralizes client-side synthetic watermarks.
 */
@SuppressWarnings("unused")
public final class TikTokMediaHook {

    private static final String TAG = "MorpheTikTok";
    public static volatile boolean forceWatermarkFreeDownload = true;

    private static volatile boolean videoReflectionInitialized = false;
    private static Field downloadNoWatermarkAddrField;
    private static Field downloadAddrField;
    private static Field h264PlayAddrValueField;
    private static Field playAddrValueField;
    private static Field playAddrBytevc1ValueField;
    private static Method getPlayAddrMethod;
    private static Method getH264PlayAddrMethod;
    private static Method getProperPlayAddrMethod;
    private static Method getUrlListMethod;

    private TikTokMediaHook() {}

    private static void ensureVideoReflection(ClassLoader classLoader) {
        if (videoReflectionInitialized) return;
        synchronized (TikTokMediaHook.class) {
            if (videoReflectionInitialized) return;
            if (classLoader == null) {
                classLoader = TikTokMediaHook.class.getClassLoader();
            }
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) return;

            try {
                Class<?> videoClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.Video");
                try {
                    downloadNoWatermarkAddrField = videoClass.getDeclaredField("downloadNoWatermarkAddr");
                    downloadNoWatermarkAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    downloadAddrField = videoClass.getDeclaredField("downloadAddr");
                    downloadAddrField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    h264PlayAddrValueField = videoClass.getDeclaredField("h264PlayAddrValue");
                    h264PlayAddrValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    playAddrValueField = videoClass.getDeclaredField("playAddrValue");
                    playAddrValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    playAddrBytevc1ValueField = videoClass.getDeclaredField("playAddrBytevc1Value");
                    playAddrBytevc1ValueField.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    getPlayAddrMethod = videoClass.getMethod("getPlayAddr");
                    getPlayAddrMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    getH264PlayAddrMethod = videoClass.getMethod("getH264PlayAddr");
                    getH264PlayAddrMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    Method h264Alt = videoClass.getMethod("getPlayAddrH264");
                    h264Alt.setAccessible(true);
                    if (getH264PlayAddrMethod == null) getH264PlayAddrMethod = h264Alt;
                } catch (Throwable ignored) {}

                try {
                    getProperPlayAddrMethod = videoClass.getMethod("getProperPlayAddr");
                    getProperPlayAddrMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                try {
                    Class<?> urlModelClass = classLoader.loadClass("com.ss.android.ugc.aweme.base.model.UrlModel");
                    getUrlListMethod = urlModelClass.getMethod("getUrlList");
                    getUrlListMethod.setAccessible(true);
                } catch (Throwable ignored) {}

                videoReflectionInitialized = true;
                Log.i(TAG, "[Watermark Free] Video reflection initialized successfully.");
            } catch (Throwable t) {
                Log.w(TAG, "[Watermark Free] Could not initialize Video reflection: " + t.getMessage());
            }
        }
    }

    private static boolean hasUsableUrl(Object urlModel) {
        if (urlModel == null) return false;
        try {
            if (getUrlListMethod != null) {
                Object listObj = getUrlListMethod.invoke(urlModel);
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    for (Object item : list) {
                        if (item instanceof String) {
                            String s = ((String) item).trim();
                            if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                                return true;
                            }
                        }
                    }
                }
            }
            Method getUriMethod = urlModel.getClass().getMethod("getUri");
            getUriMethod.setAccessible(true);
            Object uriObj = getUriMethod.invoke(urlModel);
            if (uriObj instanceof String) {
                String uri = ((String) uriObj).trim();
                if (!uri.isEmpty() && !"null".equalsIgnoreCase(uri)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isPhotoMode(Object awemeObj) {
        if (awemeObj == null) return false;
        try {
            Method getAwemeTypeMethod = awemeObj.getClass().getMethod("getAwemeType");
            Object typeObj = getAwemeTypeMethod.invoke(awemeObj);
            if (typeObj instanceof Integer) {
                int type = (Integer) typeObj;
                if (type == 68 || type == 150) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Method isPhotoModeMethod = awemeObj.getClass().getMethod("isPhotoMode");
            Object res = isPhotoModeMethod.invoke(awemeObj);
            if (Boolean.TRUE.equals(res)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> extKtClass = awemeObj.getClass().getClassLoader().loadClass("com.ss.android.ugc.aweme.feed.model.AwemeExtKt");
            Method extMethod = extKtClass.getMethod("isPhotoMode", awemeObj.getClass());
            Object res = extMethod.invoke(null, awemeObj);
            if (Boolean.TRUE.equals(res)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Method getPhotoInfoMethod = awemeObj.getClass().getMethod("getPhotoModeImageInfo");
            Object info = getPhotoInfoMethod.invoke(awemeObj);
            if (info != null) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
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

    public static boolean isH264VideoUrlModel(Object model) {
        if (!hasUsableUrl(model)) return false;
        if (TikTokVideoQualityHook.isBytevcPlayAddr(model)) {
            return false;
        }
        try {
            Method mBytevc1 = model.getClass().getMethod("isBytevc1");
            Object res = mBytevc1.invoke(model);
            if (res instanceof Integer && ((Integer) res) != 0) return false;
            if (Boolean.TRUE.equals(res)) return false;
        } catch (Throwable ignored) {}

        try {
            Method mCodec = model.getClass().getMethod("getCodecType");
            Object codec = mCodec.invoke(model);
            if (codec instanceof Integer) {
                int c = ((Integer) codec);
                if (c == 1 || c == 2) return false;
            }
        } catch (Throwable ignored) {}

        try {
            Method mFormat = model.getClass().getMethod("getFormat");
            Object fmt = mFormat.invoke(model);
            if (fmt instanceof String) {
                String sFmt = ((String) fmt).toLowerCase();
                if (sFmt.contains("bytevc1") || sFmt.contains("bytevc2") || sFmt.contains("bvc2") || sFmt.contains("hevc") || sFmt.contains("h265") || sFmt.contains("vvc") || sFmt.contains("h266")) return false;
                if (sFmt.contains("audio") || sFmt.contains("m4a") || sFmt.contains("mp3")) return false;
            }
        } catch (Throwable ignored) {}

        try {
            Method mUri = model.getClass().getMethod("getUri");
            Object uriObj = mUri.invoke(model);
            if (uriObj instanceof String) {
                String uri = ((String) uriObj).toLowerCase();
                if (uri.contains("bytevc1") || uri.contains("bytevc2") || uri.contains("bvc2") || uri.contains("hevc") || uri.contains("h265") || uri.contains("vvc") || uri.contains("h266") || uri.contains("codec_type=bytevc1") || uri.contains("codec_type=bytevc2") || uri.contains("codec_type=bvc2")) return false;
                if (uri.contains("dash_audio") || uri.contains("audio_id=") || uri.endsWith(".m4a") || uri.endsWith(".mp3")) return false;
            }
        } catch (Throwable ignored) {}

        try {
            Method mUrls = model.getClass().getMethod("getUrlList");
            Object urlsObj = mUrls.invoke(model);
            if (urlsObj instanceof List) {
                for (Object u : (List) urlsObj) {
                    if (u instanceof String) {
                        String url = ((String) u).toLowerCase();
                        if (url.contains("bytevc1") || url.contains("bytevc2") || url.contains("bvc2") || url.contains("hevc") || url.contains("h265") || url.contains("vvc") || url.contains("h266") || url.contains("codec_type=bytevc1") || url.contains("codec_type=bytevc2") || url.contains("codec_type=bvc2")) return false;
                        if (url.contains("mime_type=audio") || url.contains("dash_audio") || url.endsWith(".m4a") || url.endsWith(".mp3")) return false;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return true;
    }

    private static Object selectCleanFallback(Object videoObj) {
        if (videoObj == null || !isValidVideo(videoObj)) return null;

        // 1. High-resolution stream preserved by Video Quality Governor (guaranteed H.264 video)
        try {
            Object governorStream = TikTokVideoQualityHook.getBestDownloadPlayAddr(videoObj);
            if (isH264VideoUrlModel(governorStream)) return governorStream;
        } catch (Throwable ignored) {}

        // 2. Native H.264 streams (matches LX/0oMC.LIZ)
        if (h264PlayAddrValueField != null) {
            try {
                Object model = h264PlayAddrValueField.get(videoObj);
                if (isH264VideoUrlModel(model)) return model;
            } catch (Throwable ignored) {}
        }

        if (getH264PlayAddrMethod != null) {
            try {
                Object model = getH264PlayAddrMethod.invoke(videoObj);
                if (isH264VideoUrlModel(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 3. Standard playAddr (default TikTok stream)
        if (playAddrValueField != null) {
            try {
                Object model = playAddrValueField.get(videoObj);
                if (isH264VideoUrlModel(model)) return model;
            } catch (Throwable ignored) {}
        }

        if (getPlayAddrMethod != null) {
            try {
                Object model = getPlayAddrMethod.invoke(videoObj);
                if (isH264VideoUrlModel(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 4. getProperPlayAddr()
        if (getProperPlayAddrMethod != null) {
            try {
                Object model = getProperPlayAddrMethod.invoke(videoObj);
                if (isH264VideoUrlModel(model)) return model;
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static void patchVideoObject(Object videoObj) {
        patchVideoObject(videoObj, null);
    }

    /**
     * Intercepts Aweme.getVideo() to ensure video.downloadNoWatermarkAddr is pre-populated
     * with the clean playback stream if missing from TikTok's API response.
     */
    public static void patchVideoObject(Object videoObj, Object awemeObj) {
        if (!forceWatermarkFreeDownload || videoObj == null || !isValidVideo(videoObj)) return;
        if (isPhotoMode(awemeObj)) {
            Log.i(TAG, "[Watermark Free] Skipped patchVideoObject for photo mode post.");
            return;
        }
        try {
            ensureVideoReflection(videoObj.getClass().getClassLoader());

            Object currentNoWatermark = null;
            if (downloadNoWatermarkAddrField != null) {
                currentNoWatermark = downloadNoWatermarkAddrField.get(videoObj);
            }

            if (isH264VideoUrlModel(currentNoWatermark)) {
                return;
            }

            Object cleanStream = selectCleanFallback(videoObj);
            if (cleanStream != null) {
                if (downloadNoWatermarkAddrField != null) {
                    downloadNoWatermarkAddrField.set(videoObj, cleanStream);
                }
                if (downloadAddrField != null) {
                    downloadAddrField.set(videoObj, cleanStream);
                }
                Log.i(TAG, "[Watermark Free] Injected unwatermarked stream into video.downloadNoWatermarkAddr.");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Watermark Free] Error in patchVideoObject: " + t.getMessage());
        }
    }

    /**
     * Intercepts Video.getDownloadAddr() and Video.getDownloadNoWatermarkAddr()
     * and returns the watermark-free stream.
     */
    public static Object getWatermarkFreeDownloadUrl(Object originalUrl, Object videoObj) {
        if (!forceWatermarkFreeDownload || videoObj == null || !isValidVideo(videoObj)) return originalUrl;
        try {
            ensureVideoReflection(videoObj.getClass().getClassLoader());

            // 1. Prefer high-resolution uncapped stream preserved by Video Quality Governor
            try {
                Object governorStream = TikTokVideoQualityHook.getBestDownloadPlayAddr(videoObj);
                if (isH264VideoUrlModel(governorStream)) {
                    Log.i(TAG, "[Watermark Free] Used high-resolution quality governor stream for download.");
                    return governorStream;
                }
            } catch (Throwable ignored) {}

            // 2. Prefer official clean unwatermarked stream if present and H.264
            if (downloadNoWatermarkAddrField != null) {
                Object currentNoWatermark = downloadNoWatermarkAddrField.get(videoObj);
                if (isH264VideoUrlModel(currentNoWatermark)) {
                    return currentNoWatermark;
                }
            }

            // 3. Select clean fallback adhering strictly to H.264 priority
            Object cleanStream = selectCleanFallback(videoObj);
            if (cleanStream != null) {
                Log.i(TAG, "[Watermark Free] Replaced download stream with clean playAddr.");
                return cleanStream;
            }
        } catch (Throwable ignored) {}
        return originalUrl;
    }

    /**
     * Safe client-side watermark transcoder bypass.
     * When client-side watermarking is triggered, copies the raw unwatermarked input file
     * directly to the output destination and invokes WaterMarkListener.onSuccess().
     */
    public static boolean handleWatermark(Object builderObj) {
        if (builderObj == null) return false;
        try {
            Class<?> builderClass = builderObj.getClass();
            Field inputPathField = builderClass.getDeclaredField("inputPath");
            Field outPathField = builderClass.getDeclaredField("outPath");
            Field listenerField = builderClass.getDeclaredField("listener");

            inputPathField.setAccessible(true);
            outPathField.setAccessible(true);
            listenerField.setAccessible(true);

            String inputPath = (String) inputPathField.get(builderObj);
            String outPath = (String) outPathField.get(builderObj);
            Object listener = listenerField.get(builderObj);

            if (inputPath == null || outPath == null) return false;

            File src = new File(inputPath);
            File dst = new File(outPath);

            if (!src.exists() || src.length() == 0) return false;

            // Ensure parent directories exist
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Copy unwatermarked input file directly to destination
            copyFile(src, dst);
            Log.i(TAG, "[Watermark Free] Copied clean video (" + src.length() + " bytes) directly to: " + outPath);

            // Notify listener of successful completion
            if (listener != null) {
                try {
                    Method onSuccessMethod = null;
                    try {
                        Class<?> listenerInterface = builderClass.getClassLoader().loadClass("com.ss.android.ugc.aweme.services.watermark.WaterMarkListener");
                        onSuccessMethod = listenerInterface.getMethod("onSuccess", String.class);
                    } catch (Throwable ignored) {
                        onSuccessMethod = listener.getClass().getMethod("onSuccess", String.class);
                    }
                    if (onSuccessMethod != null) {
                        onSuccessMethod.setAccessible(true);
                        onSuccessMethod.invoke(listener, outPath);
                        Log.i(TAG, "[Watermark Free] WaterMarkListener.onSuccess() invoked cleanly.");
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "[Watermark Free] Could not notify WaterMarkListener: " + t.getMessage());
                }
            }

            return true;
        } catch (Throwable t) {
            Log.w(TAG, "[Watermark Free] Failed to bypass watermark transcoder: " + t.getMessage());
            return false;
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        if (src.equals(dst)) return;
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }
}
