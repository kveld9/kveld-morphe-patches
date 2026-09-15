package com.kveld9.morphe.extension.tiktok;

import android.net.Uri;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * High-performance, crash-safe feed ad filter for TikTok (com.zhiliaoapp.musically / com.ss.android.ugc.trill).
 * Neutralizes sponsored cards, brand promotions, affiliate videos, promotional audio, and shop anchors.
 */
@SuppressWarnings("unused")
public final class TikTokFeedAdFilter {

    private static final String TAG = "MorpheTikTok";
    private static volatile boolean initialized = false;

    public static volatile boolean stripShopAnchors = true;

    private static Class<?> awemeClass;
    private static Method isAdMethod;
    private static Method isSoftAdMethod;
    private static Method isWithPromotionalMusicMethod;
    private static Method getAwemeRawAdMethod;
    private static Method getLinkAdDataMethod;
    private static Method getShareUrlMethod;
    private static Method isLiveMethod;
    private static Method getAwemeTypeMethod;
    private static Method setAnchorsMethod;
    private static Method setAnchorInfoMethod;

    private static Method followGetAwemeMethod;
    private static Field followAwemeField;

    private static final String SHOP_PROMO_MARKER = "placeholder_product_id";

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        "is_from_webapp", "sender_device", "_r", "checksum", "sec_user_id",
        "ug_source", "share_app_id", "share_item_id", "share_link_id",
        "source", "timestamp", "user_id", "u_code", "tt_from",
        "utm_source", "utm_campaign", "utm_medium"
    ));

    private TikTokFeedAdFilter() {}

    private static void ensureInitialized(ClassLoader classLoader) {
        if (initialized) return;
        synchronized (TikTokFeedAdFilter.class) {
            if (initialized) return;
            if (classLoader == null || classLoader.getClass().getName().contains("BootClassLoader")) {
                classLoader = TikTokFeedAdFilter.class.getClassLoader();
            }
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) return;

            awemeClass = null;
            try {
                awemeClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.Aweme");
            } catch (Throwable t) {
                try {
                    ClassLoader fallback = TikTokFeedAdFilter.class.getClassLoader();
                    if (fallback != null && fallback != classLoader) {
                        awemeClass = fallback.loadClass("com.ss.android.ugc.aweme.feed.model.Aweme");
                    }
                } catch (Throwable ignored) {}
            }

            if (awemeClass == null) {
                // Do not mark initialized = true if Aweme class cannot be loaded yet
                return;
            }

            try { isAdMethod = awemeClass.getMethod("isAd"); isAdMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { isSoftAdMethod = awemeClass.getMethod("isSoftAd"); isSoftAdMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { isWithPromotionalMusicMethod = awemeClass.getMethod("isWithPromotionalMusic"); isWithPromotionalMusicMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getAwemeRawAdMethod = awemeClass.getMethod("getAwemeRawAd"); getAwemeRawAdMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getLinkAdDataMethod = awemeClass.getMethod("getLinkAdData"); getLinkAdDataMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getShareUrlMethod = awemeClass.getMethod("getShareUrl"); getShareUrlMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { isLiveMethod = awemeClass.getMethod("isLive"); isLiveMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getAwemeTypeMethod = awemeClass.getMethod("getAwemeType"); getAwemeTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { setAnchorsMethod = awemeClass.getMethod("setAnchors", List.class); setAnchorsMethod.setAccessible(true); } catch (Throwable ignored) {}
            try {
                for (Method m : awemeClass.getMethods()) {
                    if ("setAnchorInfo".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        setAnchorInfoMethod = m;
                        setAnchorInfoMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}

            try {
                Class<?> followClass = awemeClass.getClassLoader().loadClass("com.ss.android.ugc.aweme.follow.presenter.FollowFeed");
                try { followGetAwemeMethod = followClass.getMethod("getAweme"); followGetAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
                try {
                    followAwemeField = followClass.getDeclaredField("aweme");
                    followAwemeField.setAccessible(true);
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}

            Log.i(TAG, "[Feed Ad Blocker] Engine initialized. Monitoring feed streams for sponsored content.");
            initialized = true;
        }
    }

    public static boolean isAd(Object aweme) {
        if (aweme == null) return false;
        if (!initialized) {
            ensureInitialized(aweme.getClass().getClassLoader());
        }
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return false;
        }
        try {
            if (isAdMethod != null && Boolean.TRUE.equals(isAdMethod.invoke(aweme))) {
                return true;
            }
            if (isSoftAdMethod != null && Boolean.TRUE.equals(isSoftAdMethod.invoke(aweme))) {
                return true;
            }
            if (getAwemeRawAdMethod != null && getAwemeRawAdMethod.invoke(aweme) != null) {
                return true;
            }
            if (getLinkAdDataMethod != null && getLinkAdDataMethod.invoke(aweme) != null) {
                return true;
            }
            if (isWithPromotionalMusicMethod != null && Boolean.TRUE.equals(isWithPromotionalMusicMethod.invoke(aweme))) {
                return true;
            }
            if (getShareUrlMethod != null) {
                Object url = getShareUrlMethod.invoke(aweme);
                if (url instanceof String && ((String) url).contains(SHOP_PROMO_MARKER)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isLiveStream(Object aweme) {
        if (aweme == null) return false;
        if (!initialized) {
            ensureInitialized(aweme.getClass().getClassLoader());
        }
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return false;
        }
        try {
            if (isLiveMethod != null && Boolean.TRUE.equals(isLiveMethod.invoke(aweme))) {
                return true;
            }
            if (getAwemeTypeMethod != null) {
                Object type = getAwemeTypeMethod.invoke(aweme);
                if (type instanceof Integer && ((Integer) type) == 101) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }


    /**
     * Strips shopping anchors, promo cards, and product showcase overlays from video models.
     */
    public static void stripCommercialAnchors(Object aweme) {
        if (!stripShopAnchors || aweme == null) return;
        if (!initialized) {
            ensureInitialized(aweme.getClass().getClassLoader());
        }
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return;
        }
        try {
            if (setAnchorsMethod != null) {
                setAnchorsMethod.invoke(aweme, (Object) null);
            }
            if (setAnchorInfoMethod != null) {
                setAnchorInfoMethod.invoke(aweme, (Object) null);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Purges ByteDance tracking parameters and campaign telemetry from shared URLs.
     */
    public static String sanitizeShareUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("tiktok.com")) {
            return originalUrl;
        }
        try {
            Uri uri = Uri.parse(originalUrl);
            if (uri.getQuery() == null || uri.getQueryParameterNames().isEmpty()) {
                return originalUrl;
            }
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : uri.getQueryParameterNames()) {
                if (!TRACKING_PARAMS.contains(param.toLowerCase())) {
                    builder.appendQueryParameter(param, uri.getQueryParameter(param));
                }
            }
            String cleaned = builder.build().toString();
            if (cleaned.endsWith("?")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            return cleaned;
        } catch (Throwable t) {
            return originalUrl;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractFeedItems(Object feedItemList) {
        if (feedItemList == null) return null;
        try {
            Field itemsField = feedItemList.getClass().getDeclaredField("items");
            itemsField.setAccessible(true);
            Object itemsObj = itemsField.get(feedItemList);
            if (itemsObj instanceof List) return (List<Object>) itemsObj;
        } catch (Throwable ignored) {}

        try {
            Field mItemsField = feedItemList.getClass().getDeclaredField("mItems");
            mItemsField.setAccessible(true);
            Object itemsObj = mItemsField.get(feedItemList);
            if (itemsObj instanceof List) return (List<Object>) itemsObj;
        } catch (Throwable ignored) {}

        return null;
    }

    private static Object extractAwemeFromFollowItem(Object followItem) {
        if (followItem == null) return null;
        if (followGetAwemeMethod != null) {
            try {
                Object aweme = followGetAwemeMethod.invoke(followItem);
                if (aweme != null) return aweme;
            } catch (Throwable ignored) {}
        }
        if (followAwemeField != null) {
            try {
                return followAwemeField.get(followItem);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractFollowList(Object followFeedList) {
        if (followFeedList instanceof List) return (List<Object>) followFeedList;
        try {
            Field itemsField = followFeedList.getClass().getDeclaredField("items");
            itemsField.setAccessible(true);
            Object val = itemsField.get(followFeedList);
            if (val instanceof List) return (List<Object>) val;
        } catch (Throwable ignored) {}
        return null;
    }

    // =========================================================================
    // 1. FEED AD BLOCKER (Ads, Shop Anchors, Promotional Audio)
    // =========================================================================

    @SuppressWarnings("unchecked")
    public static void filterAdsInList(Object listObj) {
        if (!(listObj instanceof List)) return;
        List<Object> items = (List<Object>) listObj;
        if (items.isEmpty()) return;

        synchronized (items) {
            try {
                if (!initialized) {
                    for (Object item : items) {
                        if (item != null) {
                            ensureInitialized(item.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (isAd(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Ad Blocker] Pruned " + removed + " sponsored ad(s) from feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterAdsInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());

            try {
                Field hasAdField = feedItemList.getClass().getDeclaredField("hasAd");
                hasAdField.setAccessible(true);
                hasAdField.setBoolean(feedItemList, false);
            } catch (Throwable ignored) {}

            try {
                Field preloadAdsField = feedItemList.getClass().getDeclaredField("preloadAds");
                preloadAdsField.setAccessible(true);
                preloadAdsField.set(feedItemList, null);
            } catch (Throwable ignored) {}

            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                filterAdsInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static void filterAdsInFollowFeedList(Object followFeedList) {
        if (followFeedList == null) return;
        try {
            List<Object> items = extractFollowList(followFeedList);
            if (items == null || items.isEmpty()) return;

            synchronized (items) {
                if (!initialized) {
                    for (Object followItem : items) {
                        if (followItem != null) {
                            ensureInitialized(followItem.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object followItem = iterator.next();
                    Object aweme = extractAwemeFromFollowItem(followItem);
                    if (isAd(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Ad Blocker] Pruned " + removed + " sponsored ad(s) from Following feed.");
                }
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // 2. TIKTOK SHOP ANCHORS STRIPPER (Independent Patch)
    // =========================================================================

    @SuppressWarnings("unchecked")
    public static void stripShopAnchorsInList(Object listObj) {
        if (!(listObj instanceof List)) return;
        List<Object> items = (List<Object>) listObj;
        if (items.isEmpty()) return;

        synchronized (items) {
            try {
                if (!initialized) {
                    for (Object item : items) {
                        if (item != null) {
                            ensureInitialized(item.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                for (Object item : items) {
                    stripCommercialAnchors(item);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void stripShopAnchorsInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());
            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                stripShopAnchorsInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static void stripShopAnchorsInFollowFeedList(Object followFeedList) {
        if (followFeedList == null) return;
        try {
            List<Object> items = extractFollowList(followFeedList);
            if (items == null || items.isEmpty()) return;

            synchronized (items) {
                if (!initialized) {
                    for (Object followItem : items) {
                        if (followItem != null) {
                            ensureInitialized(followItem.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                for (Object followItem : items) {
                    Object aweme = extractAwemeFromFollowItem(followItem);
                    if (aweme != null) {
                        stripCommercialAnchors(aweme);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // 3. FEED LIVE STREAM BLOCKER (Independent Patch)
    // =========================================================================

    @SuppressWarnings("unchecked")
    public static void filterLiveStreamsInList(Object listObj) {
        if (!(listObj instanceof List)) return;
        List<Object> items = (List<Object>) listObj;
        if (items.isEmpty()) return;

        synchronized (items) {
            try {
                if (!initialized) {
                    for (Object item : items) {
                        if (item != null) {
                            ensureInitialized(item.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (isLiveStream(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Live Stream Blocker] Pruned " + removed + " live stream(s) from feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterLiveStreamsInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());
            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                filterLiveStreamsInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static void filterLiveStreamsInFollowFeedList(Object followFeedList) {
        if (followFeedList == null) return;
        try {
            List<Object> items = extractFollowList(followFeedList);
            if (items == null || items.isEmpty()) return;

            synchronized (items) {
                if (!initialized) {
                    for (Object followItem : items) {
                        if (followItem != null) {
                            ensureInitialized(followItem.getClass().getClassLoader());
                            break;
                        }
                    }
                }

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object followItem = iterator.next();
                    Object aweme = extractAwemeFromFollowItem(followItem);
                    if (isLiveStream(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Live Stream Blocker] Pruned " + removed + " live stream(s) from Following feed.");
                }
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // Backward Compatibility Delegates
    // =========================================================================

    public static void filterAwemeList(Object listObj) {
        filterAdsInList(listObj);
    }

    public static void filterFeedItemList(Object feedItemList) {
        filterAdsInFeedItemList(feedItemList);
    }

    public static void filterFollowFeedList(Object followFeedList) {
        filterAdsInFollowFeedList(followFeedList);
    }
}
