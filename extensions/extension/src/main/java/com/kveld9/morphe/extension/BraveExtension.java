package com.kveld9.morphe.extension;

import android.content.Intent;
import android.net.Uri;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension helper class for Morphe Brave patches.
 */
@SuppressWarnings("unused")
public class BraveExtension {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");

    private static final Set<String> TRACKING_PARAM_EXACT = new HashSet<>(Arrays.asList(
        // Facebook / Meta
        "fbclid",
        // Google Ads / Analytics / DoubleClick
        "gclid",
        "gbraid",
        "wbraid",
        "dclid",
        // Instagram
        "igshid",
        // YouTube / Spotify
        "si",
        // Microsoft / Bing
        "msclkid",
        // Twitter / X
        "twclid",
        "ref_src",
        "ref_url",
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
        // LinkedIn / TikTok
        "trk",
        "tt_medium",
        "tt_content",
        // Marketo / Wicked Reports
        "mkt_tok",
        "wickedid",
        // Snapchat & Advertising
        "sc_channel",
        "sc_campaign",
        "sc_geo",
        "zanpid",
        "vero_id",
        "vero_conv",
        "cmpid",
        "spref"
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

    private static boolean isTrackingParam(String param) {
        if (param == null) return false;
        String lower = param.toLowerCase(Locale.ROOT);
        return lower.startsWith("utm_")
            || lower.startsWith("ga_")
            || lower.startsWith("pk_")
            || lower.startsWith("matomo_")
            || TRACKING_PARAM_EXACT.contains(lower);
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
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String param : uri.getQueryParameterNames()) {
                if (isTrackingParam(param)) {
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

    public static boolean filterTelemetryPref(String pref, boolean originalValue) {
        if (pref == null) return originalValue;
        switch (pref) {
            case "brave.p3a.enabled":
            case "brave.stats.reporting_enabled":
            case "brave.web_discovery_enabled":
                return false;
            default:
                return originalValue;
        }
    }

    public static boolean filterNtpPref(String pref, boolean originalValue) {
        if (pref == null) return originalValue;
        switch (pref) {
            case "brave.new_tab_page.show_sponsored_images":
            case "brave.today.enabled":
            case "brave.today.opted_in":
            case "brave.new_tab_page.show_brave_news":
                return false;
            default:
                return originalValue;
        }
    }
}

