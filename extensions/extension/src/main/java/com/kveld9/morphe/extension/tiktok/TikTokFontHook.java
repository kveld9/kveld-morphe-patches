package com.kveld9.morphe.extension.tiktok;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;

public final class TikTokFontHook {

    private TikTokFontHook() {}

    private static final String TAG = "Morphe";
    private static volatile boolean logged = false;

    private static void logOnce(int weight, boolean italic) {
        if (logged) {
            return;
        }
        logged = true;
        android.util.Log.e(TAG, "[System Font] TikTok system font redirection active (weight=" + weight + ", italic=" + italic + ")");
    }

    private static Typeface resolveModernTypeface(int weight, boolean italic) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        try {
            return Typeface.create(Typeface.DEFAULT, weight, italic);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int resolveLegacyStyle(int weight, boolean italic) {
        if (weight >= 600) {
            return italic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
        }
        return italic ? Typeface.ITALIC : Typeface.NORMAL;
    }

    private static Typeface resolveLegacyTypeface(int weight, boolean italic) {
        try {
            return Typeface.create(Typeface.DEFAULT, resolveLegacyStyle(weight, italic));
        } catch (Throwable ignored) {
            return Typeface.DEFAULT;
        }
    }

    /**
     * Resolves a system typeface matching the requested weight and italic slant.
     *
     * @param weight Font weight (1-1000, e.g. 400 = regular, 500 = medium, 700 = bold)
     * @param italic Whether italic/slant is requested
     * @return System Typeface instance
     */
    public static Typeface getSystemTypeface(int weight, boolean italic) {
        logOnce(weight, italic);
        int normalizedWeight = weight <= 0 ? 400 : weight;
        Typeface modern = resolveModernTypeface(normalizedWeight, italic);
        if (modern != null) {
            return modern;
        }
        return resolveLegacyTypeface(normalizedWeight, italic);
    }

    private static int parseWeight(String lower) {
        if (lower.contains("black") || lower.contains("heavy")) {
            return 900;
        }
        if (lower.contains("bold")) {
            return 700;
        }
        if (lower.contains("medium") || lower.contains("semibold")) {
            return 500;
        }
        if (lower.contains("light")) {
            return 300;
        }
        return 400;
    }

    private static boolean parseItalic(String lower) {
        return lower.contains("italic") || lower.contains("slant");
    }

    /**
     * Resolves a system typeface from a font asset path (e.g. "font/TikTok-Display-Bold.otf").
     */
    public static Typeface getSystemTypefaceForPath(String fontPath) {
        if (fontPath == null) {
            return Typeface.DEFAULT;
        }
        String lower = fontPath.toLowerCase();
        return getSystemTypeface(parseWeight(lower), parseItalic(lower));
    }

    /**
     * Intercepts AssetManager font loading to redirect TikTok font assets to system font.
     */
    public static Typeface interceptAssetTypeface(AssetManager assets, String fontPath) {
        if (fontPath == null) {
            return null;
        }
        String lower = fontPath.toLowerCase();
        if (!lower.contains("tiktok") && !lower.contains("font/tiktok")) {
            return null;
        }
        return getSystemTypefaceForPath(fontPath);
    }

    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * Intercepts WebView font asset loading requests to fall back to system font in WebViews.
     */
    public static boolean shouldInterceptWebFont(WebResourceRequest req) {
        if (req == null || req.getUrl() == null) {
            return false;
        }
        String urlString = req.getUrl().toString().toLowerCase();
        return urlString.contains("tiktoksans")
            || urlString.contains("tiktokvffont")
            || urlString.contains("tiktokdisplayfont");
    }

    /**
     * Intercepts WebView font asset loading requests and returns an empty response so CSS falls back to system font.
     */
    public static WebResourceResponse interceptWebFont(WebResourceRequest req) {
        if (!shouldInterceptWebFont(req)) {
            return null;
        }
        try {
            return new WebResourceResponse("font/otf", "UTF-8", new ByteArrayInputStream(EMPTY_BYTES));
        } catch (Throwable t) {
            return null;
        }
    }
}
