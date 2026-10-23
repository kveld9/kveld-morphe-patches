package com.kveld9.morphe.extension;

import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings({"deprecation", "unused"})
public class SpoofSignaturePatch {

    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private static final Set<InstallSourceInfo> sOwnInstallSourceInfos =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<InstallSourceInfo, Boolean>()));

    private static final Set<SigningInfo> sOwnSigningInfos =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<SigningInfo, Boolean>()));

    private static Signature[] sOriginalSignatures;

    private static String ownPackage() {
        return ""; // Baked during patching.
    }

    private static String certificates() {
        return ""; // Baked during patching.
    }

    private static synchronized Signature[] getOriginalSignatures() {
        if (sOriginalSignatures == null) {
            String certHex = certificates();
            if (certHex == null || certHex.isEmpty()) {
                sOriginalSignatures = new Signature[0];
            } else {
                String[] hexArray = certHex.split(",");
                Signature[] sigs = new Signature[hexArray.length];
                for (int i = 0; i < hexArray.length; i++) {
                    sigs[i] = new Signature(hexArray[i].trim());
                }
                sOriginalSignatures = sigs;
            }
        }
        return sOriginalSignatures;
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

    // --- Patch 2: Spoof signature ---

    public static PackageInfo getPackageInfo(PackageManager pm, String packageName, int flags)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = pm.getPackageInfo(packageName, flags);
        return processPackageInfo(info);
    }

    @android.annotation.TargetApi(33)
    public static PackageInfo getPackageInfo(
            PackageManager pm,
            String packageName,
            PackageManager.PackageInfoFlags flags
    ) throws PackageManager.NameNotFoundException {
        PackageInfo info = pm.getPackageInfo(packageName, flags);
        return processPackageInfo(info);
    }

    private static boolean isOwnPackage(PackageInfo info) {
        if (info == null || info.packageName == null) {
            return false;
        }
        return info.packageName.equals(ownPackage());
    }

    private static void replaceSignaturesIfPresent(PackageInfo info) {
        if (info.signatures != null) {
            info.signatures = getOriginalSignatures().clone();
        }
    }

    private static void rememberSigningInfoIfSupported(PackageInfo info) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            rememberSigningInfoApi28(info);
        }
    }

    private static PackageInfo processPackageInfo(PackageInfo info) {
        if (!isOwnPackage(info)) {
            return info;
        }
        replaceSignaturesIfPresent(info);
        rememberSigningInfoIfSupported(info);
        return info;
    }

    @android.annotation.TargetApi(28)
    private static void rememberSigningInfoApi28(PackageInfo info) {
        if (info.signingInfo != null) {
            sOwnSigningInfos.add(info.signingInfo);
        }
    }

    @android.annotation.TargetApi(28)
    public static boolean hasSigningCertificate(
            PackageManager pm,
            String packageName,
            byte[] certificate,
            int type
    ) {
        if (packageName != null && packageName.equals(ownPackage())) {
            return checkHasSigningCertificate(certificate, type);
        }
        return pm.hasSigningCertificate(packageName, certificate, type);
    }

    private static boolean checkHasSigningCertificate(byte[] certificate, int type) {
        if (certificate == null) {
            return false;
        }
        Signature[] sigs = getOriginalSignatures();
        for (Signature sig : sigs) {
            if (matchesCertificate(sig, certificate, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCertificate(Signature sig, byte[] certificate, int type) {
        byte[] raw = sig.toByteArray();
        if (type == 0 /* PackageManager.CERT_INPUT_RAW_X509 */) {
            return Arrays.equals(raw, certificate);
        }
        if (type == 1 /* PackageManager.CERT_INPUT_SHA256 */) {
            byte[] sha256 = sha256Digest(raw);
            return sha256 != null && Arrays.equals(sha256, certificate);
        }
        return false;
    }

    private static byte[] sha256Digest(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @android.annotation.TargetApi(28)
    public static Signature[] getApkContentsSigners(SigningInfo signingInfo) {
        if (signingInfo != null && sOwnSigningInfos.contains(signingInfo)) {
            return getOriginalSignatures().clone();
        }
        return signingInfo.getApkContentsSigners();
    }

    @android.annotation.TargetApi(28)
    public static Signature[] getSigningCertificateHistory(SigningInfo signingInfo) {
        if (signingInfo != null && sOwnSigningInfos.contains(signingInfo)) {
            return getOriginalSignatures().clone();
        }
        return signingInfo.getSigningCertificateHistory();
    }

    @android.annotation.TargetApi(28)
    public static boolean hasMultipleSigners(SigningInfo signingInfo) {
        if (signingInfo != null && sOwnSigningInfos.contains(signingInfo)) {
            return getOriginalSignatures().length > 1;
        }
        return signingInfo.hasMultipleSigners();
    }
}
