package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runtime filter for the Feed Content Filter patch.
 * Removes stories, photo posts, blocked creators, and videos outside the configured
 * duration, age, or view/like ranges from feed item lists.
 */
@SuppressWarnings("unused")
public final class TikTokContentFilterHook {

    private static final String TAG = "MorpheTikTok";

    public static volatile long minViews = 0;
    public static volatile long maxViews = 0;
    public static volatile long minLikes = 0;
    public static volatile long maxLikes = 0;
    public static volatile int maxAgeDays = 0;
    public static volatile int minDurationSec = 0;
    public static volatile int maxDurationSec = 0;
    public static volatile boolean hideStories = true;
    public static volatile boolean hidePhotoPosts = false;
    public static volatile String blockedAuthorsCsv = "";
    public static volatile String blockedHashtagsCsv = "";
    public static volatile String blockedKeywordsCsv = "";
    public static volatile String hiddenRegionsCsv = "";

    private static final CsvSet blockedAuthorsSet = new CsvSet();
    private static final CsvSet blockedHashtagsSet = new CsvSet();
    private static final CsvSet blockedKeywordsSet = new CsvSet();
    private static final CsvSet hiddenRegionsSet = new CsvSet();

    private static final class CsvSet {
        private volatile String ref = null;
        private volatile Set<String> set = Collections.emptySet();

        public Set<String> get(String csv) {
            if (csv != null && csv == ref) {
                return set;
            }
            return update(csv);
        }

        private synchronized Set<String> update(String csv) {
            if (csv != null && csv == ref) {
                return set;
            }
            Set<String> parsed = parse(csv);
            set = parsed;
            ref = csv;
            return parsed;
        }

        private static Set<String> parse(String csv) {
            if (csv == null || csv.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> result = new HashSet<>();
            String[] tokens = csv.split(",");
            for (String raw : tokens) {
                addNormalized(result, raw);
            }
            return Collections.unmodifiableSet(result);
        }

        private static void addNormalized(Set<String> target, String raw) {
            String norm = normalize(raw);
            if (!norm.isEmpty()) {
                target.add(norm);
            }
        }

        static String normalize(String raw) {
            if (raw == null) return "";
            String trimmed = raw.trim();
            if (trimmed.startsWith("@") || trimmed.startsWith("#")) {
                trimmed = trimmed.substring(1);
            }
            return trimmed.toLowerCase(Locale.ROOT);
        }
    }

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
        if (maxAgeDays > 0 || hasDurationRules()) return true;
        return hasRangeRules() || hasAnyCsvRules();
    }

    private static boolean hasDurationRules() {
        return minDurationSec > 0 || maxDurationSec > 0;
    }

    private static boolean hasAnyCsvRules() {
        if (!getBlockedAuthors().isEmpty() || !getBlockedHashtags().isEmpty()) return true;
        return !getBlockedKeywords().isEmpty() || !getHiddenRegions().isEmpty();
    }

    private static boolean hasRangeRules() {
        return minViews > 0 || maxViews > 0 || minLikes > 0 || maxLikes > 0;
    }

    public static Set<String> getBlockedAuthors() {
        return blockedAuthorsSet.get(blockedAuthorsCsv);
    }

    public static Set<String> getBlockedHashtags() {
        return blockedHashtagsSet.get(blockedHashtagsCsv);
    }

    public static Set<String> getBlockedKeywords() {
        return blockedKeywordsSet.get(blockedKeywordsCsv);
    }

    public static Set<String> getHiddenRegions() {
        return hiddenRegionsSet.get(hiddenRegionsCsv);
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
        return matchesListRules(aweme);
    }

    private static boolean shouldFilterByListRules(Object item) {
        if (item == null) return false;
        Object aweme = resolveAweme(item);
        return matchesListRules(aweme);
    }

    private static boolean matchesForYouRules(Object aweme) {
        if (hideStories && isStory(aweme)) return true;
        return hidePhotoPosts && isPhotoPost(aweme);
    }

    private static boolean matchesListRules(Object aweme) {
        if (matchesRangeRules(aweme) || matchesAgeRule(aweme)) return true;
        if (matchesDurationRule(aweme)) return true;
        return matchesMetadataRules(aweme);
    }

    private static boolean matchesMetadataRules(Object aweme) {
        if (matchesAuthorRule(aweme) || matchesHashtagRule(aweme)) return true;
        return matchesKeywordRule(aweme) || matchesRegionRule(aweme);
    }

    private static boolean matchesAuthorRule(Object aweme) {
        if (aweme == null) return false;
        Set<String> blocked = getBlockedAuthors();
        if (blocked.isEmpty()) return false;
        String handle = getAuthorUniqueId(aweme);
        if (handle == null) return false;
        return blocked.contains(CsvSet.normalize(handle));
    }

    private static Object getAuthor(Object aweme) {
        if (aweme == null) return null;
        try {
            Method m = aweme.getClass().getMethod("getAuthor");
            return m.invoke(aweme);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String getAuthorUniqueId(Object aweme) {
        Object author = getAuthor(aweme);
        return getStringProperty(author, "getUniqueId");
    }

    private static boolean matchesHashtagRule(Object aweme) {
        if (aweme == null) return false;
        Set<String> blocked = getBlockedHashtags();
        if (blocked.isEmpty()) return false;
        if (matchesTagList(getListProperty(aweme, "getTextExtra"), "getHashTagName", blocked)) {
            return true;
        }
        return matchesTagList(getListProperty(aweme, "getChallengeList"), "getChallengeName", blocked);
    }

    private static boolean matchesKeywordRule(Object aweme) {
        Set<String> keywords = getBlockedKeywords();
        if (keywords.isEmpty()) return false;
        String caption = getStringProperty(aweme, "getDesc");
        if (caption == null || caption.isEmpty()) return false;
        return containsAnyKeyword(caption.toLowerCase(Locale.ROOT), keywords);
    }

    private static boolean containsAnyKeyword(String lowerCaption, Set<String> keywords) {
        for (String keyword : keywords) {
            if (lowerCaption.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRegionRule(Object aweme) {
        Set<String> regions = getHiddenRegions();
        if (regions.isEmpty()) return false;
        String region = getStringProperty(aweme, "getRegion");
        if (region == null || region.isEmpty()) return false;
        return regions.contains(CsvSet.normalize(region));
    }

    private static boolean matchesTagList(List<?> list, String methodName, Set<String> blocked) {
        if (list == null) return false;
        for (Object item : list) {
            if (matchesTagItem(item, methodName, blocked)) return true;
        }
        return false;
    }

    private static boolean matchesTagItem(Object item, String methodName, Set<String> blocked) {
        String val = getStringProperty(item, methodName);
        if (val == null || val.isEmpty()) return false;
        return blocked.contains(CsvSet.normalize(val));
    }

    private static List<?> getListProperty(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof List) return (List<?>) v;
        } catch (Throwable ignored) {}
        return null;
    }

    private static String getStringProperty(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof String) return (String) v;
        } catch (Throwable ignored) {}
        return null;
    }

    private static long getLongProperty(Object target, String methodName) {
        if (target == null) return -1;
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof Number) return ((Number) v).longValue();
        } catch (Throwable ignored) {}
        return -1;
    }

    private static boolean matchesAgeRule(Object aweme) {
        if (maxAgeDays <= 0) return false;
        long createTime = getLongProperty(aweme, "getCreateTime");
        if (createTime <= 0) return false;
        long createSeconds = normalizeToSeconds(createTime);
        long ageSeconds = (System.currentTimeMillis() / 1000L) - createSeconds;
        return ageSeconds > (maxAgeDays * 86400L);
    }

    private static boolean matchesDurationRule(Object aweme) {
        if (!hasDurationRules() || aweme == null) return false;
        Object video = getObjectProperty(aweme, "getVideo");
        long durationMs = getLongProperty(video, "getDuration");
        if (durationMs <= 0) return false;
        return isCountOutOfRange(durationMs, minDurationSec * 1000L, maxDurationSec * 1000L);
    }

    private static Object getObjectProperty(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {}
        return null;
    }

    private static long normalizeToSeconds(long time) {
        if (time > 100_000_000_000L) {
            return time / 1000L;
        }
        return time;
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
