package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Visual indicator hook for active Camera and Microphone hardware usage in TikTok.
 * Renders an on-screen corner indicator (green square for camera, orange diamond for mic).
 */
public final class TikTokCameraMicHook {
    private static final String TAG = "MorpheTikTok";

    private static final AtomicInteger cameraCount = new AtomicInteger(0);
    private static final AtomicInteger micCount = new AtomicInteger(0);

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static volatile WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static volatile IndicatorView indicatorView = null;

    private TikTokCameraMicHook() {}

    public static void install(Activity activity) {
        if (activity == null) return;
        activityRef = new WeakReference<>(activity);
        mainHandler.post(() -> attachIndicatorIfPossible(activity));
    }

    public static void onCameraOpened(Camera camera) {
        if (camera == null) {
            return;
        }
        int count = cameraCount.incrementAndGet();
        Log.d(TAG, "[Camera Mic Indicator] Camera opened (active=" + count + ")");
        updateIndicator();
    }

    public static void onCameraStart() {
        int count = cameraCount.incrementAndGet();
        Log.d(TAG, "[Camera Mic Indicator] Camera started (active=" + count + ")");
        updateIndicator();
    }

    public static void onCameraStop() {
        int count = cameraCount.updateAndGet(c -> Math.max(0, c - 1));
        Log.d(TAG, "[Camera Mic Indicator] Camera stopped (active=" + count + ")");
        updateIndicator();
    }

    public static void onMicStart() {
        int count = micCount.incrementAndGet();
        Log.d(TAG, "[Camera Mic Indicator] Mic started (active=" + count + ")");
        updateIndicator();
    }

    public static void onMicStop() {
        int count = micCount.updateAndGet(c -> Math.max(0, c - 1));
        Log.d(TAG, "[Camera Mic Indicator] Mic stopped (active=" + count + ")");
        updateIndicator();
    }

    private static void updateIndicator() {
        mainHandler.post(() -> {
            Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (indicatorView == null) {
                attachIndicatorIfPossible(activity);
            }
            if (indicatorView != null) {
                indicatorView.update(cameraCount.get() > 0, micCount.get() > 0);
            }
        });
    }

    private static void attachIndicatorIfPossible(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) return;
            ViewGroup root = (ViewGroup) decor;

            if (indicatorView != null && indicatorView.getParent() != null) {
                if (indicatorView.getContext() == activity) {
                    return;
                }
                ((ViewGroup) indicatorView.getParent()).removeView(indicatorView);
                indicatorView = null;
            }

            IndicatorView view = new IndicatorView(activity);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dpToPx(activity, 48),
                dpToPx(activity, 24)
            );
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.topMargin = dpToPx(activity, 12);
            lp.rightMargin = dpToPx(activity, 12);
            view.setLayoutParams(lp);

            root.addView(view);
            indicatorView = view;
            indicatorView.update(cameraCount.get() > 0, micCount.get() > 0);
        } catch (Throwable t) {
            Log.w(TAG, "[Camera Mic Indicator] Failed to attach indicator view", t);
        }
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private static final class IndicatorView extends View {
        private final Paint cameraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint micPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path diamondPath = new Path();

        private boolean showCamera = false;
        private boolean showMic = false;

        IndicatorView(Context context) {
            super(context);
            // Green square for camera (#00E676)
            cameraPaint.setColor(Color.parseColor("#00E676"));
            cameraPaint.setStyle(Paint.Style.FILL);

            // Orange diamond for mic (#FF9100)
            micPaint.setColor(Color.parseColor("#FF9100"));
            micPaint.setStyle(Paint.Style.FILL);

            setVisibility(GONE);
        }

        void update(boolean cameraActive, boolean micActive) {
            this.showCamera = cameraActive;
            this.showMic = micActive;
            if (showCamera || showMic) {
                setVisibility(VISIBLE);
                bringToFront();
                invalidate();
            } else {
                setVisibility(GONE);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!showCamera && !showMic) return;

            float dotSize = getHeight() * 0.55f;
            float margin = (getHeight() - dotSize) / 2f;
            float leftOffset = margin;

            if (showCamera) {
                // Green square
                canvas.drawRect(leftOffset, margin, leftOffset + dotSize, margin + dotSize, cameraPaint);
                leftOffset += dotSize + margin;
            }

            if (showMic) {
                // Orange diamond
                float cx = leftOffset + dotSize / 2f;
                float cy = margin + dotSize / 2f;
                float half = dotSize / 2f;

                diamondPath.reset();
                diamondPath.moveTo(cx, cy - half);
                diamondPath.lineTo(cx + half, cy);
                diamondPath.lineTo(cx, cy + half);
                diamondPath.lineTo(cx - half, cy);
                diamondPath.close();

                canvas.drawPath(diamondPath, micPaint);
            }
        }
    }
}
