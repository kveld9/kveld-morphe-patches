package com.kveld9.morphe.extension.tiktok;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Runtime state for the Remember Clear Display patch.
 * Records the user's clear-display choice from clear-mode events and re-applies
 * it when new videos render their first frame.
 */
@SuppressWarnings("unused")
public final class TikTokClearDisplayHook {

    private static final String TAG = "MorpheTikTok";

    // Event types that must never update the remembered state (page switches and exits).
    private static final int EVENT_SWITCH_PAGE = 3;
    private static final int EVENT_NOTIFY_EXIT = 9;

    private static volatile boolean clearDisplayRemembered = false;
    private static volatile boolean wantsClear = false;

    private static volatile Class<?> cachedEventType = null;
    private static volatile Field cachedCleanField = null;
    private static volatile Field cachedTypeField = null;
    private static volatile boolean loggedResolutionPath = false;

    private static final long REAPPLY_DELAY_MS = 250L;
    private static volatile boolean reapplyPending = false;

    private static volatile String dispatcherClassName = null;
    private static volatile ClassLoader dispatcherLoader = null;

    private static volatile Class<?> cachedDispatcherClass = null;
    private static volatile Field cachedStateField = null;
    private static volatile Method cachedLcMethod = null;
    private static volatile Object cachedOpenConstant = null;
    private static volatile Object cachedCloseConstant = null;

    private TikTokClearDisplayHook() {}

    public static boolean getClearDisplayState() {
        return clearDisplayRemembered;
    }

    public static boolean getWantsClear() {
        return wantsClear;
    }

    public static void setWantsClear(boolean wants) {
        wantsClear = wants;
        Log.i(TAG, "[Clear Display Lite] wantsClear=" + wants);
    }

    public static void onVideoSwitchR5() { scheduleReapply("r5"); }
    public static void onVideoSwitchBind() { scheduleReapply("bind"); }
    public static void onVideoSwitchFirstFrame() { scheduleReapply("first_frame"); }
    public static void onVideoSwitchBindAll() { scheduleReapply("bind_all"); }
    public static void onVideoSwitchPager() { scheduleReapply("pager"); }
    public static void onVideoSwitchPagerWrapper() { scheduleReapply("pager_wrapper"); }
    public static void onVideoSwitchSelectIndex() { scheduleReapply("select_index"); }
    public static void onVideoSwitchSdkFirstFrame() { scheduleReapply("sdk_first_frame"); }
    public static void onVideoSwitchSdkChainA() { scheduleReapply("sdk_chain_a"); }
    public static void onVideoSwitchSdkChainB() { scheduleReapply("sdk_chain_b"); }

    private static void scheduleReapply(final String tag) {
        if (!clearDisplayRemembered || reapplyPending) {
            return;
        }
        reapplyPending = true;
        try {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    reapplyPending = false;
                    if (!clearDisplayRemembered) {
                        return;
                    }
                    try {
                        applyClearMode(tag);
                    } catch (Throwable t) {
                        Log.w(TAG, "[Clear Display Lite] Re-apply failed (" + tag + "): " + t);
                    }
                }
            }, REAPPLY_DELAY_MS);
        } catch (Throwable t) {
            reapplyPending = false;
            Log.w(TAG, "[Clear Display Lite] Could not schedule re-apply (" + tag + "): " + t);
        }
    }

    private static synchronized void captureDispatcher(Object enumStatus) {
        if (dispatcherClassName != null) {
            return;
        }
        StackTraceElement[] frames = new Throwable().getStackTrace();
        for (StackTraceElement f : frames) {
            if (f.getClassName().equals(TikTokClearDisplayHook.class.getName())) {
                continue;
            }
            dispatcherClassName = f.getClassName();
            break;
        }
        if (enumStatus != null) {
            dispatcherLoader = enumStatus.getClass().getClassLoader();
        }
        if (dispatcherClassName != null) {
            Log.i(TAG, "[Clear Display Lite] Dispatcher resolved: " + dispatcherClassName);
        }
    }

    private static Class<?> resolveDispatcherClass() {
        if (cachedDispatcherClass != null) {
            return cachedDispatcherClass;
        }
        String name = dispatcherClassName;
        if (name == null) {
            return null;
        }
        Class<?> resolved = null;
        if (dispatcherLoader != null) {
            try {
                resolved = Class.forName(name, false, dispatcherLoader);
            } catch (Throwable ignored) {
            }
        }
        if (resolved == null) {
            try {
                ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                if (ccl != null) {
                    resolved = Class.forName(name, false, ccl);
                }
            } catch (Throwable ignored) {
            }
        }
        if (resolved == null) {
            try {
                ClassLoader cl = TikTokClearDisplayHook.class.getClassLoader();
                if (cl != null) {
                    resolved = Class.forName(name, false, cl);
                }
            } catch (Throwable ignored) {
            }
        }
        if (resolved == null) {
            try {
                resolved = Class.forName(name);
            } catch (Throwable ignored) {
            }
        }
        return resolved;
    }

    public static boolean onClearModeCall(Object enumStatus, String reason) {
        if (enumStatus == null) return true;
        String status = enumStatus.toString();
        if ("OPEN".equals(status)) {
            clearDisplayRemembered = true;
            wantsClear = true;
            Log.i(TAG, "[Clear Display Lite] onClearModeCall OPEN (reason=" + reason + ") -> clearDisplayRemembered=true");
            captureDispatcher(enumStatus);
            return true;
        } else if ("CLOSE".equals(status)) {
            if (("switch_video".equals(reason) || "gesture".equals(reason)) && clearDisplayRemembered) {
                Log.i(TAG, "[Clear Display Lite] Suppressing CLOSE for " + reason + " (clear display remembered)");
                captureDispatcher(enumStatus);
                return false;
            }
            if ("switch_video_reapply".equals(reason)) {
                return false;
            }
            clearDisplayRemembered = false;
            wantsClear = false;
            Log.i(TAG, "[Clear Display Lite] onClearModeCall CLOSE (reason=" + reason + ") -> clearDisplayRemembered=false");
            captureDispatcher(enumStatus);
            return true;
        }
        captureDispatcher(enumStatus);
        return true;
    }

    private static synchronized void resolveDispatcherFields(Class<?> dispatcher) {
        if (cachedDispatcherClass == dispatcher) {
            return;
        }

        Field stateField = null;
        for (Field f : dispatcher.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && (f.getType().isEnum() || Enum.class.isAssignableFrom(f.getType()))) {
                stateField = f;
                break;
            }
        }

        Method lcMethod = null;
        for (Method m : dispatcher.getDeclaredMethods()) {
            if ("LC".equals(m.getName()) && Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length == 4) {
                lcMethod = m;
                break;
            }
        }
        if (lcMethod == null) {
            Log.w(TAG, "[Clear Display Lite] Could not find static 4-argument method LC on " + dispatcher.getName());
            return;
        }
        lcMethod.setAccessible(true);

        Class<?> enumType = lcMethod.getParameterTypes()[0];
        if (stateField == null) {
            for (Field f : dispatcher.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == enumType) {
                    stateField = f;
                    break;
                }
            }
        }
        if (stateField == null) {
            Log.w(TAG, "[Clear Display Lite] Could not find static state field of type " + enumType.getName() + " on " + dispatcher.getName());
            return;
        }
        stateField.setAccessible(true);

        Object openConstant = null;
        Object closeConstant = null;
        try {
            Field openField = enumType.getDeclaredField("OPEN");
            openField.setAccessible(true);
            openConstant = openField.get(null);
        } catch (Throwable ignored) {
        }

        try {
            Field closeField = enumType.getDeclaredField("CLOSE");
            closeField.setAccessible(true);
            closeConstant = closeField.get(null);
        } catch (Throwable ignored) {
        }

        if (openConstant == null || closeConstant == null) {
            Object[] enumConstants = enumType.getEnumConstants();
            if (enumConstants != null) {
                for (Object c : enumConstants) {
                    if ("OPEN".equals(c.toString())) {
                        openConstant = c;
                    } else if ("CLOSE".equals(c.toString())) {
                        closeConstant = c;
                    }
                }
            }
        }

        if (openConstant == null || closeConstant == null) {
            Log.w(TAG, "[Clear Display Lite] Could not resolve OPEN/CLOSE constants on " + enumType.getName());
            return;
        }

        cachedStateField = stateField;
        cachedOpenConstant = openConstant;
        cachedCloseConstant = closeConstant;
        cachedLcMethod = lcMethod;
        cachedDispatcherClass = dispatcher;
    }

    private static void applyClearMode(String tag) {
        if (!clearDisplayRemembered) {
            return;
        }
        Class<?> dispatcher = resolveDispatcherClass();
        if (dispatcher == null) {
            Log.w(TAG, "[Clear Display Lite] Dispatcher class unresolvable (" + tag + ")");
            return;
        }
        try {
            if (cachedDispatcherClass != dispatcher) {
                resolveDispatcherFields(dispatcher);
            }
            Field stateField = cachedStateField;
            Method lcMethod = cachedLcMethod;
            Object openConstant = cachedOpenConstant;
            Object closeConstant = cachedCloseConstant;

            if (stateField == null || lcMethod == null || openConstant == null || closeConstant == null) {
                Log.w(TAG, "[Clear Display Lite] Dispatcher reflection targets unresolvable (" + tag + ")");
                return;
            }

            stateField.set(null, closeConstant);
            lcMethod.invoke(null, openConstant, "switch_video_reapply", false, null);

            Log.i(TAG, "[Clear Display Lite] Re-applied clear mode (" + tag + ")");
        } catch (Throwable t) {
            Log.w(TAG, "[Clear Display Lite] Re-apply failed (" + tag + "): " + t);
        }
    }

    private static synchronized void resolveFields(Class<?> type) {
        if (cachedEventType == type) {
            return;
        }
        Field boolField = null;
        Field intField = null;
        int boolCount = 0;
        int intCount = 0;

        for (Field f : type.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isStatic(mods) || f.isSynthetic()) {
                continue;
            }
            if (f.getType() == boolean.class) {
                boolField = f;
                boolCount++;
            } else if (f.getType() == int.class) {
                intField = f;
                intCount++;
            }
        }

        if (boolCount == 1 && intCount == 1) {
            boolField.setAccessible(true);
            intField.setAccessible(true);
            cachedCleanField = boolField;
            cachedTypeField = intField;
            cachedEventType = type;
            if (!loggedResolutionPath) {
                loggedResolutionPath = true;
                Log.i(TAG, "[Clear Display] Resolved event fields structurally: " + boolField.getName() + " (boolean), " + intField.getName() + " (int)");
            }
            return;
        }

        try {
            Field legacyBool = type.getDeclaredField("LIZ");
            Field legacyInt = type.getDeclaredField("LIZIZ");
            legacyBool.setAccessible(true);
            legacyInt.setAccessible(true);
            cachedCleanField = legacyBool;
            cachedTypeField = legacyInt;
            cachedEventType = type;
            if (!loggedResolutionPath) {
                loggedResolutionPath = true;
                Log.i(TAG, "[Clear Display] Falling back to legacy event field names (LIZ, LIZIZ)");
            }
        } catch (Throwable t) {
            Log.w(TAG, "[Clear Display] Could not resolve legacy event fields: " + t.getMessage());
        }
    }

    public static void rememberClearDisplayEvent(Object event) {
        if (event == null) return;
        try {
            Class<?> type = event.getClass();
            if (cachedEventType != type) {
                resolveFields(type);
            }
            Field cleanField = cachedCleanField;
            Field typeField = cachedTypeField;
            if (cleanField == null || typeField == null) {
                return;
            }
            boolean isClean = cleanField.getBoolean(event);
            int eventType = typeField.getInt(event);
            if (eventType == EVENT_SWITCH_PAGE || eventType == EVENT_NOTIFY_EXIT) {
                return;
            }
            clearDisplayRemembered = isClean;
            Log.i(TAG, "[Clear Display] Remembered state=" + isClean + " (eventType=" + eventType + ")");
        } catch (Throwable t) {
            Log.w(TAG, "[Clear Display] Could not read clear-display event: " + t.getMessage());
        }
    }
}
