package com.kveld9.morphe.extension.tiktok;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;

/**
 * Runtime hook helper for TikTok comment interactions.
 * Strips creator username prefix from copied comments to copy clean comment text only,
 * manages comment send screen resolution, and filters comment surprise popup animations.
 */
public final class TikTokCommentHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokCommentHook() {}

    private static final ThreadLocal<String> capturedCommentText = new ThreadLocal<>();

    // Global debounce timestamp for community-flagged fold entrance auto-expansion
    private static long lastFoldAutoExpandMs = 0L;

    // Weak reference for comment panel Activity tracking
    private static WeakReference<Activity> panelActivityRef = new WeakReference<>(null);

    // Marker tracking for comment popup ads / surprises
    private static final int PATH_PAGE_LOADER = 1;
    private static final int PATH_PUBLISH_RESPONSE = 2;
    private static final int PATH_MILESTONE = 3;

    private static final ThreadLocal<Integer> currentSurprisePath = new ThreadLocal<>();

    public static void captureCommentText(String text) {
        capturedCommentText.set(text);
    }

    public static String sanitizeCopiedComment(String copiedText) {
        String commentText = capturedCommentText.get();
        capturedCommentText.remove(); // consume once and prevent memory leak
        if (commentText == null || commentText.isEmpty()) {
            return copiedText;
        }
        Log.d(TAG, "[CommentCopy] Copied comment without author username (len=" + commentText.length() + ")");
        return commentText;
    }

    public static void setPanelActivity(Activity a) {
        if (a != null) {
            panelActivityRef = new WeakReference<>(a);
        }
    }

    public static void noteActivity(Activity a) {
        if (a != null && !a.isFinishing()) {
            panelActivityRef = new WeakReference<>(a);
        }
    }

    public static Activity panelActivity() {
        Activity act = panelActivityRef.get();
        if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            return act;
        }
        return null;
    }

    public static void markPageLoaderSurprise() {
        currentSurprisePath.set(PATH_PAGE_LOADER);
    }

    public static void markPublishResponseSurprise() {
        currentSurprisePath.set(PATH_PUBLISH_RESPONSE);
    }

    public static void markMilestoneSurprise() {
        currentSurprisePath.set(PATH_MILESTONE);
    }

    public static Object filterSurprise(Object surprise) {
        Integer path = currentSurprisePath.get();
        currentSurprisePath.remove();
        if (path != null && path == PATH_MILESTONE) {
            return surprise;
        }
        return null;
    }

    private static Activity findActivity(Object obj) {
        if (obj instanceof Activity) {
            return (Activity) obj;
        }
        if (obj instanceof Context) {
            Context context = (Context) obj;
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
            return null;
        }
        return null;
    }

    public static void hideCommentQuickActions(final View view) {
        if (view == null) return;
        Activity act = findActivity(view.getContext());
        if (act != null) {
            setPanelActivity(act);
        }
        try {
            view.setVisibility(View.GONE);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.width = 0;
                lp.height = 0;
                view.setLayoutParams(lp);
            }
            if (view instanceof ViewGroup) {
                ((ViewGroup) view).removeAllViews();
            }
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    if (v.getVisibility() != View.GONE) {
                        v.setVisibility(View.GONE);
                    }
                }
            });
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(final View v) {
                    v.setVisibility(View.GONE);
                    v.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            if (v.getVisibility() != View.GONE) {
                                v.setVisibility(View.GONE);
                            }
                            return true;
                        }
                    });
                }

                @Override
                public void onViewDetachedFromWindow(View v) {}
            });
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (view.getVisibility() != View.GONE) {
                        view.setVisibility(View.GONE);
                    }
                    return true;
                }
            });
            Log.d(TAG, "[CommentQuickActions] Quick actions bar hidden.");
        } catch (Throwable t) {
            Log.e(TAG, "[CommentQuickActions] Failed to hide quick action bar: " + t.getMessage());
        }
    }

    public static void autoExpandFoldEntrance(final View view) {
        if (view == null) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastFoldAutoExpandMs < 2000L) {
            return;
        }
        lastFoldAutoExpandMs = now;
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    view.performClick();
                    Log.d(TAG, "[FoldAutoExpand] Dispatched click to auto-expand community-flagged comments entrance.");
                } catch (Throwable t) {
                    Log.e(TAG, "[FoldAutoExpand] Failed to auto-expand fold entrance: " + t.getMessage());
                }
            }
        });
    }
}
