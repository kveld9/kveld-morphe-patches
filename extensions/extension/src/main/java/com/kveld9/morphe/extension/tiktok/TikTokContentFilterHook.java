package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Runtime filter for the Feed Content Filter patch.
 * Removes stories, photo posts, and videos outside the configured
 * view/like ranges from feed item lists.
 */
@SuppressWarnings("unused")
public final class TikTokContentFilterHook {

    private static final String TAG = "MorpheTikTok";

    public static volatile long minViews = 0;
    public static volatile long maxViews = 0;
    public static volatile long minLikes = 0;
    public static volatile long maxLikes = 0;
    public static volatile boolean hideStories = true;
    public static volatile boolean hidePhotoPosts = false;

    private TikTokContentFilterHook() {}

    public static void filterContentInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            Method getItems = feedItemList.getClass().getMethod("getItems");
            Object items = getItems.invoke(feedItemList);
            filterContentInList(items);
        } catch (Throwable ignored) {}
    }

    public static void filterContentInFollowFeedList(Object followFeedList) {
        if (followFeedList == null) return;
        try {
            Method getItems = followFeedList.getClass().getMethod("getItems");
            Object items = getItems.invoke(followFeedList);
            filterContentInList(items);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static void filterContentInList(Object listObj) {
        if (!(listObj instanceof List)) return;
        List<Object> items = (List<Object>) listObj;
        if (items.isEmpty()) return;
        int removed = 0;
        Iterator<Object> it = items.iterator();
        while (it.hasNext()) {
            Object aweme = it.next();
            try {
                if (shouldFilter(aweme)) {
                    it.remove();
                    removed++;
                }
            } catch (Throwable ignored) {}
        }
        if (removed > 0) {
            Log.i(TAG, "[Content Filter] Removed " + removed + " item(s) by content rules.");
        }
    }

    private static boolean shouldFilter(Object aweme) {
        if (aweme == null) return false;

        if (hideStories && isStory(aweme)) return true;
        if (hidePhotoPosts && isPhotoPost(aweme)) return true;

        long views = getPlayCount(aweme);
        long likes = getLikeCount(aweme);
        if (minViews > 0 && views >= 0 && views < minViews) return true;
        if (maxViews > 0 && views > maxViews) return true;
        if (minLikes > 0 && likes >= 0 && likes < minLikes) return true;
        if (maxLikes > 0 && likes > maxLikes) return true;
        return false;
    }

    private static boolean isStory(Object aweme) {
        try {
            Method m = aweme.getClass().getMethod("getIsTikTokStory");
            return Boolean.TRUE.equals(m.invoke(aweme));
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isPhotoPost(Object aweme) {
        try {
            Method imageInfo = aweme.getClass().getMethod("getPhotoModeImageInfo");
            if (imageInfo.invoke(aweme) != null) return true;
        } catch (Throwable ignored) {}
        try {
            Method textInfo = aweme.getClass().getMethod("getPhotoModeTextInfo");
            if (textInfo.invoke(aweme) != null) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static long getPlayCount(Object aweme) {
        Object stats = getStatistics(aweme);
        if (stats == null) return -1;
        try {
            Method m = stats.getClass().getMethod("getPlayCount");
            Object v = m.invoke(stats);
            if (v instanceof Number) return ((Number) v).longValue();
        } catch (Throwable ignored) {}
        return -1;
    }

    private static long getLikeCount(Object aweme) {
        Object stats = getStatistics(aweme);
        if (stats == null) return -1;
        try {
            Method m = stats.getClass().getMethod("getDiggCount");
            Object v = m.invoke(stats);
            if (v instanceof Number) return ((Number) v).longValue();
        } catch (Throwable ignored) {}
        return -1;
    }

    private static Object getStatistics(Object aweme) {
        try {
            Method m = aweme.getClass().getMethod("getStatistics");
            return m.invoke(aweme);
        } catch (Throwable ignored) {}
        return null;
    }
}
