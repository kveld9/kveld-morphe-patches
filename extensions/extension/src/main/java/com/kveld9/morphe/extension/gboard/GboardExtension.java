package com.kveld9.morphe.extension.gboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
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
    public static final String PREF_KEY_TOOLBAR_ITEM_COUNT = "morphe_toolbar_item_count";
    public static final String PREF_KEY_EMOJI_SCALE = "morphe_emoji_scale";
    public static final String PREF_KEY_KEY_SHAPE_SELECTION = "morphe_key_shape_selection";
    public static final String PREF_KEY_ACCESS_POINTS_REDESIGN = "morphe_access_points_redesign";
    public static final String PREF_KEY_DISMISS_SUGGESTIONS = "morphe_dismiss_suggestions";
    public static final String PREF_KEY_CURSOR_TRACKPAD = "morphe_cursor_trackpad";
    public static final String PREF_KEY_CLIPBOARD_EXTENDED_RETENTION = "morphe_clipboard_extended_retention";
    public static final String PREF_KEY_CLIPBOARD_RETENTION_HOURS = "morphe_clipboard_retention_hours";
    public static final String PREF_KEY_CLIPBOARD_RAISE_LIMIT = "morphe_clipboard_raise_limit";
    public static final String PREF_KEY_CLIPBOARD_UNPINNED_LIMIT = "morphe_clipboard_unpinned_limit";
    public static final String PREF_KEY_CLIPBOARD_GRID_LAYOUT = "morphe_clipboard_grid_layout";
    public static final String PREF_KEY_CLIPBOARD_GRID_COLUMNS = "morphe_clipboard_grid_columns";
    public static final String PREF_KEY_GRAMMAR_CHECKER = "morphe_grammar_checker";
    public static final String PREF_KEY_BLUETOOTH_MIC = "morphe_bluetooth_mic";
    public static final String PREF_KEY_FORCE_INCOGNITO = "morphe_force_incognito";
    public static final String PREF_KEY_HIDE_INCOGNITO_ICON = "morphe_hide_incognito_icon";
    public static final String PREF_KEY_VOICE_INCOGNITO = "morphe_voice_typing_incognito";
    public static final String PREF_KEY_DECOUPLE_TOUCH_FEEDBACK = "morphe_decouple_touch_feedback";

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

    public static final int MIN_EMOJI_SCALE = 50;
    public static final int MAX_EMOJI_SCALE = 150;
    public static final int DEFAULT_EMOJI_SCALE = 100;

    private static class PrefTranslation {
        final String enTitle;
        final String enSummary;
        final String esTitle;
        final String esSummary;

        PrefTranslation(String enTitle, String enSummary, String esTitle, String esSummary) {
            this.enTitle = enTitle;
            this.enSummary = enSummary;
            this.esTitle = esTitle;
            this.esSummary = esSummary;
        }
    }

    private static final java.util.Map<String, PrefTranslation> TRANSLATIONS = createTranslations();

    private static java.util.Map<String, PrefTranslation> createTranslations() {
        java.util.Map<String, PrefTranslation> map = new java.util.HashMap<>();
        addTrans(map, PREF_KEY_HEADER,
            "Morphe Patches", "Customization and patch toggles",
            "Parches de Morphe", "Personalización y ajustes de parches");
        addTrans(map, PREF_KEY_SCREEN,
            "Morphe Patches", null,
            "Parches de Morphe", null);
        addTrans(map, PREF_KEY_CAT_ACTIONS,
            "Actions & Status", null,
            "Acciones y estado", null);
        addTrans(map, PREF_KEY_ENABLE_IME,
            "Enable Gboard in System Settings", "Gboard is disabled in Android. Tap to enable it in Manage Keyboards.",
            "Habilitar Gboard en ajustes del sistema", "Gboard está deshabilitado en Android. Toca para habilitarlo en Administrar teclados.");
        addTrans(map, PREF_KEY_SELECT_IME,
            "Select Gboard Input Method", "Gboard is enabled but not active. Tap to choose Gboard as your keyboard.",
            "Seleccionar método de entrada de Gboard", "Gboard está habilitado pero no activo. Toca para elegir Gboard como teclado.");
        addTrans(map, PREF_KEY_RESTART_GBOARD,
            "Restart Gboard Process", "Tap to apply changes (required for most options to take effect)",
            "Reiniciar proceso de Gboard", "Toca para aplicar cambios (necesario para la mayoría de opciones)");
        addTrans(map, PREF_KEY_CAT_APPEARANCE,
            "Appearance & Theme", null,
            "Apariencia y tema", null);
        addTrans(map, PREF_KEY_AMOLED,
            "Pure AMOLED Theme", "Force pure black (#000000) background on dark themes",
            "Tema AMOLED puro", "Forzar fondo negro puro (#000000) en temas oscuros");
        addTrans(map, PREF_KEY_ZERO_BOTTOM_INSET,
            "Zero Bottom Inset", "Eliminate bottom margin chin under keyboard in gesture navigation",
            "Margen inferior cero", "Eliminar el margen inferior debajo del teclado en navegación por gestos");
        addTrans(map, PREF_KEY_BOTTOM_PADDING,
            "Bottom Padding (px)", "Forced bottom margin padding in pixels (0 for completely flush, default: 0)",
            "Relleno inferior (px)", "Relleno del margen inferior en píxeles (0 para rasante total, predeterminado: 0)");
        addTrans(map, PREF_KEY_KEY_SHAPE_SELECTION,
            "Key Border Shapes", "Enable rounded and borderless key styles in themes",
            "Forma de borde de teclas", "Habilitar estilos de teclas redondeadas y sin bordes en temas");
        addTrans(map, PREF_KEY_EMOJI_SCALE,
            "Emoji Size Scaling", "Adjust emoji visual size on the keyboard (50% - 150%)",
            "Escala de tamaño de emojis", "Ajustar el tamaño visual de emojis en el teclado (50% - 150%)");
        addTrans(map, PREF_KEY_CAT_TOOLBAR,
            "Toolbar & Navigation", null,
            "Barra de herramientas y navegación", null);
        addTrans(map, PREF_KEY_ACCESS_POINTS_REDESIGN,
            "Access Points Redesign", "Enable redesigned access points menu bar and panel (Panel V2)",
            "Rediseño de barra de acceso", "Habilitar barra de acceso y panel rediseñados (Panel V2)");
        addTrans(map, PREF_KEY_TOOLBAR_ITEM_COUNT,
            "Toolbar Item Count", "Maximum number of access point icons displayed on top toolbar (default: 5)",
            "Cantidad de elementos en barra", "Cantidad máxima de íconos mostrados en la barra superior (predeterminado: 5)");
        addTrans(map, PREF_KEY_DISMISS_SUGGESTIONS,
            "Dismiss Suggestions Button", "Show close button (X) on proactive suggestions bar",
            "Botón de descartar sugerencias", "Mostrar botón de cerrar (X) en la barra de sugerencias");
        addTrans(map, PREF_KEY_CURSOR_TRACKPAD,
            "Cursor Trackpad Mode", "2D spacebar trackpad cursor navigation and cursor lock mode",
            "Modo trackpad de cursor", "Navegación de cursor bidireccional (2D) en barra espaciadora");
        addTrans(map, PREF_KEY_CAT_CLIPBOARD,
            "Clipboard", null,
            "Portapapeles", null);
        addTrans(map, PREF_KEY_CLIPBOARD_EXTENDED_RETENTION,
            "Extended History Retention", "Enable custom retention time limit for unpinned clips",
            "Retención extendida de historial", "Habilitar límite de tiempo personalizado para elementos no fijados");
        addTrans(map, PREF_KEY_CLIPBOARD_RETENTION_HOURS,
            "Retention Time Limit (Hours)", "Hours to retain unpinned clips in history before cleanup (default: 24h)",
            "Límite de tiempo de retención (horas)", "Horas de retención de elementos no fijados antes de eliminarlos (predeterminado: 24h)");
        addTrans(map, PREF_KEY_CLIPBOARD_RAISE_LIMIT,
            "Raise Unpinned Clips Limit", "Enable custom limit for unpinned clipboard history items",
            "Aumentar límite de elementos no fijados", "Habilitar límite personalizado para elementos no fijados en el historial");
        addTrans(map, PREF_KEY_CLIPBOARD_UNPINNED_LIMIT,
            "Unpinned Clips Limit", "Maximum number of unpinned items displayed in clipboard (default: 50)",
            "Límite de elementos no fijados", "Cantidad máxima de elementos no fijados en el portapapeles (predeterminado: 50)");
        addTrans(map, PREF_KEY_CLIPBOARD_GRID_LAYOUT,
            "Clipboard Grid Layout", "Enable custom multi-column layout for clipboard clips",
            "Diseño en cuadrícula del portapapeles", "Habilitar diseño en múltiples columnas para el portapapeles");
        addTrans(map, PREF_KEY_CLIPBOARD_GRID_COLUMNS,
            "Clipboard Grid Columns", "Number of columns in clipboard layout (1, 2, or 3. Default: 2)",
            "Columnas de cuadrícula del portapapeles", "Número de columnas en el portapapeles (1, 2 o 3. Predeterminado: 2)");
        addTrans(map, PREF_KEY_CAT_HAPTICS,
            "Haptics & Vibration", null,
            "Vibración y respuesta háptica", null);
        addTrans(map, PREF_KEY_DECOUPLE_TOUCH_FEEDBACK,
            "Independent Keyboard Vibration", "Keep keyboard vibration active even when Android's system Touch feedback and gesture haptics are disabled",
            "Vibración independiente del teclado", "Mantener la vibración del teclado activa aunque la respuesta táctil del sistema Android esté desactivada");
        addTrans(map, PREF_KEY_CAT_SMART,
            "Smart Features & Voice", null,
            "Funciones inteligentes y voz", null);
        addTrans(map, PREF_KEY_GRAMMAR_CHECKER,
            "Grammar Checker & Smart Compose", "Inline grammar review and Smart Compose predictions",
            "Corrector gramatical y Redacción inteligente", "Revisión gramatical integrada y predicciones de Redacción inteligente");
        addTrans(map, PREF_KEY_BLUETOOTH_MIC,
            "Bluetooth Microphone", "Enable Bluetooth microphone audio input for voice typing",
            "Micrófono Bluetooth", "Habilitar entrada de audio por micrófono Bluetooth para dictado por voz");
        addTrans(map, PREF_KEY_CAT_PRIVACY,
            "Privacy & Security", null,
            "Privacidad y seguridad", null);
        addTrans(map, PREF_KEY_FORCE_INCOGNITO,
            "Force Incognito Mode", "Always operate in incognito mode (disables input history and learning)",
            "Forzar modo incógnito", "Operar siempre en modo incógnito (deshabilita el historial de entrada y aprendizaje)");
        addTrans(map, PREF_KEY_HIDE_INCOGNITO_ICON,
            "Hide Incognito Icon", "Hide the incognito mask icon on the toolbar",
            "Ocultar ícono de incógnito", "Ocultar el ícono de la máscara de incógnito en la barra de herramientas");
        addTrans(map, PREF_KEY_VOICE_INCOGNITO,
            "Voice Typing in Incognito", "Enable voice typing and microphone dictation in private fields and incognito mode",
            "Dictado por voz en incógnito", "Habilitar dictado por voz en campos privados y modo incógnito");
        return map;
    }

    private static void addTrans(java.util.Map<String, PrefTranslation> map, String key,
                                 String enTitle, String enSummary,
                                 String esTitle, String esSummary) {
        map.put(key, new PrefTranslation(enTitle, enSummary, esTitle, esSummary));
    }

    public static boolean isSpanish(Context context) {
        Context ctx = getContext(context);
        if (ctx != null) {
            Boolean configResult = checkConfigSpanish(ctx);
            if (configResult != null) return configResult;
        }
        return checkDefaultLocaleSpanish();
    }

    private static Boolean checkConfigSpanish(Context ctx) {
        try {
            android.content.res.Configuration cfg = ctx.getResources().getConfiguration();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList locales = cfg.getLocales();
                if (locales != null && !locales.isEmpty()) {
                    String lang = locales.get(0).getLanguage();
                    if (lang != null && !lang.isEmpty()) {
                        return isSpanishTag(lang);
                    }
                }
            }
            @SuppressWarnings("deprecation")
            java.util.Locale loc = cfg.locale;
            if (loc != null && loc.getLanguage() != null && !loc.getLanguage().isEmpty()) {
                return isSpanishTag(loc.getLanguage());
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean checkDefaultLocaleSpanish() {
        try {
            java.util.Locale def = java.util.Locale.getDefault();
            return def != null && isSpanishTag(def.getLanguage());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSpanishTag(String tag) {
        return tag != null && tag.equalsIgnoreCase("es");
    }

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
    private static volatile int cachedToolbarItemCount = 5;
    private static volatile int cachedEmojiScale = 100;
    private static volatile boolean cachedDecoupleHaptics = true;
    private static volatile boolean listenersInitialized = false;

    private static void refreshHotPathCache() {
        cachedZeroInsetEnabled = getBooleanPref(PREF_KEY_ZERO_BOTTOM_INSET, true);
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
                        String msg = isSpanish(ctx) ? "Reinicia Gboard para aplicar los cambios" : "Restart Gboard to apply changes";
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
            return 0;
        }
        return originalUsage;
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
                android.os.VibrationEffect effect = null;
                if (android.os.Build.VERSION.SDK_INT >= 30 && vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    effect = android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                            .compose();
                } else {
                    effect = android.os.VibrationEffect.createOneShot(FALLBACK_VIBRATION_DURATION_MS, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                }
                vibrator.vibrate(effect);
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

    public static boolean isGrammarCheckerEnabled() {
        return getBooleanPref(PREF_KEY_GRAMMAR_CHECKER, true);
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

        boolean es = isSpanish(getContextFromPref(pref));
        switch (key) {
            case PREF_KEY_BOTTOM_PADDING:
                return value + " px";
            case PREF_KEY_TOOLBAR_ITEM_COUNT:
                return Math.max(MIN_TOOLBAR_ITEM_COUNT, value) + (es ? " íconos" : " icons");
            case PREF_KEY_CLIPBOARD_RETENTION_HOURS:
                return Math.max(MIN_CLIPBOARD_RETENTION_HOURS, value) + " h";
            case PREF_KEY_CLIPBOARD_UNPINNED_LIMIT:
                return Math.max(MIN_CLIPBOARD_UNPINNED_LIMIT, value) + (es ? " elementos" : " clips");
            case PREF_KEY_CLIPBOARD_GRID_COLUMNS:
                int cols = Math.max(MIN_CLIPBOARD_GRID_COLUMNS, value);
                return formatCols(cols, es);
            case PREF_KEY_EMOJI_SCALE:
                return Math.max(MIN_EMOJI_SCALE, value) + " %";
            default:
                return String.valueOf(value);
        }
    }

    private static String formatCols(int cols, boolean es) {
        if (es) {
            return cols + (cols == 1 ? " columna" : " columnas");
        }
        return cols + (cols == 1 ? " col" : " cols");
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
            attachClickListener(holder, () -> onPreferenceClick(pref));
        } else if (PREF_KEY_ENABLE_IME.equals(key)) {
            boolean enabled = isImeEnabled(ctx);
            applyActionCardVisibility(holder, !enabled, 0xFFFF9800);
            attachClickListener(holder, () -> onPreferenceClick(pref));
        } else if (PREF_KEY_SELECT_IME.equals(key)) {
            boolean enabled = isImeEnabled(ctx);
            boolean selected = isImeSelected(ctx);
            applyActionCardVisibility(holder, enabled && !selected, 0xFF2196F3);
            attachClickListener(holder, () -> onPreferenceClick(pref));
        }
    }

    private static void applyDynamicLocalization(Object pref, Object holder, String key, Context ctx) {
        if (PREF_KEY_RESTART_GBOARD.equals(key)) return;

        PrefTranslation trans = TRANSLATIONS.get(key);
        if (trans == null) return;

        boolean es = isSpanish(ctx);
        String title = es ? trans.esTitle : trans.enTitle;
        String summary = es ? trans.esSummary : trans.enSummary;

        android.view.View itemView = getItemViewFromHolder(holder);
        if (itemView != null) {
            itemView.post(() -> {
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
            });
        }
    }

    private static void attachClickListener(Object holder, Runnable action) {
        try {
            android.view.View itemView = getItemViewFromHolder(holder);
            if (itemView != null) {
                itemView.post(() -> {
                    try {
                        itemView.setFocusable(true);
                        itemView.setClickable(true);
                        itemView.setOnClickListener(v -> {
                            try {
                                action.run();
                            } catch (Throwable ignored) {}
                        });
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
                itemView.post(() -> {
                    try {
                        android.widget.TextView sView = itemView.findViewById(android.R.id.summary);
                        if (sView != null) {
                            sView.setTextColor(accentColor);
                        }
                    } catch (Throwable ignored) {}
                });
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
            boolean es = isSpanish(ctx);
            String title = getRestartTitle(restartPending, es);
            String summary = getRestartSummary(restartPending, es);

            if (holder != null) {
                android.view.View itemView = getItemViewFromHolder(holder);
                if (itemView != null) {
                    final int summaryColor = restartPending ? 0xFFFF5252 : 0xFF888888;
                    itemView.post(() -> {
                        try {
                            android.widget.TextView titleView = itemView.findViewById(android.R.id.title);
                            android.widget.TextView summaryView = itemView.findViewById(android.R.id.summary);
                            if (titleView != null) titleView.setText(title);
                            if (summaryView != null) {
                                summaryView.setText(summary);
                                summaryView.setTextColor(summaryColor);
                            }
                        } catch (Throwable ignored) {}
                    });
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

    private static String getRestartTitle(boolean pending, boolean es) {
        if (pending) {
            return es ? "Reiniciar Gboard (Reinicio pendiente)" : "Restart Gboard (Restart Pending)";
        }
        return es ? "Reiniciar proceso de Gboard" : "Restart Gboard Process";
    }

    private static String getRestartSummary(boolean pending, boolean es) {
        if (pending) {
            return es ? "¡Cambios pendientes! Toca aquí para reiniciar Gboard y aplicar los cambios ahora."
                      : "Changes pending! Tap here to restart Gboard and apply changes now.";
        }
        return es ? "Toca para aplicar cambios (necesario para la mayoría de opciones)"
                  : "Tap to apply changes (required for most options to take effect)";
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
                    String msg = isSpanish(ctx) ? "Reiniciando Gboard..." : "Restarting Gboard...";
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
