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
            int[] container = resolveContainerSize(view);
            int containerW = container[0];
            int containerH = container[1];
            if (containerW <= 0 || containerH <= 0) {
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
                float scale = Math.min((float) containerW / origW, (float) containerH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                transX = (containerW - newW) / 2.0f;
                transY = (containerH - newH) / 2.0f;
                if (!isValidFit(newW, newH, containerW, containerH)) {
                    return originalResult;
                }
            } else if ("fill".equals(fitMode)) {
                // Video fills container completely (center crop)
                float scale = Math.max((float) containerW / origW, (float) containerH / origH);
                newW = Math.round(origW * scale);
                newH = Math.round(origH * scale);
                transX = (containerW - newW) / 2.0f;
                transY = (containerH - newH) / 2.0f;
                if (!isValidFill(newW, newH, containerW, containerH)) {
                    return originalResult;
                }
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
}
