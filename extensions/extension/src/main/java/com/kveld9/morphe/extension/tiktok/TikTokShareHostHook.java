package com.kveld9.morphe.extension.tiktok;

import android.net.Uri;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
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
        "is_from_webapp", "sender_device", "_r", "_d", "_s", "_t", "checksum",
        "checksum_invitation_scene", "invitation_scene", "sec_user_id", "preview_pb",
        "sharer_language", "ug_source", "share_app_id", "share_item_id", "share_link_id",
        "share_iid", "sharer_id", "share_sign", "share_token", "share_sec_user_id",
        "share_author_id", "source", "timestamp", "user_id", "u_code", "tt_from",
        "utm_source", "utm_campaign", "utm_medium", "utm_term", "utm_content",
        "ugbiz_name", "social_share_type", "enter_method", "enter_from"
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
        if (!isTikTokShareUrl(originalUrl, host)) {
            return originalUrl;
        }

        try {
            Uri uri = Uri.parse(originalUrl);
            Uri.Builder builder = uri.buildUpon().clearQuery();

            // 1. Filter out tracking query parameters
            if (uri.getQuery() != null && !uri.getQueryParameterNames().isEmpty()) {
                for (String param : uri.getQueryParameterNames()) {
                    if (!TRACKING_PARAMS.contains(param.toLowerCase(Locale.ROOT))) {
                        builder.appendQueryParameter(param, uri.getQueryParameter(param));
                    }
                }
            }

            // 2. Replace host if custom host is supplied
            String targetHost = (host != null) ? cleanHost(host) : "";
            if (!targetHost.isEmpty()) {
                builder.authority(targetHost);
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

    public static boolean isTikTokShareUrl(String originalUrl) {
        return isTikTokShareUrl(originalUrl, customHost);
    }

    public static boolean isTikTokShareUrl(String originalUrl, String customHostCandidate) {
        if (originalUrl == null || originalUrl.isEmpty()) {
            return false;
        }

        String targetCustom = cleanHost(customHostCandidate);
        String globalCustom = cleanHost(customHost);

        boolean quickMatch = originalUrl.contains("tiktok.com")
                || originalUrl.contains("tiktxk.com")
                || (!targetCustom.isEmpty() && originalUrl.contains(targetCustom))
                || (!globalCustom.isEmpty() && originalUrl.contains(globalCustom));
        if (!quickMatch) {
            return false;
        }

        try {
            Uri uri = Uri.parse(originalUrl);
            String host = uri.getHost();
            if (host == null && uri.getScheme() == null) {
                uri = Uri.parse("https://" + originalUrl);
                host = uri.getHost();
            }
            if (host == null) {
                return false;
            }
            if (isHostOrSubdomain(host, "tiktok.com") || isHostOrSubdomain(host, "tiktxk.com")) {
                return true;
            }
            if (!targetCustom.isEmpty() && isHostOrSubdomain(host, targetCustom)) {
                return true;
            }
            if (!globalCustom.isEmpty() && isHostOrSubdomain(host, globalCustom)) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static String cleanHost(String host) {
        if (host == null) {
            return "";
        }
        String h = host.trim();
        if (h.startsWith("http://")) {
            h = h.substring(7);
        } else if (h.startsWith("https://")) {
            h = h.substring(8);
        }
        if (h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        int slashIdx = h.indexOf('/');
        if (slashIdx != -1) {
            h = h.substring(0, slashIdx);
        }
        return h;
    }

    private static boolean isHostOrSubdomain(String host, String baseDomain) {
        if (host == null || baseDomain == null || baseDomain.isEmpty()) {
            return false;
        }
        return host.equalsIgnoreCase(baseDomain)
                || host.toLowerCase(Locale.ROOT).endsWith("." + baseDomain.toLowerCase(Locale.ROOT));
    }
}
