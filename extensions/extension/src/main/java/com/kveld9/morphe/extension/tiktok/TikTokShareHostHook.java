package com.kveld9.morphe.extension.tiktok;

import android.net.Uri;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime extension hook for sanitizing TikTok share URLs and redirecting share hosts.
 * Purges tracking tokens, device telemetry, and marketing campaign parameters from shared links,
 * and replaces the TikTok host with a user-configured alternative (e.g., vxtiktok.com, tiktxk.com).
 */
@SuppressWarnings("unused")
public final class TikTokShareHostHook {

    private static final String TAG = "MorpheTikTok";

    public static volatile String customHost = "";

    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        "is_from_webapp", "sender_device", "_r", "checksum", "sec_user_id",
        "ug_source", "share_app_id", "share_item_id", "share_link_id",
        "source", "timestamp", "user_id", "u_code", "tt_from",
        "utm_source", "utm_campaign", "utm_medium",
        "share_iid", "sharer_id", "ugbiz_name"
    ));

    private TikTokShareHostHook() {}

    /**
     * Sanitizes share URL tracking parameters and replaces the host with customHost if configured.
     *
     * @param originalUrl Original share URL returned by Aweme.getShareUrl().
     * @return Sanitized URL with tracking query parameters removed and custom host applied.
     */
    public static String sanitizeShareUrlWithHost(String originalUrl) {
        return sanitizeShareUrlWithHost(originalUrl, customHost);
    }

    /**
     * Sanitizes share URL tracking parameters and replaces the host with the specified host.
     *
     * @param originalUrl Original share URL.
     * @param host Target custom host name (e.g., "vxtiktok.com") or empty to retain original host.
     * @return Sanitized URL.
     */
    public static String sanitizeShareUrlWithHost(String originalUrl, String host) {
        if (originalUrl == null || !originalUrl.contains("tiktok.com")) {
            return originalUrl;
        }

        try {
            Uri uri = Uri.parse(originalUrl);
            Uri.Builder builder = uri.buildUpon().clearQuery();

            // 1. Filter out tracking query parameters
            if (uri.getQuery() != null && !uri.getQueryParameterNames().isEmpty()) {
                for (String param : uri.getQueryParameterNames()) {
                    if (!TRACKING_PARAMS.contains(param.toLowerCase())) {
                        builder.appendQueryParameter(param, uri.getQueryParameter(param));
                    }
                }
            }

            // 2. Replace host if custom host is supplied
            String targetHost = (host != null) ? host.trim() : "";
            if (!targetHost.isEmpty()) {
                if (targetHost.startsWith("http://")) {
                    targetHost = targetHost.substring(7);
                } else if (targetHost.startsWith("https://")) {
                    targetHost = targetHost.substring(8);
                }
                if (targetHost.endsWith("/")) {
                    targetHost = targetHost.substring(0, targetHost.length() - 1);
                }

                if (!targetHost.isEmpty()) {
                    builder.authority(targetHost);
                }
            }

            String cleaned = builder.build().toString();
            if (cleaned.endsWith("?")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }

            Log.d(TAG, "[Clean Share URL] Sanitized share link -> " + cleaned);
            return cleaned;
        } catch (Throwable t) {
            Log.w(TAG, "[Clean Share URL] Failed to sanitize share URL: " + t.getMessage());
            return originalUrl;
        }
    }
}
