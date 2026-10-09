package com.kveld9.morphe.extension.tiktok;

import android.net.Uri;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * High-performance, crash-safe feed ad filter for TikTok (com.zhiliaoapp.musically).
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
    private static Method getLiveAwesomeSplashInfoMethod;
    private static Field liveAwesomeSplashInfoField;
    private static Method getAdAwemeSourceMethod;
    private static Field adAwemeSourceField;
    private static Field isAdField;
    private static Field isSoftAdField;
    private static Field awemeRawAdField;
    private static Field linkAdDataField;
    private static Method getAdLinkTypeMethod;
    private static Field adLinkTypeField;
    private static Method getCommercialVideoInfoMethod;
    private static Field commercialVideoInfoField;

    private static Method isFriendsTabFakeAwemeMethod;
    private static Method getRecommendCardTypeMethod;
    private static Field recommendCardTypeField;
    private static Method getCardInsertInfoMethod;
    private static Field cardInsertInfoField;
    private static Method getCardTypeMethod;
    private static Field cardTypeField;
    private static Method getExploreCommunityCommentShowTypeMethod;
    private static Field exploreCommunityCommentShowTypeField;

    private static Method getWithSurveyMethod;
    private static Field withSurveyField;
    private static Method getSurveyInfoMethod;
    private static Field surveyInfoField;
    private static Field surveyInfoAltField;
    private static Method getSurveyInfosMethod;
    private static Field surveyInfosField;
    private static Method getSurveyKeyMethod;
    private static Field surveyKeyField;
    private static Method getPersonalizedSurveyUIMethod;
    private static Field mPersonalizedSurveyUIField;
    private static Method getOnboardingSurveyMethod;
    private static Field mOnboardingSurveyField;
    private static Method getPersonalizedOnboardingSurveyMethod;
    private static Field mPersonalizedOnboardingSurveyField;
    private static Field questionInfoField;
    private static Field ueFeedInfoField;

    private static Method setWithSurveyMethod;
    private static Method setSurveyInfoMethod;
    private static Method setSurveyInfosMethod;
    private static Method setSurveyKeyMethod;
    private static Method setMPersonalizedSurveyUIMethod;
    private static Method setOnboardingSurveyMethod;
    private static Method setMPersonalizedOnboardingSurveyMethod;

    private static Method getVideoMethod;
    private static Field videoField;

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
    private static Field friendsV3ReposterField;

    private static Method friendsFeedGetAwemeMethod;
    private static Field friendsFeedAwemeField;

    private static Method awemeGetAuthorMethod;
    private static Field awemeAuthorField;
    private static Method awemeGetFeedRelationLabelMethod;
    private static Field awemeFeedRelationLabelField;
    private static Method awemeGetRelationLabelMethod;
    private static Field awemeRelationLabelField;
    private static Method awemeGetRelationRecommendInfoMethod;
    private static Field awemeRelationRecommendInfoField;
    private static Method awemeGetRecReasonsStructMethod;
    private static Field awemeRecReasonsStructField;

    private static Method userGetFollowStatusMethod;
    private static Field userFollowStatusField;
    private static Method userGetMatchedFriendStructMethod;
    private static Field userMatchedFriendStructField;
    private static Method userIsMatchedFriendAvailableMethod;
    private static Field userMatchedFriendAvailableField;
    private static Method userGetUidMethod;
    private static Field userUidField;
    private static Method userGetRecTypeMethod;
    private static Field userRecTypeField;

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

    private static Method getCommerceVideoAuthInfoMethod;
    private static Field mCommerceVideoAuthInfoField;
    private static Field commerceVideoAuthInfoAltField;
    private static Method getBrandContentAccountsMethod;
    private static Field brandContentAccountsField;
    private static Method getStarAtlasOrderIdMethod;
    private static Field starAtlasOrderIdField;
    private static Method getPromoteIconTextMethod;
    private static Field promoteIconTextField;
    private static Method getPromoteModelMethod;
    private static Field promoteModelField;

    private static Method getBCHashtagMethod;
    private static Field bcHashtagField;
    private static Method isBrandedContentMethod;
    private static Field brandedContentTypeField;
    private static Method isBrandOrganicContentMethod;
    private static Field brandOrganicTypeField;
    private static Method getCommerceLabelInfoMethod;
    private static Field commerceLabelInfoField;
    private static Field bcLabelDisplayTypeField;
    private static Method getEcSearchBoBcLabelTextMethod;
    private static Field ecSearchBoBcLabelTextField;

    private static final java.util.regex.Pattern AI_TAG_PATTERN = java.util.regex.Pattern.compile(
        "(?i)(?:#(?:aigenerated|ai_generated|aigc|generadoporia|generado_por_ia|generadoconia|aiart|aivideo|iaart|iavideo|midjourney|sora|stable_diffusion|stablediffusion|dalle|chatgpt|runwayml|klingai|lumaai)\\b|\\[(?:ai[-_ ]?generated|generado por ia|aigc)\\]|\\b(?:ai[-_ ]generated|generado por ia|generado con ia)\\b)"
    );

    private static final java.util.regex.Pattern PROMOTIONAL_TAG_PATTERN = java.util.regex.Pattern.compile(
        "(?i)(?:#(?:paidpartnership|paid_partnership|brandedcontent|branded_content|contenidopromocional|contenido_promocional|promocional|colaboracionpagada|colaboracion_pagada|parceriapaga|parceria_paga|ad|publicidad|werbung|gesponsert|publi)\\b|\\[(?:paid partnership|branded content|contenido promocional|contenu sponsorisé|contenu sponsorise|colaboración pagada|colaboracion pagada|parceria paga|werbung|gesponsert)\\]|\\b(?:paid partnership|branded content|contenido promocional|contenu sponsorisé|contenu sponsorise|colaboración pagada|colaboracion pagada|parceria paga)\\b)"
    );

    private static final String SHOP_PROMO_MARKER = "placeholder_product_id";

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        "is_from_webapp", "sender_device", "_r", "checksum", "sec_user_id",
        "ug_source", "share_app_id", "share_item_id", "share_link_id",
        "source", "timestamp", "user_id", "u_code", "tt_from",
        "utm_source", "utm_campaign", "utm_medium",
        "share_iid", "sharer_id", "ugbiz_name"
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
            try { getLiveAwesomeSplashInfoMethod = awemeClass.getMethod("getLiveAwesomeSplashInfo"); getLiveAwesomeSplashInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { liveAwesomeSplashInfoField = awemeClass.getDeclaredField("mLiveAwesomeSplashInfo"); liveAwesomeSplashInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { getAdAwemeSourceMethod = awemeClass.getMethod("getAdAwemeSource"); getAdAwemeSourceMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { adAwemeSourceField = awemeClass.getDeclaredField("adAwemeSource"); adAwemeSourceField.setAccessible(true); } catch (Throwable ignored) {}
            try { isAdField = awemeClass.getDeclaredField("_isAd"); isAdField.setAccessible(true); } catch (Throwable ignored) {}
            try { isSoftAdField = awemeClass.getDeclaredField("_isSoftAd"); isSoftAdField.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeRawAdField = awemeClass.getDeclaredField("awemeRawAd"); awemeRawAdField.setAccessible(true); } catch (Throwable ignored) {}
            try { linkAdDataField = awemeClass.getDeclaredField("linkAdData"); linkAdDataField.setAccessible(true); } catch (Throwable ignored) {}
            try { getAdLinkTypeMethod = awemeClass.getMethod("getAdLinkType"); getAdLinkTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { adLinkTypeField = awemeClass.getDeclaredField("adLinkType"); adLinkTypeField.setAccessible(true); } catch (Throwable ignored) {}
            try { getCommercialVideoInfoMethod = awemeClass.getMethod("getCommercialVideoInfo"); getCommercialVideoInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { commercialVideoInfoField = awemeClass.getDeclaredField("commercialVideoInfo"); commercialVideoInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { isFriendsTabFakeAwemeMethod = awemeClass.getMethod("isFriendsTabFakeAweme"); isFriendsTabFakeAwemeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { getRecommendCardTypeMethod = awemeClass.getMethod("getRecommendCardType"); getRecommendCardTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { recommendCardTypeField = awemeClass.getDeclaredField("recommendCardType"); recommendCardTypeField.setAccessible(true); } catch (Throwable ignored) {}
            try { getCardInsertInfoMethod = awemeClass.getMethod("getCardInsertInfo"); getCardInsertInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { cardInsertInfoField = awemeClass.getDeclaredField("cardInsertInfo"); cardInsertInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { getExploreCommunityCommentShowTypeMethod = awemeClass.getMethod("getExploreCommunityCommentShowType"); getExploreCommunityCommentShowTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { exploreCommunityCommentShowTypeField = awemeClass.getDeclaredField("exploreCommunityCommentShowType"); exploreCommunityCommentShowTypeField.setAccessible(true); } catch (Throwable ignored) {}
            try { getVideoMethod = awemeClass.getMethod("getVideo"); getVideoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { videoField = awemeClass.getDeclaredField("video"); videoField.setAccessible(true); } catch (Throwable ignored) {}

            try { getWithSurveyMethod = awemeClass.getMethod("getWithSurvey"); getWithSurveyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { withSurveyField = awemeClass.getDeclaredField("withSurvey"); withSurveyField.setAccessible(true); } catch (Throwable ignored) {}
            try { getSurveyInfoMethod = awemeClass.getMethod("getSurveyInfo"); getSurveyInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { surveyInfoField = awemeClass.getDeclaredField("_surveyInfo"); surveyInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { surveyInfoAltField = awemeClass.getDeclaredField("surveyInfo"); surveyInfoAltField.setAccessible(true); } catch (Throwable ignored) {}
            try { getSurveyInfosMethod = awemeClass.getMethod("getSurveyInfos"); getSurveyInfosMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { surveyInfosField = awemeClass.getDeclaredField("surveyInfos"); surveyInfosField.setAccessible(true); } catch (Throwable ignored) {}
            try { getSurveyKeyMethod = awemeClass.getMethod("getSurveyKey"); getSurveyKeyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { surveyKeyField = awemeClass.getDeclaredField("surveyKey"); surveyKeyField.setAccessible(true); } catch (Throwable ignored) {}
            try { getPersonalizedSurveyUIMethod = awemeClass.getMethod("getPersonalizedSurveyUI"); getPersonalizedSurveyUIMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { mPersonalizedSurveyUIField = awemeClass.getDeclaredField("mPersonalizedSurveyUI"); mPersonalizedSurveyUIField.setAccessible(true); } catch (Throwable ignored) {}
            try { getOnboardingSurveyMethod = awemeClass.getMethod("getOnboardingSurvey"); getOnboardingSurveyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { mOnboardingSurveyField = awemeClass.getDeclaredField("mOnboardingSurvey"); mOnboardingSurveyField.setAccessible(true); } catch (Throwable ignored) {}
            try { getPersonalizedOnboardingSurveyMethod = awemeClass.getMethod("getPersonalizedOnboardingSurvey"); getPersonalizedOnboardingSurveyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { mPersonalizedOnboardingSurveyField = awemeClass.getDeclaredField("mPersonalizedOnboardingSurvey"); mPersonalizedOnboardingSurveyField.setAccessible(true); } catch (Throwable ignored) {}
            try { questionInfoField = awemeClass.getDeclaredField("questionInfo"); questionInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { ueFeedInfoField = awemeClass.getDeclaredField("ueFeedInfo"); ueFeedInfoField.setAccessible(true); } catch (Throwable ignored) {}

            try { setWithSurveyMethod = awemeClass.getMethod("setWithSurvey", boolean.class); setWithSurveyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try {
                for (Method m : awemeClass.getMethods()) {
                    if ("setSurveyInfo".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        setSurveyInfoMethod = m;
                        setSurveyInfoMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            try { setSurveyInfosMethod = awemeClass.getMethod("setSurveyInfos", List.class); setSurveyInfosMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { setSurveyKeyMethod = awemeClass.getMethod("setSurveyKey", String.class); setSurveyKeyMethod.setAccessible(true); } catch (Throwable ignored) {}
            try {
                for (Method m : awemeClass.getMethods()) {
                    if ("setMPersonalizedSurveyUI".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        setMPersonalizedSurveyUIMethod = m;
                        setMPersonalizedSurveyUIMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            try {
                for (Method m : awemeClass.getMethods()) {
                    if ("setOnboardingSurvey".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        setOnboardingSurveyMethod = m;
                        setOnboardingSurveyMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            try {
                for (Method m : awemeClass.getMethods()) {
                    if ("setMPersonalizedOnboardingSurvey".equals(m.getName()) && m.getParameterTypes().length == 1) {
                        setMPersonalizedOnboardingSurveyMethod = m;
                        setMPersonalizedOnboardingSurveyMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}

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
                    try { friendsV3ReposterField = repostClass.getDeclaredField("reposter"); friendsV3ReposterField.setAccessible(true); } catch (Throwable ignored) {}
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

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> userClass = null;
                try {
                    userClass = loader.loadClass("com.ss.android.ugc.aweme.profile.model.User");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        userClass = classLoader.loadClass("com.ss.android.ugc.aweme.profile.model.User");
                    }
                }
                if (userClass != null) {
                    try { userGetFollowStatusMethod = userClass.getMethod("getFollowStatus"); userGetFollowStatusMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { userFollowStatusField = userClass.getDeclaredField("followStatus"); userFollowStatusField.setAccessible(true); } catch (Throwable ignored) {}
                    try { userGetMatchedFriendStructMethod = userClass.getMethod("getMatchedFriendStruct"); userGetMatchedFriendStructMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { userMatchedFriendStructField = userClass.getDeclaredField("matchedFriendStruct"); userMatchedFriendStructField.setAccessible(true); } catch (Throwable ignored) {}
                    try { userIsMatchedFriendAvailableMethod = userClass.getMethod("isMatchedFriendAvailable"); userIsMatchedFriendAvailableMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { userMatchedFriendAvailableField = userClass.getDeclaredField("matchedFriendAvailable"); userMatchedFriendAvailableField.setAccessible(true); } catch (Throwable ignored) {}
                    try { userGetUidMethod = userClass.getMethod("getUid"); userGetUidMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { userUidField = userClass.getDeclaredField("uid"); userUidField.setAccessible(true); } catch (Throwable ignored) {}
                    try { userGetRecTypeMethod = userClass.getMethod("getRecType"); userGetRecTypeMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { userRecTypeField = userClass.getDeclaredField("recType"); userRecTypeField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try { awemeGetAuthorMethod = awemeClass.getMethod("getAuthor"); awemeGetAuthorMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeAuthorField = awemeClass.getDeclaredField("author"); awemeAuthorField.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeGetFeedRelationLabelMethod = awemeClass.getMethod("getFeedRelationLabel"); awemeGetFeedRelationLabelMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeFeedRelationLabelField = awemeClass.getDeclaredField("feedRelationLabel"); awemeFeedRelationLabelField.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeGetRelationLabelMethod = awemeClass.getMethod("getRelationLabel"); awemeGetRelationLabelMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeRelationLabelField = awemeClass.getDeclaredField("relationLabel"); awemeRelationLabelField.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeGetRelationRecommendInfoMethod = awemeClass.getMethod("getRelationRecommendInfo"); awemeGetRelationRecommendInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeRelationRecommendInfoField = awemeClass.getDeclaredField("relationRecommendInfo"); awemeRelationRecommendInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeGetRecReasonsStructMethod = awemeClass.getMethod("getRecReasonsStruct"); awemeGetRecReasonsStructMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { awemeRecReasonsStructField = awemeClass.getDeclaredField("recReasonsStruct"); awemeRecReasonsStructField.setAccessible(true); } catch (Throwable ignored) {}

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

            try { getCommerceVideoAuthInfoMethod = awemeClass.getMethod("getCommerceVideoAuthInfo"); getCommerceVideoAuthInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { mCommerceVideoAuthInfoField = awemeClass.getDeclaredField("mCommerceVideoAuthInfo"); mCommerceVideoAuthInfoField.setAccessible(true); } catch (Throwable ignored) {}
            try { commerceVideoAuthInfoAltField = awemeClass.getDeclaredField("commerceVideoAuthInfo"); commerceVideoAuthInfoAltField.setAccessible(true); } catch (Throwable ignored) {}
            try { getBrandContentAccountsMethod = awemeClass.getMethod("getBrandContentAccounts"); getBrandContentAccountsMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { brandContentAccountsField = awemeClass.getDeclaredField("brandContentAccounts"); brandContentAccountsField.setAccessible(true); } catch (Throwable ignored) {}
            try { getStarAtlasOrderIdMethod = awemeClass.getMethod("getStarAtlasOrderId"); getStarAtlasOrderIdMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { starAtlasOrderIdField = awemeClass.getDeclaredField("starAtlasOrderId"); starAtlasOrderIdField.setAccessible(true); } catch (Throwable ignored) {}
            try { getPromoteIconTextMethod = awemeClass.getMethod("getPromoteIconText"); getPromoteIconTextMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { promoteIconTextField = awemeClass.getDeclaredField("promoteIconText"); promoteIconTextField.setAccessible(true); } catch (Throwable ignored) {}
            try { getPromoteModelMethod = awemeClass.getMethod("getPromoteModel"); getPromoteModelMethod.setAccessible(true); } catch (Throwable ignored) {}
            try { promoteModelField = awemeClass.getDeclaredField("promoteModel"); promoteModelField.setAccessible(true); } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> commerceStructClass = null;
                try {
                    commerceStructClass = loader.loadClass("com.ss.android.ugc.aweme.commerce.AwemeCommerceStruct");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        commerceStructClass = classLoader.loadClass("com.ss.android.ugc.aweme.commerce.AwemeCommerceStruct");
                    }
                }
                if (commerceStructClass != null) {
                    try { getBCHashtagMethod = commerceStructClass.getMethod("getBCHashtag"); getBCHashtagMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { bcHashtagField = commerceStructClass.getDeclaredField("bcHashtag"); bcHashtagField.setAccessible(true); } catch (Throwable ignored) {}
                    try { isBrandedContentMethod = commerceStructClass.getMethod("isBrandedContent"); isBrandedContentMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { brandedContentTypeField = commerceStructClass.getDeclaredField("brandedContentType"); brandedContentTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { isBrandOrganicContentMethod = commerceStructClass.getMethod("isBrandOrganicContent"); isBrandOrganicContentMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { brandOrganicTypeField = commerceStructClass.getDeclaredField("brandOrganicType"); brandOrganicTypeField.setAccessible(true); } catch (Throwable ignored) {}
                    try { getCommerceLabelInfoMethod = commerceStructClass.getMethod("getCommerceLabelInfo"); getCommerceLabelInfoMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { commerceLabelInfoField = commerceStructClass.getDeclaredField("commerceLabelInfo"); commerceLabelInfoField.setAccessible(true); } catch (Throwable ignored) {}
                    try { getEcSearchBoBcLabelTextMethod = commerceStructClass.getMethod("getEcSearchBoBcLabelText"); getEcSearchBoBcLabelTextMethod.setAccessible(true); } catch (Throwable ignored) {}
                    try { ecSearchBoBcLabelTextField = commerceStructClass.getDeclaredField("ecSearchBoBcLabelText"); ecSearchBoBcLabelTextField.setAccessible(true); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
                Class<?> labelInfoClass = null;
                try {
                    labelInfoClass = loader.loadClass("com.ss.android.ugc.aweme.commerce.CommerceLabelInfo");
                } catch (Throwable t) {
                    if (classLoader != loader) {
                        labelInfoClass = classLoader.loadClass("com.ss.android.ugc.aweme.commerce.CommerceLabelInfo");
                    }
                }
                if (labelInfoClass != null) {
                    try { bcLabelDisplayTypeField = labelInfoClass.getDeclaredField("bcLabelDisplayType"); bcLabelDisplayTypeField.setAccessible(true); } catch (Throwable ignored) {}
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
            return isSearchAdItem(aweme);
        }
        try {
            if (isAdMethod != null) {
                try {
                    if (Boolean.TRUE.equals(isAdMethod.invoke(aweme))) return true;
                } catch (Throwable ignored) {}
            }
            if (isAdField != null) {
                try {
                    Object val = isAdField.get(aweme);
                    if (Boolean.TRUE.equals(val) || (val instanceof Number && ((Number) val).intValue() > 0)) {
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (isSoftAdMethod != null) {
                try {
                    if (Boolean.TRUE.equals(isSoftAdMethod.invoke(aweme))) return true;
                } catch (Throwable ignored) {}
            }
            if (isSoftAdField != null) {
                try {
                    Object val = isSoftAdField.get(aweme);
                    if (Boolean.TRUE.equals(val) || (val instanceof Number && ((Number) val).intValue() > 0)) {
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (getAwemeRawAdMethod != null) {
                try {
                    if (getAwemeRawAdMethod.invoke(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (awemeRawAdField != null) {
                try {
                    if (awemeRawAdField.get(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (getLinkAdDataMethod != null) {
                try {
                    if (getLinkAdDataMethod.invoke(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (linkAdDataField != null) {
                try {
                    if (linkAdDataField.get(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (isWithPromotionalMusicMethod != null) {
                try {
                    if (Boolean.TRUE.equals(isWithPromotionalMusicMethod.invoke(aweme))) return true;
                } catch (Throwable ignored) {}
            }
            if (getLiveAwesomeSplashInfoMethod != null) {
                try {
                    if (getLiveAwesomeSplashInfoMethod.invoke(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (liveAwesomeSplashInfoField != null) {
                try {
                    if (liveAwesomeSplashInfoField.get(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (getAdAwemeSourceMethod != null) {
                try {
                    Object src = getAdAwemeSourceMethod.invoke(aweme);
                    if (src instanceof Number && ((Number) src).intValue() > 0) return true;
                } catch (Throwable ignored) {}
            }
            if (adAwemeSourceField != null) {
                try {
                    Object src = adAwemeSourceField.get(aweme);
                    if (src instanceof Number && ((Number) src).intValue() > 0) return true;
                } catch (Throwable ignored) {}
            }
            if (getAdLinkTypeMethod != null) {
                try {
                    Object linkType = getAdLinkTypeMethod.invoke(aweme);
                    if (linkType instanceof Number && ((Number) linkType).intValue() > 0) return true;
                } catch (Throwable ignored) {}
            }
            if (adLinkTypeField != null) {
                try {
                    Object linkType = adLinkTypeField.get(aweme);
                    if (linkType instanceof Number && ((Number) linkType).intValue() > 0) return true;
                } catch (Throwable ignored) {}
            }
            if (getCommercialVideoInfoMethod != null) {
                try {
                    if (getCommercialVideoInfoMethod.invoke(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (commercialVideoInfoField != null) {
                try {
                    if (commercialVideoInfoField.get(aweme) != null) return true;
                } catch (Throwable ignored) {}
            }
            if (getShareUrlMethod != null) {
                try {
                    Object url = getShareUrlMethod.invoke(aweme);
                    if (url instanceof String && ((String) url).contains(SHOP_PROMO_MARKER)) return true;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isSearchAdItem(Object item) {
        if (item == null) return false;
        try {
            Class<?> clazz = item.getClass();
            String name = clazz.getName();
            if (name.contains("SearchMixFeed") || name.contains("SearchItemStruct")) {
                try {
                    Method m = clazz.getMethod("getPreciseAd");
                    if (m.invoke(item) != null) return true;
                } catch (Throwable ignored) {}
                try {
                    Method m = clazz.getMethod("getBrandZoneCard");
                    if (m.invoke(item) != null) return true;
                } catch (Throwable ignored) {}
                try {
                    Method m = clazz.getMethod("getAweme");
                    Object inner = m.invoke(item);
                    if (inner != null && isAd(inner)) return true;
                } catch (Throwable ignored) {}
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

    public static boolean hasMediaContent(Object aweme) {
        if (aweme == null) return false;
        try {
            if (getVideoMethod != null && getVideoMethod.invoke(aweme) != null) return true;
            if (videoField != null && videoField.get(aweme) != null) return true;
        } catch (Throwable ignored) {}
        try {
            if (TikTokMediaHook.isPhotoMode(aweme)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isSurveyCard(Object aweme) {
        if (aweme == null) return false;
        try {
            if (getWithSurveyMethod != null) {
                Object res = getWithSurveyMethod.invoke(aweme);
                if (Boolean.TRUE.equals(res)) return true;
            } else if (withSurveyField != null) {
                if (withSurveyField.getBoolean(aweme)) return true;
            }
            if (getSurveyInfoMethod != null && getSurveyInfoMethod.invoke(aweme) != null) return true;
            if (surveyInfoField != null && surveyInfoField.get(aweme) != null) return true;
            if (surveyInfoAltField != null && surveyInfoAltField.get(aweme) != null) return true;
            if (getSurveyInfosMethod != null) {
                Object infos = getSurveyInfosMethod.invoke(aweme);
                if (infos instanceof List && !((List<?>) infos).isEmpty()) return true;
            } else if (surveyInfosField != null) {
                Object infos = surveyInfosField.get(aweme);
                if (infos instanceof List && !((List<?>) infos).isEmpty()) return true;
            }
            if (mPersonalizedSurveyUIField != null && mPersonalizedSurveyUIField.get(aweme) != null) return true;
            if (mOnboardingSurveyField != null && mOnboardingSurveyField.get(aweme) != null) return true;
            if (mPersonalizedOnboardingSurveyField != null && mPersonalizedOnboardingSurveyField.get(aweme) != null) return true;
            if (questionInfoField != null && questionInfoField.get(aweme) != null) return true;
            if (ueFeedInfoField != null && ueFeedInfoField.get(aweme) != null) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static void stripSurveyBloat(Object aweme) {
        if (aweme == null) return;
        if (awemeClass != null && !awemeClass.isInstance(aweme)) {
            return;
        }
        try {
            if (setWithSurveyMethod != null) setWithSurveyMethod.invoke(aweme, false);
            if (withSurveyField != null) withSurveyField.setBoolean(aweme, false);
            if (setSurveyInfoMethod != null) setSurveyInfoMethod.invoke(aweme, (Object) null);
            if (surveyInfoField != null) surveyInfoField.set(aweme, null);
            if (surveyInfoAltField != null) surveyInfoAltField.set(aweme, null);
            if (setSurveyInfosMethod != null) setSurveyInfosMethod.invoke(aweme, (Object) null);
            if (surveyInfosField != null) surveyInfosField.set(aweme, null);
            if (setSurveyKeyMethod != null) setSurveyKeyMethod.invoke(aweme, (Object) null);
            if (surveyKeyField != null) surveyKeyField.set(aweme, null);
            if (setMPersonalizedSurveyUIMethod != null) setMPersonalizedSurveyUIMethod.invoke(aweme, (Object) null);
            if (mPersonalizedSurveyUIField != null) mPersonalizedSurveyUIField.set(aweme, null);
            if (setOnboardingSurveyMethod != null) setOnboardingSurveyMethod.invoke(aweme, (Object) null);
            if (mOnboardingSurveyField != null) mOnboardingSurveyField.set(aweme, null);
            if (setMPersonalizedOnboardingSurveyMethod != null) setMPersonalizedOnboardingSurveyMethod.invoke(aweme, (Object) null);
            if (mPersonalizedOnboardingSurveyField != null) mPersonalizedOnboardingSurveyField.set(aweme, null);
            if (questionInfoField != null) questionInfoField.set(aweme, null);
            if (ueFeedInfoField != null) ueFeedInfoField.set(aweme, null);
            if (exploreCommunityCommentShowTypeField != null) exploreCommunityCommentShowTypeField.set(aweme, null);
        } catch (Throwable ignored) {}
    }

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
            // Any CardInsertInfo attached to Aweme indicates an inserted non-video card
            // (Explore Community/Topic Lynx cards, RecUser, Instant Games, Search Interest, etc.)
            if (getCardInsertInfoMethod != null) {
                Object cardInfo = getCardInsertInfoMethod.invoke(aweme);
                if (cardInfo != null) {
                    return true;
                }
            } else if (cardInsertInfoField != null) {
                Object cardInfo = cardInsertInfoField.get(aweme);
                if (cardInfo != null) {
                    return true;
                }
            }

            // 4. In-Feed Recommendation Cards
            if (getRecommendCardTypeMethod != null) {
                Object rct = getRecommendCardTypeMethod.invoke(aweme);
                if (rct instanceof Number && ((Number) rct).intValue() > 0) {
                    return true;
                }
            } else if (recommendCardTypeField != null) {
                Object rct = recommendCardTypeField.get(aweme);
                if (rct instanceof Number && ((Number) rct).intValue() > 0) {
                    return true;
                }
            }

            // 5. Standalone In-Feed Surveys / Questionnaires without media content
            if (isSurveyCard(aweme) && !hasMediaContent(aweme)) {
                return true;
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
                    } else {
                        stripSurveyBloat(item);
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
                    } else {
                        Object aweme = extractAwemeFromFollowItem(followItem);
                        if (aweme != null) {
                            stripSurveyBloat(aweme);
                        }
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
                    } else {
                        Object aweme = extractAwemeFromFriendsV3FeedModel(item);
                        if (aweme != null) {
                            stripSurveyBloat(aweme);
                        }
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
                    } else {
                        Object aweme = extractAwemeFromFriendsFeed(item);
                        if (aweme != null) {
                            stripSurveyBloat(aweme);
                        }
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

    public static void disableRecUserBigCardInFriendsTab(Object config) {
        if (config == null) return;
        try {
            try {
                Field friendsTabField = config.getClass().getDeclaredField("friendsTabConfig");
                friendsTabField.setAccessible(true);
                Object friendsTab = friendsTabField.get(config);
                if (friendsTab != null) {
                    for (String name : new String[]{"showsPerDay", "showsPerDayLite", "showsPerDayPlus"}) {
                        try {
                            Field f = friendsTab.getClass().getDeclaredField(name);
                            f.setAccessible(true);
                            f.setInt(friendsTab, 0);
                        } catch (Throwable ignored) {}
                    }
                    for (String name : new String[]{"interval", "intervalLite", "intervalPlus"}) {
                        try {
                            Field f = friendsTab.getClass().getDeclaredField(name);
                            f.setAccessible(true);
                            f.setInt(friendsTab, Integer.MAX_VALUE);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
            try {
                Field fypField = config.getClass().getDeclaredField("fypConfig");
                fypField.setAccessible(true);
                Object fyp = fypField.get(config);
                if (fyp != null) {
                    for (String name : new String[]{"showsPerDay", "showsPerDayLite", "showsPerDayPlus"}) {
                        try {
                            Field f = fyp.getClass().getDeclaredField(name);
                            f.setAccessible(true);
                            f.setInt(fyp, 0);
                        } catch (Throwable ignored) {}
                    }
                    for (String name : new String[]{"interval", "intervalLite", "intervalPlus"}) {
                        try {
                            Field f = fyp.getClass().getDeclaredField(name);
                            f.setAccessible(true);
                            f.setInt(fyp, Integer.MAX_VALUE);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
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
                            String s = ((String) seg).toLowerCase(Locale.ROOT);
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
                        String bStr = b.toString().toLowerCase(Locale.ROOT);
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
                        String aStr = a.toString().toLowerCase(Locale.ROOT);
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

    private static volatile Field friendsV3FeedsField;
    private static volatile Field friendFeedDataField;
    private static volatile Field newlyShownMafIdsField;
    private static volatile Field cardInsertResultsField;
    private static volatile Field insertedResultsField;

    public static void filterAiContentInFriendsV3Response(Object response) {
        if (response == null) return;
        try {
            Field field = friendsV3FeedsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendsV3Feeds");
                field.setAccessible(true);
                friendsV3FeedsField = field;
            }
            filterAiContentInFriendsV3Feeds(field.get(response));
        } catch (Throwable ignored) {}
    }

    public static void filterAiContentInFriendsFeedResponse(Object response) {
        if (response == null) return;
        try {
            Field field = friendFeedDataField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendFeedData");
                field.setAccessible(true);
                friendFeedDataField = field;
            }
            filterAiContentInFriendsFeedData(field.get(response));
        } catch (Throwable ignored) {}
    }

    public static void filterFeedBloatInFriendsV3Response(Object response) {
        if (response == null) return;
        try {
            Field field = friendsV3FeedsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendsV3Feeds");
                field.setAccessible(true);
                friendsV3FeedsField = field;
            }
            filterFeedBloatInFriendsV3Feeds(field.get(response));
        } catch (Throwable ignored) {}
        try {
            Field field = newlyShownMafIdsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("newlyShownMafIds");
                field.setAccessible(true);
                newlyShownMafIdsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
    }

    public static void filterFeedBloatInFriendsFeedResponse(Object response) {
        if (response == null) return;
        try {
            Field field = friendFeedDataField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendFeedData");
                field.setAccessible(true);
                friendFeedDataField = field;
            }
            filterFeedBloatInFriendsFeedData(field.get(response));
        } catch (Throwable ignored) {}
        try {
            Field field = cardInsertResultsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("cardInsertResults");
                field.setAccessible(true);
                cardInsertResultsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
        try {
            Field field = insertedResultsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("insertedResults");
                field.setAccessible(true);
                insertedResultsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // 6. HIDE / FILTER PROMOTIONAL CONTENT (Independent Patch)
    // =========================================================================

    public static boolean isPromotionalDescription(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            return PROMOTIONAL_TAG_PATTERN.matcher(text).find();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isPromotionalContent(Object aweme) {
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

            // 1. Native AwemeCommerceStruct metadata (bCHashtag, brandedContentType, brandOrganicType, etc.)
            Object commerceAuth = null;
            if (getCommerceVideoAuthInfoMethod != null) {
                try { commerceAuth = getCommerceVideoAuthInfoMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (commerceAuth == null && mCommerceVideoAuthInfoField != null) {
                try { commerceAuth = mCommerceVideoAuthInfoField.get(aweme); } catch (Throwable ignored) {}
            }
            if (commerceAuth == null && commerceVideoAuthInfoAltField != null) {
                try { commerceAuth = commerceVideoAuthInfoAltField.get(aweme); } catch (Throwable ignored) {}
            }
            if (commerceAuth != null) {
                if (getBCHashtagMethod != null) {
                    try {
                        Object tag = getBCHashtagMethod.invoke(commerceAuth);
                        if (tag instanceof String && !((String) tag).trim().isEmpty()) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.bCHashtag=" + tag + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (bcHashtagField != null) {
                    try {
                        Object tag = bcHashtagField.get(commerceAuth);
                        if (tag instanceof String && !((String) tag).trim().isEmpty()) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.bcHashtagField=" + tag + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (isBrandedContentMethod != null) {
                    try {
                        if (Boolean.TRUE.equals(isBrandedContentMethod.invoke(commerceAuth))) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.isBrandedContent=true] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (brandedContentTypeField != null) {
                    try {
                        long bct = brandedContentTypeField.getLong(commerceAuth);
                        if (bct > 0) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.brandedContentType=" + bct + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (isBrandOrganicContentMethod != null) {
                    try {
                        if (Boolean.TRUE.equals(isBrandOrganicContentMethod.invoke(commerceAuth))) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.isBrandOrganicContent=true] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (brandOrganicTypeField != null) {
                    try {
                        long bot = brandOrganicTypeField.getLong(commerceAuth);
                        if (bot > 0) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.brandOrganicType=" + bot + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (getCommerceLabelInfoMethod != null || commerceLabelInfoField != null) {
                    Object labelInfo = null;
                    if (getCommerceLabelInfoMethod != null) {
                        try { labelInfo = getCommerceLabelInfoMethod.invoke(commerceAuth); } catch (Throwable ignored) {}
                    }
                    if (labelInfo == null && commerceLabelInfoField != null) {
                        try { labelInfo = commerceLabelInfoField.get(commerceAuth); } catch (Throwable ignored) {}
                    }
                    if (labelInfo != null && bcLabelDisplayTypeField != null) {
                        try {
                            int dt = bcLabelDisplayTypeField.getInt(labelInfo);
                            if (dt == 1) {
                                Log.i(TAG, "[Hide Promotional Content] Match [CommerceLabelInfo.bcLabelDisplayType=1] on aid=" + aid);
                                return true;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
                if (getEcSearchBoBcLabelTextMethod != null) {
                    try {
                        Object txt = getEcSearchBoBcLabelTextMethod.invoke(commerceAuth);
                        if (txt instanceof String && !((String) txt).trim().isEmpty()) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.ecSearchBoBcLabelText=" + txt + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (ecSearchBoBcLabelTextField != null) {
                    try {
                        Object txt = ecSearchBoBcLabelTextField.get(commerceAuth);
                        if (txt instanceof String && !((String) txt).trim().isEmpty()) {
                            Log.i(TAG, "[Hide Promotional Content] Match [AwemeCommerceStruct.ecSearchBoBcLabelTextField=" + txt + "] on aid=" + aid);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // 2. Tagged Brand Accounts list (getBrandContentAccounts())
            Object brandAccounts = null;
            if (getBrandContentAccountsMethod != null) {
                try { brandAccounts = getBrandContentAccountsMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (brandAccounts == null && brandContentAccountsField != null) {
                try { brandAccounts = brandContentAccountsField.get(aweme); } catch (Throwable ignored) {}
            }
            if (brandAccounts instanceof List && !((List<?>) brandAccounts).isEmpty()) {
                Log.i(TAG, "[Hide Promotional Content] Match [brandContentAccounts.count=" + ((List<?>) brandAccounts).size() + "] on aid=" + aid);
                return true;
            }

            // 3. Star Atlas commercial order ID
            if (getStarAtlasOrderIdMethod != null) {
                try {
                    Object orderId = getStarAtlasOrderIdMethod.invoke(aweme);
                    if (orderId instanceof Number && ((Number) orderId).longValue() > 0) {
                        Log.i(TAG, "[Hide Promotional Content] Match [starAtlasOrderId=" + orderId + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (starAtlasOrderIdField != null) {
                try {
                    long orderId = starAtlasOrderIdField.getLong(aweme);
                    if (orderId > 0) {
                        Log.i(TAG, "[Hide Promotional Content] Match [starAtlasOrderIdField=" + orderId + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }

            // 4. Commercial Video Info
            if (getCommercialVideoInfoMethod != null) {
                try {
                    Object cvi = getCommercialVideoInfoMethod.invoke(aweme);
                    if (cvi instanceof String && !((String) cvi).trim().isEmpty()) {
                        Log.i(TAG, "[Hide Promotional Content] Match [commercialVideoInfo=" + cvi + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (commercialVideoInfoField != null) {
                try {
                    Object cvi = commercialVideoInfoField.get(aweme);
                    if (cvi instanceof String && !((String) cvi).trim().isEmpty()) {
                        Log.i(TAG, "[Hide Promotional Content] Match [commercialVideoInfoField=" + cvi + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }

            // 5. Promote Model / Icon
            if (getPromoteIconTextMethod != null) {
                try {
                    Object pit = getPromoteIconTextMethod.invoke(aweme);
                    if (pit instanceof String && !((String) pit).trim().isEmpty()) {
                        Log.i(TAG, "[Hide Promotional Content] Match [promoteIconText=" + pit + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (promoteIconTextField != null) {
                try {
                    Object pit = promoteIconTextField.get(aweme);
                    if (pit instanceof String && !((String) pit).trim().isEmpty()) {
                        Log.i(TAG, "[Hide Promotional Content] Match [promoteIconTextField=" + pit + "] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (getPromoteModelMethod != null) {
                try {
                    if (getPromoteModelMethod.invoke(aweme) != null) {
                        Log.i(TAG, "[Hide Promotional Content] Match [promoteModel] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
            if (promoteModelField != null) {
                try {
                    if (promoteModelField.get(aweme) != null) {
                        Log.i(TAG, "[Hide Promotional Content] Match [promoteModelField] on aid=" + aid);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }

            // 6. Fallback: Secondary multi-locale description & content description pattern matching
            Object descObj = null;
            if (getDescMethod != null) {
                try { descObj = getDescMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (descObj == null && descField != null) {
                try { descObj = descField.get(aweme); } catch (Throwable ignored) {}
            }
            if (descObj instanceof String && isPromotionalDescription((String) descObj)) {
                Log.i(TAG, "[Hide Promotional Content] Match [Desc=" + descObj + "] on aid=" + aid);
                return true;
            }

            Object contentDescObj = null;
            if (getContentDescMethod != null) {
                try { contentDescObj = getContentDescMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (contentDescObj == null && contentDescField != null) {
                try { contentDescObj = contentDescField.get(aweme); } catch (Throwable ignored) {}
            }
            if (contentDescObj instanceof String && isPromotionalDescription((String) contentDescObj)) {
                Log.i(TAG, "[Hide Promotional Content] Match [ContentDesc=" + contentDescObj + "] on aid=" + aid);
                return true;
            }

            // 7. Fallback: Banners and Anchors
            Object banners = null;
            if (getBannersMethod != null) {
                try { banners = getBannersMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (banners == null && bannersField != null) {
                try { banners = bannersField.get(aweme); } catch (Throwable ignored) {}
            }
            if (banners instanceof List) {
                for (Object b : (List<?>) banners) {
                    if (b != null && isPromotionalDescription(b.toString())) {
                        Log.i(TAG, "[Hide Promotional Content] Match [Banner=" + b + "] on aid=" + aid);
                        return true;
                    }
                }
            }

            Object anchors = null;
            if (getAnchorsMethod != null) {
                try { anchors = getAnchorsMethod.invoke(aweme); } catch (Throwable ignored) {}
            }
            if (anchors == null && anchorsField != null) {
                try { anchors = anchorsField.get(aweme); } catch (Throwable ignored) {}
            }
            if (anchors instanceof List) {
                for (Object a : (List<?>) anchors) {
                    if (a != null && isPromotionalDescription(a.toString())) {
                        Log.i(TAG, "[Hide Promotional Content] Match [Anchor=" + a + "] on aid=" + aid);
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @SuppressWarnings("unchecked")
    public static void filterPromotionalInList(Object listObj) {
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
                    if (isPromotionalContent(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide Promotional Content] Pruned " + removed + " promotional/branded video(s) from feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterPromotionalInFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            ensureInitialized(feedItemList.getClass().getClassLoader());
            List<Object> items = extractFeedItems(feedItemList);
            if (items != null) {
                filterPromotionalInList(items);
            }
        } catch (Throwable ignored) {}
    }

    public static void filterPromotionalInFollowFeedList(Object followFeedList) {
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
                    if (isPromotionalContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide Promotional Content] Pruned " + removed + " promotional/branded video(s) from Following feed.");
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static void filterPromotionalInFriendsV3Feeds(Object listObj) {
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
                    if (isPromotionalContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide Promotional Content] Pruned " + removed + " promotional/branded video(s) from Friends V3 feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static void filterPromotionalInFriendsFeedData(Object listObj) {
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
                    if (isPromotionalContent(aweme)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Hide Promotional Content] Pruned " + removed + " promotional/branded video(s) from Friends feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterPromotionalInFriendsV3Response(Object response) {
        if (response == null) return;
        try {
            Field field = friendsV3FeedsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendsV3Feeds");
                field.setAccessible(true);
                friendsV3FeedsField = field;
            }
            filterPromotionalInFriendsV3Feeds(field.get(response));
        } catch (Throwable ignored) {}
    }

    public static void filterPromotionalInFriendsFeedResponse(Object response) {
        if (response == null) return;
        try {
            Field field = friendFeedDataField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendFeedData");
                field.setAccessible(true);
                friendFeedDataField = field;
            }
            filterPromotionalInFriendsFeedData(field.get(response));
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // Friends Feed Strict Mutuals Filtering (Block Suggested / Non-Mutuals)
    // =========================================================================

    private static String getCurrentUserId(ClassLoader classLoader) {
        try {
            ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
            Class<?> userServiceClass = null;
            try {
                userServiceClass = loader.loadClass("com.ss.android.ugc.aweme.framework/services/IUserService".replace('/', '.'));
            } catch (Throwable t) {
                if (classLoader != loader && classLoader != null) {
                    try { userServiceClass = classLoader.loadClass("com.ss.android.ugc.aweme.framework/services/IUserService".replace('/', '.')); } catch (Throwable ignored) {}
                }
            }

            if (userServiceClass != null) {
                Class<?> serviceManagerClass = loader.loadClass("com.ss.android.ugc.aweme.framework.services.ServiceManager");
                Method getMethod = serviceManagerClass.getMethod("get");
                Object serviceManager = getMethod.invoke(null);
                if (serviceManager != null) {
                    Method getServiceMethod = serviceManagerClass.getMethod("getService", Class.class);
                    Object userService = getServiceMethod.invoke(serviceManager, userServiceClass);
                    if (userService != null) {
                        Method getUserIdMethod = userService.getClass().getMethod("getCurrentUserID");
                        Object uid = getUserIdMethod.invoke(userService);
                        if (uid instanceof String && !((String) uid).isEmpty()) {
                            return (String) uid;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            ClassLoader loader = awemeClass != null ? awemeClass.getClassLoader() : classLoader;
            Class<?> baseUserServiceClass = loader.loadClass("com.ss.android.ugc.aweme.services.BaseUserService");
            Object baseUserService = baseUserServiceClass.getDeclaredConstructor().newInstance();
            Method getUserIdMethod = baseUserServiceClass.getMethod("getCurrentUserID");
            Object uid = getUserIdMethod.invoke(baseUserService);
            if (uid instanceof String && !((String) uid).isEmpty()) {
                return (String) uid;
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static Object getAuthorFromAweme(Object aweme) {
        if (aweme == null) return null;
        try {
            if (awemeGetAuthorMethod != null) {
                Object author = awemeGetAuthorMethod.invoke(aweme);
                if (author != null) return author;
            }
            if (awemeAuthorField != null) {
                Object author = awemeAuthorField.get(aweme);
                if (author != null) return author;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int getFollowStatusFromUser(Object user) {
        if (user == null) return -1;
        try {
            if (userGetFollowStatusMethod != null) {
                Object status = userGetFollowStatusMethod.invoke(user);
                if (status instanceof Number) return ((Number) status).intValue();
            }
            if (userFollowStatusField != null) {
                Object status = userFollowStatusField.get(user);
                if (status instanceof Number) return ((Number) status).intValue();
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static String getUidFromUser(Object user) {
        if (user == null) return null;
        try {
            if (userGetUidMethod != null) {
                Object uid = userGetUidMethod.invoke(user);
                if (uid instanceof String) return (String) uid;
            }
            if (userUidField != null) {
                Object uid = userUidField.get(user);
                if (uid instanceof String) return (String) uid;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isSuggestedAccountUser(Object user) {
        if (user == null) return false;
        try {
            if (userGetMatchedFriendStructMethod != null) {
                Object matched = userGetMatchedFriendStructMethod.invoke(user);
                if (matched != null) return true;
            }
            if (userMatchedFriendStructField != null) {
                Object matched = userMatchedFriendStructField.get(user);
                if (matched != null) return true;
            }
            if (userIsMatchedFriendAvailableMethod != null) {
                Object available = userIsMatchedFriendAvailableMethod.invoke(user);
                if (Boolean.TRUE.equals(available)) return true;
            }
            if (userMatchedFriendAvailableField != null) {
                Object available = userMatchedFriendAvailableField.get(user);
                if (Boolean.TRUE.equals(available)) return true;
            }
            if (userGetRecTypeMethod != null) {
                Object recType = userGetRecTypeMethod.invoke(user);
                if (recType instanceof String && !((String) recType).isEmpty() && !"0".equals(recType)) {
                    return true;
                }
            }
            if (userRecTypeField != null) {
                Object recType = userRecTypeField.get(user);
                if (recType instanceof String && !((String) recType).isEmpty() && !"0".equals(recType)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean hasAwemeRelationSuggestion(Object aweme) {
        if (aweme == null) return false;
        try {
            if (awemeGetFeedRelationLabelMethod != null && awemeGetFeedRelationLabelMethod.invoke(aweme) != null) return true;
            if (awemeFeedRelationLabelField != null && awemeFeedRelationLabelField.get(aweme) != null) return true;
            if (awemeGetRelationLabelMethod != null && awemeGetRelationLabelMethod.invoke(aweme) != null) return true;
            if (awemeRelationLabelField != null && awemeRelationLabelField.get(aweme) != null) return true;
            if (awemeGetRelationRecommendInfoMethod != null && awemeGetRelationRecommendInfoMethod.invoke(aweme) != null) return true;
            if (awemeRelationRecommendInfoField != null && awemeRelationRecommendInfoField.get(aweme) != null) return true;
            if (awemeGetRecReasonsStructMethod != null && awemeGetRecReasonsStructMethod.invoke(aweme) != null) return true;
            if (awemeRecReasonsStructField != null && awemeRecReasonsStructField.get(aweme) != null) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static Object extractReposterFromFriendsV3FeedModel(Object item) {
        if (item == null) return null;
        try {
            Object repost = null;
            if (friendsV3RepostItemField != null) {
                repost = friendsV3RepostItemField.get(item);
            }
            if (repost == null && friendsV3GetRepostItemMethod != null) {
                repost = friendsV3GetRepostItemMethod.invoke(item);
            }
            if (repost != null && friendsV3ReposterField != null) {
                return friendsV3ReposterField.get(repost);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isFriendsFeedSuggestedVideo(Object item, String currentUserId) {
        if (item == null) return false;

        // Check 1: Repost item in Friends V3 feed
        Object reposter = extractReposterFromFriendsV3FeedModel(item);
        if (reposter != null) {
            String reposterUid = getUidFromUser(reposter);
            if (currentUserId != null && currentUserId.equals(reposterUid)) {
                return false; // User's own repost
            }
            if (isSuggestedAccountUser(reposter)) {
                return true;
            }
            int reposterFollowStatus = getFollowStatusFromUser(reposter);
            // followStatus: 2 = mutual friend ("amigos" / follow each other)
            if (reposterFollowStatus != 2) {
                return true;
            }
            return false;
        }

        // Check 2: Direct Aweme item
        Object aweme = extractAwemeFromFriendsV3FeedModel(item);
        if (aweme == null) {
            aweme = extractAwemeFromFriendsFeed(item);
        }
        if (aweme == null && awemeClass != null && awemeClass.isInstance(item)) {
            aweme = item;
        }
        if (aweme == null) return false;

        Object author = getAuthorFromAweme(aweme);
        if (author == null) {
            return false;
        }

        String authorUid = getUidFromUser(author);
        if (currentUserId != null && currentUserId.equals(authorUid)) {
            return false; // User's own video
        }

        // Tagged with suggested / "Personas que quizás conozcas" metadata
        if (isSuggestedAccountUser(author)) {
            return true;
        }
        if (hasAwemeRelationSuggestion(aweme)) {
            return true;
        }

        // Strict mutual check: If not mutual follow (followStatus != 2), filter
        int followStatus = getFollowStatusFromUser(author);
        if (followStatus != 2) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static void filterSuggestedVideosInFriendsV3Feeds(Object listObj) {
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

                ClassLoader loader = items.get(0) != null ? items.get(0).getClass().getClassLoader() : null;
                String currentUserId = getCurrentUserId(loader);

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (isFriendsFeedSuggestedVideo(item, currentUserId)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Friends Feed Strict Mutuals] Pruned " + removed + " suggested / non-mutual video(s) from Friends V3 feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static void filterSuggestedVideosInFriendsFeedData(Object listObj) {
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

                ClassLoader loader = items.get(0) != null ? items.get(0).getClass().getClassLoader() : null;
                String currentUserId = getCurrentUserId(loader);

                int removed = 0;
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (isFriendsFeedSuggestedVideo(item, currentUserId)) {
                        iterator.remove();
                        removed++;
                    }
                }
                if (removed > 0) {
                    Log.i(TAG, "[Friends Feed Strict Mutuals] Pruned " + removed + " suggested / non-mutual video(s) from Friends feed.");
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void filterSuggestedVideosInFriendsV3Response(Object response) {
        if (response == null) return;
        try {
            Field field = friendsV3FeedsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendsV3Feeds");
                field.setAccessible(true);
                friendsV3FeedsField = field;
            }
            filterSuggestedVideosInFriendsV3Feeds(field.get(response));
        } catch (Throwable ignored) {}
        try {
            Field field = newlyShownMafIdsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("newlyShownMafIds");
                field.setAccessible(true);
                newlyShownMafIdsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
    }

    public static void filterSuggestedVideosInFriendsFeedResponse(Object response) {
        if (response == null) return;
        try {
            Field field = friendFeedDataField;
            if (field == null) {
                field = response.getClass().getDeclaredField("friendFeedData");
                field.setAccessible(true);
                friendFeedDataField = field;
            }
            filterSuggestedVideosInFriendsFeedData(field.get(response));
        } catch (Throwable ignored) {}
        try {
            Field field = cardInsertResultsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("cardInsertResults");
                field.setAccessible(true);
                cardInsertResultsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
        try {
            Field field = insertedResultsField;
            if (field == null) {
                field = response.getClass().getDeclaredField("insertedResults");
                field.setAccessible(true);
                insertedResultsField = field;
            }
            field.set(response, null);
        } catch (Throwable ignored) {}
    }

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
