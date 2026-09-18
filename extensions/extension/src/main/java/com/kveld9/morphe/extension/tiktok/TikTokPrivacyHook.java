package com.kveld9.morphe.extension.tiktok;

import android.content.pm.PackageManager;
import android.util.Log;

public final class TikTokPrivacyHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokPrivacyHook() {}

    public static void checkPackageAllowed(String packageName) throws PackageManager.NameNotFoundException {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (packageName.startsWith("com.zhiliao")
                || packageName.startsWith("com.ss.android")
                || packageName.startsWith("android")
                || packageName.startsWith("com.android.")
                || packageName.startsWith("com.google.android")) {
            return;
        }
        Log.w(TAG, "[Device Privacy Guard] Intercepted external package query: " + packageName);
        throw new PackageManager.NameNotFoundException("Package query blocked by Device Privacy Guard: " + packageName);
    }
}
