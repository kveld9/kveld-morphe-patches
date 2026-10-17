package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure runtime extension helper for the Video Fit patch.
 * Recalculates VideoAdaptionResult dimensions and offsets based on configured fitMode ("fit", "fill", "off").
 */
@SuppressWarnings("unused")
public final class TikTokVideoFitHook {

    private static final String TAG = "MorpheTikTok";

    public static volatile String fitMode = "off";

    // Bounded diagnostics: which video views reach fitted() per process (avoids log spam).
    private static final AtomicInteger diagCount = new AtomicInteger(0);

    private TikTokVideoFitHook() {}

    public static Object fitted(View view, Object originalResult) {
        return fitted(originalResult, view);
    }

    /**
     * Swaps an existing VideoAdaptionResult with a recalculation based on fitMode and system display metrics.
     *
     * @param originalResult VideoAdaptionResult instance from feed / adapt cell.
     * @return Transformed VideoAdaptionResult or originalResult if mode is off or dimensions invalid.
     */
    public static Object fitted(Object originalResult) {
        if (originalResult == null || fitMode == null || "off".equals(fitMode)) {
            return originalResult;
        }

        try {
            android.util.DisplayMetrics metrics = android.content.res.Resources.getSystem().getDisplayMetrics();
            if (metrics == null || metrics.widthPixels <= 0 || metrics.heightPixels <= 0) {
                Log.w(TAG, "[Video Fit] skip: unmeasured system display metrics");
                return originalResult;
            }
            return fittedInner(originalResult, metrics.widthPixels, metrics.heightPixels);
        } catch (Throwable t) {
            Log.w(TAG, "[Video Fit] display metrics resolution failed: " + t.getMessage());
            return originalResult;
        }
    }

    /**
     * Swaps an existing VideoAdaptionResult with a recalculation based on fitMode and target View bounds.
     *
     * @param originalResult VideoAdaptionResult instance from feed / adapt cell.
     * @param view Target View displaying the video content.
     * @return Transformed VideoAdaptionResult or originalResult if mode is off or dimensions invalid.
     */
    public static Object fitted(Object originalResult, View view) {
        if (originalResult == null || view == null || fitMode == null || "off".equals(fitMode)) {
            return originalResult;
        }
        if (diagCount.getAndIncrement() < 5) {
            Log.d(TAG, "[Video Fit] call mode=" + fitMode + " view=" + view.getClass().getName());
        }

        try {
            int[] container = resolveContainerSize(view);
            int containerW = container[0];
            int containerH = container[1];
            if (containerW <= 0 || containerH <= 0) {
                Log.w(TAG, "[Video Fit] skip: unmeasured container");
                return originalResult;
            }
            return fittedInner(originalResult, containerW, containerH);
        } catch (Throwable t) {
            Log.w(TAG, "[Video Fit] container resolution failed: " + t.getMessage());
            return originalResult;
        }
    }

    private static Object fittedInner(Object originalResult, int containerW, int containerH) {
        if (containerW <= 0 || containerH <= 0) {
            return originalResult;
        }

        try {
            Class<?> clazz = originalResult.getClass();
            Field widthField = clazz.getDeclaredField("width");
            widthField.setAccessible(true);
            int origW = widthField.getInt(originalResult);

            Field heightField = clazz.getDeclaredField("height");
            heightField.setAccessible(true);
            int origH = heightField.getInt(originalResult);

            if (origW <= 0 || origH <= 0) {
                return originalResult;
            }

            Field transXField = clazz.getDeclaredField("translateX");
            transXField.setAccessible(true);
            Object origTransX = transXField.get(originalResult);

            Field transYField = clazz.getDeclaredField("translateY");
            transYField.setAccessible(true);
            Object origTransY = transYField.get(originalResult);

            int newW;
            int newH;
            Float newTransX;
            Float newTransY;

            if ("fit".equals(fitMode)) {
                // Entire video visible inside container (pillarbox / letterbox)
                float scale = Math.min((float) containerW / origW, (float) containerH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                if (!isValidFit(newW, newH, containerW, containerH)) {
                    return originalResult;
                }
                newTransX = toFloat(origTransX);
                newTransY = toFloat(origTransY);
                // These offsets were computed for the original size, so reusing them
                // verbatim leaves the resized video misaligned. Re-derive them per axis
                // and only where the video was genuinely centred before; other offsets
                // are kept so unknown layouts are never guessed at.
                if (newTransX != null && newTransY != null) {
                    if (isCentered(origW, containerW, newTransX)) {
                        newTransX = (containerW - newW) / 2f;
                    }
                    if (isCentered(origH, containerH, newTransY)) {
                        newTransY = (containerH - newH) / 2f;
                    }
                }
            } else if ("fill".equals(fitMode)) {
                // Video fills container completely (center crop)
                float scale = Math.max((float) containerW / origW, (float) containerH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                if (!isValidFill(newW, newH, containerW, containerH)) {
                    return originalResult;
                }
                // Center the oversized view: translations from "fit" geometry
                // would leave the filled video off-center on later videos.
                newTransX = (containerW - newW) / 2f;
                newTransY = (containerH - newH) / 2f;
            } else {
                return originalResult;
            }

            // Always mutate fields in place first so callers that do not reassign
            // the returned instance still see the fitted dimensions.
            widthField.setInt(originalResult, newW);
            heightField.setInt(originalResult, newH);
            setFloatField(transXField, originalResult, newTransX);
            setFloatField(transYField, originalResult, newTransY);

            // Attempt copy(...) for callers that rely on a new instance
            try {
                for (Method m : clazz.getMethods()) {
                    if (!"copy".equals(m.getName())) {
                        continue;
                    }
                    Class<?>[] pts = m.getParameterTypes();
                    if (pts.length < 4 || pts[0] != int.class || pts[1] != int.class
                            || !isFloatLike(pts[2]) || !isFloatLike(pts[3])) {
                        continue;
                    }
                    Object[] args = new Object[pts.length];
                    args[0] = newW;
                    args[1] = newH;
                    args[2] = coerceFloat(pts[2], newTransX);
                    args[3] = coerceFloat(pts[3], newTransY);
                    boolean ok = true;
                    for (int i = 4; i < pts.length; i++) {
                        args[i] = defaultCopyArg(clazz, originalResult, pts[i]);
                        if (args[i] == INVALID_ARG) {
                            if (pts[i].isPrimitive()) {
                                ok = false;
                                break;
                            }
                            args[i] = null;
                        }
                    }
                    if (!ok) {
                        continue;
                    }
                    Object out = m.invoke(originalResult, args);
                    if (out != null) {
                        Log.i(TAG, "[Video Fit] " + fitMode + " " + origW + "x" + origH
                                + " -> " + newW + "x" + newH
                                + " container=" + containerW + "x" + containerH);
                        return out;
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "[Video Fit] copy failed, returning mutated original: " + t.getMessage());
            }

            Log.i(TAG, "[Video Fit] " + fitMode + " (in place) " + origW + "x" + origH
                    + " -> " + newW + "x" + newH
                    + " container=" + containerW + "x" + containerH);
            return originalResult;
        } catch (Throwable t) {
            Log.w(TAG, "[Video Fit] fitted swap failed: " + t.getMessage());
            return originalResult;
        }
    }


    private static final Object INVALID_ARG = new Object();

    private static boolean isFloatLike(Class<?> c) {
        return c == Float.class || c == float.class;
    }

    private static Float toFloat(Object o) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        return null;
    }

    private static Object coerceFloat(Class<?> target, Float value) {
        if (target == float.class) {
            return value == null ? 0f : value;
        }
        return value;
    }

    private static void setFloatField(Field f, Object obj, Float value) throws IllegalAccessException {
        if (f.getType() == float.class) {
            f.setFloat(obj, value == null ? 0f : value);
        } else {
            f.set(obj, value);
        }
    }

    /**
     * Default value for extra copy(...) parameters (e.g. resultOperator):
     * reuse the matching field value when types align, else JVM defaults.
     */
    private static Object defaultCopyArg(Class<?> clazz, Object obj, Class<?> paramType) {
        try {
            boolean hasOperator = hasField(clazz, "resultOperator");
            for (Field f : clazz.getDeclaredFields()) {
                if (paramType.isAssignableFrom(f.getType())) {
                    if (!hasOperator || "resultOperator".equals(f.getName())) {
                        f.setAccessible(true);
                        return f.get(obj);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (!paramType.isPrimitive()) {
            return null;
        }
        if (paramType == boolean.class) {
            return false;
        }
        if (paramType == int.class) {
            return 0;
        }
        return INVALID_ARG;
    }

    private static boolean hasField(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredField(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    private static int[] resolveContainerSize(View view) {
        int[] maxParent = getLargestParentSize(view);
        if (maxParent[0] > 0 && maxParent[1] > 0) {
            return maxParent;
        }
        int[] display = getDisplayMetricsSize(view);
        if (display[0] > 0 && display[1] > 0) {
            return display;
        }
        return new int[]{view.getWidth(), view.getHeight()};
    }

    private static int[] getLargestParentSize(View view) {
        int maxW = 0;
        int maxH = 0;
        android.view.ViewParent parent = view.getParent();
        while (parent instanceof View) {
            View p = (View) parent;
            int w = p.getWidth();
            int h = p.getHeight();
            if (w > 0 && h > 0 && (w * h > maxW * maxH)) {
                maxW = w;
                maxH = h;
            }
            parent = p.getParent();
        }
        return new int[]{maxW, maxH};
    }

    private static int[] getDisplayMetricsSize(View view) {
        if (view.getContext() == null || view.getContext().getResources() == null) {
            return new int[]{0, 0};
        }
        android.util.DisplayMetrics metrics = view.getContext().getResources().getDisplayMetrics();
        if (metrics == null) {
            return new int[]{0, 0};
        }
        return new int[]{metrics.widthPixels, metrics.heightPixels};
    }

    private static boolean isValidFit(int newW, int newH, int containerW, int containerH) {
        float ratioW = newW / (float) containerW;
        float ratioH = newH / (float) containerH;
        return Math.max(ratioW, ratioH) >= 0.9f;
    }

    private static boolean isValidFill(int newW, int newH, int containerW, int containerH) {
        return newW >= containerW * 0.9f && newH >= containerH * 0.9f;
    }

    /**
     * Whether an existing translation for {@code origSize} matches the offset that
     * centres it in {@code containerSize}. Layout rounding puts real-device offsets
     * a few pixels off the exact midpoint, so a small slack is tolerated.
     */
    private static boolean isCentered(int origSize, int containerSize, float offset) {
        return origSize == containerSize
                || Math.abs(offset - (containerSize - origSize) / 2f) <= Math.max(2f, containerSize * 0.02f);
    }
}
