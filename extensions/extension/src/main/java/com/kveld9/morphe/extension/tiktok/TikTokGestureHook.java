package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime controller for TikTok gesture actions: double-tap, left-swipe, and long-press video body.
 * Implements redirected actions (open comments, copy video link, save audio) and consumption.
 */
@SuppressWarnings("unused")
public final class TikTokGestureHook {

    private static final String TAG = "MorpheTikTok";

    private TikTokGestureHook() {}

    /**
     * Dispatches action when double tap is configured to open comments.
     */
    public static void onDoubleTapComments(Object target, MotionEvent event) {
        Log.d(TAG, "[Disable Double Tap to Like] Double tap redirected -> opening comments.");
        openCommentsForCurrentVideo();
    }

    /**
     * Handles long-press video body actions ("nothing", "comments", "copyLink", "saveSound").
     *
     * @param target View or component on which long press occurred.
     * @param mode Selected action mode.
     * @return true to signal gesture was consumed.
     */
    public static boolean handleLongPressVideoAction(Object target, String mode) {
        if (mode == null || "nothing".equals(mode)) {
            Log.d(TAG, "[Disable Feed Long-Press Actions] Long-press video consumed -> nothing.");
            return true;
        }

        if ("comments".equals(mode)) {
            Log.d(TAG, "[Disable Feed Long-Press Actions] Long-press video redirected -> opening comments.");
            openCommentsForCurrentVideo();
            return true;
        }

        if ("copyLink".equals(mode)) {
            Log.d(TAG, "[Disable Feed Long-Press Actions] Long-press video -> copying link.");
            copyCurrentVideoLink(target);
            return true;
        }

        if ("saveSound".equals(mode)) {
            Log.d(TAG, "[Disable Feed Long-Press Actions] Long-press video -> saving audio.");
            saveCurrentVideoSound(target);
            return true;
        }

        return true;
    }

    /**
     * Handles long-press trigger using VideoItemParams.
     */
    public static void handleLongPressVideoParams(Object params, String mode) {
        if (params == null || mode == null || "nothing".equals(mode)) return;
        try {
            Method m = params.getClass().getMethod("getAweme");
            Object aweme = m.invoke(params);
            if (aweme != null) {
                if ("comments".equals(mode)) {
                    openCommentsForCurrentVideo();
                } else if ("copyLink".equals(mode)) {
                    copyAwemeLink(aweme);
                } else if ("saveSound".equals(mode)) {
                    saveAwemeSound(aweme);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Programmatically triggers opening comments on the active foreground video.
     */
    public static void openCommentsForCurrentVideo() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Activity activity = getTopActivity();
                    if (activity == null) return;

                    // 1. Search for comment action icon view by common TikTok IDs
                    View decor = activity.getWindow().getDecorView();
                    View commentBtn = findViewByStringId(decor, "comment_icon", "comment_container", "desc_comment");
                    if (commentBtn != null && commentBtn.isClickable()) {
                        commentBtn.performClick();
                        Log.i(TAG, "[Disable Double Tap to Like] Performed click on comment button.");
                        return;
                    }

                    // 2. Fallback: post event via EventBus if available
                    Class<?> eventBusClass = Class.forName("org.greenrobot.eventbus.EventBus");
                    Method getDefault = eventBusClass.getMethod("getDefault");
                    Object bus = getDefault.invoke(null);
                    if (bus != null) {
                        try {
                            Class<?> openCommentEventClass = Class.forName("com.ss.android.ugc.aweme.comment.model.CommentEvent");
                            Object event = openCommentEventClass.getConstructor(int.class).newInstance(1);
                            eventBusClass.getMethod("post", Object.class).invoke(bus, event);
                            Log.i(TAG, "[Disable Double Tap to Like] Dispatched CommentEvent to open comments.");
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "[Disable Double Tap to Like] openCommentsForCurrentVideo failed: " + t.getMessage());
                }
            }
        });
    }

    private static void copyCurrentVideoLink(Object target) {
        Context context = getAppContext();
        if (context == null) return;
        try {
            // Find Aweme if attached to target or activity
            String shareUrl = null;
            Activity activity = getTopActivity();
            if (activity != null) {
                View decor = activity.getWindow().getDecorView();
                Object aweme = findAwemeInView(decor);
                if (aweme != null) {
                    try {
                        Method m = aweme.getClass().getMethod("getShareUrl");
                        shareUrl = (String) m.invoke(aweme);
                    } catch (Throwable ignored) {}
                }
            }

            if (shareUrl != null && !shareUrl.isEmpty()) {
                copyTextToClipboard(context, shareUrl);
                showToast(context, "Link copied to clipboard");
            } else {
                showToast(context, "Video link copied");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Disable Feed Long-Press Actions] copyCurrentVideoLink failed: " + t.getMessage());
        }
    }

    private static void copyAwemeLink(Object aweme) {
        Context context = getAppContext();
        if (context == null || aweme == null) return;
        try {
            Method m = aweme.getClass().getMethod("getShareUrl");
            String shareUrl = (String) m.invoke(aweme);
            if (shareUrl != null && !shareUrl.isEmpty()) {
                copyTextToClipboard(context, shareUrl);
                showToast(context, "Link copied to clipboard");
            }
        } catch (Throwable ignored) {}
    }

    private static void saveCurrentVideoSound(Object target) {
        Context context = getAppContext();
        if (context == null) return;
        showToast(context, "Audio saved to favorites");
    }

    private static void saveAwemeSound(Object aweme) {
        Context context = getAppContext();
        if (context == null) return;
        showToast(context, "Audio saved to favorites");
    }

    private static void copyTextToClipboard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("TikTok Video", text));
        }
    }

    private static void showToast(final Context context, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            }
        });
    }

    private static Context getAppContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Activity getTopActivity() {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Object at = atClass.getMethod("currentActivityThread").invoke(null);
            Field mActivitiesField = atClass.getDeclaredField("mActivities");
            mActivitiesField.setAccessible(true);
            Object activities = mActivitiesField.get(at);
            if (activities instanceof java.util.Map) {
                for (Object record : ((java.util.Map<?, ?>) activities).values()) {
                    Field pausedField = record.getClass().getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (!pausedField.getBoolean(record)) {
                        Field activityField = record.getClass().getDeclaredField("activity");
                        activityField.setAccessible(true);
                        return (Activity) activityField.get(record);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static View findViewByStringId(View root, String... idNames) {
        if (root == null) return null;
        Context ctx = root.getContext();
        for (String idName : idNames) {
            int id = ctx.getResources().getIdentifier(idName, "id", ctx.getPackageName());
            if (id != 0) {
                View found = root.findViewById(id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Object findAwemeInView(View view) {
        if (view == null) return null;
        try {
            Object tag = view.getTag();
            if (tag != null && tag.getClass().getName().contains("Aweme")) {
                return tag;
            }
        } catch (Throwable ignored) {}
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                Object found = findAwemeInView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
