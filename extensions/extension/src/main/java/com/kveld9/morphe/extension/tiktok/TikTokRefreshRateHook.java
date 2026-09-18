package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TikTokRefreshRateHook {
    private static final String TAG = "MorpheTikTok";
    public static volatile float targetRefreshRate = 0f;

    private TikTokRefreshRateHook() {}

    private static Display getDisplay(Activity activity) {
        if (activity == null) {
            return null;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return activity.getDisplay();
            }
            WindowManager wm = activity.getWindowManager();
            if (wm != null) {
                return wm.getDefaultDisplay();
            }
            wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            if (wm != null) {
                return wm.getDefaultDisplay();
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Refresh Rate Governor] Failed to query display: " + t.getMessage());
        }
        return null;
    }

    private static List<Float> getSupportedRates(Display display) {
        List<Float> result = new ArrayList<Float>();
        if (display == null) {
            return result;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Display.Mode[] modes = display.getSupportedModes();
                if (modes != null) {
                    for (Display.Mode mode : modes) {
                        if (mode != null && mode.getRefreshRate() > 0f && !result.contains(mode.getRefreshRate())) {
                            result.add(mode.getRefreshRate());
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                float[] rates = display.getSupportedRefreshRates();
                if (rates != null) {
                    for (float r : rates) {
                        if (r > 0f && !result.contains(r)) {
                            result.add(r);
                        }
                    }
                }
            }
            float currentRate = display.getRefreshRate();
            if (currentRate > 0f && !result.contains(currentRate)) {
                result.add(currentRate);
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Refresh Rate Governor] Failed to query display refresh rates: " + t.getMessage());
        }
        Collections.sort(result);
        return result;
    }

    private static float getMaxSupportedRate(List<Float> supportedRates, Display display) {
        if (!supportedRates.isEmpty()) {
            return supportedRates.get(supportedRates.size() - 1);
        }
        if (display != null) {
            try {
                float current = display.getRefreshRate();
                if (current > 60f) {
                    return current;
                }
            } catch (Throwable ignored) {}
        }
        return 60f;
    }

    private static float findBestMatch(List<Float> supportedRates, float desiredRate) {
        if (supportedRates.isEmpty()) {
            return desiredRate;
        }
        float bestMatch = supportedRates.get(0);
        for (Float rate : supportedRates) {
            if (Math.abs(rate - desiredRate) <= 1.0f) {
                return rate;
            }
            if (rate <= desiredRate) {
                bestMatch = rate;
            }
        }
        return bestMatch;
    }

    public static float resolveTargetRate(Activity activity) {
        Display display = getDisplay(activity);
        List<Float> supportedRates = getSupportedRates(display);
        float maxSupportedRate = getMaxSupportedRate(supportedRates, display);
        float desiredRate = targetRefreshRate;

        if (desiredRate <= 0f) {
            return maxSupportedRate;
        }

        if (desiredRate > maxSupportedRate) {
            Log.w(TAG, "[Refresh Rate Governor] Target " + desiredRate + " Hz exceeds physical display peak "
                    + maxSupportedRate + " Hz; clamping to " + maxSupportedRate + " Hz to prevent crash");
            return maxSupportedRate;
        }

        return findBestMatch(supportedRates, desiredRate);
    }

    public static void applyToWindow(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
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
            if (Math.abs(params.preferredRefreshRate - rate) > 0.01f) {
                params.preferredRefreshRate = rate;
                window.setAttributes(params);
                Log.i(TAG, "[Refresh Rate Governor] Locked window preferredRefreshRate to " + rate + " Hz");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Refresh Rate Governor] Failed to apply refresh rate: " + t.getMessage());
        }
    }
}
