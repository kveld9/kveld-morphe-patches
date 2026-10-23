package com.kveld9.morphe.extension;

import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings({"deprecation", "unused"})
public class SpoofSignaturePatch {

    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private static final Set<InstallSourceInfo> sOwnInstallSourceInfos =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<InstallSourceInfo, Boolean>()));

    private static String ownPackage() {
        return ""; // Baked during patching.
    }

    // --- Patch 1: Report Play Store as installer ---

    public static String getInstallerPackageName(PackageManager pm, String packageName) {
        if (packageName != null && packageName.equals(ownPackage())) {
            return PLAY_STORE_PACKAGE;
        }
        return pm.getInstallerPackageName(packageName);
    }

    @android.annotation.TargetApi(30)
    public static InstallSourceInfo getInstallSourceInfo(PackageManager pm, String packageName)
            throws PackageManager.NameNotFoundException {
        InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
        if (packageName != null && packageName.equals(ownPackage()) && info != null) {
            sOwnInstallSourceInfos.add(info);
        }
        return info;
    }

    @android.annotation.TargetApi(30)
    public static String getInstallingPackageName(InstallSourceInfo info) {
        if (info != null && sOwnInstallSourceInfos.contains(info)) {
            return PLAY_STORE_PACKAGE;
        }
        return info.getInstallingPackageName();
    }

    @android.annotation.TargetApi(30)
    public static String getInitiatingPackageName(InstallSourceInfo info) {
        if (info != null && sOwnInstallSourceInfos.contains(info)) {
            return PLAY_STORE_PACKAGE;
        }
        return info.getInitiatingPackageName();
    }

}
