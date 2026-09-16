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
    private static Method getRoomMethod;
    private static Method getRoomFeedCellStructMethod;
    private static Method getLiveIdMethod;
    private static Method getStreamUrlModelMethod;
    private static Method getAuthorLiveMethod;
    private static Method getLiveTypeMethod;
    private static Field roomField;
    private static Field roomFeedCellField;
    private static Field newLiveRoomDataField;
    private static Field liveIdField;
    private static Field streamUrlModelField;
    private static Field authorLiveField;
    private static Method setAnchorsMethod;
    private static Method setAnchorInfoMethod;

    private static Method isFriendsTabFakeAwemeMethod;
    private static Method getRecommendCardTypeMethod;
    private static Method getCardInsertInfoMethod;
    private static Method getCardTypeMethod;
    private static Field cardTypeField;

    private static Method followGetAwemeMethod;
    private static Field followAwemeField;
    private static Method followGetFeedTypeMethod;
    private static Field followFeedTypeField;
    private static Method followGetRoomMethod;
    private static Field followRoomField;

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
            try { getRoomMethod = awemeClass.getMethod("getRoom"); getRoomMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getRoomFeedCellStructMethod = awemeClass.getMethod("getRoomFeedCellStruct"); getRoomFeedCellStructMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getLiveIdMethod = awemeClass.getMethod("getLiveId"); getLiveIdMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getStreamUrlModelMethod = awemeClass.getMethod("getStreamUrlModel"); getStreamUrlModelMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getAuthorLiveMethod = awemeClass.getMethod("getAuthorLive"); getAuthorLiveMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getLiveTypeMethod = awemeClass.getMethod("getLiveType"); getLiveTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { roomField = awemeClass.getDeclaredField("room"); roomField.setAccessible(true); } catch (Throwable ignored) {}
            try { roomFeedCellField = awemeClass.getDeclaredField("mRoomFeedCellStruct"); roomFeedCellField.setAccessible(true); } catch (Throwable ignored) {}
            try { newLiveRoomDataField = awemeClass.getDeclaredField("newLiveRoomData"); newLiveRoomDataField.setAccessible(true); } catch (Throwable ignored) {}
            try { liveIdField = awemeClass.getDeclaredField("liveId"); liveIdField.setAccessible(true); } catch (Throwable ignored) {}
            try { streamUrlModelField = awemeClass.getDeclaredField("streamUrlModel"); streamUrlModelField.setAccessible(true); } catch (Throwable ignored) {}
            try { authorLiveField = awemeClass.getDeclaredField("authorLive"); authorLiveField.setAccessible(true); } catch (Throwable ignored) {}
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
            try { isFriendsTabFakeAwemeMethod = awemeClass.getMethod("isFriendsTabFakeAweme"); isFriendsTabFakeAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getRecommendCardTypeMethod = awemeClass.getMethod("getRecommendCardType"); getRecommendCardTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getCardInsertInfoMethod = awemeClass.getMethod("getCardInsertInfo"); getCardInsertInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> cardInfoClass = null;
                try {
                    cardInfoClass = loader.loadClass("com.ss.android.ugc.aweme.feed.model.cardinsert.CardInsertInfo");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        cardInfoClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.cardinsert.CardInsertInfo");
                    }
                }
                if (cardInfoClass != null) {
                    try { getCardTypeMethod = cardInfoClass.getMethod("getCardType"); getCardTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { cardTypeField = cardInfoClass.getDeclaredField("cardType"); cardTypeField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> followClass = null;
                try {
                    followClass = loader.loadClass("com.ss.android.ugc.aweme.follow.presenter.FollowFeed");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        followClass = classLoader.loadClass("com.ss.android.ugc.aweme.follow.presenter.FollowFeed");
                    }
                }
                if (followClass != null) {
                    try { followGetAwemeMethod = followClass.getMethod("getAweme"); followGetAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followAwemeField = followClass.getDeclaredField("aweme"); followAwemeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { followGetFeedTypeMethod = followClass.getMethod("getFeedType"); followGetFeedTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followFeedTypeField = followClass.getDeclaredField("feedType"); followFeedTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { followGetRoomMethod = followClass.getMethod("getRoom"); followGetRoomMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followRoomField = followClass.getDeclaredField("room"); followRoomField.setAccessible(true); } catch (Throwable ignored) {}
                }
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
            if (getRoomMethod != null && getRoomMethod.invoke(aweme) != null) {
                return true;
            }
            if (getRoomFeedCellStructMethod != null && getRoomFeedCellStructMethod.invoke(aweme) != null) {
                return true;
            }
            if (getLiveIdMethod != null) {
                Object id = getLiveIdMethod.invoke(aweme);
                if (id instanceof Long && ((Long) id) > 0) {
                    return true;
                }
            }
            if (getStreamUrlModelMethod != null && getStreamUrlModelMethod.invoke(aweme) != null) {
                return true;
            }
            if (getAuthorLiveMethod != null && Boolean.TRUE.equals(getAuthorLiveMethod.invoke(aweme))) {
                return true;
            }
            if (getLiveTypeMethod != null) {
                Object lt = getLiveTypeMethod.invoke(aweme);
                if (lt instanceof String && !((String) lt).isEmpty()) {
                    return true;
                }
            }

            if (roomField != null && roomField.get(aweme) != null) {
                return true;
            }
            if (roomFeedCellField != null && roomFeedCellField.get(aweme) != null) {
                return true;
            }
            if (newLiveRoomDataField != null && newLiveRoomDataField.get(aweme) != null) {
                return true;
            }
            if (liveIdField != null) {
                long lid = liveIdField.getLong(aweme);
                if (lid > 0) return true;
            }
            if (streamUrlModelField != null && streamUrlModelField.get(aweme) != null) {
                return true;
            }
            if (authorLiveField != null && Boolean.TRUE.equals(authorLiveField.get(aweme))) {
                return true;
            }

            if (isLiveMethod != null && Boolean.TRUE.equals(isLiveMethod.invoke(aweme))) {
                return true;
            }
            if (getAwemeTypeMethod != null) {
                Object type = getAwemeTypeMethod.invoke(aweme);
                if (type instanceof Integer) {
                    int t = ((Integer) type).intValue();
                    if (t == 101 || t == 68 || t == 102 || t == 69) {
                        return true;
                    }
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
        if (awemeClass != null && awemeClass.isInstance(followItem)) {
            return followItem;
        }
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

    public static boolean isFollowItemLive(Object followItem) {
        if (followItem == null) return false;
        if (!initialized) {
            ensureInitialized(followItem.getClass().getClassLoader());
        }
        try {
            if (followGetFeedTypeMethod != null) {
                Object ft = followGetFeedTypeMethod.invoke(followItem);
                if (ft instanceof Integer && ((Integer) ft) == 2) return true;
            }
            if (followFeedTypeField != null) {
                Object ft = followFeedTypeField.get(followItem);
                if (ft instanceof Integer && ((Integer) ft) == 2) return true;
            }
            if (followGetRoomMethod != null) {
                Object room = followGetRoomMethod.invoke(followItem);
                if (room != null) return true;
            }
            if (followRoomField != null) {
                Object room = followRoomField.get(followItem);
                if (room != null) return true;
            }
        } catch (Throwable ignored) {}
        return false;
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
                    if (isLiveStream(aweme) || isFollowItemLive(followItem)) {
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
    // 4. FEED BLOAT & DISTRACTION BLOCKER (Independent Patch)
    // =========================================================================

    public static boolean isFeedBloat(Object aweme) {
        if (aweme == null) return false;
        if (!initialized) {
            ensureInitialized(aweme.getClass().getClassLoader());
        }
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return false;
        }
        try {
            // 1. Check Aweme Types:
            // 4004: RecUser / Suggested Accounts Big Card
            // 104: Mini-Game Instant Play
            // 110: Mini-Drama / Series Card
            // 106: Detail Lynx / In-Feed Promotion Card
            if (getAwemeTypeMethod != null) {
                Object type = getAwemeTypeMethod.invoke(aweme);
                if (type instanceof Integer) {
                    int awemeType = ((Integer) type).intValue();
                    if (awemeType == 4004 || awemeType == 104 || awemeType == 110 || awemeType == 106) {
                        return true;
                    }
                }
            }

            // 2. Friends Tab Fake Aweme Placeholders
            if (isFriendsTabFakeAwemeMethod != null && Boolean.TRUE.equals(isFriendsTabFakeAwemeMethod.invoke(aweme))) {
                return true;
            }

            // 3. Recommendation Card Types (>0 indicates recommendation slot)
            if (getRecommendCardTypeMethod != null) {
                Object recType = getRecommendCardTypeMethod.invoke(aweme);
                if (recType instanceof Integer && ((Integer) recType) > 0) {
                    return true;
                }
            }

            // 4. CardInsertInfo checks:
            // 49: RecUser / Suggested Accounts Card Insert
            // 120: Mini-Game Instant Play Card
            // 127: On This Day (Recuerdos) Creation Card
            // 84: Creation EOY Card (Recap)
            // 176: Inspiration Card
            // 113: AI Remix Card
            // 2: Effect Recommendation Card
            // 4, 16: Platform Survey / Feedback Prompts
            // 188..191: Creation Feed Cards (CapCut / Templates / Camera)
            if (getCardInsertInfoMethod != null) {
                Object cardInfo = getCardInsertInfoMethod.invoke(aweme);
                if (cardInfo != null) {
                    int cType = -1;
                    if (getCardTypeMethod != null) {
                        Object res = getCardTypeMethod.invoke(cardInfo);
                        if (res instanceof Number) {
                            cType = ((Number) res).intValue();
                        }
                    } else if (cardTypeField != null) {
                        Object res = cardTypeField.get(cardInfo);
                        if (res instanceof Number) {
                            cType = ((Number) res).intValue();
                        }
                    }
                    if (cType == 49 || cType == 120 || cType == 127 || cType == 84 ||
                        cType == 176 || cType == 113 || cType == 2 || cType == 4 || cType == 16 ||
                        (cType >= 188 && cType <= 191)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isSuggestedAccount(Object aweme) {
        return isFeedBloat(aweme);
    }

    @SuppressWarnings("unchecked")
    public static void filterFeedBloatInList(Object listObj) {
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
                    if (isFeedBloat(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Bloat Blocker] Pruned " + removed + " non-video bloat card(s) from feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterFeedBloatInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());
            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                filterFeedBloatInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isFollowFeedBloat(Object followItem) {
        if (followItem == null) return false;
        if (!initialized) {
            ensureInitialized(followItem.getClass().getClassLoader());
        }
        if (awemeClass != null && awemeClass.isInstance(followItem)) {
            return isFeedBloat(followItem);
        }
        if (isFollowItemLive(followItem)) {
            // Live streams in Following feed belong to FeedLiveStreamBlockerPatch, not bloat blocker
            return false;
        }
        if (followGetAwemeMethod == null && followAwemeField == null) {
            return false;
        }
        Object aweme = extractAwemeFromFollowItem(followItem);
        if (aweme == null) {
            // Non-video, non-live recommendation cards in Following feed (e.g. suggested friends carousels)
            return true;
        }
        if (isFeedBloat(aweme)) {
            return true;
        }
        if (followGetFeedTypeMethod != null || followFeedTypeField != null) {
            try {
                int feedType = -1;
                if (followGetFeedTypeMethod != null) {
                    Object res = followGetFeedTypeMethod.invoke(followItem);
                    if (res instanceof Number) feedType = ((Number) res).intValue();
                } else if (followFeedTypeField != null) {
                    Object res = followFeedTypeField.get(followItem);
                    if (res instanceof Number) feedType = ((Number) res).intValue();
                }
                // Feed types >= 3 represent user recommendation / friend suggestion carousels
                if (feedType >= 3) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static void filterFeedBloatInFollowFeedList(Object followFeedList) {
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
                    if (isFollowFeedBloat(followItem)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Bloat Blocker] Pruned " + removed + " non-video bloat card(s) from Following feed.");
                }
            }
        } catch (Throwable ignored) {}
    }

    // Backward compatibility delegates
    public static void filterSuggestedAccountsInList(Object listObj) {
        filterFeedBloatInList(listObj);
    }

    public static void filterSuggestedAccountsInFeedItemList(Object feedItemList) {
        filterFeedBloatInFeedItemList(feedItemList);
    }

    public static void filterSuggestedAccountsInFollowFeedList(Object followFeedList) {
        filterFeedBloatInFollowFeedList(followFeedList);
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
