package com.kveld9.morphe.extension;

/**
 * Extension helper class for Morphe Brave patches.
 * Inherits Chromium-wide link sanitization logic from {@link ChromiumExtension}.
 */
@SuppressWarnings("unused")
public class BraveExtension extends ChromiumExtension {

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
