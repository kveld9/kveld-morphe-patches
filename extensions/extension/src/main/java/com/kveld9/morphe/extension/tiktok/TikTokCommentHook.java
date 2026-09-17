package com.kveld9.morphe.extension.tiktok;

import android.util.Log;

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
}
