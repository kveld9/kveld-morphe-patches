package com.kveld9.morphe.extension.tiktok;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public final class TikTokSearchHook {
    private static final String TAG = "MorpheTikTok";

    private TikTokSearchHook() {}

    private static boolean isSuggestedSearch(String type) {
        if (type == null) return false;
        return type.startsWith("recom_search") || "guess_search".equals(type);
    }

    private static boolean isPopularLive(String type) {
        if (type == null) return false;
        return "trending_rank_live".equals(type) || "live_popular".equals(type) || type.endsWith("rank_live");
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
                        boolean match = isSuggestedSearchFilter ? isSuggestedSearch(type) : isPopularLive(type);
                        if (match) {
                            Log.i(TAG, "[Search Filter] Filtered card: " + type);
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
                        Method getTypeMethod = item.getClass().getMethod("getType");
                        Object type = getTypeMethod.invoke(item);
                        if (type instanceof String) {
                            String typeStr = (String) type;
                            boolean match = isSuggestedSearchFilter ? isSuggestedSearch(typeStr) : isPopularLive(typeStr);
                            if (match) {
                                it.remove();
                                Log.i(TAG, "[Search Filter] Removed response item: " + typeStr);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterResponse error: " + t.getMessage());
        }
    }
}
