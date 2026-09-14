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

    private static Object selectCleanFallback(Object videoObj) {
        if (videoObj == null) return null;

        // 1. Try h264PlayAddrValue field
        if (h264PlayAddrValueField != null) {
            try {
                Object model = h264PlayAddrValueField.get(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 2. Try getH264PlayAddr()
        if (getH264PlayAddrMethod != null) {
            try {
                Object model = getH264PlayAddrMethod.invoke(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 3. Try playAddrValue field
        if (playAddrValueField != null) {
            try {
                Object model = playAddrValueField.get(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 4. Try getPlayAddr()
        if (getPlayAddrMethod != null) {
            try {
                Object model = getPlayAddrMethod.invoke(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 5. Try playAddrBytevc1Value field
        if (playAddrBytevc1ValueField != null) {
            try {
                Object model = playAddrBytevc1ValueField.get(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        // 6. Try getProperPlayAddr()
        if (getProperPlayAddrMethod != null) {
            try {
                Object model = getProperPlayAddrMethod.invoke(videoObj);
                if (hasUsableUrl(model)) return model;
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Intercepts Aweme.getVideo() to ensure video.downloadNoWatermarkAddr is pre-populated
     * with the clean playback stream if missing from TikTok's API response.
     */
    public static void patchVideoObject(Object videoObj) {
        if (!forceWatermarkFreeDownload || videoObj == null) return;
        try {
            ensureVideoReflection(videoObj.getClass().getClassLoader());

            Object currentNoWatermark = null;
            if (downloadNoWatermarkAddrField != null) {
                currentNoWatermark = downloadNoWatermarkAddrField.get(videoObj);
            }

            if (hasUsableUrl(currentNoWatermark)) {
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
        if (!forceWatermarkFreeDownload || videoObj == null) return originalUrl;
        try {
            ensureVideoReflection(videoObj.getClass().getClassLoader());

            if (downloadNoWatermarkAddrField != null) {
                Object currentNoWatermark = downloadNoWatermarkAddrField.get(videoObj);
                if (hasUsableUrl(currentNoWatermark)) {
                    return currentNoWatermark;
                }
            }

            Object cleanStream = selectCleanFallback(videoObj);
            if (cleanStream != null) {
                Log.i(TAG, "[Watermark Free] Replaced download stream with unwatermarked playAddr.");
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
