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

    public static String filterGuessSearchRaw(String rawString) {
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
                        if (isSuggestedSearch(type)) {
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

    public static void filterGuessSearchResponse(Object responseObj) {
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
                        if (type instanceof String && isSuggestedSearch((String) type)) {
                            it.remove();
                            Log.i(TAG, "[Search Filter] Removed response item: " + type);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterResponse error: " + t.getMessage());
        }
    }

    public static String filterPopularLivesRaw(String rawString) {
        return filterRaw(rawString, "live_popular");
    }

    public static void filterPopularLivesResponse(Object responseObj) {
        filterResponse(responseObj, "live_popular");
    }

    private static String filterRaw(String rawString, String blockType) {
        if (rawString == null || rawString.isEmpty() || blockType == null) {
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
                        if (blockType.equals(type)) {
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

    private static void filterResponse(Object responseObj, String blockType) {
        if (responseObj == null || blockType == null) return;
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
                        if (blockType.equals(type)) {
                            it.remove();
                            Log.i(TAG, "[Search Filter] Removed response item: " + type);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "[Search Filter] filterResponse error: " + t.getMessage());
        }
    }
}
