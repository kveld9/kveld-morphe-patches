package com.kveld9.morphe.extension;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Extension helper class for Morphe Brave patches.
 */
@SuppressWarnings("unused")
public class BraveExtension {

    private static final String[] CANDIDATE_EXTENSIONS = new String[]{"jpg", "jpeg", "png", "webp"};
    private static volatile String sDeployedWallpaperPath = null;

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

    public static boolean filterNtpPrefWithWallpaper(String pref, boolean originalValue) {
        if (pref == null) return originalValue;
        switch (pref) {
            case "brave.new_tab_page.show_sponsored_images":
            case "brave.brave_ads.sponsored.enabled":
            case "brave.new_tab_page.show_stats":
            case "brave.new_tab_page.show_sponsored_sites":
            case "brave.today.enabled":
            case "brave.today.opted_in":
            case "brave.new_tab_page.show_brave_news":
                return false;
            case "brave.new_tab_page.show_background_image":
            case "brave.new_tab_page.show_branded_background_image":
            default:
                return originalValue;
        }
    }

    private static Context getApplicationContext() {
        try {
            Class<?> contextUtils = Class.forName("org.chromium.base.ContextUtils");
            Method getAppContext = contextUtils.getMethod("getApplicationContext");
            return (Context) getAppContext.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String ntpWallpaperPath() {
        String cached = sDeployedWallpaperPath;
        if (cached != null) {
            File f = new File(cached);
            if (f.isFile() && f.length() > 0) {
                return cached;
            }
        }
        try {
            Context context = getApplicationContext();
            if (context == null) {
                return null;
            }

            String foundAsset = null;
            String matchedExt = null;
            for (String ext : CANDIDATE_EXTENSIONS) {
                String candidateName = "morphe_ntp_wallpaper." + ext;
                try (InputStream is = context.getAssets().open(candidateName)) {
                    foundAsset = candidateName;
                    matchedExt = ext;
                    break;
                } catch (Throwable ignored) {
                }
            }

            if (foundAsset == null) {
                return null;
            }

            File filesDir = context.getFilesDir();
            if (filesDir == null) {
                return null;
            }
            File destFile = new File(filesDir, "morphe_ntp_wallpaper." + matchedExt);

            try (InputStream is = context.getAssets().open(foundAsset)) {
                int assetSize = is.available();
                if (destFile.exists() && assetSize > 0 && destFile.length() == assetSize) {
                    sDeployedWallpaperPath = destFile.getAbsolutePath();
                    return sDeployedWallpaperPath;
                }

                File tempFile = new File(filesDir, "morphe_ntp_wallpaper." + matchedExt + ".tmp");
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }

                if (!tempFile.renameTo(destFile)) {
                    if (destFile.delete()) {
                        tempFile.renameTo(destFile);
                    }
                }
            }

            if (destFile.isFile() && destFile.length() > 0) {
                sDeployedWallpaperPath = destFile.getAbsolutePath();
                return sDeployedWallpaperPath;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String overrideWallpaperPath(String stockPath) {
        String deployed = ntpWallpaperPath();
        return deployed != null ? deployed : stockPath;
    }

    public static int overrideWallpaperType(int stockType) {
        String deployed = ntpWallpaperPath();
        return deployed != null ? 1 : stockType;
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
