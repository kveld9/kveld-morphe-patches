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

    public static boolean hasListRules() {
        return minViews > 0 || maxViews > 0 || minLikes > 0 || maxLikes > 0;
    }

    @SuppressWarnings("unchecked")
    public static void filterRulesInList(Object listObj) {
        if (!hasListRules() || !isNonEmptyList(listObj)) return;
        int removed = removeItemsByListRules((List<Object>) listObj);
        logFilterResult(removed);
    }

    @SuppressWarnings("unchecked")
    public static void filterContentInList(Object listObj) {
        if (!isNonEmptyList(listObj)) return;
        int removed = removeItemsByContentRules((List<Object>) listObj);
        logFilterResult(removed);
    }

    private static boolean isNonEmptyList(Object obj) {
        if (!(obj instanceof List)) return false;
        return !((List<?>) obj).isEmpty();
    }

    private static int removeItemsByListRules(List<Object> items) {
        int removed = 0;
        Iterator<Object> it = items.iterator();
        while (it.hasNext()) {
            if (removeIfMatchesListRules(it, it.next())) {
                removed++;
            }
        }
        return removed;
    }

    private static boolean removeIfMatchesListRules(Iterator<Object> it, Object item) {
        try {
            if (shouldFilterByListRules(item)) {
                it.remove();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static int removeItemsByContentRules(List<Object> items) {
        int removed = 0;
        Iterator<Object> it = items.iterator();
        while (it.hasNext()) {
            if (removeIfMatchesContentRules(it, it.next())) {
                removed++;
            }
        }
        return removed;
    }

    private static boolean removeIfMatchesContentRules(Iterator<Object> it, Object item) {
        try {
            if (shouldFilter(item)) {
                it.remove();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void logFilterResult(int removed) {
        if (removed > 0) {
            Log.i(TAG, "[Content Filter] Removed " + removed + " item(s) by content rules.");
        }
    }

    private static boolean shouldFilter(Object item) {
        if (item == null) return false;
        Object aweme = resolveAweme(item);
        if (matchesForYouRules(aweme)) return true;
        return matchesRangeRules(aweme);
    }

    private static boolean shouldFilterByListRules(Object item) {
        if (item == null) return false;
        Object aweme = resolveAweme(item);
        return matchesRangeRules(aweme);
    }

    private static boolean matchesForYouRules(Object aweme) {
        if (hideStories && isStory(aweme)) return true;
        return hidePhotoPosts && isPhotoPost(aweme);
    }

    private static boolean matchesRangeRules(Object aweme) {
        if (aweme == null) return false;
        if (isCountOutOfRange(getPlayCount(aweme), minViews, maxViews)) return true;
        return isCountOutOfRange(getLikeCount(aweme), minLikes, maxLikes);
    }

    private static boolean isCountOutOfRange(long value, long min, long max) {
        return isBelowMin(value, min) || isAboveMax(value, max);
    }

    private static boolean isBelowMin(long value, long min) {
        return min > 0 && value >= 0 && value < min;
    }

    private static boolean isAboveMax(long value, long max) {
        return max > 0 && value > max;
    }

    private static Object resolveAweme(Object item) {
        if (item == null) return null;
        try {
            Method m = item.getClass().getMethod("getAweme");
            Object aweme = m.invoke(item);
            if (aweme != null) return aweme;
        } catch (Throwable ignored) {}
        return item;
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
