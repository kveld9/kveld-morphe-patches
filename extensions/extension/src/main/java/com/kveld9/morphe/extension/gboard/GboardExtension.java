package com.kveld9.morphe.extension.gboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import com.kveld9.morphe.extension.gboard.i18n.GboardI18n;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class GboardExtension {
    public static final String PREF_KEY_HEADER = "morphe_patches_header";
    public static final String PREF_KEY_SCREEN = "morphe_patches_screen";
    public static final String PREF_KEY_CAT_ACTIONS = "morphe_cat_actions";
    public static final String PREF_KEY_CAT_APPEARANCE = "morphe_cat_appearance";
    public static final String PREF_KEY_CAT_TOOLBAR = "morphe_cat_toolbar";
    public static final String PREF_KEY_CAT_CLIPBOARD = "morphe_cat_clipboard";
    public static final String PREF_KEY_CAT_HAPTICS = "morphe_cat_haptics";
    public static final String PREF_KEY_CAT_SMART = "morphe_cat_smart";
    public static final String PREF_KEY_CAT_PRIVACY = "morphe_cat_privacy";

    public static final String PREF_KEY_RESTART_GBOARD = "morphe_restart_gboard";
    public static final String PREF_KEY_ENABLE_IME = "morphe_enable_ime";
    public static final String PREF_KEY_SELECT_IME = "morphe_select_ime";
    public static final String PREF_KEY_AMOLED = "morphe_amoled_enabled";
    public static final String PREF_KEY_ZERO_BOTTOM_INSET = "morphe_zero_bottom_inset";
    public static final String PREF_KEY_BOTTOM_PADDING = "morphe_bottom_padding";
    public static final String PREF_KEY_HIDE_IME_NAV_BAR = "morphe_hide_ime_nav_bar";
    public static final String PREF_KEY_TOOLBAR_ITEM_COUNT = "morphe_toolbar_item_count";
    public static final String PREF_KEY_EMOJI_SCALE = "morphe_emoji_scale";
    public static final String PREF_KEY_KEY_SHAPE_SELECTION = "morphe_key_shape_selection";
    public static final String PREF_KEY_ACCESS_POINTS_REDESIGN = "morphe_access_points_redesign";
    public static final String PREF_KEY_DISMISS_SUGGESTIONS = "morphe_dismiss_suggestions";
    public static final String PREF_KEY_CURSOR_TRACKPAD = "morphe_cursor_trackpad";
    public static final String PREF_KEY_HIDE_NUMBER_HINTS = "morphe_hide_number_hints";
    public static final String PREF_KEY_CLIPBOARD_EXTENDED_RETENTION = "morphe_clipboard_extended_retention";
    public static final String PREF_KEY_CLIPBOARD_RETENTION_HOURS = "morphe_clipboard_retention_hours";
    public static final String PREF_KEY_CLIPBOARD_RAISE_LIMIT = "morphe_clipboard_raise_limit";
    public static final String PREF_KEY_CLIPBOARD_UNPINNED_LIMIT = "morphe_clipboard_unpinned_limit";
    public static final String PREF_KEY_CLIPBOARD_GRID_LAYOUT = "morphe_clipboard_grid_layout";
    public static final String PREF_KEY_CLIPBOARD_GRID_COLUMNS = "morphe_clipboard_grid_columns";
    public static final String PREF_KEY_CLIPBOARD_CHAR_LIMIT = "morphe_clipboard_char_limit";
    public static final String PREF_KEY_GRAMMAR_CHECKER = "morphe_grammar_checker";
    public static final String PREF_KEY_BLUETOOTH_MIC = "morphe_bluetooth_mic";
    public static final String PREF_KEY_FORCE_INCOGNITO = "morphe_force_incognito";
    public static final String PREF_KEY_HIDE_INCOGNITO_ICON = "morphe_hide_incognito_icon";
    public static final String PREF_KEY_VOICE_INCOGNITO = "morphe_voice_typing_incognito";
    public static final String PREF_KEY_CLIPBOARD_INCOGNITO = "morphe_clipboard_incognito";
    public static final String PREF_KEY_DECOUPLE_TOUCH_FEEDBACK = "morphe_decouple_touch_feedback";
    public static final String PREF_KEY_MODERN_HAPTICS = "morphe_modern_haptics";

    public static final int MIN_BOTTOM_PADDING = 0;
    public static final int MAX_BOTTOM_PADDING = 150;
    public static final int DEFAULT_BOTTOM_PADDING = 0;

    public static final int MIN_TOOLBAR_ITEM_COUNT = 4;
    public static final int MAX_TOOLBAR_ITEM_COUNT = 8;
    public static final int DEFAULT_TOOLBAR_ITEM_COUNT = 5;

    public static final int MIN_CLIPBOARD_RETENTION_HOURS = 1;
    public static final int MAX_CLIPBOARD_RETENTION_HOURS = 168;
    public static final int DEFAULT_CLIPBOARD_RETENTION_HOURS = 24;

    public static final int MIN_CLIPBOARD_UNPINNED_LIMIT = 5;
    public static final int MAX_CLIPBOARD_UNPINNED_LIMIT = 100;
    public static final int DEFAULT_CLIPBOARD_UNPINNED_LIMIT = 50;

    public static final int MIN_CLIPBOARD_GRID_COLUMNS = 1;
    public static final int MAX_CLIPBOARD_GRID_COLUMNS = 3;
    public static final int DEFAULT_CLIPBOARD_GRID_COLUMNS = 2;

    public static final int MIN_CLIPBOARD_CHAR_LIMIT_K = 5;
    public static final int MAX_CLIPBOARD_CHAR_LIMIT_K = 200;
    public static final int DEFAULT_CLIPBOARD_CHAR_LIMIT_K = 20;

    public static final int MIN_EMOJI_SCALE = 50;
    public static final int MAX_EMOJI_SCALE = 150;
    public static final int DEFAULT_EMOJI_SCALE = 100;



    private static final int FLAG_IGNORE_GLOBAL_SETTING = 2; // HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    private static final int FALLBACK_VIBRATION_DURATION_MS = 10;

    private static volatile Context appContext = null;

    public static Context getContext(Context fallback) {
        if (fallback != null) {
            Context app = fallback.getApplicationContext();
            appContext = (app != null) ? app : fallback;
            return appContext;
        }
        if (appContext != null) {
            return appContext;
        }
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method method = atClass.getMethod("currentApplication");
            Object app = method.invoke(null);
            if (app instanceof Context) {
                appContext = ((Context) app).getApplicationContext();
                return appContext;
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> agClass = Class.forName("android.app.AppGlobals");
            Method method = agClass.getMethod("getInitialApplication");
            Object app = method.invoke(null);
            if (app instanceof Context) {
                appContext = ((Context) app).getApplicationContext();
                return appContext;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static volatile SharedPreferences cachedDePrefs = null;
    private static volatile SharedPreferences cachedCePrefs = null;

    private static volatile boolean restartPending = false;
    private static volatile java.lang.ref.WeakReference<Object> restartPrefRef = null;
    private static volatile long lastRestartToastTime = 0;

    private static final SharedPreferences.OnSharedPreferenceChangeListener PREF_LISTENER = (prefs, key) -> {
        if (key != null && key.startsWith("morphe_")) {
            if (!key.equals(PREF_KEY_RESTART_GBOARD) && !key.equals(PREF_KEY_ENABLE_IME) && !key.equals(PREF_KEY_SELECT_IME)) {
                restartPending = true;
                Context ctx = getContext(null);
                if (ctx != null) {
                    showRestartToast(ctx);
                }
                updateRestartPreferenceStatus(restartPrefRef != null ? restartPrefRef.get() : null);
            }
            refreshHotPathCache();
        }
    };

    private static final java.util.Set<SharedPreferences> registeredPrefs =
        java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    // Hot-path cached primitives to avoid reflection/disk lookups during UI measure & layout
    private static volatile boolean cachedZeroInsetEnabled = true;
    private static volatile int cachedBottomPadding = 0;
    private static volatile boolean cachedHideImeNavBar = false;
    private static volatile int cachedToolbarItemCount = 5;
    private static volatile int cachedEmojiScale = 100;
    private static volatile boolean cachedDecoupleHaptics = true;
    private static volatile boolean listenersInitialized = false;

    private static void refreshHotPathCache() {
        cachedZeroInsetEnabled = getBooleanPref(PREF_KEY_ZERO_BOTTOM_INSET, true);
        cachedHideImeNavBar = getBooleanPref(PREF_KEY_HIDE_IME_NAV_BAR, false);
        cachedDecoupleHaptics = getBooleanPref(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, true);
        int pad = getIntPref(PREF_KEY_BOTTOM_PADDING, DEFAULT_BOTTOM_PADDING);
        cachedBottomPadding = Math.max(MIN_BOTTOM_PADDING, Math.min(MAX_BOTTOM_PADDING, pad));
        int tbCount = getIntPref(PREF_KEY_TOOLBAR_ITEM_COUNT, DEFAULT_TOOLBAR_ITEM_COUNT);
        cachedToolbarItemCount = Math.max(MIN_TOOLBAR_ITEM_COUNT, Math.min(MAX_TOOLBAR_ITEM_COUNT, tbCount));
        int scale = getIntPref(PREF_KEY_EMOJI_SCALE, DEFAULT_EMOJI_SCALE);
        cachedEmojiScale = Math.max(MIN_EMOJI_SCALE, Math.min(MAX_EMOJI_SCALE, scale));
    }

    private static void showRestartToast(Context ctx) {
        long now = System.currentTimeMillis();
        if (now - lastRestartToastTime > 3000) {
            lastRestartToastTime = now;
            try {
                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.post(() -> {
                    try {
                        String msg = GboardI18n.getRestartToast(ctx);
                        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                });
            } catch (Throwable ignored) {}
        }
    }

    private static void ensureListeners() {
        ensureListeners(null);
    }

    private static void ensureListeners(Object pref) {
        synchronized (GboardExtension.class) {
            SharedPreferences de = getDePrefs();
            if (de != null && registeredPrefs.add(de)) {
                de.registerOnSharedPreferenceChangeListener(PREF_LISTENER);
            }
            SharedPreferences ce = getCePrefs();
            if (ce != null && registeredPrefs.add(ce)) {
                ce.registerOnSharedPreferenceChangeListener(PREF_LISTENER);
            }
            if (pref != null) {
                try {
                    Method m = pref.getClass().getMethod("getSharedPreferences");
                    Object sp = m.invoke(pref);
                    if (sp instanceof SharedPreferences && registeredPrefs.add((SharedPreferences) sp)) {
                        ((SharedPreferences) sp).registerOnSharedPreferenceChangeListener(PREF_LISTENER);
                    }
                } catch (Throwable ignored) {}
            }
            refreshHotPathCache();
            listenersInitialized = true;
        }
    }

    public static SharedPreferences getDePrefs() {
        if (cachedDePrefs != null) return cachedDePrefs;
        Context ctx = getContext(null);
        if (ctx == null) return null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Context deCtx = ctx.createDeviceProtectedStorageContext();
                if (deCtx != null) {
                    cachedDePrefs = PreferenceManager.getDefaultSharedPreferences(deCtx);
                    return cachedDePrefs;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static SharedPreferences getCePrefs() {
        if (cachedCePrefs != null) return cachedCePrefs;
        Context ctx = getContext(null);
        if (ctx == null) return null;
        try {
            cachedCePrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            return cachedCePrefs;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean getBooleanPref(String key, boolean defaultValue) {
        SharedPreferences dePrefs = getDePrefs();
        if (dePrefs != null && dePrefs.contains(key)) {
            try {
                return dePrefs.getBoolean(key, defaultValue);
            } catch (Throwable ignored) {}
        }
        SharedPreferences cePrefs = getCePrefs();
        if (cePrefs != null && cePrefs.contains(key)) {
            try {
                return cePrefs.getBoolean(key, defaultValue);
            } catch (Throwable ignored) {}
        }
        return defaultValue;
    }

    public static int getIntPref(String key, int defaultValue) {
        SharedPreferences dePrefs = getDePrefs();
        if (dePrefs != null && dePrefs.contains(key)) {
            try {
                return dePrefs.getInt(key, defaultValue);
            } catch (ClassCastException e) {
                String s = dePrefs.getString(key, null);
                if (s != null) return Integer.parseInt(s.trim());
            } catch (Throwable ignored) {}
        }
        SharedPreferences cePrefs = getCePrefs();
        if (cePrefs != null && cePrefs.contains(key)) {
            try {
                return cePrefs.getInt(key, defaultValue);
            } catch (ClassCastException e) {
                String s = cePrefs.getString(key, null);
                if (s != null) return Integer.parseInt(s.trim());
            } catch (Throwable ignored) {}
        }
        return defaultValue;
    }

    public static boolean isAmoledEnabled(Context context) {
        return getBooleanPref(PREF_KEY_AMOLED, true);
    }

    public static boolean isAmoledEnabled() {
        return isAmoledEnabled(null);
    }

    public static void addAmoledTheme(Context context, Object listObj) {
        if (!isAmoledEnabled(context)) return;
        if (context == null || !(listObj instanceof java.util.List)) return;
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> clsXss = Class.forName("xss", false, cl);
            Constructor<?> ctorXss = clsXss.getDeclaredConstructor(String.class);
            ctorXss.setAccessible(true);
            Object xss = ctorXss.newInstance("assets:theme_package_metadata_color_black.binarypb");

            Class<?> clsNck = Class.forName("nck", false, cl);
            Method mE = clsNck.getDeclaredMethod("e", Context.class, clsXss);
            mE.setAccessible(true);
            Object nck = mE.invoke(null, context, xss);

            Class<?> clsNbg = Class.forName("nbg", false, cl);
            Constructor<?> ctorNbg = clsNbg.getDeclaredConstructor(String.class, clsNck);
            ctorNbg.setAccessible(true);
            Object nbg = ctorNbg.newInstance("AMOLED", nck);

            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) listObj;
            list.add(nbg);
        } catch (Throwable ignored) {}
    }

    public static boolean isZeroBottomInsetEnabled(Context context) {
        ensureListeners();
        return cachedZeroInsetEnabled;
    }

    public static boolean isZeroBottomInsetEnabled() {
        return isZeroBottomInsetEnabled(null);
    }

    public static int getBottomPadding() {
        ensureListeners();
        if (!cachedZeroInsetEnabled) {
            return -1;
        }
        return cachedBottomPadding;
    }

    public static boolean isHideImeNavBarEnabled() {
        ensureListeners();
        return cachedHideImeNavBar;
    }

    private static java.lang.ref.WeakReference<View> imeNavBarFrame = new java.lang.ref.WeakReference<>(null);

    /**
     * Since Android 13 the framework draws its own navigation bar inside the IME window
     * (back chevron + IME switcher) and shrinks the IME content above it, so Gboard's own
     * bottom offsets cannot remove that gap. Hide the frame and let the content reach the bottom.
     * Disabled via Hide IME navigation bar toggle to keep the system switcher/collapse buttons.
     */
    public static void applyImeNavBarInset(android.inputmethodservice.InputMethodService service) {
        try {
            if (android.os.Build.VERSION.SDK_INT < 33) return;
            android.app.Dialog dialog = service.getWindow();
            if (dialog == null) return;
            android.view.Window window = dialog.getWindow();
            if (window == null) return;
            final android.view.ViewGroup decor = (android.view.ViewGroup) window.getDecorView();
            View frame = null;
            for (int i = 0; i < decor.getChildCount(); i++) {
                View child = decor.getChildAt(i);
                if (child.getClass().getName().endsWith("NavigationBarFrame")) {
                    frame = child;
                    break;
                }
            }
            if (frame == null) return;
            if (getBottomPadding() < 0 || !isHideImeNavBarEnabled()) {
                restoreImeNavBar(window, frame);
                return;
            }
            window.setDecorFitsSystemWindows(false);
            hideImeNavBar(window, frame);
            if (imeNavBarFrame.get() != frame) {
                imeNavBarFrame = new java.lang.ref.WeakReference<>(frame);
                // The framework re-shows the frame and Gboard re-tints the nav bar background on
                // every nav button/theme update; re-apply before the frame is drawn.
                decor.getViewTreeObserver().addOnPreDrawListener(() -> {
                    View f = imeNavBarFrame.get();
                    if (f == null) return true;
                    if (getBottomPadding() < 0 || !isHideImeNavBarEnabled()) {
                        restoreImeNavBar(window, f);
                        return true;
                    }
                    return !hideImeNavBar(window, f);
                });
            }
        } catch (Throwable ignored) {}
    }

    private static boolean hideImeNavBar(android.view.Window window, View frame) {
        boolean changed = false;
        if (frame.getVisibility() != View.GONE) {
            frame.setVisibility(View.GONE);
            changed = true;
        }
        if (window.getNavigationBarColor() != android.graphics.Color.TRANSPARENT) {
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            changed = true;
        }
        return changed;
    }

    private static void restoreImeNavBar(android.view.Window window, View frame) {
        try {
            if (frame.getVisibility() != View.VISIBLE) {
                frame.setVisibility(View.VISIBLE);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isForceIncognitoEnabled(Context context) {
        return getBooleanPref(PREF_KEY_FORCE_INCOGNITO, false);
    }

    public static boolean isForceIncognitoEnabled() {
        return isForceIncognitoEnabled(null);
    }

    public static boolean isHideIncognitoIconEnabled(Context context) {
        return getBooleanPref(PREF_KEY_HIDE_INCOGNITO_ICON, false);
    }

    public static boolean isHideIncognitoIconEnabled() {
        return isHideIncognitoIconEnabled(null);
    }

    public static boolean isVoiceTypingIncognitoEnabled(Context context) {
        return getBooleanPref(PREF_KEY_VOICE_INCOGNITO, true);
    }

    public static boolean isVoiceTypingIncognitoEnabled() {
        return isVoiceTypingIncognitoEnabled(null);
    }

    public static boolean overrideVoiceTypingIncognito(boolean isIncognito) {
        if (isVoiceTypingIncognitoEnabled()) {
            return false;
        }
        return isIncognito;
    }

    public static boolean isClipboardIncognitoEnabled(Context context) {
        return getBooleanPref(PREF_KEY_CLIPBOARD_INCOGNITO, true);
    }

    public static boolean isClipboardIncognitoEnabled() {
        return isClipboardIncognitoEnabled(null);
    }

    public static boolean overrideClipboardIncognito(boolean isIncognito) {
        if (isClipboardIncognitoEnabled()) {
            return false;
        }
        return isIncognito;
    }

    public static boolean isDecoupleTouchFeedbackEnabled(Context context) {
        if (!listenersInitialized) {
            ensureListeners();
        }
        if (context != null && appContext == null) {
            getContext(context);
        }
        return cachedDecoupleHaptics;
    }

    public static boolean isDecoupleTouchFeedbackEnabled() {
        if (!listenersInitialized) {
            ensureListeners();
        }
        return cachedDecoupleHaptics;
    }

    public static int overrideSystemHapticStatus(int originalStatus) {
        if (isDecoupleTouchFeedbackEnabled()) {
            return 1;
        }
        return originalStatus;
    }

    public static boolean overrideSystemHapticAllowed(boolean original) {
        if (isDecoupleTouchFeedbackEnabled()) {
            return true;
        }
        return original;
    }


    public static int getVibrationUsage(int originalUsage) {
        if (isDecoupleTouchFeedbackEnabled()) {
            return 17; // VibrationAttributes.USAGE_ALARM
        }
        return originalUsage;
    }

    @android.annotation.SuppressLint("MissingPermission")
    public static boolean vibrateCustomDuration(Object player, int durationMs) {
        if (!isDecoupleTouchFeedbackEnabled()) {
            return false;
        }
        if (durationMs <= 0) {
            return true;
        }
        Context ctx = null;
        if (player != null) {
            try {
                Field fContext = findField(player.getClass(), "h");
                if (fContext != null) {
                    Object val = fContext.get(player);
                    if (val instanceof Context) ctx = (Context) val;
                }
            } catch (Throwable ignored) {}
        }
        if (ctx == null) {
            ctx = getContext(null);
        }
        if (ctx == null) return false;

        try {
            android.os.Vibrator vibrator = null;
            if (player != null) {
                try {
                    Field fVibrator = findField(player.getClass(), "j");
                    if (fVibrator != null) {
                        Object supplier = fVibrator.get(player);
                        if (supplier != null) {
                            Method getMethod = supplier.getClass().getMethod("get");
                            Object v = getMethod.invoke(supplier);
                            if (v instanceof android.os.Vibrator) vibrator = (android.os.Vibrator) v;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (vibrator == null) {
                vibrator = (android.os.Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator == null || !vibrator.hasVibrator()) return false;

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                android.os.VibrationEffect effect;
                if (android.os.Build.VERSION.SDK_INT >= 30 && vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    float scale = Math.min(1.0f, Math.max(0.05f, ((float) durationMs) * 0.01f));
                    effect = android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, scale)
                            .compose();
                } else {
                    effect = android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                }

                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    android.os.VibrationAttributes attrs = new android.os.VibrationAttributes.Builder()
                            .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(effect, attrs);
                } else {
                    android.media.AudioAttributes audioAttrs = new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(effect, audioAttrs);
                }
            } else {
                vibrator.vibrate(durationMs);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean performHapticFeedback(View view, int feedbackConstant) {
        if (view == null) return false;
        if (isDecoupleTouchFeedbackEnabled()) {
            boolean handled = false;
            try {
                handled = view.performHapticFeedback(feedbackConstant, FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Throwable ignored) {}
            if (handled) {
                return true;
            }
            return triggerVibratorFallback(view.getContext());
        }
        return view.performHapticFeedback(feedbackConstant);
    }

    @android.annotation.SuppressLint("MissingPermission")
    private static boolean triggerVibratorFallback(Context context) {
        Context ctx = getContext(context);
        if (ctx == null) return false;
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return false;
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.os.VibrationEffect effect;
                if (android.os.Build.VERSION.SDK_INT >= 30 && vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    effect = android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                            .compose();
                } else {
                    effect = android.os.VibrationEffect.createOneShot(FALLBACK_VIBRATION_DURATION_MS, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                }
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    android.os.VibrationAttributes attrs = new android.os.VibrationAttributes.Builder()
                            .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(effect, attrs);
                } else {
                    android.media.AudioAttributes audioAttrs = new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(effect, audioAttrs);
                }
            } else {
                vibrator.vibrate(FALLBACK_VIBRATION_DURATION_MS);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isKeyShapeSelectionEnabled() {
        return getBooleanPref(PREF_KEY_KEY_SHAPE_SELECTION, true);
    }

    public static boolean isClipboardRetentionExtended() {
        return getBooleanPref(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, true);
    }

    public static long getClipboardRetentionMillis() {
        if (!isClipboardRetentionExtended()) {
            return 3600000L;
        }
        int hours = getIntPref(PREF_KEY_CLIPBOARD_RETENTION_HOURS, DEFAULT_CLIPBOARD_RETENTION_HOURS);
        if (hours < MIN_CLIPBOARD_RETENTION_HOURS) hours = MIN_CLIPBOARD_RETENTION_HOURS;
        return ((long) hours) * 3600000L;
    }

    public static boolean isClipboardLimitRaised() {
        return getBooleanPref(PREF_KEY_CLIPBOARD_RAISE_LIMIT, true);
    }

    public static int getClipboardUnpinnedLimit() {
        if (!isClipboardLimitRaised()) {
            return MIN_CLIPBOARD_UNPINNED_LIMIT;
        }
        int limit = getIntPref(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, DEFAULT_CLIPBOARD_UNPINNED_LIMIT);
        return Math.max(MIN_CLIPBOARD_UNPINNED_LIMIT, Math.min(MAX_CLIPBOARD_UNPINNED_LIMIT, limit));
    }

    public static boolean isClipboardGridLayoutEnabled() {
        return getBooleanPref(PREF_KEY_CLIPBOARD_GRID_LAYOUT, true);
    }

    public static int getClipboardGridColumns() {
        if (!isClipboardGridLayoutEnabled()) {
            return MIN_CLIPBOARD_GRID_COLUMNS;
        }
        int cols = getIntPref(PREF_KEY_CLIPBOARD_GRID_COLUMNS, DEFAULT_CLIPBOARD_GRID_COLUMNS);
        return Math.max(MIN_CLIPBOARD_GRID_COLUMNS, Math.min(MAX_CLIPBOARD_GRID_COLUMNS, cols));
    }

    /** Per-item text clip character limit (text_clip_item_char_limit), slider stored in thousands. */
    public static long getClipboardCharLimit() {
        int k = getIntPref(PREF_KEY_CLIPBOARD_CHAR_LIMIT, DEFAULT_CLIPBOARD_CHAR_LIMIT_K);
        k = Math.max(MIN_CLIPBOARD_CHAR_LIMIT_K, Math.min(MAX_CLIPBOARD_CHAR_LIMIT_K, k));
        return ((long) k) * 1000L;
    }

    public static boolean isAccessPointsRedesignEnabled() {
        return getBooleanPref(PREF_KEY_ACCESS_POINTS_REDESIGN, true);
    }

    public static int getToolbarItemCount() {
        ensureListeners();
        return cachedToolbarItemCount;
    }

    public static boolean isDismissSuggestionsEnabled() {
        return getBooleanPref(PREF_KEY_DISMISS_SUGGESTIONS, true);
    }

    public static boolean isCursorTrackpadEnabled() {
        return getBooleanPref(PREF_KEY_CURSOR_TRACKPAD, false);
    }

    public static boolean isHideNumberHintsEnabled() {
        return getBooleanPref(PREF_KEY_HIDE_NUMBER_HINTS, false);
    }

    /**
     * Clears single-digit secondary labels from a SoftKeyDef so the small
     * number hints above the letter row are not rendered. Long-press actions
     * live in a separate field and keep working. Fail-safe: any reflection
     * mismatch is ignored and the hints stay visible.
     */
    public static void sanitizeNumberHints(Object def) {
        try {
            if (!isHideNumberHintsEnabled()) return;
            if (def == null) return;
            Field fHints = findField(def.getClass(), "h");
            if (fHints == null || fHints.getType() != CharSequence[].class) return;
            CharSequence[] hints = (CharSequence[]) fHints.get(def);
            if (hints == null) return;
            for (int i = 0; i < hints.length; i++) {
                CharSequence cs = hints[i];
                if (cs != null && cs.length() == 1 && Character.isDigit(cs.charAt(0))) {
                    hints[i] = "";
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isGrammarCheckerEnabled() {
        return getBooleanPref(PREF_KEY_GRAMMAR_CHECKER, true);
    }

    /**
     * Minimum SDK for Gboard's haptic primitive keypress path (vibration_effect_min_sdk).
     * Gboard ships 1024, which disables the path on every device; 30 is the platform floor for
     * VibrationEffect.Composition. Gboard's own areAllEffectsSupported() check still applies.
     */
    public static long getVibrationEffectMinSdk() {
        return getBooleanPref(PREF_KEY_MODERN_HAPTICS, true) ? 30L : 1024L;
    }

    public static boolean isBluetoothMicEnabled() {
        return getBooleanPref(PREF_KEY_BLUETOOTH_MIC, true);
    }

    public static float getEmojiScale(Context context) {
        ensureListeners();
        return ((float) cachedEmojiScale) / 100.0f;
    }

    public static float getEmojiScale() {
        return getEmojiScale(null);
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            try {
                Field f = curr.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                curr = curr.getSuperclass();
            }
        }
        return null;
    }

    public static String getPreferenceKey(Object pref) {
        if (pref == null) return null;
        try {
            Field fKey = findField(pref.getClass(), "r");
            if (fKey != null) {
                Object val = fKey.get(pref);
                if (val instanceof String && !((String) val).isEmpty()) return (String) val;
            }
            Field fKeyAlt = findField(pref.getClass(), "mKey");
            if (fKeyAlt != null) {
                Object val = fKeyAlt.get(pref);
                if (val instanceof String && !((String) val).isEmpty()) return (String) val;
            }
            Method m = pref.getClass().getMethod("getKey");
            Object val = m.invoke(pref);
            if (val instanceof String && !((String) val).isEmpty()) return (String) val;
        } catch (Throwable ignored) {}
        return null;
    }

    public static void onBindSeekBar(Object pref) {
        if (pref == null) return;
        try {
            String key = getPreferenceKey(pref);
            int min = 0;
            if (PREF_KEY_TOOLBAR_ITEM_COUNT.equals(key)) {
                min = MIN_TOOLBAR_ITEM_COUNT;
            } else if (PREF_KEY_CLIPBOARD_RETENTION_HOURS.equals(key)) {
                min = MIN_CLIPBOARD_RETENTION_HOURS;
            } else if (PREF_KEY_CLIPBOARD_UNPINNED_LIMIT.equals(key)) {
                min = MIN_CLIPBOARD_UNPINNED_LIMIT;
            } else if (PREF_KEY_CLIPBOARD_GRID_COLUMNS.equals(key)) {
                min = MIN_CLIPBOARD_GRID_COLUMNS;
            } else if (PREF_KEY_CLIPBOARD_CHAR_LIMIT.equals(key)) {
                min = MIN_CLIPBOARD_CHAR_LIMIT_K;
            } else if (PREF_KEY_EMOJI_SCALE.equals(key)) {
                min = MIN_EMOJI_SCALE;
            }

            if (min > 0) {
                Field fMin = findField(pref.getClass(), "b");
                if (fMin != null) {
                    fMin.setInt(pref, min);
                }
                Field fVal = findField(pref.getClass(), "a");
                if (fVal != null) {
                    int currVal = fVal.getInt(pref);
                    if (currVal < min) {
                        fVal.setInt(pref, min);
                    }
                }
            }
            Field fShow = findField(pref.getClass(), "J");
            if (fShow != null) {
                fShow.setBoolean(pref, true);
            }
            Field fContinuous = findField(pref.getClass(), "f");
            if (fContinuous != null) {
                fContinuous.setBoolean(pref, true);
            }
        } catch (Throwable ignored) {}
    }

    public static void updateSeekBarLabel(Object pref, int value) {
        if (pref == null) return;
        try {
            Field fText = findField(pref.getClass(), "i");
            if (fText != null) {
                TextView tv = (TextView) fText.get(pref);
                if (tv != null) {
                    tv.setText(formatSeekBarValue(pref, value));
                }
            }
        } catch (Throwable ignored) {}
    }

    public static String formatSeekBarValue(Object pref, int value) {
        String key = getPreferenceKey(pref);
        if (key == null) return String.valueOf(value);
        Context ctx = getContextFromPref(pref);
        return GboardI18n.formatUnit(ctx, key, value);
    }

    public static boolean isImeEnabled(Context context) {
        Context ctx = getContext(context);
        if (ctx == null) return true;
        try {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return true;
            java.util.List<android.view.inputmethod.InputMethodInfo> imes = imm.getEnabledInputMethodList();
            if (imes == null) return true;
            String pkg = ctx.getPackageName();
            for (android.view.inputmethod.InputMethodInfo imi : imes) {
                if (pkg.equals(imi.getPackageName())) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean isImeSelected(Context context) {
        Context ctx = getContext(context);
        if (ctx == null) return true;
        try {
            String defaultIme = android.provider.Settings.Secure.getString(
                ctx.getContentResolver(),
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            );
            return defaultIme != null && defaultIme.startsWith(ctx.getPackageName() + "/");
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean checkFirstRun(android.app.Activity activity) {
        if (activity == null) return false;
        try {
            Context ctx = activity.getApplicationContext();
            if (!isImeEnabled(ctx) || !isImeSelected(ctx)) {
                android.content.Intent intent = new android.content.Intent("com.google.android.libraries.inputmethod.launcher.FIRST_RUN");
                intent.setClassName(activity.getPackageName(), "com.google.android.apps.inputmethod.latin.firstrun.LatinFirstRunActivity");
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                activity.finish();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void onPreferenceClick(Object pref) {
        if (pref == null) return;
        String key = getPreferenceKey(pref);
        if (PREF_KEY_RESTART_GBOARD.equals(key)) {
            restartGboard(pref);
        } else if (PREF_KEY_ENABLE_IME.equals(key)) {
            Context ctx = getContextFromPref(pref);
            if (ctx != null) {
                try {
                    android.content.Intent intent = new android.content.Intent("com.google.android.libraries.inputmethod.launcher.FIRST_RUN");
                    intent.setClassName(ctx.getPackageName(), "com.google.android.apps.inputmethod.latin.firstrun.LatinFirstRunActivity");
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                } catch (Throwable ignored) {}
            }
        } else if (PREF_KEY_SELECT_IME.equals(key)) {
            Context ctx = getContextFromPref(pref);
            if (ctx != null) {
                try {
                    android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showInputMethodPicker();
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private static volatile Field cachedTitleField = null;
    private static volatile Field cachedSummaryField = null;
    private static volatile boolean prefFieldsResolved = false;

    private static void resolvePreferenceFields(Class<?> prefClass) {
        if (prefFieldsResolved) return;
        synchronized (GboardExtension.class) {
            if (prefFieldsResolved) return;
            try {
                // NOTE: verified against the target Gboard dex: the title holder is
                // field "q" (read by v() into android.R.id.title) and the summary
                // holder is field "a" (read by m() into android.R.id.summary).
                // Mapping them the other way around swaps every title/summary pair.
                Field fTitle = findField(prefClass, "q");
                if (fTitle == null || !CharSequence.class.isAssignableFrom(fTitle.getType())) {
                    fTitle = findField(prefClass, "mTitle");
                }
                if (fTitle != null && CharSequence.class.isAssignableFrom(fTitle.getType())) {
                    cachedTitleField = fTitle;
                }

                Field fSummary = findField(prefClass, "a");
                if (fSummary == null || !CharSequence.class.isAssignableFrom(fSummary.getType())) {
                    fSummary = findField(prefClass, "mSummary");
                }
                if (fSummary != null && CharSequence.class.isAssignableFrom(fSummary.getType())) {
                    cachedSummaryField = fSummary;
                }

                if (cachedTitleField == null || cachedSummaryField == null) {
                    java.util.List<Field> csFields = new java.util.ArrayList<>();
                    for (Field f : prefClass.getDeclaredFields()) {
                        if (CharSequence.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            csFields.add(f);
                        }
                    }
                    if (csFields.size() >= 2) {
                        if (cachedSummaryField == null) cachedSummaryField = csFields.get(0);
                        if (cachedTitleField == null) cachedTitleField = csFields.get(1);
                    }
                }
            } catch (Throwable ignored) {}
            prefFieldsResolved = true;
        }
    }

    private static void updatePreferenceFields(Object pref, String title, String summary) {
        if (pref == null) return;
        try {
            Class<?> prefClass = pref.getClass();
            while (prefClass != null && !prefClass.getName().equals("androidx.preference.Preference")) {
                prefClass = prefClass.getSuperclass();
            }
            if (prefClass == null) return;

            resolvePreferenceFields(prefClass);

            if (title != null && cachedTitleField != null) {
                cachedTitleField.set(pref, title);
            }
            if (summary != null && cachedSummaryField != null) {
                cachedSummaryField.set(pref, summary);
            }
        } catch (Throwable ignored) {}
    }

    public static void onBindPreference(Object pref, Object holder) {
        if (pref == null || holder == null) return;
        ensureListeners(pref);
        String key = getPreferenceKey(pref);
        if (key == null) return;

        android.view.View itemView = getItemViewFromHolder(holder);
        if (itemView != null) {
            if (itemView.getVisibility() != android.view.View.VISIBLE) {
                itemView.setVisibility(android.view.View.VISIBLE);
            }
            android.view.ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null && lp.height != android.view.ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                itemView.setLayoutParams(lp);
            }
        }

        Context ctx = getContextFromPref(pref);
        applyDynamicLocalization(pref, holder, key, ctx);

        if (PREF_KEY_RESTART_GBOARD.equals(key)) {
            restartPrefRef = new java.lang.ref.WeakReference<>(pref);
            updateRestartPreferenceStatus(pref, holder);
            attachClickListener(pref, holder, () -> onPreferenceClick(pref));
        } else if (PREF_KEY_ENABLE_IME.equals(key)) {
            boolean enabled = isImeEnabled(ctx);
            applyActionCardVisibility(holder, !enabled, 0xFFFF9800);
            attachClickListener(pref, holder, () -> onPreferenceClick(pref));
        } else if (PREF_KEY_SELECT_IME.equals(key)) {
            boolean enabled = isImeEnabled(ctx);
            boolean selected = isImeSelected(ctx);
            applyActionCardVisibility(holder, enabled && !selected, 0xFF2196F3);
            attachClickListener(pref, holder, () -> onPreferenceClick(pref));
        }
    }

    private static void applyDynamicLocalization(Object pref, Object holder, String key, Context ctx) {
        if (PREF_KEY_RESTART_GBOARD.equals(key)) return;

        String title = GboardI18n.getTitle(ctx, key);
        String summary = GboardI18n.getSummary(ctx, key);
        if (title == null && summary == null) return;

        updatePreferenceFields(pref, title, summary);

        android.view.View itemView = getItemViewFromHolder(holder);
        if (itemView != null) {
            try {
                if (title != null) {
                    android.widget.TextView tvTitle = itemView.findViewById(android.R.id.title);
                    if (tvTitle != null) tvTitle.setText(title);
                }
                if (summary != null) {
                    android.widget.TextView tvSummary = itemView.findViewById(android.R.id.summary);
                    if (tvSummary != null) tvSummary.setText(summary);
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void attachClickListener(Object pref, Object holder, Runnable action) {
        if (pref != null) {
            try {
                Class<?> prefClass = pref.getClass();
                while (prefClass != null && !prefClass.getName().equals("androidx.preference.Preference")) {
                    prefClass = prefClass.getSuperclass();
                }
                if (prefClass != null) {
                    Field fN = findField(prefClass, "N");
                    if (fN == null) fN = findField(prefClass, "mClickListener");
                    if (fN != null && android.view.View.OnClickListener.class.isAssignableFrom(fN.getType())) {
                        fN.set(pref, (android.view.View.OnClickListener) v -> {
                            try {
                                action.run();
                            } catch (Throwable ignored) {}
                        });
                    }
                }
            } catch (Throwable ignored) {}
        }
        try {
            android.view.View itemView = getItemViewFromHolder(holder);
            if (itemView != null) {
                itemView.setFocusable(true);
                itemView.setClickable(true);
                itemView.setOnClickListener(v -> {
                    try {
                        action.run();
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {}
    }

    private static void applyActionCardVisibility(Object holder, boolean visible, int accentColor) {
        try {
            android.view.View itemView = getItemViewFromHolder(holder);
            if (itemView == null) return;
            if (!visible) {
                itemView.setVisibility(android.view.View.GONE);
                android.view.ViewGroup.LayoutParams lp = itemView.getLayoutParams();
                if (lp != null) {
                    lp.height = 0;
                    itemView.setLayoutParams(lp);
                }
            } else {
                itemView.setVisibility(android.view.View.VISIBLE);
                android.view.ViewGroup.LayoutParams lp = itemView.getLayoutParams();
                if (lp != null) {
                    lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                    itemView.setLayoutParams(lp);
                }
                android.widget.TextView summaryView = itemView.findViewById(android.R.id.summary);
                if (summaryView != null) {
                    summaryView.setTextColor(accentColor);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void updateRestartPreferenceStatus(Object pref) {
        updateRestartPreferenceStatus(pref, null);
    }

    public static void updateRestartPreferenceStatus(Object pref, Object holder) {
        try {
            if (pref == null && restartPrefRef != null) {
                pref = restartPrefRef.get();
            }
            Context ctx = (pref != null) ? getContextFromPref(pref) : getContext(null);
            String title = GboardI18n.getRestartTitle(ctx, restartPending);
            String summary = GboardI18n.getRestartSummary(ctx, restartPending);

            if (pref != null) {
                updatePreferenceFields(pref, title, summary);
            }

            if (holder != null) {
                android.view.View itemView = getItemViewFromHolder(holder);
                if (itemView != null) {
                    final int summaryColor = restartPending ? 0xFFFF5252 : 0xFF888888;
                    try {
                        android.widget.TextView titleView = itemView.findViewById(android.R.id.title);
                        android.widget.TextView summaryView = itemView.findViewById(android.R.id.summary);
                        if (titleView != null) titleView.setText(title);
                        if (summaryView != null) {
                            summaryView.setText(summary);
                            summaryView.setTextColor(summaryColor);
                        }
                    } catch (Throwable ignored) {}
                    if (restartPending) {
                        itemView.post(() -> {
                            try {
                                android.widget.TextView summaryView = itemView.findViewById(android.R.id.summary);
                                if (summaryView != null) {
                                    summaryView.setTextColor(summaryColor);
                                }
                            } catch (Throwable ignored) {}
                        });
                    }
                }
            } else if (pref != null) {
                final Object targetPref = pref;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        Method mSetTitle = targetPref.getClass().getMethod("setTitle", CharSequence.class);
                        Method mSetSummary = targetPref.getClass().getMethod("setSummary", CharSequence.class);
                        mSetTitle.invoke(targetPref, title);
                        mSetSummary.invoke(targetPref, summary);
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {}
    }

    private static android.view.View getItemViewFromHolder(Object holder) {
        if (holder == null) return null;
        try {
            Field f = findField(holder.getClass(), "a");
            if (f != null) {
                Object val = f.get(holder);
                if (val instanceof android.view.View) return (android.view.View) val;
            }
            Field fItemView = findField(holder.getClass(), "itemView");
            if (fItemView != null) {
                Object val = fItemView.get(holder);
                if (val instanceof android.view.View) return (android.view.View) val;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Context getContextFromPref(Object pref) {
        if (pref == null) return null;
        try {
            Field fCtx = findField(pref.getClass(), "j");
            if (fCtx != null) {
                Object val = fCtx.get(pref);
                if (val instanceof Context) return (Context) val;
            }
            Field fCtxAlt = findField(pref.getClass(), "mContext");
            if (fCtxAlt != null) {
                Object val = fCtxAlt.get(pref);
                if (val instanceof Context) return (Context) val;
            }
            Method m = pref.getClass().getMethod("getContext");
            Object val = m.invoke(pref);
            if (val instanceof Context) return (Context) val;
        } catch (Throwable ignored) {}
        return getContext(null);
    }

    public static void restartGboard(Object pref) {
        try {
            Context ctx = getContextFromPref(pref);
            if (ctx != null) {
                try {
                    String msg = GboardI18n.getRestartingToast(ctx);
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}

                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                    intent.setClassName(ctx.getPackageName(), "com.google.android.apps.inputmethod.latin.preference.SettingsActivity");
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    @android.annotation.SuppressLint("WrongConstant")
                    int flags = android.app.PendingIntent.FLAG_CANCEL_CURRENT;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
                    }

                    android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                        ctx,
                        0,
                        intent,
                        flags
                    );

                    android.app.AlarmManager mgr = (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                    if (mgr != null) {
                        mgr.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 350, pendingIntent);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            } finally {
                System.exit(0);
            }
        }).start();
    }
}
