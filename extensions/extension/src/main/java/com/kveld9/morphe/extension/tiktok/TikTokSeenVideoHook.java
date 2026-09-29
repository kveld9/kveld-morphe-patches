package com.kveld9.morphe.extension.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Crash-safe seen video filter for TikTok.
 * Records watched video IDs during playback and filters them from incoming network feed payloads,
 * without mutating active UI RecyclerView adapter collections.
 * Persists seen IDs across app sessions with a bounded LRU cache.
 */
@SuppressWarnings("unused")
public final class TikTokSeenVideoHook {

    private static final String TAG = "MorpheTikTok";
    private static final String PREFS_NAME = "morphe_tiktok_seen_videos";
    private static final String KEY_SEEN_IDS = "seen_ids";
    private static final int MAX_SEEN_HISTORY = 2000;

    public static volatile boolean enabled = true;

    private static final Map<String, Long> seenVideos = Collections.synchronizedMap(
        new LinkedHashMap<String, Long>(MAX_SEEN_HISTORY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_SEEN_HISTORY;
            }
        }
    );

    private static volatile SharedPreferences prefs = null;
    private static final AtomicBoolean persistedLoaded = new AtomicBoolean(false);
    private static final AtomicBoolean savePending = new AtomicBoolean(false);
    private static final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    private static volatile Field itemsField = null;
    private static volatile Field mItemsField = null;
    private static volatile Method getAidMethod = null;
    private static volatile boolean reflectionInitialized = false;

    private TikTokSeenVideoHook() {}

    private static SharedPreferences getPrefs() {
        if (prefs != null) return prefs;
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null);
            if (app != null) {
                prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {}
        return prefs;
    }

    private static void ensureHistoryLoaded() {
        if (persistedLoaded.get()) return;
        if (!persistedLoaded.compareAndSet(false, true)) return;

        try {
            SharedPreferences sp = getPrefs();
            if (sp == null) {
                persistedLoaded.set(false);
                return;
            }
            Set<String> saved = sp.getStringSet(KEY_SEEN_IDS, null);
            if (saved != null && !saved.isEmpty()) {
                long now = System.currentTimeMillis();
                for (String aid : saved) {
                    if (aid != null && !aid.isEmpty()) {
                        seenVideos.put(aid, now);
                    }
                }
                Log.i(TAG, "[Hide Seen Videos] Loaded " + seenVideos.size() + " persisted seen video(s).");
            }
        } catch (Throwable t) {
            persistedLoaded.set(false);
            Log.w(TAG, "[Hide Seen Videos] History load note: " + t.getMessage());
        }
    }

    private static void scheduleSave() {
        if (savePending.compareAndSet(false, true)) {
            diskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    savePending.set(false);
                    try {
                        SharedPreferences sp = getPrefs();
                        if (sp == null) return;
                        Set<String> snapshot;
                        synchronized (seenVideos) {
                            snapshot = new HashSet<String>(seenVideos.keySet());
                        }
                        sp.edit().putStringSet(KEY_SEEN_IDS, snapshot).apply();
                    } catch (Throwable ignored) {}
                }
            });
        }
    }

    /**
     * Records video as seen upon full playback completion.
     */
    public static void onPlayCompleted(String aid) {
        if (!enabled || aid == null || aid.isEmpty()) return;
        ensureHistoryLoaded();
        seenVideos.put(aid, System.currentTimeMillis());
        scheduleSave();
    }

    /**
     * Records video as seen when watched for >= 5s or >= 70% duration.
     */
    public static void onPlayProgressChange(String aid, long progressMs, long durationMs) {
        if (!enabled || aid == null || aid.isEmpty()) return;
        if (progressMs < 5000L && (durationMs <= 0 || progressMs * 10 < durationMs * 7)) return;

        ensureHistoryLoaded();
        seenVideos.put(aid, System.currentTimeMillis());
        scheduleSave();
    }

    /**
     * Filters previously watched videos from incoming network FeedItemList response batches.
     * Prevents duplicate/seen videos from appearing in newly fetched feed pages while preserving
     * active RecyclerView adapter items.
     */
    public static void filterSeenVideosInFeedItemList(Object feedItemList) {
        if (!enabled || feedItemList == null) return;
        ensureHistoryLoaded();
        if (seenVideos.isEmpty()) return;

        try {
            List<Object> items = extractFeedItems(feedItemList);
            if (items == null || items.isEmpty()) return;

            int originalCount = items.size();
            int pruned = 0;
            Object fallback = null;

            synchronized (items) {
                Iterator<Object> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Object aweme = iterator.next();
                    String aid = getAid(aweme);
                    if (aid != null && seenVideos.containsKey(aid)) {
                        fallback = aweme;
                        iterator.remove();
                        pruned++;
                    }
                }

                // If all items were seen, retain one item to prevent feed presenter failure
                if (items.isEmpty() && fallback != null && originalCount > 0) {
                    items.add(fallback);
                    pruned--;
                }
            }

            if (pruned > 0) {
                Log.i(TAG, "[Hide Seen Videos] Filtered " + pruned + " previously viewed video(s) from incoming feed batch.");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Hide Seen Videos] Error filtering feed item list: " + t.getMessage());
        }
    }

    private static String getAid(Object aweme) {
        if (aweme == null) return null;
        try {
            initReflection(aweme.getClass(), null);
            if (getAidMethod != null) {
                Object result = getAidMethod.invoke(aweme);
                return result != null ? result.toString() : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractFeedItems(Object feedItemList) {
        if (feedItemList == null) return null;
        try {
            initReflection(null, feedItemList.getClass());
            if (itemsField != null) {
                Object itemsObj = itemsField.get(feedItemList);
                if (itemsObj instanceof List) return (List<Object>) itemsObj;
            }
            if (mItemsField != null) {
                Object itemsObj = mItemsField.get(feedItemList);
                if (itemsObj instanceof List) return (List<Object>) itemsObj;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void initReflection(Class<?> awemeClass, Class<?> feedListClass) {
        if (reflectionInitialized) return;
        synchronized (TikTokSeenVideoHook.class) {
            if (awemeClass != null && getAidMethod == null) {
                try {
                    getAidMethod = awemeClass.getMethod("getAid");
                    getAidMethod.setAccessible(true);
                } catch (Throwable ignored) {}
            }
            if (feedListClass != null && itemsField == null && mItemsField == null) {
                try {
                    itemsField = feedListClass.getDeclaredField("items");
                    itemsField.setAccessible(true);
                } catch (Throwable ignored) {}
                try {
                    mItemsField = feedListClass.getDeclaredField("mItems");
                    mItemsField.setAccessible(true);
                } catch (Throwable ignored) {}
            }
            if (getAidMethod != null && (itemsField != null || mItemsField != null)) {
                reflectionInitialized = true;
            }
        }
    }
}
