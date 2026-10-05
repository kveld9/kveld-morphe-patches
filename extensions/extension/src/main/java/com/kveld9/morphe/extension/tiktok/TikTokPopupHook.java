package com.kveld9.morphe.extension.tiktok;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension hook to intercept and suppress intrusive TikTok PopLayer dialogs and prompts.
 */
public final class TikTokPopupHook {

    private static final Set<String> BLACKLISTED_LABELS;
    private static final Set<String> BLACKLISTED_CLASS_FRAGMENTS;

    static {
        Set<String> classFragments = new HashSet<>(Arrays.asList(
            "UpvoteNewbieGuideFragment",
            "StoryRevealFriendsIntroPanel",
            "FollowingSkylightPopup"
        ));
        BLACKLISTED_CLASS_FRAGMENTS = Collections.unmodifiableSet(classFragments);

        Set<String> labels = new HashSet<>(Arrays.asList(
            "create_collection_guide_popup",
            "shortcut_guide_add_dialog_task",
            "stem_feed_popup_intro_guide",
            "campus_education_sheet",
            "has_seen_add_school_popup",
            "pro_inbox_mode_guide_popup_show",
            "minis_guide_popup_show",
            "praise_dialog",
            "tiktok_sms_popup_show",
            "show_email_permission_pop_up",
            "fyp_popup_survey_dialog",
            "request_capcut_campaing_popup",
            "show_lemon8_intro_popup",
            "profile_visitor_popup",
            "profile_view_history_turnon_nscreen"
        ));
        labels.addAll(classFragments);
        BLACKLISTED_LABELS = Collections.unmodifiableSet(labels);
    }

    private TikTokPopupHook() {}

    private static boolean matchesLabel(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (BLACKLISTED_LABELS.contains(value)) {
            return true;
        }
        for (String blacklisted : BLACKLISTED_LABELS) {
            if (value.contains(blacklisted)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldSuppressPopLayer(Object popup) {
        if (popup == null) {
            return false;
        }
        try {
            Class<?> clazz = popup.getClass();
            String className = clazz.getName();
            for (String fragment : BLACKLISTED_CLASS_FRAGMENTS) {
                if (className.contains(fragment)) {
                    return true;
                }
            }

            try {
                Method getLabelMethod = clazz.getMethod("getElementLabel");
                Object label = getLabelMethod.invoke(popup);
                if (label instanceof String && matchesLabel((String) label)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            try {
                Method getTriggerMethod = clazz.getMethod("getTriggerId");
                Object trigger = getTriggerMethod.invoke(popup);
                if (trigger instanceof String && matchesLabel((String) trigger)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            try {
                Method getIdMethod = clazz.getMethod("getId");
                Object id = getIdMethod.invoke(popup);
                if (id instanceof String && matchesLabel((String) id)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return false;
    }
}
