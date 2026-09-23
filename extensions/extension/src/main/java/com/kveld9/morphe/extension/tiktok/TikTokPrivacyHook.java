package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TikTokPrivacyHook {
    private static final String TAG = "MorpheTikTok";

    private static final Set<String> BLOCKED_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.ACCESS_LOCAL_NETWORK",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.ACTIVITY_RECOGNITION",
        "com.google.android.gms.permission.AD_ID"
    ));

    private TikTokPrivacyHook() {}

    public static boolean isPermissionBlocked(String permission) {
        if (permission == null) return false;
        return BLOCKED_PERMISSIONS.contains(permission);
    }

    public static boolean interceptPermissionRequest(Activity activity, String[] permissions, int requestCode) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        boolean hasBlocked = false;
        for (String perm : permissions) {
            if (isPermissionBlocked(perm)) {
                hasBlocked = true;
                break;
            }
        }

        if (!hasBlocked) {
            return false;
        }

        Log.i(TAG, "[Device Privacy Guard] Intercepted runtime permission request for: " + Arrays.toString(permissions));

        recordKevaDenials(permissions);

        if (activity != null) {
            final int[] grantResults = new int[permissions.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Method m = Activity.class.getDeclaredMethod("onRequestPermissionsResult", int.class, String[].class, int[].class);
                    m.setAccessible(true);
                    m.invoke(activity, requestCode, permissions, grantResults);
                } catch (Throwable ignored) {}
            });
        }

        return true;
    }

    public static boolean interceptPowerPermissions(Object fakeFragment, Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        boolean hasBlocked = false;
        for (String p : permissions) {
            if (isPermissionBlocked(p)) {
                hasBlocked = true;
                break;
            }
        }

        if (!hasBlocked) {
            return false;
        }

        Log.i(TAG, "[Device Privacy Guard] Intercepted PowerPermissions request for: " + permissions);
        recordKevaDenials(permissions.toArray(new String[0]));

        if (fakeFragment != null) {
            try {
                String[] permsArr = permissions.toArray(new String[0]);
                int[] results = new int[permsArr.length];
                Arrays.fill(results, PackageManager.PERMISSION_DENIED);
                Method hT = fakeFragment.getClass().getDeclaredMethod("hT", int[].class, String[].class);
                hT.setAccessible(true);
                hT.invoke(fakeFragment, results, permsArr);
            } catch (Throwable t) {
                Log.w(TAG, "[Device Privacy Guard] Error dispatching PowerPermissions denial", t);
            }
        }

        return true;
    }

    private static void recordKevaDenials(String[] permissions) {
        try {
            Class<?> kevaClass = Class.forName("com.bytedance.keva.Keva");
            Method getRepo = kevaClass.getMethod("getRepo", String.class);

            Object friendsRepo = getRepo.invoke(null, "FriendsSharePreferences");
            if (friendsRepo != null) {
                Method storeBoolean = friendsRepo.getClass().getMethod("storeBoolean", String.class, boolean.class);
                storeBoolean.invoke(friendsRepo, "read_contact_denied", true);
            }

            Object permRepo = getRepo.invoke(null, "permission_store");
            if (permRepo != null) {
                Method storeBoolean = permRepo.getClass().getMethod("storeBoolean", String.class, boolean.class);
                for (String perm : permissions) {
                    storeBoolean.invoke(permRepo, perm, true);
                }
            }
        } catch (Throwable ignored) {}
    }

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
