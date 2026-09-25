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

    private static boolean isSuggestedSearch(String type, String source, String title) {
        if (type != null) {
            String t = type.toLowerCase();
            if (t.contains("recom") || t.contains("guess") || t.contains("suggest")) {
                return true;
            }
        }
        if (source != null) {
            String s = source.toLowerCase();
            if (s.contains("recom") || s.contains("guess") || s.contains("suggest")) {
                return true;
            }
        }
        if (title != null) {
            String ti = title.toLowerCase();
            if (ti.contains("suggest") || ti.contains("interesarte") || ti.contains("you may like")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPopularLive(String type, String source, String title) {
        if (type != null) {
            String t = type.toLowerCase();
            if (t.contains("live") || t.contains("rank") || t.contains("billboard")) {
                return true;
            }
        }
        if (source != null) {
            String s = source.toLowerCase();
            if (s.contains("live") || s.contains("rank") || s.contains("billboard")) {
                return true;
            }
        }
        if (title != null) {
            String ti = title.toLowerCase();
            if (ti.contains("live") || ti.contains("en vivo") || ti.contains("populares")) {
                return true;
            }
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
                        String title = null;
                        JSONObject params = item.optJSONObject("params");
                        if (params != null) {
                            title = params.optString("title");
                        }
                        boolean match = isSuggestedSearchFilter ? isSuggestedSearch(type, source, title) : isPopularLive(type, source, title);
                        if (match) {
                            Log.i(TAG, "[Search Filter] Filtered card: type=" + type + ", source=" + source + ", title=" + title);
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
                        String typeStr = null;
                        try {
                            Method getTypeMethod = item.getClass().getMethod("getType");
                            Object type = getTypeMethod.invoke(item);
                            if (type instanceof String) typeStr = (String) type;
                        } catch (Throwable ignored) {}

                        String sourceStr = null;
                        try {
                            Method getSourceMethod = item.getClass().getMethod("getSource");
                            Object source = getSourceMethod.invoke(item);
                            if (source instanceof String) sourceStr = (String) source;
                        } catch (Throwable ignored) {}

                        boolean match = isSuggestedSearchFilter ? isSuggestedSearch(typeStr, sourceStr, null) : isPopularLive(typeStr, sourceStr, null);
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
}
