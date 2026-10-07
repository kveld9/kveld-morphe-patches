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
        titles.put(PREF_KEY_ENABLE_IME, "Habilitar Gboard en ajustes del sistema");
        titles.put(PREF_KEY_SELECT_IME, "Seleccionar método de entrada de Gboard");
        titles.put(PREF_KEY_RESTART_GBOARD, "Reiniciar proceso de Gboard");
        titles.put(PREF_KEY_CAT_APPEARANCE, "Apariencia y tema");
        titles.put(PREF_KEY_AMOLED, "Tema AMOLED puro");
        titles.put(PREF_KEY_ZERO_BOTTOM_INSET, "Margen inferior cero");
        titles.put(PREF_KEY_BOTTOM_PADDING, "Relleno inferior (px)");
        titles.put(PREF_KEY_KEY_SHAPE_SELECTION, "Forma de borde de teclas");
        titles.put(PREF_KEY_EMOJI_SCALE, "Escala de tamaño de emojis");
        titles.put(PREF_KEY_CAT_TOOLBAR, "Barra de herramientas y navegación");
        titles.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Rediseño de barra de acceso");
        titles.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Cantidad de elementos en barra");
        titles.put(PREF_KEY_DISMISS_SUGGESTIONS, "Botón de descartar sugerencias");
        titles.put(PREF_KEY_CURSOR_TRACKPAD, "Modo trackpad de cursor");
        titles.put(PREF_KEY_HIDE_NUMBER_HINTS, "Ocultar números sobre letras");
        titles.put(PREF_KEY_CAT_CLIPBOARD, "Portapapeles");
        titles.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Retención extendida de historial");
        titles.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Límite de tiempo de retención (horas)");
        titles.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Aumentar límite de elementos no fijados");
        titles.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Límite de elementos no fijados");
        titles.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Diseño en cuadrícula del portapapeles");
        titles.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Columnas de cuadrícula del portapapeles");
        titles.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Límite de caracteres por elemento");
        titles.put(PREF_KEY_CAT_HAPTICS, "Vibración y respuesta háptica");
        titles.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibración independiente del teclado");
        titles.put(PREF_KEY_MODERN_HAPTICS, "Vibración háptica moderna");
        titles.put(PREF_KEY_CAT_SMART, "Funciones inteligentes y voz");
        titles.put(PREF_KEY_GRAMMAR_CHECKER, "Corrector gramatical y Redacción inteligente");
        titles.put(PREF_KEY_BLUETOOTH_MIC, "Micrófono Bluetooth");
        titles.put(PREF_KEY_CAT_PRIVACY, "Privacidad y seguridad");
        titles.put(PREF_KEY_FORCE_INCOGNITO, "Forzar modo incógnito");
        titles.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Ocultar ícono de incógnito");
        titles.put(PREF_KEY_VOICE_INCOGNITO, "Dictado por voz en incógnito");
        titles.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Portapapeles en incógnito");
    }

    @Override
    public void populateSummaries(Map<String, String> summaries) {
        summaries.put(PREF_KEY_HEADER, "Personalización y ajustes de parches");
        summaries.put(PREF_KEY_ENABLE_IME, "Gboard está deshabilitado en Android. Toca para habilitarlo en Administrar teclados.");
        summaries.put(PREF_KEY_SELECT_IME, "Gboard está habilitado pero no activo. Toca para elegir Gboard como teclado.");
        summaries.put(PREF_KEY_RESTART_GBOARD, "Toca para aplicar cambios (necesario para la mayoría de opciones)");
        summaries.put(PREF_KEY_AMOLED, "Forzar fondo negro puro (#000000) en temas oscuros");
        summaries.put(PREF_KEY_ZERO_BOTTOM_INSET, "Eliminar el margen inferior debajo del teclado en navegación por gestos");
        summaries.put(PREF_KEY_BOTTOM_PADDING, "Relleno del margen inferior en píxeles (0 para rasante total, predeterminado: 0)");
        summaries.put(PREF_KEY_KEY_SHAPE_SELECTION, "Habilitar estilos de teclas redondeadas y sin bordes en temas");
        summaries.put(PREF_KEY_EMOJI_SCALE, "Ajustar el tamaño visual de emojis en el teclado (50% - 150%)");
        summaries.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Habilitar barra de acceso y panel rediseñados (Panel V2)");
        summaries.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Cantidad máxima de íconos mostrados en la barra superior (predeterminado: 5)");
        summaries.put(PREF_KEY_DISMISS_SUGGESTIONS, "Mostrar botón de cerrar (X) en la barra de sugerencias");
        summaries.put(PREF_KEY_CURSOR_TRACKPAD, "Navegación de cursor bidireccional (2D) en barra espaciadora");
        summaries.put(PREF_KEY_HIDE_NUMBER_HINTS, "Oculta los números pequeños sobre la fila de letras. La pulsación larga sigue funcionando");
        summaries.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Habilitar límite de tiempo personalizado para elementos no fijados");
        summaries.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Horas de retención de elementos no fijados antes de eliminarlos (predeterminado: 24h)");
        summaries.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Habilitar límite personalizado para elementos no fijados en el historial");
        summaries.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Cantidad máxima de elementos no fijados en el portapapeles (predeterminado: 50)");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Habilitar diseño en múltiples columnas para el portapapeles");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Número de columnas en el portapapeles (1, 2 o 3. Predeterminado: 2)");
        summaries.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Máximo de caracteres guardados por elemento de texto, en miles (predeterminado: 20 mil). Reinicia Gboard para aplicar");
        summaries.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Mantener la vibración del teclado activa aunque la respuesta táctil del sistema Android esté desactivada");
        summaries.put(PREF_KEY_MODERN_HAPTICS, "Usar primitivas hápticas de Android (toque nítido) en vez de un zumbido simple. El control de intensidad pasa a medir fuerza. Reinicia Gboard para aplicar");
        summaries.put(PREF_KEY_GRAMMAR_CHECKER, "Revisión gramatical integrada y predicciones de Redacción inteligente");
        summaries.put(PREF_KEY_BLUETOOTH_MIC, "Habilitar entrada de audio por micrófono Bluetooth para dictado por voz");
        summaries.put(PREF_KEY_FORCE_INCOGNITO, "Operar siempre en modo incógnito (deshabilita el historial de entrada y aprendizaje)");
        summaries.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Ocultar el ícono de la máscara de incógnito en la barra de herramientas");
        summaries.put(PREF_KEY_VOICE_INCOGNITO, "Habilitar dictado por voz en campos privados y modo incógnito");
        summaries.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Habilitar historial del portapapeles y pegado en campos privados y modo incógnito");
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
            case PREF_KEY_EMOJI_SCALE:
                return value + " %";
            default:
                return String.valueOf(value);
        }
    }
}
