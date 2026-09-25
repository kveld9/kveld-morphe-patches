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
    private static Method followGetRoomStructMethod;
    private static Field followRoomField;
    private static Field followRoomStructField;
    private static Method followGetLastViewDataMethod;
    private static Field followLastViewDataField;
    private static Method followGetRecommendUserMethod;
    private static Field followRecommendUserField;

    private static Method friendsV3GetAwemeMethod;
    private static Field friendsV3AwemeField;
    private static Method friendsV3GetRepostItemMethod;
    private static Field friendsV3RepostItemField;
    private static Method friendsV3GetRepostedAwemeMethod;
    private static Field friendsV3RepostedAwemeField;

    private static Method friendsFeedGetAwemeMethod;
    private static Field friendsFeedAwemeField;

    private static Method getAigcInfoMethod;
    private static Field aigcInfoField;
    private static Method getAigcLabelTypeMethod;
    private static Field aigcLabelTypeField;
    private static Field createByAiField;

    private static Method getModerationAigcInfoMethod;
    private static Field moderationAigcInfoField;
    private static Field moderationAigcLabelTypeField;
    private static Field moderationUserLabelStatusField;
    private static Field creatorGuidanceStatusField;
    private static Field moderationCreatorSegmentField;

    private static Method getC2paInfoMethod;
    private static Field c2paInfoField;
    private static Field c2paAigcSrcField;
    private static Field c2paFirstAigcSrcField;
    private static Field c2paLastAigcSrcField;

    private static Field aiAliveInfoField;
    private static Field aiPortraitInfoField;
    private static Field aiRemixInfoField;
    private static Field aiTheaterInfoField;
    private static Field aiChatEditorInfoField;

    private static Method getBannersMethod;
    private static Field bannersField;
    private static Method getAnchorsMethod;
    private static Field anchorsField;
    private static Method getDescMethod;
    private static Field descField;
    private static Method getContentDescMethod;
    private static Field contentDescField;

    private static final java.util.regex.Pattern AI_TAG_PATTERN = java.util.regex.Pattern.compile(
        "(?i)(?:#(?:aigenerated|ai_generated|aigc|generadoporia|generado_por_ia|generadoconia|aiart|aivideo|iaart|iavideo|midjourney|sora|stable_diffusion|stablediffusion|dalle|chatgpt|runwayml|klingai|lumaai)\\b|\\[(?:ai[-_ ]?generated|generado por ia|aigc)\\]|\\b(?:ai[-_ ]generated|generado por ia|generado con ia)\\b)"
    );

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
                    if (followGetFeedTypeMethod == null) {
                        try { followGetFeedTypeMethod = followClass.getMethod("getFeedTypeValue"); followGetFeedTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    }
                    try { followFeedTypeField = followClass.getDeclaredField("feedType"); followFeedTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    if (followFeedTypeField == null) {
                        try { followFeedTypeField = followClass.getDeclaredField("feedTypeValue"); followFeedTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    }
                    try { followGetRoomMethod = followClass.getMethod("getRoom"); followGetRoomMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followGetRoomStructMethod = followClass.getMethod("getRoomStruct"); followGetRoomStructMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followRoomField = followClass.getDeclaredField("room"); followRoomField.setAccessible(true); } catch (Throwable ignored) {}
                    try { followRoomStructField = followClass.getDeclaredField("roomStruct"); followRoomStructField.setAccessible(true); } catch (Throwable ignored) {}
                    try { followGetLastViewDataMethod = followClass.getMethod("getLastViewData"); followGetLastViewDataMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followLastViewDataField = followClass.getDeclaredField("lastViewData"); followLastViewDataField.setAccessible(true); } catch (Throwable ignored) {}
                    try { followGetRecommendUserMethod = followClass.getMethod("getRecommendUser"); followGetRecommendUserMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { followRecommendUserField = followClass.getDeclaredField("recommendUser"); followRecommendUserField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> friendsV3ModelClass = null;
                try {
                    friendsV3ModelClass = loader.loadClass("com.ss.android.ugc.aweme.friendstab.repo.FriendsV3FeedModel");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        friendsV3ModelClass = classLoader.loadClass("com.ss.android.ugc.aweme.friendstab.repo.FriendsV3FeedModel");
                    }
                }
                if (friendsV3ModelClass != null) {
                    try { friendsV3GetAwemeMethod = friendsV3ModelClass.getMethod("getAweme"); friendsV3GetAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { friendsV3AwemeField = friendsV3ModelClass.getDeclaredField("aweme"); friendsV3AwemeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { friendsV3GetRepostItemMethod = friendsV3ModelClass.getMethod("getRepostItem"); friendsV3GetRepostItemMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { friendsV3RepostItemField = friendsV3ModelClass.getDeclaredField("repostItem"); friendsV3RepostItemField.setAccessible(true); } catch (Throwable ignored) {}
                }
                Class<?> repostClass = null;
                try {
                    repostClass = loader.loadClass("com.ss.android.ugc.aweme.friendstab.repo.FriendsV3RepostModel");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        repostClass = classLoader.loadClass("com.ss.android.ugc.aweme.friendstab.repo.FriendsV3RepostModel");
                    }
                }
                if (repostClass != null) {
                    try { friendsV3GetRepostedAwemeMethod = repostClass.getMethod("getRepostedAweme"); friendsV3GetRepostedAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { friendsV3RepostedAwemeField = repostClass.getDeclaredField("repostedAweme"); friendsV3RepostedAwemeField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> friendsFeedClass = null;
                try {
                    friendsFeedClass = loader.loadClass("com.ss.android.ugc.aweme.feed.model.friends.FriendsFeed");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        friendsFeedClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.friends.FriendsFeed");
                    }
                }
                if (friendsFeedClass != null) {
                    try { friendsFeedGetAwemeMethod = friendsFeedClass.getMethod("getAweme"); friendsFeedGetAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { friendsFeedAwemeField = friendsFeedClass.getDeclaredField("aweme"); friendsFeedAwemeField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try { getAigcInfoMethod = awemeClass.getMethod("getAigcInfo"); getAigcInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { aigcInfoField = awemeClass.getDeclaredField("aigcInfo"); aigcInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { getModerationAigcInfoMethod = awemeClass.getMethod("getModerationAigcInfo"); getModerationAigcInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { moderationAigcInfoField = awemeClass.getDeclaredField("moderationAigcInfo"); moderationAigcInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { getC2paInfoMethod = awemeClass.getMethod("getC2paInfo"); getC2paInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { c2paInfoField = awemeClass.getDeclaredField("c2paInfo"); c2paInfoField.setAccessible(true); } catch (Throwable ignored) {}

            try { aiAliveInfoField = awemeClass.getDeclaredField("aiAliveInfo"); aiAliveInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { aiPortraitInfoField = awemeClass.getDeclaredField("aiPortraitInfo"); aiPortraitInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { aiRemixInfoField = awemeClass.getDeclaredField("aiRemixInfo"); aiRemixInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { aiTheaterInfoField = awemeClass.getDeclaredField("aiTheaterInfo"); aiTheaterInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { aiChatEditorInfoField = awemeClass.getDeclaredField("aiChatEditorInfo"); aiChatEditorInfoField.setAccessible(true); } catch (Throwable ignored) {}

            try { getBannersMethod = awemeClass.getMethod("getBanners"); getBannersMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { bannersField = awemeClass.getDeclaredField("banners"); bannersField.setAccessible(true); } catch (Throwable ignored) {}
            try { getAnchorsMethod = awemeClass.getMethod("getAnchors"); getAnchorsMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { anchorsField = awemeClass.getDeclaredField("anchors"); anchorsField.setAccessible(true); } catch (Throwable ignored) {}
            try { getDescMethod = awemeClass.getMethod("getDesc"); getDescMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { descField = awemeClass.getDeclaredField("desc"); descField.setAccessible(true); } catch (Throwable ignored) {}
            try { getContentDescMethod = awemeClass.getMethod("getContentDesc"); getContentDescMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { contentDescField = awemeClass.getDeclaredField("contentDesc"); contentDescField.setAccessible(true); } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> aigcInfoClass = null;
                try {
                    aigcInfoClass = loader.loadClass("com.ss.android.ugc.aweme.feed.AIGCInfo");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        aigcInfoClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.AIGCInfo");
                    }
                }
                if (aigcInfoClass != null) {
                    try { getAigcLabelTypeMethod = aigcInfoClass.getMethod("getAIGCLabelType"); getAigcLabelTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { aigcLabelTypeField = aigcInfoClass.getDeclaredField("AIGCLabelType"); aigcLabelTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { createByAiField = aigcInfoClass.getDeclaredField("createByAI"); createByAiField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> modAigcClass = null;
                try {
                    modAigcClass = loader.loadClass("com.ss.android.ugc.aweme.feed.model.ModerationAigcInfo");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        modAigcClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.ModerationAigcInfo");
                    }
                }
                if (modAigcClass != null) {
                    try { moderationAigcLabelTypeField = modAigcClass.getDeclaredField("moderationAigcLabelType"); moderationAigcLabelTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { moderationUserLabelStatusField = modAigcClass.getDeclaredField("moderationUserLabelStatus"); moderationUserLabelStatusField.setAccessible(true); } catch (Throwable ignored) {}
                    try { creatorGuidanceStatusField = modAigcClass.getDeclaredField("creatorGuidanceStatus"); creatorGuidanceStatusField.setAccessible(true); } catch (Throwable ignored) {}
                    try { moderationCreatorSegmentField = modAigcClass.getDeclaredField("moderationCreatorSegment"); moderationCreatorSegmentField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> c2paClass = null;
                try {
                    c2paClass = loader.loadClass("com.ss.android.ugc.aweme.feed.model.C2PAInfo");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        c2paClass = classLoader.loadClass("com.ss.android.ugc.aweme.feed.model.C2PAInfo");
                    }
                }
                if (c2paClass != null) {
                    try { c2paAigcSrcField = c2paClass.getDeclaredField("aigcSrc"); c2paAigcSrcField.setAccessible(true); } catch (Throwable ignored) {}
                    try { c2paFirstAigcSrcField = c2paClass.getDeclaredField("firstAigcSrc"); c2paFirstAigcSrcField.setAccessible(true); } catch (Throwable ignored) {}
                    try { c2paLastAigcSrcField = c2paClass.getDeclaredField("lastAigcSrc"); c2paLastAigcSrcField.setAccessible(true); } catch (Throwable ignored) {}
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

    private static int getFollowItemFeedType(Object followItem) {
        if (followItem == null) return -1;
        try {
            if (followGetFeedTypeMethod != null) {
                Object ft = followGetFeedTypeMethod.invoke(followItem);
                if (ft instanceof Number) return ((Number) ft).intValue();
            }
            if (followFeedTypeField != null) {
                Object ft = followFeedTypeField.get(followItem);
                if (ft instanceof Number) return ((Number) ft).intValue();
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static boolean isLastWatchHistoryItem(Object followItem) {
        if (followItem == null) return false;
        try {
            int feedType = getFollowItemFeedType(followItem);
            if (feedType == 65280 || feedType == 65465 || feedType == 65298) {
                return true;
            }
            if (followGetLastViewDataMethod != null && followGetLastViewDataMethod.invoke(followItem) != null) {
                return true;
            }
            if (followLastViewDataField != null && followLastViewDataField.get(followItem) != null) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isFollowRecommendationCard(Object followItem) {
        if (followItem == null) return false;
        try {
            int feedType = getFollowItemFeedType(followItem);
            if (feedType == 3 || feedType == 62) {
                return true;
            }
            if (followGetRecommendUserMethod != null && followGetRecommendUserMethod.invoke(followItem) != null) {
                return true;
            }
            if (followRecommendUserField != null && followRecommendUserField.get(followItem) != null) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isFollowItemLive(Object followItem) {
        if (followItem == null) return false;
        if (!initialized) {
            ensureInitialized(followItem.getClass().getClassLoader());
        }
        if (isLastWatchHistoryItem(followItem)) {
            return false;
        }
        try {
            int feedType = getFollowItemFeedType(followItem);
            if (feedType == 2) return true;

            if (followGetRoomMethod != null && followGetRoomMethod.invoke(followItem) != null) return true;
            if (followGetRoomStructMethod != null && followGetRoomStructMethod.invoke(followItem) != null) return true;
            if (followRoomField != null && followRoomField.get(followItem) != null) return true;
            if (followRoomStructField != null && followRoomStructField.get(followItem) != null) return true;
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

            // 3. CardInsertInfo checks:
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
        return isFollowFeedBloat(followItem, true);
    }

    public static boolean isFollowFeedBloat(Object followItem, boolean allowPruningCards) {
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
        if (isLastWatchHistoryItem(followItem)) {
            // Vital position and history markers must never be pruned as doing so triggers
            // IndexOutOfBoundsException in presenter index calculations (Issue #42).
            return false;
        }

        Object aweme = extractAwemeFromFollowItem(followItem);
        if (aweme != null) {
            return isFeedBloat(aweme);
        }

        // For non-video items without an Aweme (e.g. suggested accounts carousels):
        // Only prune when allowPruningCards is true and the item is confirmed as a recommendation card.
        if (allowPruningCards && isFollowRecommendationCard(followItem)) {
            return true;
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

                // Guard: Count valid video aweme items in the list.
                // If there are zero videos (e.g. fresh account or caught up feed with only suggested accounts),
                // pruning all items will cause the Following presenter (LX/1IVX) to see an empty list and fail
                // with a network error. In that scenario, keep recommendation cards so the feed does not break.
                int validVideos = 0;
                for (Object item : items) {
                    if (item == null) continue;
                    Object aweme = extractAwemeFromFollowItem(item);
                    if (aweme != null && !isFeedBloat(aweme) && !isLiveStream(aweme)) {
                        validVideos++;
                    }
                }
                boolean allowPruningCards = validVideos > 0;

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object followItem = iterator.next();
                    if (isFollowFeedBloat(followItem, allowPruningCards)) {
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

    private static Object extractAwemeFromFriendsV3FeedModel(Object item) {
        if (item == null) return null;
        try {
            if (friendsV3AwemeField != null) {
                Object aweme = friendsV3AwemeField.get(item);
                if (aweme != null) return aweme;
            }
            if (friendsV3GetAwemeMethod != null) {
                Object aweme = friendsV3GetAwemeMethod.invoke(item);
                if (aweme != null) return aweme;
            }
            if (friendsV3RepostItemField != null) {
                Object repost = friendsV3RepostItemField.get(item);
                if (repost != null) {
                    if (friendsV3RepostedAwemeField != null) {
                        Object aweme = friendsV3RepostedAwemeField.get(repost);
                        if (aweme != null) return aweme;
                    }
                    if (friendsV3GetRepostedAwemeMethod != null) {
                        Object aweme = friendsV3GetRepostedAwemeMethod.invoke(repost);
                        if (aweme != null) return aweme;
                    }
                }
            }
            if (friendsV3GetRepostItemMethod != null) {
                Object repost = friendsV3GetRepostItemMethod.invoke(item);
                if (repost != null) {
                    if (friendsV3RepostedAwemeField != null) {
                        Object aweme = friendsV3RepostedAwemeField.get(repost);
                        if (aweme != null) return aweme;
                    }
                    if (friendsV3GetRepostedAwemeMethod != null) {
                        Object aweme = friendsV3GetRepostedAwemeMethod.invoke(repost);
                        if (aweme != null) return aweme;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object extractAwemeFromFriendsFeed(Object item) {
        if (item == null) return null;
        try {
            if (friendsFeedAwemeField != null) {
                Object aweme = friendsFeedAwemeField.get(item);
                if (aweme != null) return aweme;
            }
            if (friendsFeedGetAwemeMethod != null) {
                Object aweme = friendsFeedGetAwemeMethod.invoke(item);
                if (aweme != null) return aweme;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isFriendsV3FeedBloat(Object item) {
        if (item == null) return false;
        Object aweme = extractAwemeFromFriendsV3FeedModel(item);
        if (aweme != null) {
            return isFeedBloat(aweme);
        }
        return false;
    }

    public static boolean isFriendsFeedBloat(Object item) {
        if (item == null) return false;
        Object aweme = extractAwemeFromFriendsFeed(item);
        if (aweme != null) {
            return isFeedBloat(aweme);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static void filterFeedBloatInFriendsV3Feeds(Object listObj) {
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
                    if (isFriendsV3FeedBloat(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Bloat Blocker] Pruned " + removed + " bloat card(s) from Friends V3 feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static void filterFeedBloatInFriendsFeedData(Object listObj) {
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
                    if (isFriendsFeedBloat(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Feed Bloat Blocker] Pruned " + removed + " bloat card(s) from Friends V2 feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void collapseRecUserCardCell(Object cellObj) {
        if (cellObj == null) return;
        try {
            Field field = null;
            Class<?> clazz = cellObj.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    field = clazz.getDeclaredField("itemView");
                    field.setAccessible(true);
                    break;
                } catch (Throwable t) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field != null) {
                Object viewObj = field.get(cellObj);
                if (viewObj instanceof android.view.View) {
                    android.view.View view = (android.view.View) viewObj;
                    view.setVisibility(android.view.View.GONE);
                    android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
                    if (params != null) {
                        params.width = 0;
                        params.height = 0;
                        view.setLayoutParams(params);
                    }
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
    // 5. HIDE / FILTER AI-GENERATED CONTENT (Independent Patch)
    // =========================================================================

    public static boolean isAiDescription(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            return AI_TAG_PATTERN.matcher(text).find();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isAiContent(Object aweme) {
        if (aweme == null) return false;
        if (!initialized) {
            ensureInitialized(aweme.getClass().getClassLoader());
        }
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return false;
        }
        try {
            String aid = null;
            try {
                Method getAid = aweme.getClass().getMethod("getAid");
                Object idObj = getAid.invoke(aweme);
                if (idObj != null) aid = idObj.toString();
            } catch (Throwable ignored) {}

            // 1. Native AIGCInfo metadata (Creator disclosed or platform detected)
            Object aigc = null;
            if (getAigcInfoMethod != null) {
                try { aigc = getAigcInfoMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (aigc == null && aigcInfoField != null) {
                try { aigc = aigcInfoField.get(aweme); } catch (Throwable ignored) {}
            }
            if (aigc != null) {
                if (getAigcLabelTypeMethod != null) {
                    try {
                        Object type = getAigcLabelTypeMethod.invoke(aigc);
                        if (type instanceof Number && ((Number) type).intValue() > 0) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [AIGCInfo.AIGCLabelType=" + type + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (aigcLabelTypeField != null) {
                    try {
                        int type = aigcLabelTypeField.getInt(aigc);
                        if (type > 0) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [AIGCInfo.AIGCLabelTypeField=" + type + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (createByAiField != null) {
                    try {
                        if (createByAiField.getBoolean(aigc)) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [AIGCInfo.createByAI=true] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 2. Moderation AIGC metadata
            Object modAigc = null;
            if (getModerationAigcInfoMethod != null) {
                try { modAigc = getModerationAigcInfoMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (modAigc == null && moderationAigcInfoField != null) {
                try { modAigc = moderationAigcInfoField.get(aweme); } catch (Throwable ignored) {}
            }
            if (modAigc != null) {
                if (moderationAigcLabelTypeField != null) {
                    try {
                        int type = moderationAigcLabelTypeField.getInt(modAigc);
                        if (type > 0) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [ModerationAigcInfo.labelType=" + type + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (moderationUserLabelStatusField != null) {
                    try {
                        int status = moderationUserLabelStatusField.getInt(modAigc);
                        if (status > 0) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [ModerationAigcInfo.userLabelStatus=" + status + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (creatorGuidanceStatusField != null) {
                    try {
                        int status = creatorGuidanceStatusField.getInt(modAigc);
                        if (status > 0) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [ModerationAigcInfo.creatorGuidance=" + status + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (moderationCreatorSegmentField != null) {
                    try {
                        Object seg = moderationCreatorSegmentField.get(modAigc);
                        if (seg instanceof String && !((String) seg).isEmpty()) {
                            String s = ((String) seg).toLowerCase();
                            if (s.contains("aigc") || s.contains("short_drama_aigc") || s.equals("ai")) {
                                Log.i(TAG, "[Hide AI-Generated Content] Match [ModerationAigcInfo.segment=" + seg + "] on aid=" + aid);
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 3. C2PA Content Credentials metadata
            Object c2pa = null;
            if (getC2paInfoMethod != null) {
                try { c2pa = getC2paInfoMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (c2pa == null && c2paInfoField != null) {
                try { c2pa = c2paInfoField.get(aweme); } catch (Throwable ignored) {}
            }
            if (c2pa != null) {
                if (c2paAigcSrcField != null) {
                    try {
                        Object src = c2paAigcSrcField.get(c2pa);
                        if (src instanceof String && !((String) src).trim().isEmpty()) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [C2PA.aigcSrc=" + src + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (c2paFirstAigcSrcField != null) {
                    try {
                        Object src = c2paFirstAigcSrcField.get(c2pa);
                        if (src instanceof String && !((String) src).trim().isEmpty()) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [C2PA.firstAigcSrc=" + src + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (c2paLastAigcSrcField != null) {
                    try {
                        Object src = c2paLastAigcSrcField.get(c2pa);
                        if (src instanceof String && !((String) src).trim().isEmpty()) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [C2PA.lastAigcSrc=" + src + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 4. Native AI Creation & Feature Structs
            if (aiAliveInfoField != null && aiAliveInfoField.get(aweme) != null) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [aiAliveInfo] on aid=" + aid);
                return true;
            }
            if (aiPortraitInfoField != null && aiPortraitInfoField.get(aweme) != null) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [aiPortraitInfo] on aid=" + aid);
                return true;
            }
            if (aiRemixInfoField != null) {
                try {
                    Object remix = aiRemixInfoField.get(aweme);
                    if (remix != null) {
                        boolean isRemix = false;
                        try {
                            Field taskIdField = remix.getClass().getDeclaredField("taskId");
                            taskIdField.setAccessible(true);
                            Object taskId = taskIdField.get(remix);
                            if (taskId instanceof String && !((String) taskId).trim().isEmpty()) {
                                isRemix = true;
                            }
                        } catch (Throwable ignored) {}
                        try {
                            Field promptIdField = remix.getClass().getDeclaredField("promptId");
                            promptIdField.setAccessible(true);
                            Object promptId = promptIdField.get(remix);
                            if (promptId instanceof String && !((String) promptId).trim().isEmpty()) {
                                isRemix = true;
                            }
                        } catch (Throwable ignored) {}
                        if (isRemix) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [aiRemixInfo] on aid=" + aid);
                            return true;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (aiTheaterInfoField != null && aiTheaterInfoField.get(aweme) != null) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [aiTheaterInfo] on aid=" + aid);
                return true;
            }
            if (aiChatEditorInfoField != null && aiChatEditorInfoField.get(aweme) != null) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [aiChatEditorInfo] on aid=" + aid);
                return true;
            }

            // 5. Banners containing AIGC tags/overlays
            Object banners = null;
            if (getBannersMethod != null) {
                try { banners = getBannersMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (banners == null && bannersField != null) {
                try { banners = bannersField.get(aweme); } catch (Throwable ignored) {}
            }
            if (banners instanceof List) {
                List<?> bannerList = (List<?>) banners;
                for (Object b : bannerList) {
                    if (b != null) {
                        String bStr = b.toString().toLowerCase();
                        if (bStr.contains("aigc") || bStr.contains("ai_generated")) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [Banner=" + bStr + "] on aid=" + aid);
                            return true;
                        }
                    }
                }
            }

            // 6. Anchors containing AIGC tags
            Object anchors = null;
            if (getAnchorsMethod != null) {
                try { anchors = getAnchorsMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (anchors == null && anchorsField != null) {
                try { anchors = anchorsField.get(aweme); } catch (Throwable ignored) {}
            }
            if (anchors instanceof List) {
                List<?> anchorList = (List<?>) anchors;
                for (Object a : anchorList) {
                    if (a != null) {
                        String aStr = a.toString().toLowerCase();
                        if (aStr.contains("aigc") || aStr.contains("anchor_aigc")) {
                            Log.i(TAG, "[Hide AI-Generated Content] Match [Anchor=" + aStr + "] on aid=" + aid);
                            return true;
                        }
                    }
                }
            }

            // 7. Caption & Description tags (#aigenerated, #aigc, etc.)
            Object descObj = null;
            if (getDescMethod != null) {
                try { descObj = getDescMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (descObj == null && descField != null) {
                try { descObj = descField.get(aweme); } catch (Throwable ignored) {}
            }
            if (descObj instanceof String && isAiDescription((String) descObj)) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [Desc=" + descObj + "] on aid=" + aid);
                return true;
            }

            Object contentDescObj = null;
            if (getContentDescMethod != null) {
                try { contentDescObj = getContentDescMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (contentDescObj == null && contentDescField != null) {
                try { contentDescObj = contentDescField.get(aweme); } catch (Throwable ignored) {}
            }
            if (contentDescObj instanceof String && isAiDescription((String) contentDescObj)) {
                Log.i(TAG, "[Hide AI-Generated Content] Match [ContentDesc=" + contentDescObj + "] on aid=" + aid);
                return true;
            }

        } catch (Throwable ignored) {}
        return false;
    }

    @SuppressWarnings("unchecked")
    public static void filterAiContentInList(Object listObj) {
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
                    if (isAiContent(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide AI-Generated Content] Pruned " + removed + " AI-generated video(s) from feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterAiContentInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());
            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                filterAiContentInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static void filterAiContentInFollowFeedList(Object followFeedList) {
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
                    if (isAiContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide AI-Generated Content] Pruned " + removed + " AI-generated video(s) from Following feed.");
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static void filterAiContentInFriendsV3Feeds(Object listObj) {
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
                    Object aweme = extractAwemeFromFriendsV3FeedModel(item);
                    if (isAiContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide AI-Generated Content] Pruned " + removed + " AI-generated video(s) from Friends V3 feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static void filterAiContentInFriendsFeedData(Object listObj) {
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
                    Object aweme = extractAwemeFromFriendsFeed(item);
                    if (isAiContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide AI-Generated Content] Pruned " + removed + " AI-generated video(s) from Friends feed.");
                }
            } catch (Throwable ignored) {}
        }
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
