package com.kveld9.morphe.extension.gboard.i18n;

import java.util.Map;
import static com.kveld9.morphe.extension.gboard.GboardExtension.*;

public class EsLanguagePack extends BaseLanguagePack {
    @Override
    public String getLanguageCode() {
        return "es";
    }

    @Override
    public String getLanguageName() {
        return "Español";
    }

    @Override
    public void populateTitles(Map<String, String> titles) {
        titles.put(PREF_KEY_HEADER, "Parches de Morphe");
        titles.put(PREF_KEY_SCREEN, "Parches de Morphe");
        titles.put(PREF_KEY_CAT_ACTIONS, "Acciones y estado");
        titles.put(PREF_KEY_ENABLE_IME, "Activar Gboard");
        titles.put(PREF_KEY_SELECT_IME, "Elegir Gboard");
        titles.put(PREF_KEY_RESTART_GBOARD, "Reiniciar Gboard");
        titles.put(PREF_KEY_CAT_APPEARANCE, "Apariencia y tema");
        titles.put(PREF_KEY_AMOLED, "Tema AMOLED puro");
        titles.put(PREF_KEY_ZERO_BOTTOM_INSET, "Sin hueco abajo");
        titles.put(PREF_KEY_BOTTOM_PADDING, "Espacio abajo (px)");
        titles.put(PREF_KEY_HIDE_IME_NAV_BAR, "Ocultar barra del sistema");
        titles.put(PREF_KEY_KEY_SHAPE_SELECTION, "Forma de teclas");
        titles.put(PREF_KEY_CAT_TOOLBAR, "Barra de herramientas y navegación");
        titles.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Nuevo menú de barra");
        titles.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Íconos de barra");
        titles.put(PREF_KEY_DISMISS_SUGGESTIONS, "Botón cerrar sugerencias");
        titles.put(PREF_KEY_CURSOR_TRACKPAD, "Trackpad de cursor");
        titles.put(PREF_KEY_CAT_CLIPBOARD, "Portapapeles");
        titles.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Historial más largo");
        titles.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Guardar por (horas)");
        titles.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Más elementos no fijados");
        titles.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Límite no fijados");
        titles.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Vista cuadrícula");
        titles.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Columnas cuadrícula");
        titles.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Límite de texto");
        titles.put(PREF_KEY_CAT_HAPTICS, "Vibración");
        titles.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibración propia");
        titles.put(PREF_KEY_MODERN_HAPTICS, "Vibración moderna");
        titles.put(PREF_KEY_CAT_SMART, "Inteligente y voz");
        titles.put(PREF_KEY_GRAMMAR_CHECKER, "Corrector gramatical");
        titles.put(PREF_KEY_BLUETOOTH_MIC, "Micro Bluetooth");
        titles.put(PREF_KEY_CAT_PRIVACY, "Privacidad");
        titles.put(PREF_KEY_FORCE_INCOGNITO, "Siempre incógnito");
        titles.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Ocultar ícono de incógnito");
        titles.put(PREF_KEY_VOICE_INCOGNITO, "Voz en privado");
        titles.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Portapapeles en privado");
    }

    @Override
    public void populateSummaries(Map<String, String> summaries) {
        summaries.put(PREF_KEY_HEADER, "Ajustes de parches");
        summaries.put(PREF_KEY_ENABLE_IME, "Gboard está apagado. Toca para activarlo.");
        summaries.put(PREF_KEY_SELECT_IME, "Gboard está activo pero no elegido. Tócalo para elegirlo.");
        summaries.put(PREF_KEY_RESTART_GBOARD, "Toca para aplicar cambios.");
        summaries.put(PREF_KEY_AMOLED, "Fondo negro puro en temas oscuros.");
        summaries.put(PREF_KEY_ZERO_BOTTOM_INSET, "Quita el hueco bajo el teclado.");
        summaries.put(PREF_KEY_BOTTOM_PADDING, "Espacio extra abajo, en píxeles. 0 = nada.");
        summaries.put(PREF_KEY_HIDE_IME_NAV_BAR, "Oculta la barra del sistema bajo el teclado (Android 13+).");
        summaries.put(PREF_KEY_KEY_SHAPE_SELECTION, "Teclas redondeadas y sin bordes.");
        summaries.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Nuevo menú de barra y panel.");
        summaries.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Íconos máximos arriba (defecto 5).");
        summaries.put(PREF_KEY_DISMISS_SUGGESTIONS, "Muestra una X para cerrar sugerencias.");
        summaries.put(PREF_KEY_CURSOR_TRACKPAD, "Mueve el cursor deslizando en espacio.");
        summaries.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Tiempo personalizado para no fijados.");
        summaries.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Horas antes de borrar no fijados (defecto 24).");
        summaries.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Límite personalizado de no fijados.");
        summaries.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Máximo de no fijados (defecto 50).");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Portapapeles en columnas.");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Columnas: 1, 2 o 3 (defecto 2).");
        summaries.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Máximo por texto, en miles. Reinicia para aplicar.");
        summaries.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibra aunque el sistema la tenga apagada.");
        summaries.put(PREF_KEY_MODERN_HAPTICS, "Toque nítido en vez de zumbido. Reinicia para aplicar.");
        summaries.put(PREF_KEY_GRAMMAR_CHECKER, "Corrección y predicciones.");
        summaries.put(PREF_KEY_BLUETOOTH_MIC, "Dicta con micro Bluetooth.");
        summaries.put(PREF_KEY_FORCE_INCOGNITO, "Siempre privado: sin historial.");
        summaries.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Oculta el ícono de incógnito.");
        summaries.put(PREF_KEY_VOICE_INCOGNITO, "Voz en campos privados.");
        summaries.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Portapapeles en campos privados.");
    }

    @Override
    public String getRestartToast() {
        return "Reinicia Gboard para aplicar los cambios";
    }

    @Override
    public String getRestartingToast() {
        return "Reiniciando Gboard...";
    }

    @Override
    public String getRestartTitle(boolean pending) {
        if (pending) {
            return "Reiniciar Gboard (Reinicio pendiente)";
        }
        return "Reiniciar proceso de Gboard";
    }

    @Override
    public String getRestartSummary(boolean pending) {
        if (pending) {
            return "¡Cambios pendientes! Toca aquí para reiniciar Gboard y aplicar los cambios ahora.";
        }
        return "Toca para aplicar cambios (necesario para la mayoría de opciones)";
    }

    @Override
    public String formatUnit(String prefKey, int value) {
        switch (prefKey) {
            case PREF_KEY_BOTTOM_PADDING:
                return value + " px";
            case PREF_KEY_TOOLBAR_ITEM_COUNT:
                return value + (value == 1 ? " ícono" : " íconos");
            case PREF_KEY_CLIPBOARD_RETENTION_HOURS:
                return value + " h";
            case PREF_KEY_CLIPBOARD_UNPINNED_LIMIT:
                return value + (value == 1 ? " elemento" : " elementos");
            case PREF_KEY_CLIPBOARD_GRID_COLUMNS:
                return value + (value == 1 ? " columna" : " columnas");
            case PREF_KEY_CLIPBOARD_CHAR_LIMIT:
                return value + " mil caracteres";
            default:
                return String.valueOf(value);
        }
    }
}
