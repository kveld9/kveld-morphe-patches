package com.kveld9.morphe.extension.tiktok;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runtime companion hook for TikTok comment auto-translation.
 * Bridges loaded comments and bound comment cells with TikTok's native batch translation engine.
 */
public final class TikTokCommentTranslateHook {
    private static final String TAG = "MorpheTikTok";
    private static final long STALE_ENTRY_MS = 15_000L;
    private static final long LOADED_BATCH_STALE_MS = 60_000L;
    private static final int MAX_LOADED_BATCHES = 4;
    private static final int MAX_REQUESTED_BATCH_KEYS = 12;

    private static final Object LOCK = new Object();
    private static final LinkedHashMap<String, VisibleComment> visibleComments = new LinkedHashMap<>();
    private static final LinkedHashMap<String, LoadedBatch> loadedBatches = new LinkedHashMap<>();
    private static final LinkedHashSet<String> requestedLoadedBatchKeys = new LinkedHashSet<>();
    private static LoadedBatch latestLoadedBatch;
    private static WeakReference<Object> lastManager = new WeakReference<>(null);
    private static volatile Context applicationContext;
    private static volatile Object nativeLanguageService;
    private static volatile Method nativeTargetLanguageGetter;
    private static volatile Object nativeLanguageSettings;
    private static volatile Method nativeDoNotTranslateGetter;

    public static volatile boolean enabled = true;

    private TikTokCommentTranslateHook() {}

    public static void registerCommentCell(View itemView, Object manager) {
        if (!enabled || itemView == null || manager == null) return;

        try {
            captureContext(itemView);
            AnchorParts parts = resolveAnchorParts(manager);
            if (parts == null) return;

            String cid = invokeString(parts.comment, "getCid");
            if (isBlank(cid)) return;

            long now = System.currentTimeMillis();
            synchronized (LOCK) {
                pruneLocked(now);
                visibleComments.put(cid, new VisibleComment(manager, parts.comment, parts.context, now));
                lastManager = new WeakReference<>(manager);
            }

            translateLoadedBatchIfReady(manager, false);
            itemView.postDelayed(() -> translateLoadedBatchIfReady(manager, true), 350);
        } catch (Throwable ex) {
            Log.d(TAG, "[Comment Auto Translate] registerCommentCell note: " + ex.getMessage());
        }
    }

    public static void onCommentListLoaded(Object commentItemList) {
        if (!enabled || commentItemList == null) return;

        try {
            Object itemsObject = readField(commentItemList, "items");
            if (!(itemsObject instanceof List)) return;

            List<?> items = (List<?>) itemsObject;
            ArrayList<Object> comments = new ArrayList<>();
            LinkedHashSet<String> cids = new LinkedHashSet<>();
            String aid = null;

            for (Object item : items) {
                if (item == null) continue;
                String cid = invokeStringQuiet(item, "getCid");
                if (isBlank(cid) || cids.contains(cid)) continue;

                String itemAid = invokeStringQuiet(item, "getAwemeId");
                if (isBlank(aid) && !isBlank(itemAid)) {
                    aid = itemAid;
                }
                comments.add(item);
                cids.add(cid);
            }

            if (comments.isEmpty()) return;

            LoadedBatch batch = new LoadedBatch(aid, comments, cids, System.currentTimeMillis());
            Log.d(TAG, "[Comment Auto Translate] Captured " + comments.size() + " loaded comment(s) (aid=" + aid + ")");
            synchronized (LOCK) {
                pruneLocked(batch.loadedAtMs);
                latestLoadedBatch = batch;
                loadedBatches.put(batch.key(), batch);
                trimLoadedBatchesLocked();
            }

            translateLoadedBatchIfReady(lastManager.get(), false);
        } catch (Throwable ex) {
            Log.d(TAG, "[Comment Auto Translate] onCommentListLoaded note: " + ex.getMessage());
        }
    }

    private static void captureContext(View itemView) {
        if (applicationContext == null && itemView != null && itemView.getContext() != null) {
            applicationContext = itemView.getContext().getApplicationContext();
        }
    }

    private static void trimLoadedBatchesLocked() {
        while (loadedBatches.size() > MAX_LOADED_BATCHES) {
            Iterator<String> iterator = loadedBatches.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    private static void translateLoadedBatchIfReady(Object anchor, boolean allowVisibleFallback) {
        Batch batch = buildLoadedBatch(anchor, allowVisibleFallback);
        if (batch.comments.isEmpty()) return;

        String effectiveRequestKey = batch.requestKey + ":lang:" + currentLanguagePolicyKey();
        synchronized (LOCK) {
            if (requestedLoadedBatchKeys.contains(effectiveRequestKey)) return;
        }

        try {
            Method method = findNativeBatchMethod(batch.nativeManagerClass, batch.context.getClass());
            if (method == null) return;

            method.setAccessible(true);
            method.invoke(null, batch.comments, batch.context, false);

            synchronized (LOCK) {
                recordRequestedKeyLocked(effectiveRequestKey);
            }
            Log.d(TAG, "[Comment Auto Translate] Dispatched batch translation for "
                    + batch.comments.size() + " comment(s).");
        } catch (Throwable ex) {
            Log.d(TAG, "[Comment Auto Translate] Batch dispatch note: " + ex.getMessage());
        }
    }

    private static void recordRequestedKeyLocked(String key) {
        requestedLoadedBatchKeys.add(key);
        while (requestedLoadedBatchKeys.size() > MAX_REQUESTED_BATCH_KEYS) {
            Iterator<String> iterator = requestedLoadedBatchKeys.iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    private static Method findNativeBatchMethod(Class<?> managerClass, Class<?> contextClass) {
        Class<?> current = managerClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 3) continue;
                if (!List.class.isAssignableFrom(parameters[0])) continue;
                if (!parameters[1].isAssignableFrom(contextClass)) continue;
                if (parameters[2] != boolean.class && parameters[2] != Boolean.class) continue;
                return method;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Batch buildLoadedBatch(Object anchor, boolean allowVisibleFallback) {
        long now = System.currentTimeMillis();
        AnchorParts parts = resolveAnchorParts(anchor);
        if (parts == null) return new Batch(new ArrayList<>(), null, null, null);

        String anchorAid = invokeStringQuiet(parts.comment, "getAwemeId");
        String anchorCid = invokeStringQuiet(parts.comment, "getCid");
        Set<?> pending = readPendingSet(parts.nativeManager);

        ArrayList<Object> comments = new ArrayList<>();
        String requestKey;

        synchronized (LOCK) {
            pruneLocked(now);
            LoadedBatch loadedBatch = findLoadedBatchLocked(anchorAid, anchorCid);
            if (loadedBatch != null) {
                requestKey = loadedBatch.requestKey();
                collectEligibleComments(loadedBatch.comments, anchorAid, pending, comments);
            } else if (allowVisibleFallback) {
                requestKey = buildVisibleFallbackLocked(anchorAid, parts.context, pending, comments);
            } else {
                requestKey = null;
            }
        }

        if (requestKey == null) comments.clear();
        return new Batch(comments, parts.context, parts.nativeManager.getClass(), requestKey);
    }

    private static void collectEligibleComments(
            List<Object> candidates,
            String anchorAid,
            Set<?> pending,
            List<Object> output
    ) {
        for (Object comment : candidates) {
            if (comment == null) continue;
            if (!matchesAid(anchorAid, invokeStringQuiet(comment, "getAwemeId"))) continue;
            if (isTranslated(comment) || shouldSkipTranslation(comment)) continue;

            String cid = invokeStringQuiet(comment, "getCid");
            if (pending != null && cid != null && pending.contains(cid)) continue;

            output.add(comment);
        }
    }

    private static String buildVisibleFallbackLocked(
            String anchorAid,
            Object anchorContext,
            Set<?> pending,
            ArrayList<Object> comments
    ) {
        if (anchorContext == null) return null;

        LinkedHashSet<String> cids = new LinkedHashSet<>();
        for (VisibleComment entry : visibleComments.values()) {
            if (entry.context.get() != anchorContext) continue;

            Object comment = entry.comment.get();
            if (comment == null || isTranslated(comment) || shouldSkipTranslation(comment)) continue;
            if (!matchesAid(anchorAid, invokeStringQuiet(comment, "getAwemeId"))) continue;

            String cid = invokeStringQuiet(comment, "getCid");
            if (isBlank(cid) || cids.contains(cid)) continue;
            if (pending != null && pending.contains(cid)) continue;

            comments.add(comment);
            cids.add(cid);
        }

        if (cids.isEmpty()) return null;
        String firstCid = cids.iterator().next();
        String scope = isBlank(anchorAid) ? "ctx:" + System.identityHashCode(anchorContext) : anchorAid;
        return "vis:" + scope + ":" + comments.size() + ":" + firstCid;
    }

    private static LoadedBatch findLoadedBatchLocked(String anchorAid, String anchorCid) {
        if (!isBlank(anchorCid)) {
            if (latestLoadedBatch != null && latestLoadedBatch.cids.contains(anchorCid)) {
                return latestLoadedBatch;
            }
            for (LoadedBatch batch : loadedBatches.values()) {
                if (batch.cids.contains(anchorCid)) return batch;
            }
            return null;
        }

        if (!isBlank(anchorAid)) {
            LoadedBatch batch = loadedBatches.get(anchorAid);
            if (batch != null) return batch;
            if (latestLoadedBatch != null && sameValue(anchorAid, latestLoadedBatch.aid)) {
                return latestLoadedBatch;
            }
        }
        return null;
    }

    private static void pruneLocked(long now) {
        Iterator<Map.Entry<String, VisibleComment>> iterator = visibleComments.entrySet().iterator();
        while (iterator.hasNext()) {
            VisibleComment entry = iterator.next().getValue();
            if (now - entry.lastSeenMs > STALE_ENTRY_MS
                    || entry.manager.get() == null
                    || entry.comment.get() == null
                    || entry.context.get() == null) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<String, LoadedBatch>> batchIterator = loadedBatches.entrySet().iterator();
        while (batchIterator.hasNext()) {
            LoadedBatch entry = batchIterator.next().getValue();
            if (now - entry.loadedAtMs > LOADED_BATCH_STALE_MS) {
                batchIterator.remove();
                if (entry == latestLoadedBatch) latestLoadedBatch = null;
            }
        }
    }

    private static boolean isTranslated(Object comment) {
        String translated = invokeStringQuiet(comment, "isTranslated");
        return "true".equalsIgnoreCase(translated);
    }

    private static boolean shouldSkipTranslation(Object comment) {
        String commentLanguage = primaryLanguageTag(invokeStringQuiet(comment, "getCommentLanguage"));
        if (isBlank(commentLanguage)) return false;

        String targetLanguage = primaryLanguageTag(getNativeTranslationTargetLanguage());
        if (!isBlank(targetLanguage) && commentLanguage.equals(targetLanguage)) return true;

        for (String language : getNativeDoNotTranslateLanguages()) {
            if (commentLanguage.equals(primaryLanguageTag(language))) return true;
        }
        return false;
    }

    private static String currentLanguagePolicyKey() {
        StringBuilder key = new StringBuilder("target:")
                .append(value(primaryLanguageTag(getNativeTranslationTargetLanguage())))
                .append(":dnt");
        for (String language : getNativeDoNotTranslateLanguages()) {
            key.append(':').append(value(primaryLanguageTag(language)));
        }
        return key.toString().toLowerCase(Locale.ROOT);
    }

    private static String getNativeTranslationTargetLanguage() {
        resolveNativeTargetLanguageGetter();
        if (nativeLanguageService != null && nativeTargetLanguageGetter != null) {
            try {
                Object selected = nativeTargetLanguageGetter.invoke(nativeLanguageService);
                if (selected instanceof String && !isBlank((String) selected)) {
                    return (String) selected;
                }
            } catch (Throwable ignored) {}
        }

        Context ctx = applicationContext;
        if (ctx != null) {
            Configuration cfg = ctx.getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList locales = cfg.getLocales();
                if (!locales.isEmpty()) return locales.get(0).toLanguageTag();
            } else if (cfg.locale != null) {
                return cfg.locale.toLanguageTag();
            }
        }
        return Locale.getDefault().toLanguageTag();
    }

    private static void resolveNativeTargetLanguageGetter() {
        if (nativeLanguageService != null && nativeTargetLanguageGetter != null) return;
        synchronized (LOCK) {
            if (nativeLanguageService != null && nativeTargetLanguageGetter != null) return;
            try {
                Class<?> serviceClass = Class.forName(
                        "com.ss.android.ugc.aweme.translation.service.TranslationLangKevaServiceImpl"
                );
                Object service = serviceClass.getDeclaredConstructor().newInstance();
                for (Method candidate : serviceClass.getDeclaredMethods()) {
                    if (candidate.getParameterTypes().length == 0 && candidate.getReturnType() == String.class) {
                        candidate.setAccessible(true);
                        nativeLanguageService = service;
                        nativeTargetLanguageGetter = candidate;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static String[] getNativeDoNotTranslateLanguages() {
        resolveNativeDoNotTranslateGetter();
        if (nativeLanguageSettings != null && nativeDoNotTranslateGetter != null) {
            try {
                Object selected = nativeDoNotTranslateGetter.invoke(nativeLanguageSettings);
                if (selected instanceof String[]) return (String[]) selected;
                if (selected instanceof Collection) {
                    ArrayList<String> languages = new ArrayList<>();
                    for (Object item : (Collection<?>) selected) {
                        if (item != null && !isBlank(String.valueOf(item))) {
                            languages.add(String.valueOf(item));
                        }
                    }
                    return languages.toArray(new String[0]);
                }
            } catch (Throwable ignored) {}
        }
        return new String[0];
    }

    private static void resolveNativeDoNotTranslateGetter() {
        if (nativeLanguageSettings != null && nativeDoNotTranslateGetter != null) return;
        synchronized (LOCK) {
            if (nativeLanguageSettings != null && nativeDoNotTranslateGetter != null) return;
            try {
                Class<?> serviceClass = Class.forName(
                        "com.ss.android.ugc.aweme.translation.service.TranslationLangKevaServiceImpl"
                );
                Object service = serviceClass.getDeclaredConstructor().newInstance();
                for (Method provider : serviceClass.getDeclaredMethods()) {
                    if (provider.getParameterTypes().length != 0 || provider.getReturnType() == void.class) {
                        continue;
                    }
                    try {
                        Method candidate = provider.getReturnType().getMethod("getSelectedDoNotTranslateLanguageCodes");
                        provider.setAccessible(true);
                        Object resolvedSettings = provider.invoke(service);
                        if (resolvedSettings == null) continue;
                        candidate.setAccessible(true);
                        nativeLanguageSettings = resolvedSettings;
                        nativeDoNotTranslateGetter = candidate;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }

    private static String primaryLanguageTag(String languageTag) {
        if (isBlank(languageTag)) return null;
        String normalized = languageTag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        int separatorIndex = normalized.indexOf('-');
        return separatorIndex > 0 ? normalized.substring(0, separatorIndex) : normalized;
    }

    private static Set<?> readPendingSet(Object nativeManager) {
        if (nativeManager == null) return null;
        try {
            Class<?> current = nativeManager.getClass();
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) ||
                            !Set.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value instanceof Set) return (Set<?>) value;
                }
                current = current.getSuperclass();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static AnchorParts resolveAnchorParts(Object anchor) {
        if (anchor == null) return null;
        try {
            ArrayList<Object> values = readInstanceFieldValues(anchor);
            Object comment = findCommentInValues(values);
            if (comment == null) return null;

            for (Object nativeManager : values) {
                if (nativeManager == null || nativeManager == comment) continue;
                for (Object context : values) {
                    if (context == null || context == comment || context == nativeManager) continue;
                    if (findNativeBatchMethod(nativeManager.getClass(), context.getClass()) != null) {
                        return new AnchorParts(comment, context, nativeManager);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object findCommentInValues(List<Object> values) {
        for (Object value : values) {
            if (value != null && hasNoArgMethod(value.getClass(), "getCid")) {
                return value;
            }
        }
        return null;
    }

    private static ArrayList<Object> readInstanceFieldValues(Object instance) {
        ArrayList<Object> values = new ArrayList<>();
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    if (value != null) values.add(value);
                } catch (Throwable ignored) {}
            }
            current = current.getSuperclass();
        }
        return values;
    }

    private static boolean hasNoArgMethod(Class<?> type, String name) {
        try {
            type.getMethod(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object readField(Object instance, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance.getClass(), name);
        if (field == null) throw new NoSuchFieldException(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String invokeString(Object instance, String methodName) throws Exception {
        Method method = instance.getClass().getMethod(methodName);
        Object value = method.invoke(instance);
        return value == null ? null : String.valueOf(value);
    }

    private static String invokeStringQuiet(Object instance, String methodName) {
        if (instance == null) return null;
        try {
            return invokeString(instance, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean sameValue(String left, String right) {
        if (left == null) return right == null;
        return left.equals(right);
    }

    private static boolean matchesAid(String anchorAid, String itemAid) {
        return isBlank(anchorAid) || isBlank(itemAid) || sameValue(anchorAid, valueOrNull(itemAid));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static String value(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static final class VisibleComment {
        final WeakReference<Object> manager;
        final WeakReference<Object> comment;
        final WeakReference<Object> context;
        final long lastSeenMs;

        VisibleComment(Object manager, Object comment, Object context, long lastSeenMs) {
            this.manager = new WeakReference<>(manager);
            this.comment = new WeakReference<>(comment);
            this.context = new WeakReference<>(context);
            this.lastSeenMs = lastSeenMs;
        }
    }

    private static final class AnchorParts {
        final Object comment;
        final Object context;
        final Object nativeManager;

        AnchorParts(Object comment, Object context, Object nativeManager) {
            this.comment = comment;
            this.context = context;
            this.nativeManager = nativeManager;
        }
    }

    private static final class Batch {
        final ArrayList<Object> comments;
        final Object context;
        final Class<?> nativeManagerClass;
        final String requestKey;

        Batch(ArrayList<Object> comments, Object context, Class<?> nativeManagerClass, String requestKey) {
            this.comments = comments;
            this.context = context;
            this.nativeManagerClass = nativeManagerClass;
            this.requestKey = requestKey;
        }
    }

    private static final class LoadedBatch {
        final String aid;
        final ArrayList<Object> comments;
        final LinkedHashSet<String> cids;
        final long loadedAtMs;

        LoadedBatch(String aid, ArrayList<Object> comments, LinkedHashSet<String> cids, long loadedAtMs) {
            this.aid = aid;
            this.comments = comments;
            this.cids = cids;
            this.loadedAtMs = loadedAtMs;
        }

        String key() {
            if (!isBlank(aid)) return aid;
            return cids.isEmpty() ? "unknown" : "cid:" + cids.iterator().next();
        }

        String requestKey() {
            String firstCid = cids.isEmpty() ? "unknown" : cids.iterator().next();
            return key() + ":" + comments.size() + ":" + firstCid;
        }
    }
}
