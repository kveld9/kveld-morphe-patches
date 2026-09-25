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
 * Extension helper class for Chromium-based browsers (Brave).
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

    private static final Set<String> MERCADOLIBRE_DOMAINS = new HashSet<>(Arrays.asList(
        "mercadolibre.com",
        "mercadolibre.com.ar",
        "mercadolivre.com.br",
        "mercadolibre.com.mx",
        "mercadolibre.com.co",
        "mercadolibre.cl",
        "mercadolibre.com.uy",
        "mercadolibre.com.pe",
        "mercadolibre.com.ve",
        "mercadolibre.com.ec",
        "mercadolibre.co.cr",
        "mercadolibre.com.do",
        "mercadolibre.com.pa",
        "mercadolibre.com.gt",
        "mercadolibre.com.bo",
        "mercadolibre.com.py",
        "mercadolibre.com.hn",
        "mercadolibre.com.sv",
        "mercadolibre.com.ni",
        "meli.la",
        "mercadopago.com",
        "mercadopago.com.ar",
        "mercadopago.com.br",
        "mercadopago.com.mx",
        "mercadopago.cl",
        "mercadopago.com.co",
        "mercadopago.com.uy",
        "mercadopago.com.pe",
        "mpago.la"
    ));

    private static final Set<String> MERCADOLIBRE_TRACKING_PARAMS = new HashSet<>(Arrays.asList(
        "polycard_client",
        "be_origin",
        "overlay_label",
        "search_layout",
        "position",
        "type",
        "tracking_id",
        "wid",
        "sid",
        "pdp_filters",
        "deal_id",
        "deal_print_id",
        "promotion_id",
        "reco_id",
        "reco_backend",
        "reco_backend_type",
        "reco_client",
        "reco_model",
        "matt_tool",
        "matt_word",
        "matt_source",
        "matt_campaign_id",
        "searchvariation"
    ));

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

    /**
     * Sanitizes an Android ClipData payload by stripping marketing and tracking query parameters
     * from all text, HTML, and URI items before writing to system clipboard.
     */
    public static android.content.ClipData cleanClipData(android.content.ClipData clipData) {
        if (clipData == null || clipData.getItemCount() == 0) {
            return clipData;
        }
        try {
            boolean modified = false;
            android.content.ClipData newClip = null;
            for (int i = 0; i < clipData.getItemCount(); i++) {
                android.content.ClipData.Item item = clipData.getItemAt(i);
                Uri itemUri = item.getUri();
                CharSequence itemText = item.getText();
                String itemHtml = item.getHtmlText();

                Uri newUri = itemUri;
                CharSequence newText = itemText;
                String newHtml = itemHtml;

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
                if (itemHtml != null) {
                    String cStr = cleanShareUrl(itemHtml);
                    if (!itemHtml.equals(cStr)) {
                        newHtml = cStr;
                        modified = true;
                    }
                }

                android.content.ClipData.Item newItem = new android.content.ClipData.Item(
                    newText, newHtml, item.getIntent(), newUri
                );
                if (newClip == null) {
                    newClip = new android.content.ClipData(clipData.getDescription(), newItem);
                } else {
                    newClip.addItem(newItem);
                }
            }
            if (modified && newClip != null) {
                android.util.Log.i("ChromiumExtension", "[Clean Share URL] Sanitized ClipData with " + newClip.getItemCount() + " item(s)");
                return newClip;
            }
            return clipData;
        } catch (Throwable t) {
            return clipData;
        }
    }

    private static void sanitizeClipData(Intent intent) {
        android.content.ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return;
        }
        android.content.ClipData cleaned = cleanClipData(clipData);
        if (cleaned != clipData) {
            intent.setClipData(cleaned);
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
        if (!text.contains("?") && !text.contains("&") && !text.contains("#")) {
            return text;
        }
        try {
            // Fast-path: strictly whitespace-free single URL
            if (isPureSingleUrl(text)) {
                String cleaned = cleanSingleUrl(text);
                if (!text.equals(cleaned)) {
                    android.util.Log.i("ChromiumExtension", "[Clean Share URL] Sanitized URL: " + text + " -> " + cleaned);
                }
                return cleaned;
            }
            // Multi-token or combined text+URL payload: match and clean URLs while preserving text and punctuation
            Matcher matcher = URL_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer();
            boolean anyModified = false;
            while (matcher.find()) {
                String rawUrl = matcher.group();
                String trailing = "";
                while (rawUrl.length() > 0 && isTrailingPunctuation(rawUrl.charAt(rawUrl.length() - 1))) {
                    trailing = rawUrl.substring(rawUrl.length() - 1) + trailing;
                    rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
                }
                String cleaned = cleanSingleUrl(rawUrl);
                if (!rawUrl.equals(cleaned)) {
                    anyModified = true;
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(cleaned + trailing));
            }
            matcher.appendTail(sb);
            String result = sb.toString();
            if (anyModified) {
                android.util.Log.i("ChromiumExtension", "[Clean Share URL] Sanitized composite text: " + text + " -> " + result);
            }
            return result;
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

    private static boolean isGlobalTrackingParam(String lowerParam) {
        return lowerParam.startsWith("utm_")
            || lowerParam.startsWith("ga_")
            || lowerParam.startsWith("pk_")
            || lowerParam.startsWith("matomo_")
            || GLOBAL_TRACKING_PARAMS.contains(lowerParam);
    }

    private static boolean isMercadoLibreHost(String host) {
        if (host == null) return false;
        String lower = host.toLowerCase(Locale.ROOT);
        for (String domain : MERCADOLIBRE_DOMAINS) {
            if (matchesDomain(lower, domain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMercadoLibreTrackingParam(String lowerParam) {
        return MERCADOLIBRE_TRACKING_PARAMS.contains(lowerParam) || lowerParam.startsWith("c_");
    }

    private static boolean isDomainScopedTrackingParam(String lowerHost, String lowerParam) {
        for (Map.Entry<String, Set<String>> entry : DOMAIN_TRACKING_PARAMS.entrySet()) {
            if (matchesDomain(lowerHost, entry.getKey()) && entry.getValue().contains(lowerParam)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrackingParam(String host, String param) {
        if (param == null) return false;
        String lowerParam = param.toLowerCase(Locale.ROOT);
        if (isGlobalTrackingParam(lowerParam)) {
            return true;
        }
        if (host == null) {
            return false;
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (isMercadoLibreHost(lowerHost) && isMercadoLibreTrackingParam(lowerParam)) {
            return true;
        }
        return isDomainScopedTrackingParam(lowerHost, lowerParam);
    }

    private static boolean matchesDomain(String host, String targetDomain) {
        return host.equals(targetDomain) || host.endsWith("." + targetDomain);
    }

    private static String cleanParamString(String host, String paramString) {
        if (paramString == null || paramString.isEmpty()) {
            return "";
        }
        String[] pairs = paramString.split("&");
        StringBuilder sb = new StringBuilder();
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int eqIdx = pair.indexOf('=');
            String key = eqIdx != -1 ? pair.substring(0, eqIdx) : pair;
            String decodedKey = Uri.decode(key);
            if (isTrackingParam(host, decodedKey)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(pair);
        }
        return sb.toString();
    }

    private static String cleanFragment(String host, String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        if (!fragment.contains("=") && !fragment.contains("&")) {
            return fragment;
        }
        int qIdx = fragment.indexOf('?');
        if (qIdx != -1) {
            String route = fragment.substring(0, qIdx);
            String cleanedQuery = cleanParamString(host, fragment.substring(qIdx + 1));
            return cleanedQuery.isEmpty() ? route : route + "?" + cleanedQuery;
        }
        String cleaned = cleanParamString(host, fragment);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String cleanTrailingArtifacts(String url) {
        if (url == null) return null;
        if (url.endsWith("?")) {
            return url.substring(0, url.length() - 1);
        }
        if (url.endsWith("#")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String cleanSingleUrl(String originalUrl) {
        if (originalUrl == null || (!originalUrl.contains("?") && !originalUrl.contains("&") && !originalUrl.contains("#"))) {
            return originalUrl;
        }
        try {
            Uri uri = Uri.parse(originalUrl);
            String host = uri.getHost();
            boolean hasQuery = uri.getQuery() != null && !uri.getQueryParameterNames().isEmpty();
            String originalFragment = uri.getFragment();
            boolean hasFragment = originalFragment != null && !originalFragment.isEmpty();

            if (!hasQuery && !hasFragment) {
                return originalUrl;
            }

            Uri.Builder builder = uri.buildUpon();
            if (hasQuery) {
                builder.clearQuery();
                for (String param : uri.getQueryParameterNames()) {
                    if (isTrackingParam(host, param)) {
                        continue;
                    }
                    for (String val : uri.getQueryParameters(param)) {
                        builder.appendQueryParameter(param, val);
                    }
                }
            }

            if (hasFragment) {
                String cleanedFragment = cleanFragment(host, originalFragment);
                builder.fragment(cleanedFragment);
            }

            return cleanTrailingArtifacts(builder.build().toString());
        } catch (Throwable t) {
            return originalUrl;
        }
    }
}
