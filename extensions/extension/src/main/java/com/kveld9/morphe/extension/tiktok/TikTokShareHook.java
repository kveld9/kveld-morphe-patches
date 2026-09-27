package com.kveld9.morphe.extension.tiktok;

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
 * Runtime controller for customizing and granularly filtering the TikTok native share sheet.
 * Intercepts constructed SharePanel instances and suppresses specific third-party apps,
 * actions, and friends/contacts direct message row based on user-configured boolean toggles.
 */
public final class TikTokShareHook {

    private static final String TAG = "MorpheTikTokShare";

    // 1. Third-party apps & social networks
    public static boolean hideWhatsApp = true;
    public static boolean hideInstagram = true;
    public static boolean hideFacebook = true;
    public static boolean hideTelegram = true;
    public static boolean hideTwitter = true;
    public static boolean hideSnapchat = true;
    public static boolean hideReddit = true;
    public static boolean hideSms = true;
    public static boolean hideSecondaryApps = true;

    // 2. Native share & links
    public static boolean hideRepost = false;
    public static boolean hideQrCode = false;
    public static boolean hideCopyLink = false;
    public static boolean hideSystemShare = false;

    // 3. Contacts / DM row
    public static boolean hideFriendsRow = false;

    // 4. Actions & utilities
    public static boolean hidePromote = true;
    public static boolean hideWhyThisVideo = true;
    public static boolean hideCreateSticker = false;
    public static boolean hideDuet = false;
    public static boolean hideStitch = false;
    public static boolean hidePip = false;
    public static boolean hideClearDisplay = false;
    public static boolean hideListenAudio = false;
    public static boolean hideWallpaperAndGif = false;
    public static boolean hideNotInterested = false;
    public static boolean hideReport = false;

    // Optional advanced custom keys
    public static String hiddenApps = "";
    public static String hiddenActions = "";

    public static String isImFunctionOffFieldName = "LJJIJIL";
    public static String supportIMFieldName = "LJIJJLI";

    private static Set<String> activeHiddenKeys = null;
    private static Set<String> parsedCustomKeys = null;

    private TikTokShareHook() {}

    private static Set<String> getActiveHiddenKeys() {
        if (activeHiddenKeys == null) {
            Set<String> set = new HashSet<>();
            if (hideFriendsRow) set.add("chat_merge");
            if (hideRepost) { set.add("repost"); set.add("upvote"); }
            if (hideQrCode) set.add("qr_code");
            if (hideCopyLink) { set.add("copy"); set.add("copy_link"); }
            if (hideSystemShare) { set.add("more"); set.add("system"); }

            if (hideWhatsApp) { set.add("whatsapp"); set.add("whatsapp_status"); set.add("whatsapp_business"); }
            if (hideInstagram) { set.add("instagram"); set.add("instagram_story"); }
            if (hideFacebook) {
                set.add("facebook"); set.add("facebook_lite"); set.add("facebook_group");
                set.add("facebook_story"); set.add("messenger"); set.add("messenger_lite");
            }
            if (hideTelegram) set.add("telegram");
            if (hideTwitter) { set.add("twitter"); set.add("x"); }
            if (hideSnapchat) set.add("snapchat");
            if (hideReddit) { set.add("reddit"); set.add("discord"); }
            if (hideSms) { set.add("sms"); set.add("google_messages"); }
            if (hideSecondaryApps) {
                set.addAll(Arrays.asList(
                    "viber", "vk", "line", "band", "email", "imgur",
                    "kakao_story", "kakaotalk", "zalo", "lemon8"
                ));
            }

            if (hidePromote) set.add("promote");
            if (hideWhyThisVideo) set.add("why_this_video");
            if (hideCreateSticker) { set.add("create_sticker"); set.add("create_stickers"); }
            if (hideDuet) set.add("duet");
            if (hideStitch) set.add("stitch");
            if (hidePip) { set.add("pip_switch"); set.add("pip"); }
            if (hideClearDisplay) set.add("clear_display");
            if (hideListenAudio) { set.add("listen_audio"); set.add("background_play"); }
            if (hideWallpaperAndGif) { set.add("wallpaper"); set.add("live_photo"); set.add("gif"); }
            if (hideNotInterested) { set.add("dislike"); set.add("not_interested"); }
            if (hideReport) set.add("report");

            activeHiddenKeys = set;
        }
        return activeHiddenKeys;
    }

    private static Set<String> getParsedCustomKeys() {
        if (parsedCustomKeys == null) {
            Set<String> set = new HashSet<>();
            parseCommaKeys(hiddenApps, set);
            parseCommaKeys(hiddenActions, set);
            parsedCustomKeys = set;
        }
        return parsedCustomKeys;
    }

    private static void parseCommaKeys(String input, Set<String> out) {
        if (input == null || input.trim().isEmpty()) return;
        for (String part : input.split(",")) {
            String clean = part.trim().toLowerCase(Locale.ROOT);
            if (!clean.isEmpty()) {
                out.add(clean);
            }
        }
    }

    public static boolean shouldHide(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return getActiveHiddenKeys().contains(lowerKey) || getParsedCustomKeys().contains(lowerKey);
    }

    /**
     * Intercepts the constructed share panel instance and filters channels, actions,
     * and row visibility according to user configuration.
     */
    public static void filterSharePanel(Object panel) {
        if (panel == null) return;
        try {
            Class<?> clazz = panel.getClass();
            if (hideFriendsRow) {
                applyFriendRowSuppression(panel, clazz);
            }

            int removedCount = 0;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object val = field.get(panel);
                if (!(val instanceof List)) continue;
                List<?> list = (List<?>) val;
                if (list.isEmpty()) continue;

                Method keyMethod = findKeyMethod(list);
                if (keyMethod == null) continue;

                removedCount += filterList(list, keyMethod);
            }
            Log.i(TAG, "[Custom Share Sheet] Pruned " + removedCount + " item(s) from share sheet.");
        } catch (Throwable t) {
            Log.w(TAG, "[Custom Share Sheet] Error filtering share panel: " + t.getMessage());
        }
    }

    private static int filterList(List<?> list, Method keyMethod) {
        int removed = 0;
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (item == null) continue;
            String key = getKey(item, keyMethod);
            if (key == null) continue;
            if (shouldHide(key)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static void applyFriendRowSuppression(Object panel, Class<?> clazz) {
        if (isImFunctionOffFieldName != null && !isImFunctionOffFieldName.isEmpty()) {
            try {
                Field f = clazz.getDeclaredField(isImFunctionOffFieldName);
                f.setAccessible(true);
                f.setBoolean(panel, true);
            } catch (Throwable ignored) {}
        }
        if (supportIMFieldName != null && !supportIMFieldName.isEmpty()) {
            try {
                Field f = clazz.getDeclaredField(supportIMFieldName);
                f.setAccessible(true);
                f.setBoolean(panel, false);
            } catch (Throwable ignored) {}
        }
    }

    private static Method findKeyMethod(List<?> list) {
        for (Object item : list) {
            if (item == null) continue;
            Class<?> itemClass = item.getClass();
            try {
                return itemClass.getMethod("key");
            } catch (NoSuchMethodException ignored) {
                for (Method m : itemClass.getMethods()) {
                    if ("key".equals(m.getName()) && m.getParameterTypes().length == 0 && m.getReturnType() == String.class) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    private static String getKey(Object item, Method keyMethod) {
        try {
            return (String) keyMethod.invoke(item);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
