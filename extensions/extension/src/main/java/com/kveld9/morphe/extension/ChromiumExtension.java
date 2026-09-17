package com.kveld9.morphe.extension;

import android.content.Intent;
import android.net.Uri;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension helper class for Chromium-based browsers (Brave, Vivaldi).
 *
 * Architectural Note:
 * This sanitizer intercepts URLs at the boundary where links leave Chromium and are passed
 * to the OS (via Android share intents or the system clipboard). Network blocklists
 * (Brave Shields, AdGuard, uBlock) are blind to these operations because no network requests
 * occur when copying or sharing text.
 *
 * Design Philosophy:
 * 1. Deterministic & Static (KISS): No background network fetching, no heavy external rule parsers.
 * 2. Zero UI Jank: Uses O(1) hash lookups without runtime regular expression matching.
 * 3. False-Positive Safety: Ambiguous tokens (such as 'si', 'ref_src', 'trk') are strictly scoped
 *    to their authoritative target domains, while global stripping is reserved for unambiguous adtech keys.
 */
@SuppressWarnings("unused")
public class ChromiumExtension {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");

    private static final Set<String> GLOBAL_TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        // Facebook / Meta
        "fbclid",
        // Google Ads / Analytics / DoubleClick / Conversion
        "gclid",
        "gbraid",
        "wbraid",
        "dclid",
        // Instagram
        "igshid",
        // Microsoft / Bing
        "msclkid",
        // Twitter / X click identifier
        "twclid",
        // Yandex
        "yclid",
        // HubSpot
        "_hsenc",
        "_hsmi",
        // Mailchimp
        "mc_cid",
        "mc_eid",
        // Adobe Analytics / Omniture
        "s_kwcid",
        // Marketo / Wicked Reports
        "mkt_tok",
        "wickedid",
        // Snapchat & Advertising Networks
        "sc_channel",
        "sc_campaign",
        "sc_geo",
        "zanpid",
        "vero_id",
        "vero_conv"
    ));

    private static final Map<String, Set<String>> DOMAIN_TRACKING_PARAMS = new HashMap<>();

    static {
        // YouTube share source identifier
        Set<String> youtubeParams = new HashSet<>(Arrays.asList("si"));
        DOMAIN_TRACKING_PARAMS.put("youtube.com", youtubeParams);
        DOMAIN_TRACKING_PARAMS.put("youtu.be", youtubeParams);

        // Spotify share source identifier
        Set<String> spotifyParams = new HashSet<>(Arrays.asList("si"));
        DOMAIN_TRACKING_PARAMS.put("spotify.com", spotifyParams);

        // Twitter / X attribution
        Set<String> twitterParams = new HashSet<>(Arrays.asList("ref_src", "ref_url"));
        DOMAIN_TRACKING_PARAMS.put("x.com", twitterParams);
        DOMAIN_TRACKING_PARAMS.put("twitter.com", twitterParams);

        // LinkedIn tracking
        Set<String> linkedinParams = new HashSet<>(Arrays.asList("trk"));
        DOMAIN_TRACKING_PARAMS.put("linkedin.com", linkedinParams);

        // TikTok ad/marketing campaign parameters
        Set<String> tiktokParams = new HashSet<>(Arrays.asList("tt_medium", "tt_content"));
        DOMAIN_TRACKING_PARAMS.put("tiktok.com", tiktokParams);
    }

    /**
     * Sanitizes an Android share Intent's EXTRA_TEXT, data URI, and ClipData payloads
     * before the system share sheet displays it.
     */
    public static Intent cleanShareIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        try {
            CharSequence extraText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                String cleaned = cleanShareUrl(extraText.toString());
                intent.putExtra(Intent.EXTRA_TEXT, cleaned);
            }
            Uri data = intent.getData();
            if (data != null) {
                String dataStr = data.toString();
                String cleanedData = cleanShareUrl(dataStr);
                if (!dataStr.equals(cleanedData)) {
                    intent.setData(Uri.parse(cleanedData));
                }
            }
            sanitizeClipData(intent);
        } catch (Throwable ignored) {
        }
        return intent;
    }

    private static void sanitizeClipData(Intent intent) {
        android.content.ClipData clipData = intent.getClipData();
        if (clipData == null || clipData.getItemCount() == 0) {
            return;
        }
        boolean modified = false;
        android.content.ClipData newClip = null;
        for (int i = 0; i < clipData.getItemCount(); i++) {
            android.content.ClipData.Item item = clipData.getItemAt(i);
            Uri itemUri = item.getUri();
            CharSequence itemText = item.getText();
            Uri newUri = itemUri;
            CharSequence newText = itemText;

            if (itemUri != null) {
                String uStr = itemUri.toString();
                String cStr = cleanShareUrl(uStr);
                if (!uStr.equals(cStr)) {
                    newUri = Uri.parse(cStr);
                    modified = true;
                }
            }
            if (itemText != null) {
                String tStr = itemText.toString();
                String cStr = cleanShareUrl(tStr);
                if (!tStr.equals(cStr)) {
                    newText = cStr;
                    modified = true;
                }
            }

            android.content.ClipData.Item newItem = new android.content.ClipData.Item(
                newText, item.getHtmlText(), item.getIntent(), newUri
            );
            if (newClip == null) {
                newClip = new android.content.ClipData(clipData.getDescription(), newItem);
            } else {
                newClip.addItem(newItem);
            }
        }
        if (modified && newClip != null) {
            intent.setClipData(newClip);
        }
    }

    /**
     * Sanitizes shared or copied text / URLs by stripping marketing and tracking query parameters
     * (utm_*, fbclid, gclid, igshid, si, msclkid, etc.).
     */
    public static String cleanShareUrl(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!text.contains("?") && !text.contains("&")) {
            return text;
        }
        try {
            // Fast-path: strictly whitespace-free single URL
            if (isPureSingleUrl(text)) {
                return cleanSingleUrl(text);
            }
            // Multi-token or combined text+URL payload: match and clean URLs while preserving text and punctuation
            Matcher matcher = URL_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String rawUrl = matcher.group();
                String trailing = "";
                while (rawUrl.length() > 0 && isTrailingPunctuation(rawUrl.charAt(rawUrl.length() - 1))) {
                    trailing = rawUrl.substring(rawUrl.length() - 1) + trailing;
                    rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
                }
                String cleaned = cleanSingleUrl(rawUrl) + trailing;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(cleaned));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            return text;
        }
    }

    private static boolean isPureSingleUrl(String text) {
        return (text.startsWith("http://") || text.startsWith("https://"))
            && !text.contains(" ") && !text.contains("\t") && !text.contains("\n") && !text.contains("\r");
    }

    private static boolean isTrailingPunctuation(char c) {
        return c == '.' || c == ',' || c == ')' || c == ']' || c == ';' || c == '!' || c == '"' || c == '\'';
    }

    private static boolean isTrackingParam(String host, String param) {
        if (param == null) return false;
        String lowerParam = param.toLowerCase(Locale.ROOT);
        if (lowerParam.startsWith("utm_")
            || lowerParam.startsWith("ga_")
            || lowerParam.startsWith("pk_")
            || lowerParam.startsWith("matomo_")
            || GLOBAL_TRACKING_PARAMS.contains(lowerParam)) {
            return true;
        }
        if (host != null) {
            String lowerHost = host.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, Set<String>> entry : DOMAIN_TRACKING_PARAMS.entrySet()) {
                String domain = entry.getKey();
                if (matchesDomain(lowerHost, domain) && entry.getValue().contains(lowerParam)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesDomain(String host, String targetDomain) {
        return host.equals(targetDomain) || host.endsWith("." + targetDomain);
    }

    private static String cleanSingleUrl(String originalUrl) {
        if (originalUrl == null || (!originalUrl.contains("?") && !originalUrl.contains("&"))) {
            return originalUrl;
        }
        try {
            Uri uri = Uri.parse(originalUrl);
            if (uri.getQuery() == null || uri.getQueryParameterNames().isEmpty()) {
                return originalUrl;
            }
            String host = uri.getHost();
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : uri.getQueryParameterNames()) {
                if (isTrackingParam(host, param)) {
                    continue;
                }
                for (String val : uri.getQueryParameters(param)) {
                    builder.appendQueryParameter(param, val);
                }
            }
            String cleaned = builder.build().toString();
            return cleaned.endsWith("?") ? cleaned.substring(0, cleaned.length() - 1) : cleaned;
        } catch (Throwable t) {
            return originalUrl;
        }
    }
}
