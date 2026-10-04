# Gboard Morphe Patches Localization (i18n)

This directory contains language packs for the in-app Morphe Patches settings screen in Gboard.
Language detection is automatic based on the user's Android system locale and per-app language preferences. If a translation for the user's language is not registered, Gboard cleanly falls back to English.

## How to add a new language translation via PR

To contribute a new language translation (e.g. French `fr`, German `de`, Portuguese `pt`, Italian `it`, etc.):

1. **Create your language pack class**:
   Create a new file in this directory named `<Lang>LanguagePack.java` (e.g. `FrLanguagePack.java` for French, `DeLanguagePack.java` for German).

2. **Extend `BaseLanguagePack`**:
   ```java
   package com.kveld9.morphe.extension.gboard.i18n;

   import java.util.Map;
   import static com.kveld9.morphe.extension.gboard.GboardExtension.*;

   public class FrLanguagePack extends BaseLanguagePack {
       @Override
       public String getLanguageCode() {
           return "fr"; // ISO 639-1 code
       }

       @Override
       public String getLanguageName() {
           return "Français";
       }

       @Override
       public void populateTitles(Map<String, String> titles) {
           titles.put(PREF_KEY_HEADER, "Patchs Morphe");
           // Add translated titles...
       }

       @Override
       public void populateSummaries(Map<String, String> summaries) {
           summaries.put(PREF_KEY_HEADER, "Personnalisation et options de patchs");
           // Add translated summaries...
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
       public String getRestartSummary(boolean pending) {
           if (pending) {
               return "Modifications en attente ! Appuyez ici pour redémarrer Gboard.";
           }
           return "Appuyez pour appliquer les modifications";
       }

       @Override
       public String formatUnit(String prefKey, int value) {
           switch (prefKey) {
               case PREF_KEY_TOOLBAR_ITEM_COUNT:
                   return value + (value == 1 ? " icône" : " icônes");
               case PREF_KEY_CLIPBOARD_UNPINNED_LIMIT:
                   return value + (value == 1 ? " élément" : " éléments");
               case PREF_KEY_CLIPBOARD_GRID_COLUMNS:
                   return value + (value == 1 ? " colonne" : " colonnes");
               default:
                   return super.formatUnit(prefKey, value);
           }
       }
   }
   ```

3. **Register your pack in `GboardI18n.java`**:
   Add a single registration line in the `static` initializer of `GboardI18n.java`:
   ```java
   register(new FrLanguagePack());
   ```

4. **Verify the build**:
   ```bash
   ./gradlew check
   ```

5. **Submit your Pull Request** with commit title:
   `feat(gboard): add <language> localization for settings menu`
