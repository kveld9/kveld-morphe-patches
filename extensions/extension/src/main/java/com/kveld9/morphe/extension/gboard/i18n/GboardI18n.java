package com.kveld9.morphe.extension.gboard.i18n;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GboardI18n {
    private static final Map<String, BaseLanguagePack> PACKS = new HashMap<>();
    private static final BaseLanguagePack DEFAULT_PACK = new EnLanguagePack();

    private static final Map<String, Map<String, String>> TITLES_CACHE = new HashMap<>();
    private static final Map<String, Map<String, String>> SUMMARIES_CACHE = new HashMap<>();

    static {
        register(DEFAULT_PACK);
        register(new EsLanguagePack());
    }

    private GboardI18n() {}

    public static synchronized void register(BaseLanguagePack pack) {
        if (pack == null || pack.getLanguageCode() == null) return;
        String code = pack.getLanguageCode().toLowerCase(Locale.ROOT);
        PACKS.put(code, pack);

        Map<String, String> titles = new HashMap<>();
        pack.populateTitles(titles);
        TITLES_CACHE.put(code, titles);

        Map<String, String> summaries = new HashMap<>();
        pack.populateSummaries(summaries);
        SUMMARIES_CACHE.put(code, summaries);
    }

    public static String getDeviceLanguage(Context context) {
        if (context != null) {
            try {
                Configuration cfg = context.getResources().getConfiguration();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    LocaleList list = cfg.getLocales();
                    if (list != null && !list.isEmpty()) {
                        String lang = list.get(0).getLanguage();
                        if (lang != null && !lang.isEmpty()) {
                            return lang.toLowerCase(Locale.ROOT);
                        }
                    }
                }
                @SuppressWarnings("deprecation")
                Locale loc = cfg.locale;
                if (loc != null && loc.getLanguage() != null && !loc.getLanguage().isEmpty()) {
                    return loc.getLanguage().toLowerCase(Locale.ROOT);
                }
            } catch (Throwable ignored) {}
        }
        try {
            Locale def = Locale.getDefault();
            if (def != null && def.getLanguage() != null && !def.getLanguage().isEmpty()) {
                return def.getLanguage().toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        return "en";
    }

    public static BaseLanguagePack resolvePack(Context context) {
        String code = getDeviceLanguage(context);
        BaseLanguagePack pack = PACKS.get(code);
        return pack != null ? pack : DEFAULT_PACK;
    }

    public static String getTitle(Context context, String prefKey) {
        String code = getDeviceLanguage(context);
        Map<String, String> titles = TITLES_CACHE.get(code);
        if (titles != null) {
            String title = titles.get(prefKey);
            if (title != null) return title;
        }
        Map<String, String> defaultTitles = TITLES_CACHE.get("en");
        return defaultTitles != null ? defaultTitles.get(prefKey) : null;
    }

    public static String getSummary(Context context, String prefKey) {
        String code = getDeviceLanguage(context);
        Map<String, String> summaries = SUMMARIES_CACHE.get(code);
        if (summaries != null) {
            String summary = summaries.get(prefKey);
            if (summary != null) return summary;
        }
        Map<String, String> defaultSummaries = SUMMARIES_CACHE.get("en");
        return defaultSummaries != null ? defaultSummaries.get(prefKey) : null;
    }

    public static String getRestartToast(Context context) {
        return resolvePack(context).getRestartToast();
    }

    public static String getRestartingToast(Context context) {
        return resolvePack(context).getRestartingToast();
    }

    public static String getRestartTitle(Context context, boolean pending) {
        return resolvePack(context).getRestartTitle(pending);
    }

    public static String getRestartSummary(Context context, boolean pending) {
        return resolvePack(context).getRestartSummary(pending);
    }

    public static String formatUnit(Context context, String prefKey, int value) {
        return resolvePack(context).formatUnit(prefKey, value);
    }
}
