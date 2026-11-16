package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class TikTokSearchHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokSearchHook() {}

    private static boolean isSuggestedSearch(String type, String source, String cardType, String kind) {
        if (containsAny(type, "recom", "guess", "suggest")) return true;
        if (containsAny(source, "recom", "guess", "suggest")) return true;
        if (containsAny(cardType, "recom", "guess", "suggest")) return true;
        if (containsAny(kind, "recom", "guess", "suggest")) return true;
        return false;
    }

    private static boolean isPopularLive(String type, String source, String cardType, String kind) {
        if (containsAny(type, "live", "rank", "billboard", "popular")) return true;
        if (containsAny(source, "live", "rank", "billboard", "popular")) return true;
        if (containsAny(cardType, "live", "rank", "billboard", "popular")) return true;
        if (containsAny(kind, "live", "rank", "billboard", "popular")) return true;
        return false;
    }

    private static boolean containsAny(String value, String... targets) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        for (String target : targets) {
            if (lower.contains(target)) return true;
        }
        return false;
    }

    public static String filterGuessSearchRaw(String rawString) {
        return filterRaw(rawString, true);
    }

    public static void filterGuessSearchResponse(Object responseObj) {
        filterResponse(responseObj, true);
    }

    public static String filterPopularLivesRaw(String rawString) {
        return filterRaw(rawString, false);
    }

    public static void filterPopularLivesResponse(Object responseObj) {
        filterResponse(responseObj, false);
    }

    public static Map filterSuggestedAbParams(Map map) {
        if (map == null) return null;
        try {
            Map result = new HashMap(map);
            result.put("show_suggest_search_words", 0);
            result.put("sbp_not_login_disable_guess_search", 1);
            result.put("disable_suggest_guide", 1);
            Log.i(TAG, "[Search Filter] Filtered suggested search abParams");
            return result;
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterSuggestedAbParams error: " + t.getMessage());
            return map;
        }
    }

    public static Map filterPopularLivesAbParams(Map map) {
        if (map == null) return null;
        try {
            Map result = new HashMap(map);
            result.put("has_transfer_tab_live", 0);
            result.remove("transfer_tab_live_url");
            result.put("transfer_tab_live_url", "");
            result.put("intermediate_show_trending_billboard", 0);
            Log.i(TAG, "[Search Filter] Filtered popular lives abParams");
            return result;
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterPopularLivesAbParams error: " + t.getMessage());
            return map;
        }
    }

    public static String filterSuggestedSchema(String schema) {
        if (schema == null) return null;
        return schema.replace(",show_suggest_search_words", "")
                     .replace("show_suggest_search_words,", "")
                     .replace("show_suggest_search_words", "");
    }

    public static String filterPopularLivesSchema(String schema) {
        if (schema == null) return null;
        return schema.replace(",intermediate_show_trending_billboard", "")
                     .replace("intermediate_show_trending_billboard,", "")
                     .replace("intermediate_show_trending_billboard", "");
    }

    private static String filterRaw(String rawString, boolean isSuggestedSearchFilter) {
        if (rawString == null || rawString.isEmpty()) {
            return rawString;
        }
        try {
            JSONObject root = new JSONObject(rawString);
            JSONArray data = root.optJSONArray("data");
            if (data != null && data.length() > 0) {
                JSONArray filtered = new JSONArray();
                boolean modified = false;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item != null) {
                        String type = item.optString("type");
                        String source = item.optString("source");
                        String cardType = item.optString("card_type");
                        String kind = item.optString("kind");
                        if (kind.isEmpty()) kind = item.optString("sub_type");

                        JSONObject params = item.optJSONObject("params");
                        if (params != null) {
                            if (cardType.isEmpty()) cardType = params.optString("card_type");
                            if (kind.isEmpty()) kind = params.optString("business_type");
                        }

                        boolean isGuessFlag = item.optBoolean("is_guess", false)
                                || item.optBoolean("is_recommend", false)
                                || (params != null && (params.optBoolean("is_guess", false) || params.optBoolean("is_recommend", false)));

                        boolean match = isSuggestedSearchFilter
                                ? (isGuessFlag || isSuggestedSearch(type, source, cardType, kind))
                                : isPopularLive(type, source, cardType, kind);

                        if (match) {
                            Log.i(TAG, "[Search Filter] Filtered card: type=" + type + ", source=" + source + ", cardType=" + cardType);
                            modified = true;
                            continue;
                        }
                    }
                    filtered.put(data.get(i));
                }
                if (modified) {
                    root.put("data", filtered);
                    return root.toString();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterRaw error: " + t.getMessage());
        }
        return rawString;
    }

    private static void filterResponse(Object responseObj, boolean isSuggestedSearchFilter) {
        if (responseObj == null) return;
        try {
            Method getDataMethod = responseObj.getClass().getMethod("getData");
            Object dataList = getDataMethod.invoke(responseObj);
            if (dataList instanceof List) {
                List<?> list = (List<?>) dataList;
                Iterator<?> it = list.iterator();
                while (it.hasNext()) {
                    Object item = it.next();
                    if (item != null) {
                        String typeStr = invokeStringMethodQuiet(item, "getType");
                        String sourceStr = invokeStringMethodQuiet(item, "getSource");
                        String cardTypeStr = invokeStringMethodQuiet(item, "getCardType");
                        String kindStr = invokeStringMethodQuiet(item, "getKind");
                        if (kindStr == null) kindStr = invokeStringMethodQuiet(item, "getSubType");

                        boolean match = isSuggestedSearchFilter
                                ? isSuggestedSearch(typeStr, sourceStr, cardTypeStr, kindStr)
                                : isPopularLive(typeStr, sourceStr, cardTypeStr, kindStr);
                        if (match) {
                            it.remove();
                            Log.i(TAG, "[Search Filter] Removed response item: type=" + typeStr + ", source=" + sourceStr);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterResponse error: " + t.getMessage());
        }
    }

    private static String invokeStringMethodQuiet(Object obj, String methodName) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object res = m.invoke(obj);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}
        return null;
    }
}
