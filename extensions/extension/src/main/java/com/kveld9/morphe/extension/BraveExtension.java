package com.kveld9.morphe.extension;

import java.lang.reflect.Method;
import java.util.List;

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
            case "brave.new_tab_page.show_background_image":
            case "brave.new_tab_page.show_branded_background_image":
            case "brave.new_tab_page.show_sponsored_images":
            case "brave.brave_ads.sponsored.enabled":
            case "brave.new_tab_page.show_stats":
            case "brave.new_tab_page.show_sponsored_sites":
            case "brave.today.enabled":
            case "brave.today.opted_in":
            case "brave.new_tab_page.show_brave_news":
                return false;
            default:
                return originalValue;
        }
    }

    /**
     * Resolves issue #76: when closing the currently selected tab in a tab group,
     * pre-selects the predecessor tab in the same group before stock selection runs.
     * Falls back to stock behavior for single-tab groups, non-selected tabs, bulk close,
     * or when session restore / close-all is in progress.
     *
     * @param tabModelObj the TabCollectionTabModelImpl instance
     * @param tabsToClose list of tabs being closed
     */
    public static void preselectPreviousTabInGroup(Object tabModelObj, List<?> tabsToClose) {
        if (tabModelObj == null || tabsToClose == null || tabsToClose.size() != 1) {
            return;
        }
        try {
            Class<?> modelClass = tabModelObj.getClass();

            // 1. Session restore and close-all guards
            for (Method m : modelClass.getMethods()) {
                if (m.getParameterTypes().length == 0) {
                    if ("isSessionRestoreInProgress".equals(m.getName()) && Boolean.TRUE.equals(m.invoke(tabModelObj))) {
                        return;
                    }
                    if ("isClosingAllTabs".equals(m.getName()) && Boolean.TRUE.equals(m.invoke(tabModelObj))) {
                        return;
                    }
                }
            }

            // 2. Query model methods: index(), getTabAt(int), setIndex(int), getTabGroupTabIndices(Token)
            Method indexMethod = null;
            Method getTabAtMethod = null;
            Method setIndexMethod = null;
            Method getGroupIndicesMethod = null;

            for (Method m : modelClass.getMethods()) {
                String name = m.getName();
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 0 && "index".equals(name) && m.getReturnType() == int.class) {
                    indexMethod = m;
                } else if (params.length == 1 && params[0] == int.class && "getTabAt".equals(name)) {
                    getTabAtMethod = m;
                } else if (params.length == 1 && params[0] == int.class && "setIndex".equals(name)) {
                    setIndexMethod = m;
                } else if (params.length == 1 && "getTabGroupTabIndices".equals(name) && m.getReturnType() == int[].class) {
                    getGroupIndicesMethod = m;
                }
            }

            if (indexMethod == null || getTabAtMethod == null || setIndexMethod == null || getGroupIndicesMethod == null) {
                return;
            }

            int currentIndex = ((Number) indexMethod.invoke(tabModelObj)).intValue();
            if (currentIndex < 0) {
                return;
            }

            // 3. Verify closing tab is the currently active/selected tab
            Object currentTab = getTabAtMethod.invoke(tabModelObj, currentIndex);
            Object closingTab = tabsToClose.get(0);
            if (currentTab == null || closingTab == null) {
                return;
            }

            Method getIdMethod = null;
            Method getGroupIdMethod = null;
            for (Method m : currentTab.getClass().getMethods()) {
                String name = m.getName();
                if (m.getParameterTypes().length == 0) {
                    if ("getId".equals(name) && m.getReturnType() == int.class) {
                        getIdMethod = m;
                    } else if ("getTabGroupId".equals(name)) {
                        getGroupIdMethod = m;
                    }
                }
            }

            if (getIdMethod == null || getGroupIdMethod == null) {
                return;
            }

            int currentTabId = ((Number) getIdMethod.invoke(currentTab)).intValue();
            int closingTabId = ((Number) getIdMethod.invoke(closingTab)).intValue();
            if (currentTabId != closingTabId) {
                return;
            }

            // 4. Verify tab group membership
            Object groupId = getGroupIdMethod.invoke(currentTab);
            if (groupId == null) {
                return;
            }

            // 5. Query group range [startIndex, endIndexPlusOne]
            int[] groupIndices = (int[]) getGroupIndicesMethod.invoke(tabModelObj, groupId);
            if (groupIndices == null || groupIndices.length < 2) {
                return;
            }

            int firstIndex = groupIndices[0];
            // If active tab is the first tab in group, it has no predecessor in this group -> fall back to stock
            if (currentIndex <= firstIndex) {
                return;
            }

            int predecessorIndex = currentIndex - 1;
            Object predecessorTab = getTabAtMethod.invoke(tabModelObj, predecessorIndex);
            if (predecessorTab == null) {
                return;
            }

            // Verify predecessor tab belongs to the same group
            Object predecessorGroupId = getGroupIdMethod.invoke(predecessorTab);
            if (!groupId.equals(predecessorGroupId)) {
                return;
            }

            // 6. Pre-select predecessor tab
            setIndexMethod.invoke(tabModelObj, predecessorIndex);
        } catch (Throwable ignored) {
        }
    }
}
