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

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static Runnable pendingPauseRunnable = null;
    private static Runnable pendingPlayIconRunnable150 = null;
    private static Runnable pendingPlayIconRunnable500 = null;

    private TikTokAutoPauseHook() {}

    private static synchronized void cancelPendingCallbacks() {
        if (pendingPauseRunnable != null) {
            MAIN_HANDLER.removeCallbacks(pendingPauseRunnable);
            pendingPauseRunnable = null;
        }
        if (pendingPlayIconRunnable150 != null) {
            MAIN_HANDLER.removeCallbacks(pendingPlayIconRunnable150);
            pendingPlayIconRunnable150 = null;
        }
        if (pendingPlayIconRunnable500 != null) {
            MAIN_HANDLER.removeCallbacks(pendingPlayIconRunnable500);
            pendingPlayIconRunnable500 = null;
        }
    }

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
            cancelPendingCallbacks();
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
            cancelPendingCallbacks();
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
            cancelPendingCallbacks();
            Log.i(TAG, "[AutoPauseFirstVideo] User resumed playback -> auto-pause unlocked.");
        }
    }

    /**
     * Invoked when the feed pager scroll state changes (1 = dragging, 2 = settling, 0 = idle).
     */
    public static void onPageScroll(int state) {
        if (state != 0) {
            if (isAwaitingFirstPlay.compareAndSet(true, false)) {
                cancelPendingCallbacks();
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
        synchronized (TikTokAutoPauseHook.class) {
            if (pendingPauseRunnable != null) {
                MAIN_HANDLER.removeCallbacks(pendingPauseRunnable);
            }
            pendingPauseRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (TikTokAutoPauseHook.class) {
                        pendingPauseRunnable = null;
                    }
                    if (isAwaitingFirstPlay.get()) {
                        try {
                            Method pauseMethod = controller.getClass().getMethod("pauseVideo");
                            pauseMethod.setAccessible(true);
                            pauseMethod.invoke(controller);
                        } catch (Throwable ignored) {}
                    }
                }
            };
            MAIN_HANDLER.postDelayed(pendingPauseRunnable, 200);
        }
    }

    private static void showPlayIcon(final Object controller) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invokeShowIvPlay(controller);
        }

        synchronized (TikTokAutoPauseHook.class) {
            if (pendingPlayIconRunnable150 != null) {
                MAIN_HANDLER.removeCallbacks(pendingPlayIconRunnable150);
            }
            if (pendingPlayIconRunnable500 != null) {
                MAIN_HANDLER.removeCallbacks(pendingPlayIconRunnable500);
            }
            pendingPlayIconRunnable150 = new Runnable() {
                @Override
                public void run() {
                    synchronized (TikTokAutoPauseHook.class) {
                        pendingPlayIconRunnable150 = null;
                    }
                    if (isAwaitingFirstPlay.get()) {
                        invokeShowIvPlay(controller);
                    }
                }
            };
            pendingPlayIconRunnable500 = new Runnable() {
                @Override
                public void run() {
                    synchronized (TikTokAutoPauseHook.class) {
                        pendingPlayIconRunnable500 = null;
                    }
                    if (isAwaitingFirstPlay.get()) {
                        invokeShowIvPlay(controller);
                    }
                }
            };
            MAIN_HANDLER.postDelayed(pendingPlayIconRunnable150, 150);
            MAIN_HANDLER.postDelayed(pendingPlayIconRunnable500, 500);
        }
    }

    private static boolean invokeShowIvPlay(Object controller) {
        try {
            for (Method m : controller.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && m.getReturnType() != null) {
                    if ("com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel".equals(m.getReturnType().getName())) {
                        m.setAccessible(true);
                        Object panel = m.invoke(controller);
                        if (panel != null) {
                            Method showIvPlayMethod = panel.getClass().getMethod("showIvPlay");
                            showIvPlayMethod.setAccessible(true);
                            showIvPlayMethod.invoke(panel);
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
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
        cancelPendingCallbacks();
        firstAwemeAid = null;
        isAwaitingFirstPlay.set(true);
    }
}
