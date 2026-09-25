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
 * Runtime controller for customizing and simplifying the TikTok native share sheet.
 * Filters third-party application channels, hides specific apps or actions,
 * and suppresses the friends/contacts suggested direct message row.
 */
public final class TikTokShareHook {

    private static final String TAG = "MorpheTikTokShare";

    public static boolean simplifyShareSheet = true;
    public static boolean hideFriendsRow = false;
    public static String hiddenApps = "";
    public static String hiddenActions = "";

    public static String isImFunctionOffFieldName = "LJJIJIL";
    public static String supportIMFieldName = "LJIJJLI";

    private static final Set<String> ESSENTIAL_CHANNELS = new HashSet<>(Arrays.asList(
        "copy",
        "copy_link",
        "more",
        "system"
    ));

    private static final Set<String> KNOWN_CHANNEL_KEYS = new HashSet<>(Arrays.asList(
        "whatsapp", "whatsapp_business", "whatsapp_status",
        "facebook", "facebook_lite", "facebook_group", "facebook_story",
        "instagram", "instagram_story",
        "messenger", "messenger_lite",
        "snapchat", "sms", "twitter", "telegram", "reddit", "discord",
        "viber", "vk", "line", "band", "email", "imgur",
        "kakao_story", "kakaotalk", "zalo", "google_messages", "lemon8",
        "copy", "copy_link", "more", "system", "chat_merge", "qr_code", "repost"
    ));

    private static Set<String> parsedHiddenApps = null;
    private static Set<String> parsedHiddenActions = null;

    private TikTokShareHook() {}

    private static Set<String> getParsedHiddenApps() {
        if (parsedHiddenApps == null) {
            Set<String> set = new HashSet<>();
            if (hiddenApps != null && !hiddenApps.trim().isEmpty()) {
                for (String part : hiddenApps.split(",")) {
                    String clean = part.trim().toLowerCase(Locale.ROOT);
                    if (!clean.isEmpty()) {
                        set.add(clean);
                    }
                }
            }
            parsedHiddenApps = set;
        }
        return parsedHiddenApps;
    }

    private static Set<String> getParsedHiddenActions() {
        if (parsedHiddenActions == null) {
            Set<String> set = new HashSet<>();
            if (hiddenActions != null && !hiddenActions.trim().isEmpty()) {
                for (String part : hiddenActions.split(",")) {
                    String clean = part.trim().toLowerCase(Locale.ROOT);
                    if (!clean.isEmpty()) {
                        set.add(clean);
                    }
                }
            }
            parsedHiddenActions = set;
        }
        return parsedHiddenActions;
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

            int removedChannels = 0;
            int removedActions = 0;
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

                boolean isChannel = isChannelList(list, keyMethod);
                if (isChannel) {
                    removedChannels += filterChannels(list, keyMethod);
                } else {
                    removedActions += filterActions(list, keyMethod);
                }
            }
            Log.i(TAG, "[Custom Share Sheet] Pruned " + removedChannels + " channel(s) and " + removedActions + " action(s) from share sheet.");
        } catch (Throwable t) {
            Log.w(TAG, "[Custom Share Sheet] Error filtering share panel: " + t.getMessage());
        }
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

    private static boolean isChannelList(List<?> list, Method keyMethod) {
        for (Object item : list) {
            if (item == null) continue;
            String k = getKey(item, keyMethod);
            if (k != null) {
                String lk = k.toLowerCase(Locale.ROOT);
                if (KNOWN_CHANNEL_KEYS.contains(lk)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int filterChannels(List<?> list, Method keyMethod) {
        int removed = 0;
        Set<String> toHide = getParsedHiddenApps();
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (item == null) continue;
            String key = getKey(item, keyMethod);
            if (key == null) continue;
            String lowerKey = key.toLowerCase(Locale.ROOT);

            if (hideFriendsRow && "chat_merge".equals(lowerKey)) {
                iterator.remove();
                removed++;
                continue;
            }

            if (simplifyShareSheet) {
                boolean isEssential = ESSENTIAL_CHANNELS.contains(lowerKey);
                boolean isFriendChat = !hideFriendsRow && "chat_merge".equals(lowerKey);
                if (!isEssential && !isFriendChat) {
                    iterator.remove();
                    removed++;
                }
            } else if (toHide.contains(lowerKey)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static int filterActions(List<?> list, Method keyMethod) {
        Set<String> toHide = getParsedHiddenActions();
        if (toHide.isEmpty()) return 0;

        int removed = 0;
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (item == null) continue;
            String key = getKey(item, keyMethod);
            if (key == null) continue;
            String lowerKey = key.toLowerCase(Locale.ROOT);
            if (toHide.contains(lowerKey)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static String getKey(Object item, Method keyMethod) {
        try {
            return (String) keyMethod.invoke(item);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
