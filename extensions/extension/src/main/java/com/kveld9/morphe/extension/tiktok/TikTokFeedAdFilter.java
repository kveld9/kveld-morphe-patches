package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * High-performance, crash-safe feed ad filter for TikTok (com.zhiliaoapp.musically / com.ss.android.ugc.trill).
 * Neutralizes sponsored cards, brand promotions, affiliate videos, and promotional audio.
 */
@SuppressWarnings("unused")
public final class TikTokFeedAdFilter {

    private static final String TAG = "MorpheTikTok";
    private static volatile boolean initialized = false;

    private static Method isAdMethod;
    private static Method isSoftAdMethod;
    private static Method isWithPromotionalMusicMethod;
    private static Method getAwemeRawAdMethod;
    private static Method getLinkAdDataMethod;
    private static Method getShareUrlMethod;
    private static Method getAidMethod;

    private static Method followGetAwemeMethod;
    private static Field followAwemeField;

    private static final String SHOP_PROMO_MARKER = "placeholder_product_id";

    private TikTokFeedAdFilter() {}

    private static void ensureInitialized(ClassLoader classLoader) {
        if (initialized) return;
        synchronized (TikTokFeedAdFilter.class) {
            if (initialized) return;
            try {
                Class<?> awemeClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.Aweme");
                try { isAdMethod = awemeClass.getMethod("isAd"); } catch (Throwable ignored) {}
                try { isSoftAdMethod = awemeClass.getMethod("isSoftAd"); } catch (Throwable ignored) {}
                try { isWithPromotionalMusicMethod = awemeClass.getMethod("isWithPromotionalMusic"); } catch (Throwable ignored) {}
                try { getAwemeRawAdMethod = awemeClass.getMethod("getAwemeRawAd"); } catch (Throwable ignored) {}
                try { getLinkAdDataMethod = awemeClass.getMethod("getLinkAdData"); } catch (Throwable ignored) {}
                try { getShareUrlMethod = awemeClass.getMethod("getShareUrl"); } catch (Throwable ignored) {}
                try { getAidMethod = awemeClass.getMethod("getAid"); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}

            try {
                Class<?> followClass = classLoader.loadClass("com.ss.android.ugc.aweme.follow.presenter.FollowFeed");
                try { followGetAwemeMethod = followClass.getMethod("getAweme"); } catch (Throwable ignored) {}
                try {
                    followAwemeField = followClass.getDeclaredField("aweme");
                    followAwemeField.setAccessible(true);
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}

            Log.i(TAG, "[Feed Ad Blocker] Engine initialized. Monitoring feed streams for sponsored content.");
            initialized = true;
        }
    }

    private static String getAdReason(Object aweme) {
        if (aweme == null) return null;
        try {
            if (isAdMethod != null && Boolean.TRUE.equals(isAdMethod.invoke(aweme))) {
                return "isAd";
            }
            if (isSoftAdMethod != null && Boolean.TRUE.equals(isSoftAdMethod.invoke(aweme))) {
                return "isSoftAd";
            }
            if (getAwemeRawAdMethod != null && getAwemeRawAdMethod.invoke(aweme) != null) {
                return "awemeRawAd";
            }
            if (getLinkAdDataMethod != null && getLinkAdDataMethod.invoke(aweme) != null) {
                return "linkAdData";
            }
            if (isWithPromotionalMusicMethod != null && Boolean.TRUE.equals(isWithPromotionalMusicMethod.invoke(aweme))) {
                return "promotionalMusic";
            }
            if (getShareUrlMethod != null) {
                Object url = getShareUrlMethod.invoke(aweme);
                if (url instanceof String && ((String) url).contains(SHOP_PROMO_MARKER)) {
                    return "shopPromoMarker";
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Directly filters a List of Aweme objects without reflection re-entrancy.
     */
    @SuppressWarnings("unchecked")
    public static void filterAwemeList(Object listObj) {
        if (!(listObj instanceof List)) return;
        List<Object> items = (List<Object>) listObj;
        if (items.isEmpty()) return;

        try {
            Object first = items.get(0);
            if (first != null) {
                ensureInitialized(first.getClass().getClassLoader());
            }

            int removed = 0;
            Iterator<Object> iterator = items.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                String reason = getAdReason(item);
                if (reason != null) {
                    iterator.remove();
                    removed++;
                    String aid = "";
                    if (getAidMethod != null) {
                        try {
                            Object res = getAidMethod.invoke(item);
                            if (res instanceof String) aid = (String) res;
                        } catch (Throwable ignored) {}
                    }
                    Log.i(TAG, "[Feed Ad Blocker] Blocked sponsored video (id=" + aid + ", reason=" + reason + ")");
                }
            }
            Log.i(TAG, "[Feed Ad Blocker] Inspected FYP batch (" + items.size() + " videos), " + removed + " sponsored ad(s) pruned.");
        } catch (Throwable ignored) {}
    }

    /**
     * Filters sponsored advertisements from a FeedItemList container (Main For You / FYP Feed).
     */
    public static void filterFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());

            // 1. Reset ad flags on the FeedItemList instance
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

            // 2. Extract and filter the list of Aweme objects directly from fields
            Object itemsObj = null;
            try {
                Field itemsField = feedItemList.getClass().getDeclaredField("items");
                itemsField.setAccessible(true);
                itemsObj = itemsField.get(feedItemList);
            } catch (Throwable ignored) {}

            if (itemsObj == null) {
                try {
                    Field mItemsField = feedItemList.getClass().getDeclaredField("mItems");
                    mItemsField.setAccessible(true);
                    itemsObj = mItemsField.get(feedItemList);
                } catch (Throwable ignored) {}
            }

            if (itemsObj instanceof List) {
                filterAwemeList(itemsObj);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Filters sponsored advertisements from a FollowFeedList container or List (Following Feed).
     */
    @SuppressWarnings("unchecked")
    public static void filterFollowFeedList(Object followFeedList) {
        if (followFeedList == null) return;
        try {
            ensureInitialized(followFeedList.getClass().getClassLoader());

            List<Object> items = null;
            if (followFeedList instanceof List) {
                items = (List<Object>) followFeedList;
            } else {
                try {
                    Field itemsField = followFeedList.getClass().getDeclaredField("items");
                    itemsField.setAccessible(true);
                    Object val = itemsField.get(followFeedList);
                    if (val instanceof List) items = (List<Object>) val;
                } catch (Throwable ignored) {}
            }

            if (items == null || items.isEmpty()) return;

            int removed = 0;
            Iterator<Object> iterator = items.iterator();
            while (iterator.hasNext()) {
                Object followItem = iterator.next();
                if (followItem == null) continue;
                Object aweme = null;
                if (followGetAwemeMethod != null) {
                    try { aweme = followGetAwemeMethod.invoke(followItem); } catch (Throwable ignored) {}
                }
                if (aweme == null && followAwemeField != null) {
                    try { aweme = followAwemeField.get(followItem); } catch (Throwable ignored) {}
                }
                String reason = getAdReason(aweme);
                if (reason != null) {
                    iterator.remove();
                    removed++;
                    String aid = "";
                    if (aweme != null && getAidMethod != null) {
                        try {
                            Object res = getAidMethod.invoke(aweme);
                            if (res instanceof String) aid = (String) res;
                        } catch (Throwable ignored) {}
                    }
                    Log.i(TAG, "[Feed Ad Blocker] Blocked following feed ad (id=" + aid + ", reason=" + reason + ")");
                }
            }
            if (removed > 0) {
                Log.i(TAG, "[Feed Ad Blocker] Pruned " + removed + " sponsored ad(s) from Following feed (remaining: " + items.size() + ")");
            }
        } catch (Throwable ignored) {}
    }
}
