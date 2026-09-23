package com.kveld9.morphe.extension.tiktok;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for automatically pausing the first video upon opening TikTok.
 *
 * Execution flow:
 * 1. PlayerController.d0 loads the first video into the player engine.
 *    At the end of d0, onFirstVideoLoaded() immediately invokes pauseVideo()
 *    and displays the center play icon, keeping the video loaded and paused on frame 0.
 * 2. PlayerController.Q6 (invoked by tryResumePlay upon SplashActivity finish) is intercepted:
 *    shouldBlockQ6() blocks automatic playback for the first video while awaiting user play.
 * 3. PlayerController.onResumePlay() detects when the user taps play, instantly unlocking
 *    auto-pause so the pre-buffered video begins playback with zero latency.
 * 4. PlayerController.onPageScrollStateChanged() and AID comparison detect when the user
 *    swipes to the next video, automatically unlocking auto-play for all subsequent videos.
 */
public final class TikTokAutoPauseHook {

    private static final String TAG = "MorpheTikTok";
    private static volatile String firstAwemeAid = null;
    private static final AtomicBoolean isAwaitingFirstPlay = new AtomicBoolean(true);

    private TikTokAutoPauseHook() {}

    /**
     * Called at the end of PlayerController.d0 after video engine setup.
     * Pauses the first video and displays the play icon.
     */
    public static void onFirstVideoLoaded(final Object controller, final Object aweme) {
        if (!isAwaitingFirstPlay.get() || controller == null) {
            return;
        }

        String aid = extractAid(aweme);
        if (firstAwemeAid == null && aid != null) {
            firstAwemeAid = aid;
        } else if (firstAwemeAid != null && aid != null && !aid.equals(firstAwemeAid)) {
            Log.i(TAG, "[AutoPauseFirstVideo] New video loaded (aid=" + aid + ") -> auto-pause unlocked.");
            isAwaitingFirstPlay.set(false);
            return;
        }

        Log.i(TAG, "[AutoPauseFirstVideo] First video loaded (aid=" + aid + "). Enforcing paused state.");
        invokePause(controller);
        showPlayIcon(controller);
    }

    /**
     * Intercepts PlayerController.Q6 (invoked by tryResumePlay and feed scroll).
     *
     * @param controller The PlayerController instance.
     * @param aweme The Aweme instance requested for playback.
     * @return true to block auto-play (returns empty string); false to proceed normally.
     */
    public static boolean shouldBlockQ6(final Object controller, final Object aweme) {
        if (!isAwaitingFirstPlay.get() || controller == null) {
            return false;
        }

        String aid = extractAid(aweme);
        if (firstAwemeAid == null && aid != null) {
            firstAwemeAid = aid;
        }

        if (aid != null && firstAwemeAid != null && !aid.equals(firstAwemeAid)) {
            Log.i(TAG, "[AutoPauseFirstVideo] Scrolled to new video in Q6 (aid=" + aid + ") -> auto-pause unlocked.");
            isAwaitingFirstPlay.set(false);
            return false;
        }

        Log.i(TAG, "[AutoPauseFirstVideo] Blocked Q6 auto-play for first video (aid=" + aid + ").");
        invokePause(controller);
        showPlayIcon(controller);
        return true;
    }

    /**
     * Invoked when the user explicitly triggers playback (PlayerController.onResumePlay).
     */
    public static void onUserResumePlay() {
        if (isAwaitingFirstPlay.compareAndSet(true, false)) {
            Log.i(TAG, "[AutoPauseFirstVideo] User resumed playback -> auto-pause unlocked.");
        }
    }

    /**
     * Invoked when the feed pager scroll state changes (1 = dragging, 2 = settling, 0 = idle).
     */
    public static void onPageScroll(int state) {
        if (state != 0) {
            if (isAwaitingFirstPlay.compareAndSet(true, false)) {
                Log.i(TAG, "[AutoPauseFirstVideo] Feed scrolled (state=" + state + ") -> auto-pause unlocked.");
            }
        }
    }

    private static void invokePause(final Object controller) {
        try {
            Method pauseMethod = controller.getClass().getMethod("pauseVideo");
            pauseMethod.setAccessible(true);
            pauseMethod.invoke(controller);
        } catch (Throwable t) {
            Log.w(TAG, "[AutoPauseFirstVideo] pauseVideo invocation error: " + t.getMessage());
        }

        // Schedule brief follow-up pause on main looper in case underlying player setup is asynchronous
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAwaitingFirstPlay.get()) {
                    try {
                        Method pauseMethod = controller.getClass().getMethod("pauseVideo");
                        pauseMethod.setAccessible(true);
                        pauseMethod.invoke(controller);
                    } catch (Throwable ignored) {}
                }
            }
        }, 200);
    }

    private static void showPlayIcon(final Object controller) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invokeShowIvPlay(controller);
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAwaitingFirstPlay.get()) {
                    invokeShowIvPlay(controller);
                }
            }
        }, 150);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAwaitingFirstPlay.get()) {
                    invokeShowIvPlay(controller);
                }
            }
        }, 500);
    }

    private static boolean invokeShowIvPlay(Object controller) {
        try {
            Method getPanelMethod = controller.getClass().getMethod("LJJLI");
            getPanelMethod.setAccessible(true);
            Object panel = getPanelMethod.invoke(controller);
            if (panel != null) {
                Method showIvPlayMethod = panel.getClass().getMethod("showIvPlay");
                showIvPlayMethod.setAccessible(true);
                showIvPlayMethod.invoke(panel);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "[AutoPauseFirstVideo] Failed to show play icon: " + t.getMessage());
        }
        return false;
    }

    private static String extractAid(Object aweme) {
        if (aweme == null) return null;
        try {
            Method getAidMethod = aweme.getClass().getMethod("getAid");
            getAidMethod.setAccessible(true);
            return (String) getAidMethod.invoke(aweme);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resets the auto-pause state (e.g. for testing).
     */
    public static void reset() {
        firstAwemeAid = null;
        isAwaitingFirstPlay.set(true);
    }
}
