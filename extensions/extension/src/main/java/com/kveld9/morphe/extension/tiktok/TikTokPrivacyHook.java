package com.kveld9.morphe.extension.tiktok;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TikTokPrivacyHook {
    private static final String TAG = "MorpheTikTok";

    private static final ThreadLocal<Uri> LAST_URI = new ThreadLocal<>();
    private static final ThreadLocal<Intent> LAST_INTENT = new ThreadLocal<>();

    private static final Set<String> CONTACTS_AUTHORITIES = new HashSet<>(Arrays.asList(
        "contacts",
        "com.android.contacts",
        "contacts-provider",
        "call_log"
    ));

    private static final Set<String> BLOCKED_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.ACCESS_LOCAL_NETWORK",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.ACTIVITY_RECOGNITION",
        "com.google.android.gms.permission.AD_ID",
        "android.permission.ACCESS_ADSERVICES_AD_ID",
        "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
    ));

    private TikTokPrivacyHook() {}

    public static boolean isPermissionBlocked(String permission) {
        if (permission == null) return false;
        return BLOCKED_PERMISSIONS.contains(permission);
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
                storeBoolean.invoke(friendsRepo, "read_contact_denied", false);
            }

            Object permRepo = getRepo.invoke(null, "permission_store");
            if (permRepo != null) {
                Method storeBoolean = permRepo.getClass().getMethod("storeBoolean", String.class, boolean.class);
                for (String perm : permissions) {
                    storeBoolean.invoke(permRepo, perm, false);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isContactsUri(Uri uri) {
        if (uri == null) return false;
        String auth = uri.getAuthority();
        if (auth == null) return false;
        String lower = auth.toLowerCase();
        if (lower.contains("contacts") || CONTACTS_AUTHORITIES.contains(lower)) {
            return true;
        }
        return false;
    }

    public static void noteUri(Uri uri) {
        LAST_URI.set(uri);
    }

    public static Cursor filterNotedResult(Cursor cursor) {
        Uri uri = LAST_URI.get();
        LAST_URI.remove();
        if (isContactsUri(uri)) {
            Log.w(TAG, "[Device Privacy Guard] Filtered contacts query for URI: " + uri);
            String[] columns = null;
            if (cursor != null) {
                try {
                    columns = cursor.getColumnNames();
                } catch (Throwable ignored) {}
                try {
                    cursor.close();
                } catch (Throwable ignored) {}
            }
            // Same column shape as the original cursor but zero rows, so callers
            // using getColumnIndexOrThrow() keep working instead of crashing.
            return new MatrixCursor(columns != null ? columns : new String[0]);
        }
        return cursor;
    }

    public static void noteIntent(Intent intent) {
        LAST_INTENT.set(intent);
    }

    public static List<ResolveInfo> filterNotedIntentResult(List<ResolveInfo> activities) {
        Intent intent = LAST_INTENT.get();
        LAST_INTENT.remove();
        if (intent != null && (intent.getPackage() != null || intent.getComponent() != null)) {
            return activities;
        }
        Log.w(TAG, "[Device Privacy Guard] Filtered broad package inventory query: " + intent);
        return Collections.emptyList();
    }
}
