package com.kveld9.morphe.extension;

import android.content.Context;
import android.content.res.Resources;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Extension helper class for Morphe Brave patches.
 */
@SuppressWarnings("unused")
public class BraveExtension {

    public static boolean filterNtpPref(String pref, boolean originalValue) {
        if (pref == null) return originalValue;
        switch (pref) {
            case "brave.new_tab_page.show_background_image":
                return getCustomNtpWallpaperDrawableId() != 0 ? originalValue : false;
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

    private static final class ModelMethods {
        final Class<?> modelClass;
        final Method isSessionRestoreInProgress;
        final Method isClosingAllTabs;
        final Method indexMethod;
        final Method getTabAtMethod;
        final Method setIndexMethod;
        final Method getGroupIndicesMethod;

        ModelMethods(
            Class<?> modelClass,
            Method isSessionRestoreInProgress,
            Method isClosingAllTabs,
            Method indexMethod,
            Method getTabAtMethod,
            Method setIndexMethod,
            Method getGroupIndicesMethod
        ) {
            this.modelClass = modelClass;
            this.isSessionRestoreInProgress = isSessionRestoreInProgress;
            this.isClosingAllTabs = isClosingAllTabs;
            this.indexMethod = indexMethod;
            this.getTabAtMethod = getTabAtMethod;
            this.setIndexMethod = setIndexMethod;
            this.getGroupIndicesMethod = getGroupIndicesMethod;
        }
    }

    private static final class TabMethods {
        final Class<?> tabClass;
        final Method getIdMethod;
        final Method getGroupIdMethod;

        TabMethods(Class<?> tabClass, Method getIdMethod, Method getGroupIdMethod) {
            this.tabClass = tabClass;
            this.getIdMethod = getIdMethod;
            this.getGroupIdMethod = getGroupIdMethod;
        }
    }

    private static volatile ModelMethods sModelMethods;
    private static volatile TabMethods sTabMethods;

    private static ModelMethods getModelMethods(Class<?> modelClass) {
        ModelMethods cached = sModelMethods;
        if (cached != null && cached.modelClass == modelClass) {
            return cached;
        }

        Method isSessionRestore = null;
        Method isClosingAll = null;
        Method index = null;
        Method getTabAt = null;
        Method setIndex = null;
        Method getGroupIndices = null;

        for (Method m : modelClass.getMethods()) {
            String name = m.getName();
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 0) {
                if ("isSessionRestoreInProgress".equals(name)) {
                    isSessionRestore = m;
                } else if ("isClosingAllTabs".equals(name)) {
                    isClosingAll = m;
                } else if ("index".equals(name) && m.getReturnType() == int.class) {
                    index = m;
                }
            } else if (params.length == 1) {
                if (params[0] == int.class) {
                    if ("getTabAt".equals(name)) {
                        getTabAt = m;
                    } else if ("setIndex".equals(name)) {
                        setIndex = m;
                    }
                } else if ("getTabGroupTabIndices".equals(name) && m.getReturnType() == int[].class) {
                    getGroupIndices = m;
                }
            }
        }

        if (index == null || getTabAt == null || setIndex == null || getGroupIndices == null) {
            return null;
        }

        ModelMethods resolved = new ModelMethods(
            modelClass,
            isSessionRestore,
            isClosingAll,
            index,
            getTabAt,
            setIndex,
            getGroupIndices
        );
        sModelMethods = resolved;
        return resolved;
    }

    private static TabMethods getTabMethods(Class<?> tabClass) {
        TabMethods cached = sTabMethods;
        if (cached != null && cached.tabClass == tabClass) {
            return cached;
        }

        Method getId = null;
        Method getGroupId = null;

        for (Method m : tabClass.getMethods()) {
            if (m.getParameterTypes().length == 0) {
                String name = m.getName();
                if ("getId".equals(name) && m.getReturnType() == int.class) {
                    getId = m;
                } else if ("getTabGroupId".equals(name)) {
                    getGroupId = m;
                }
            }
        }

        if (getId == null || getGroupId == null) {
            return null;
        }

        TabMethods resolved = new TabMethods(tabClass, getId, getGroupId);
        sTabMethods = resolved;
        return resolved;
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
            ModelMethods model = getModelMethods(tabModelObj.getClass());
            if (model == null) {
                return;
            }

            // 1. Session restore and close-all guards
            if (model.isSessionRestoreInProgress != null && Boolean.TRUE.equals(model.isSessionRestoreInProgress.invoke(tabModelObj))) {
                return;
            }
            if (model.isClosingAllTabs != null && Boolean.TRUE.equals(model.isClosingAllTabs.invoke(tabModelObj))) {
                return;
            }

            int currentIndex = ((Number) model.indexMethod.invoke(tabModelObj)).intValue();
            if (currentIndex < 0) {
                return;
            }

            // 3. Verify closing tab is the currently active/selected tab
            Object currentTab = model.getTabAtMethod.invoke(tabModelObj, currentIndex);
            Object closingTab = tabsToClose.get(0);
            if (currentTab == null || closingTab == null) {
                return;
            }

            TabMethods tab = getTabMethods(currentTab.getClass());
            if (tab == null) {
                return;
            }

            int currentTabId = ((Number) tab.getIdMethod.invoke(currentTab)).intValue();
            int closingTabId = ((Number) tab.getIdMethod.invoke(closingTab)).intValue();
            if (currentTabId != closingTabId) {
                return;
            }

            // 4. Verify tab group membership
            Object groupId = tab.getGroupIdMethod.invoke(currentTab);
            if (groupId == null) {
                return;
            }

            // 5. Query group range [startIndex, endIndexPlusOne]
            int[] groupIndices = (int[]) model.getGroupIndicesMethod.invoke(tabModelObj, groupId);
            if (groupIndices == null || groupIndices.length < 2) {
                return;
            }

            int firstIndex = groupIndices[0];
            // If active tab is the first tab in group, it has no predecessor in this group -> fall back to stock
            if (currentIndex <= firstIndex) {
                return;
            }

            int predecessorIndex = currentIndex - 1;
            Object predecessorTab = model.getTabAtMethod.invoke(tabModelObj, predecessorIndex);
            if (predecessorTab == null) {
                return;
            }

            // Verify predecessor tab belongs to the same group
            Object predecessorGroupId = tab.getGroupIdMethod.invoke(predecessorTab);
            if (!groupId.equals(predecessorGroupId)) {
                return;
            }

            // 6. Pre-select predecessor tab
            model.setIndexMethod.invoke(tabModelObj, predecessorIndex);
        } catch (Throwable ignored) {
        }
    }

    private static final String NTP_WALLPAPER_DRAWABLE = "morphe_custom_ntp_wallpaper";
    private static volatile int sCustomDrawableId = -1;

    /**
     * Resolves the Morphe-installed NTP wallpaper drawable resource ID at runtime.
     * Lives in the extension so bytecode sites only need a 2-register invoke-static.
     *
     * @return the drawable resource ID, or 0 if not found
     */
    public static int getCustomNtpWallpaperDrawableId() {
        if (sCustomDrawableId != -1) {
            return sCustomDrawableId;
        }
        try {
            Context app = currentApplication();
            if (app == null) return 0;
            Resources res = app.getResources();
            if (res == null) return 0;
            int id = res.getIdentifier(NTP_WALLPAPER_DRAWABLE, "drawable", app.getPackageName());
            sCustomDrawableId = id;
            return id;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Hidden API; resolve reflectively so this class compiles against public SDK. */
    private static Context currentApplication() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object app = at.getDeclaredMethod("currentApplication").invoke(null);
            return (Context) app;
        } catch (Throwable t) {
            return null;
        }
    }
}
