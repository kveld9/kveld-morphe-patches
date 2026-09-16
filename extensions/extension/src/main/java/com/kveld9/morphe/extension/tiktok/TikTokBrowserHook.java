package com.kveld9.morphe.extension.tiktok;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TikTokBrowserHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokBrowserHook() {}

    public static boolean openInExternalBrowser(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[In-App Browser Privacy Guard] Redirected external URL to system browser: " + url);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "[In-App Browser Privacy Guard] Failed to open external browser: " + t.getMessage());
            return false;
        }
    }

    private static Context resolveContext(Object contextObj) {
        if (contextObj instanceof Context) {
            return (Context) contextObj;
        }
        if (contextObj == null) {
            return null;
        }
        try {
            Method getContextMethod = contextObj.getClass().getMethod("getContext");
            Object ctx = getContextMethod.invoke(contextObj);
            if (ctx instanceof Context) {
                return (Context) ctx;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractUrl(Object sparkContextObj) {
        if (sparkContextObj == null) {
            return null;
        }
        try {
            Field urlField;
            try {
                urlField = sparkContextObj.getClass().getField("url");
            } catch (NoSuchFieldException e) {
                urlField = sparkContextObj.getClass().getDeclaredField("url");
                urlField.setAccessible(true);
            }
            Object urlVal = urlField.get(sparkContextObj);
            if (urlVal instanceof String) {
                return (String) urlVal;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean redirectSparkThird(Object contextObj, Object sparkContextObj) {
        Context context = resolveContext(contextObj);
        if (context == null) {
            return false;
        }
        String url = extractUrl(sparkContextObj);
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return openInExternalBrowser(context, url);
    }
}

