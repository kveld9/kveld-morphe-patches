package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Runtime hook helper for TikTok comment interactions.
 * Strips creator username prefix from copied comments to copy clean comment text only.
 */
public final class TikTokCommentHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokCommentHook() {}

    private static final ThreadLocal<String> capturedCommentText = new ThreadLocal<>();

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

    public static void hideCommentQuickActions(final View view) {
        if (view == null) return;
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
}
