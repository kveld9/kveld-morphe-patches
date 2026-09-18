package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public final class TikTokRefreshRateHook {
    private static final String TAG = "MorpheTikTok";
    public static volatile float targetRefreshRate = 0f;

    private TikTokRefreshRateHook() {}

    public static float resolveTargetRate(Activity activity) {
        if (targetRefreshRate > 0f) {
            return targetRefreshRate;
        }
        if (activity == null) {
            return 120f;
        }
        try {
            Display display;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display = activity.getDisplay();
            } else {
                WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
                display = wm != null ? wm.getDefaultDisplay() : null;
            }
            if (display != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Display.Mode[] modes = display.getSupportedModes();
                    if (modes != null && modes.length > 0) {
                        float maxRate = 60f;
                        for (Display.Mode m : modes) {
                            float r = m.getRefreshRate();
                            if (r > maxRate) {
                                maxRate = r;
                            }
                        }
                        return maxRate;
                    }
                }
                float[] rates = display.getSupportedRefreshRates();
                if (rates != null && rates.length > 0) {
                    float maxRate = 60f;
                    for (float r : rates) {
                        if (r > maxRate) {
                            maxRate = r;
                        }
                    }
                    return maxRate;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Refresh Rate Governor] Failed to query display refresh rates: " + t.getMessage());
        }
        return 120f;
    }

    public static void applyToWindow(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            WindowManager.LayoutParams params = window.getAttributes();
            if (params == null) {
                return;
            }
            float rate = resolveTargetRate(activity);
            if (params.preferredRefreshRate != rate) {
                params.preferredRefreshRate = rate;
                window.setAttributes(params);
                Log.i(TAG, "[Refresh Rate Governor] Locked window preferredRefreshRate to " + rate + " Hz");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Refresh Rate Governor] Failed to apply refresh rate: " + t.getMessage());
        }
    }
}
