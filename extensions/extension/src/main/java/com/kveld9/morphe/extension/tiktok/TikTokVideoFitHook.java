package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import android.view.View;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Pure runtime extension helper for the Video Fit patch.
 * Recalculates VideoAdaptionResult dimensions and offsets based on configured fitMode ("fit", "fill", "off").
 */
@SuppressWarnings("unused")
public final class TikTokVideoFitHook {

    private static final String TAG = "MorpheTikTok";

    public static volatile String fitMode = "off";

    private TikTokVideoFitHook() {}

    public static Object fitted(View view, Object originalResult) {
        return fitted(originalResult, view);
    }

    /**
     * Swaps an existing VideoAdaptionResult with a recalculation based on fitMode and target View bounds.
     *
     * @param originalResult VideoAdaptionResult instance from feed / adapt cell.
     * @param view Target View displaying the video content.
     * @return Transformed VideoAdaptionResult or originalResult if mode is off or dimensions invalid.
     */
    public static Object fitted(Object originalResult, View view) {
        if (originalResult == null || view == null || "off".equals(fitMode) || fitMode == null) {
            return originalResult;
        }

        try {
            int viewW = view.getWidth();
            int viewH = view.getHeight();
            if (viewW <= 0 || viewH <= 0) {
                return originalResult;
            }

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

            int newW;
            int newH;
            float transX;
            float transY;

            if ("fit".equals(fitMode)) {
                // Entire video visible inside container (pillarbox / letterbox)
                float scale = Math.min((float) viewW / origW, (float) viewH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                transX = (viewW - newW) / 2.0f;
                transY = (viewH - newH) / 2.0f;
            } else if ("fill".equals(fitMode)) {
                // Video fills container completely (center crop)
                float scale = Math.max((float) viewW / origW, (float) viewH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                transX = (viewW - newW) / 2.0f;
                transY = (viewH - newH) / 2.0f;
            } else {
                return originalResult;
            }

            // Attempt copy method first (Kotlin data class copy)
            try {
                for (Method m : clazz.getMethods()) {
                    if ("copy".equals(m.getName()) && m.getParameterTypes().length >= 4) {
                        Class<?>[] pts = m.getParameterTypes();
                        boolean isBoxed = pts[0] == int.class && pts[1] == int.class
                                && pts[2] == Float.class && pts[3] == Float.class;
                        boolean isPrimitive = pts[0] == int.class && pts[1] == int.class
                                && pts[2] == float.class && pts[3] == float.class;
                        if (isBoxed || isPrimitive) {
                            Float boxedX = Float.valueOf(transX);
                            Float boxedY = Float.valueOf(transY);
                            if (pts.length == 4) {
                                return m.invoke(originalResult, newW, newH, boxedX, boxedY);
                            } else if (pts.length == 6) {
                                // Default args copy(width, height, transX, transY, mask, marker)
                                return m.invoke(originalResult, newW, newH, boxedX, boxedY, 0, null);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Fallback: reflectively mutate fields in place
            widthField.setInt(originalResult, newW);
            heightField.setInt(originalResult, newH);

            Field transXField = clazz.getDeclaredField("translateX");
            transXField.setAccessible(true);
            transXField.set(originalResult, Float.valueOf(transX));

            Field transYField = clazz.getDeclaredField("translateY");
            transYField.setAccessible(true);
            transYField.set(originalResult, Float.valueOf(transY));

            Log.d(TAG, "[Video Fit] Fitted result: " + newW + "x" + newH + " trans=(" + transX + "," + transY + ")");
            return originalResult;
        } catch (Throwable t) {
            Log.w(TAG, "[Video Fit] fitted swap failed: " + t.getMessage());
            return originalResult;
        }
    }

    /**
     * Computes fitted width for story/feed cell layout.
     * Returns negative value if inputs are unmeasurable or mode is off.
     */
    public static int fitWidthFor(int width, int height, View view) {
        if (width <= 0 || height <= 0 || view == null || "off".equals(fitMode)) {
            return -1;
        }
        int viewW = view.getWidth();
        int viewH = view.getHeight();
        if (viewW <= 0 || viewH <= 0) return -1;

        if ("fit".equals(fitMode)) {
            float scale = Math.min((float) viewW / width, (float) viewH / height);
            return Math.round(width * scale);
        } else if ("fill".equals(fitMode)) {
            float scale = Math.max((float) viewW / width, (float) viewH / height);
            return Math.round(width * scale);
        }
        return -1;
    }

    /**
     * Computes fitted height for story/feed cell layout.
     * Returns negative value if inputs are unmeasurable or mode is off.
     */
    public static int fittedHeightFor(int width, int height, View view) {
        if (width <= 0 || height <= 0 || view == null || "off".equals(fitMode)) {
            return -1;
        }
        int viewW = view.getWidth();
        int viewH = view.getHeight();
        if (viewW <= 0 || viewH <= 0) return -1;

        if ("fit".equals(fitMode)) {
            float scale = Math.min((float) viewW / width, (float) viewH / height);
            return Math.round(height * scale);
        } else if ("fill".equals(fitMode)) {
            float scale = Math.max((float) viewW / width, (float) viewH / height);
            return Math.round(height * scale);
        }
        return -1;
    }

    /**
     * Computes fitted translation offset.
     */
    public static float fittedTranslation(float translation, View view) {
        if (view == null || "off".equals(fitMode)) {
            return translation;
        }
        return translation;
    }
}
