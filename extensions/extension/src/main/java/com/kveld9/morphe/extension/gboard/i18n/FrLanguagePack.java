package com.kveld9.morphe.extension.gboard.i18n;

import java.util.Map;
import static com.kveld9.morphe.extension.gboard.GboardExtension.*;

public class FrLanguagePack extends BaseLanguagePack {
    @Override
    public String getLanguageCode() {
        return "fr";
    }

    @Override
    public String getLanguageName() {
        return "Français";
    }

    @Override
    public void populateTitles(Map<String, String> titles) {
        titles.put(PREF_KEY_HEADER, "Patchs Morphe");
        titles.put(PREF_KEY_SCREEN, "Patchs Morphe");
        titles.put(PREF_KEY_CAT_ACTIONS, "Actions et état");
        titles.put(PREF_KEY_ENABLE_IME, "Activer Gboard");
        titles.put(PREF_KEY_SELECT_IME, "Choisir Gboard");
        titles.put(PREF_KEY_RESTART_GBOARD, "Redémarrer Gboard");
        titles.put(PREF_KEY_CAT_APPEARANCE, "Apparence et thème");
        titles.put(PREF_KEY_AMOLED, "Thème AMOLED pur");
        titles.put(PREF_KEY_ZERO_BOTTOM_INSET, "Pas d'espace en bas");
        titles.put(PREF_KEY_BOTTOM_PADDING, "Espace en bas (px)");
        titles.put(PREF_KEY_HIDE_IME_NAV_BAR, "Masquer la barre système");
        titles.put(PREF_KEY_KEY_SHAPE_SELECTION, "Formes des touches");
        titles.put(PREF_KEY_CAT_TOOLBAR, "Barre d'outils et navigation");
        titles.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Nouveau menu de barre");
        titles.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Icônes de barre");
        titles.put(PREF_KEY_DISMISS_SUGGESTIONS, "Bouton fermer suggestions");
        titles.put(PREF_KEY_CURSOR_TRACKPAD, "Pavé tactile curseur");
        titles.put(PREF_KEY_CAT_CLIPBOARD, "Presse-papiers");
        titles.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Historique plus long");
        titles.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Garder pendant (heures)");
        titles.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Plus d'éléments non épinglés");
        titles.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Limite non épinglés");
        titles.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Vue grille");
        titles.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Colonnes grille");
        titles.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Limite de texte");
        titles.put(PREF_KEY_CAT_HAPTICS, "Vibration");
        titles.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibration propre");
        titles.put(PREF_KEY_MODERN_HAPTICS, "Vibration moderne");
        titles.put(PREF_KEY_CAT_SMART, "Intelligent et voix");
        titles.put(PREF_KEY_GRAMMAR_CHECKER, "Correcteur grammatical");
        titles.put(PREF_KEY_BLUETOOTH_MIC, "Micro Bluetooth");
        titles.put(PREF_KEY_CAT_PRIVACY, "Confidentialité");
        titles.put(PREF_KEY_FORCE_INCOGNITO, "Toujours privée");
        titles.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Masquer l'icône de navigation privée");
        titles.put(PREF_KEY_VOICE_INCOGNITO, "Voix en privé");
        titles.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Presse-papiers en privé");
    }

    @Override
    public void populateSummaries(Map<String, String> summaries) {
        summaries.put(PREF_KEY_HEADER, "Réglages des patchs");
        summaries.put(PREF_KEY_ENABLE_IME, "Gboard est désactivé. Touchez pour l'activer.");
        summaries.put(PREF_KEY_SELECT_IME, "Gboard est activé mais pas choisi. Touchez pour le choisir.");
        summaries.put(PREF_KEY_RESTART_GBOARD, "Touchez pour appliquer.");
        summaries.put(PREF_KEY_AMOLED, "Fond noir pur sur thèmes sombres.");
        summaries.put(PREF_KEY_ZERO_BOTTOM_INSET, "Supprime l'espace sous le clavier.");
        summaries.put(PREF_KEY_BOTTOM_PADDING, "Espace en bas, en pixels. 0 = aucun.");
        summaries.put(PREF_KEY_HIDE_IME_NAV_BAR, "Masque la barre système sous le clavier (Android 13+).");
        summaries.put(PREF_KEY_KEY_SHAPE_SELECTION, "Touches arrondies et sans bordure.");
        summaries.put(PREF_KEY_ACCESS_POINTS_REDESIGN, "Nouveau menu de barre et panneau.");
        summaries.put(PREF_KEY_TOOLBAR_ITEM_COUNT, "Icônes max en haut (défaut 5).");
        summaries.put(PREF_KEY_DISMISS_SUGGESTIONS, "Affiche un X pour fermer les suggestions.");
        summaries.put(PREF_KEY_CURSOR_TRACKPAD, "Déplace le curseur sur la barre espace.");
        summaries.put(PREF_KEY_CLIPBOARD_EXTENDED_RETENTION, "Durée perso pour non épinglés.");
        summaries.put(PREF_KEY_CLIPBOARD_RETENTION_HOURS, "Heures avant suppression (défaut 24).");
        summaries.put(PREF_KEY_CLIPBOARD_RAISE_LIMIT, "Limite perso des non épinglés.");
        summaries.put(PREF_KEY_CLIPBOARD_UNPINNED_LIMIT, "Max de non épinglés (défaut 50).");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_LAYOUT, "Presse-papiers en colonnes.");
        summaries.put(PREF_KEY_CLIPBOARD_GRID_COLUMNS, "Colonnes : 1, 2 ou 3 (défaut 2).");
        summaries.put(PREF_KEY_CLIPBOARD_CHAR_LIMIT, "Max par texte, en milliers. Redémarrez pour appliquer.");
        summaries.put(PREF_KEY_DECOUPLE_TOUCH_FEEDBACK, "Vibre même si le système est coupé.");
        summaries.put(PREF_KEY_MODERN_HAPTICS, "Toucher net au lieu du bourdonnement. Redémarrez pour appliquer.");
        summaries.put(PREF_KEY_GRAMMAR_CHECKER, "Correction et prédictions.");
        summaries.put(PREF_KEY_BLUETOOTH_MIC, "Dicte avec micro Bluetooth.");
        summaries.put(PREF_KEY_FORCE_INCOGNITO, "Toujours privé : sans historique.");
        summaries.put(PREF_KEY_HIDE_INCOGNITO_ICON, "Masque l'icône privée.");
        summaries.put(PREF_KEY_VOICE_INCOGNITO, "Voix dans champs privés.");
        summaries.put(PREF_KEY_CLIPBOARD_INCOGNITO, "Presse-papiers dans champs privés.");
    }

    @Override
    public String getRestartToast() {
        return "Redémarrez Gboard pour appliquer les modifications";
    }

    @Override
    public String getRestartingToast() {
        return "Redémarrage de Gboard...";
    }

    @Override
    public String getRestartTitle(boolean pending) {
        if (pending) {
            return "Redémarrer Gboard (redémarrage en attente)";
        }
        return "Redémarrer le processus Gboard";
    }

    @Override
    public String getRestartSummary(boolean pending) {
        if (pending) {
            return "Modifications en attente ! Touchez ici pour redémarrer Gboard et les appliquer.";
        }
        return "Touchez pour appliquer les modifications (nécessaire pour la plupart des options)";
    }

    @Override
    public String formatUnit(String prefKey, int value) {
        switch (prefKey) {
            case PREF_KEY_BOTTOM_PADDING:
                return value + " px";
            case PREF_KEY_TOOLBAR_ITEM_COUNT:
                return value + (value == 1 ? " icône" : " icônes");
            case PREF_KEY_CLIPBOARD_RETENTION_HOURS:
                return value + " h";
            case PREF_KEY_CLIPBOARD_UNPINNED_LIMIT:
                return value + (value == 1 ? " élément" : " éléments");
            case PREF_KEY_CLIPBOARD_GRID_COLUMNS:
                return value + (value == 1 ? " colonne" : " colonnes");
            case PREF_KEY_CLIPBOARD_CHAR_LIMIT:
                return value + " mille caractères";
            default:
                return String.valueOf(value);
        }
    }
}
