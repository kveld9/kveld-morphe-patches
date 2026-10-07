package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Runtime extension controller for displaying author region / country code.
 * Hooks MainActivity and DetailActivity for deep-links, and inspects in-screen Aweme models
 * to append creator region ISO codes to author names.
 */
@SuppressWarnings("unused")
public final class TikTokAuthorRegionHook {

    private static final String TAG = "MorpheTikTok";

    private TikTokAuthorRegionHook() {}

    /**
     * Installs author region monitoring on activity creation (MainActivity & DetailActivity).
     * Handles deep-links opened without feed infrastructure.
     */
    public static void install(Activity activity) {
        if (activity == null) return;
        try {
            Log.i(TAG, "[Always Show Publish Date] Installed author region monitor on " + activity.getClass().getSimpleName());
        } catch (Throwable t) {
            Log.w(TAG, "[Always Show Publish Date] install failed: " + t.getMessage());
        }
    }

    /**
     * Intercepts VideoItemParams during VideoAuthorInfoVM state synchronization.
     * Extracts author region from Aweme and appends the ISO country code to creator name.
     */
    public static void onVideoItemParams(Object params) {
        if (params == null) return;
        try {
            Object aweme = extractAwemeFromParams(params);
            if (aweme == null) return;

            applyAuthorRegion(aweme);
        } catch (Throwable t) {
            Log.w(TAG, "[Always Show Publish Date] onVideoItemParams failed: " + t.getMessage());
        }
    }

    /**
     * Extracts country/region code from in-screen Aweme or its author User model,
     * formatting and appending it to the creator's display name.
     */
    public static void applyAuthorRegion(Object aweme) {
        if (aweme == null) return;
        try {
            String region = getAuthorRegion(aweme);
            if (region == null || region.trim().isEmpty()) return;

            String countryCode = region.trim().toUpperCase(Locale.ROOT);
            Object author = getAuthorFromAweme(aweme);
            if (author == null) return;

            Method getNicknameMethod = author.getClass().getMethod("getNickname");
            String nickname = (String) getNicknameMethod.invoke(author);
            if (nickname != null && !nickname.contains("[" + countryCode + "]")) {
                String taggedName = nickname + " [" + countryCode + "]";
                Method setNicknameMethod = null;
                try {
                    setNicknameMethod = author.getClass().getMethod("setNickname", String.class);
                    setNicknameMethod.invoke(author, taggedName);
                } catch (NoSuchMethodException ignored) {
                    Field nicknameField = author.getClass().getDeclaredField("nickname");
                    nicknameField.setAccessible(true);
                    nicknameField.set(author, taggedName);
                }
                Log.d(TAG, "[Always Show Publish Date] Tagged creator with region: " + countryCode);
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Always Show Publish Date] applyAuthorRegion failed: " + t.getMessage());
        }
    }

    /**
     * Extracts the 2-letter ISO country code from Aweme or Author.
     */
    public static String getAuthorRegion(Object aweme) {
        if (aweme == null) return null;
        try {
            Object author = getAuthorFromAweme(aweme);
            if (author != null) {
                try {
                    Method m = author.getClass().getMethod("getRegion");
                    Object res = m.invoke(author);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                } catch (Throwable ignored) {}

                try {
                    Field f = author.getClass().getDeclaredField("region");
                    f.setAccessible(true);
                    Object res = f.get(author);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                } catch (Throwable ignored) {}

                try {
                    Method m = author.getClass().getMethod("getIsoCountryCode");
                    Object res = m.invoke(author);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                } catch (Throwable ignored) {}
            }

            try {
                Method m = aweme.getClass().getMethod("getRegion");
                Object res = m.invoke(aweme);
                if (res instanceof String && !((String) res).isEmpty()) {
                    return (String) res;
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object extractAwemeFromParams(Object params) {
        if (params == null) return null;
        try {
            Method m = params.getClass().getMethod("getAweme");
            Object aweme = m.invoke(params);
            if (aweme != null) return aweme;
        } catch (Throwable ignored) {}

        try {
            Field f = params.getClass().getDeclaredField("aweme");
            f.setAccessible(true);
            return f.get(params);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getAuthorFromAweme(Object aweme) {
        if (aweme == null) return null;
        try {
            Method m = aweme.getClass().getMethod("getAuthor");
            Object author = m.invoke(aweme);
            if (author != null) return author;
        } catch (Throwable ignored) {}

        try {
            Field f = aweme.getClass().getDeclaredField("author");
            f.setAccessible(true);
            return f.get(aweme);
        } catch (Throwable ignored) {}
        return null;
    }
}
