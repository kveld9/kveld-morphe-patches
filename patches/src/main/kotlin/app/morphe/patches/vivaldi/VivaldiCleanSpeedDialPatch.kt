package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.Constants

private val CLEAN_SPEED_DIAL_JSON = """
    {
      "version": "1",
      "children": [
        {
          "name": "Home",
          "title": "Home",
          "children": [
            {
              "name": "vivaldicom",
              "url": "https://vivaldi.com",
              "title": "Vivaldi Browser",
              "description": ""
            },
            {
              "name": "vivaldi-community",
              "url": "https://vivaldi.com",
              "title": "Vivaldi Community",
              "description": ""
            }
          ]
        }
      ]
    }
""".trimIndent()

private val EMPTY_PARTNERS_JSON = """
    {
      "folders": [],
      "bookmarks": []
    }
""".trimIndent()

private val vivaldiCleanSpeedDialResourcePatch = rawResourcePatch(
    name = "Clean Speed Dial Resource Layer",
    description = "Removes default commercial affiliate bookmarks JSONs while preserving valid partner database schema.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val bookmarksDir = get("assets/default-bookmarks")
        var resetFiles = 0

        if (bookmarksDir.exists() && bookmarksDir.isDirectory) {
            val specialFiles = setOf("partners.json", "partners-locale-map.json", "fallback-partner-ids.json")
            bookmarksDir.walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.name !in specialFiles }
                .forEach { jsonFile ->
                    jsonFile.writeText(CLEAN_SPEED_DIAL_JSON)
                    resetFiles++
                }

            val partnersFile = get("assets/default-bookmarks/partners.json")
            if (partnersFile.exists() && partnersFile.isFile) {
                partnersFile.writeText(EMPTY_PARTNERS_JSON)
                resetFiles++
            }

            val localeMapFile = get("assets/default-bookmarks/partners-locale-map.json")
            if (localeMapFile.exists() && localeMapFile.isFile) {
                localeMapFile.writeText("{}")
                resetFiles++
            }

            val fallbackFile = get("assets/default-bookmarks/fallback-partner-ids.json")
            if (fallbackFile.exists() && fallbackFile.isFile) {
                fallbackFile.writeText("{}")
                resetFiles++
            }
        }

        println("[Clean Speed Dial] Sanitized $resetFiles Speed Dial partner/bookmark JSON configs")
    }
}

@Suppress("unused")
val vivaldiCleanSpeedDialPatch = bytecodePatch(
    name = "Clean Speed Dial Bookmarks",
    description = "Removes sponsored commercial affiliate bookmarks, hides the 3-dot customize button, and disables phantom touch actions on the blank Start Page.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)
    dependsOn(vivaldiCleanSpeedDialResourcePatch)

    execute {
        // 1. Neutralize SpeedDialMainView touch interception on empty start page
        Fingerprint(
            definingClass = "Lorg/vivaldi/browser/speeddial/SpeedDialMainView;",
            name = "onInterceptTouchEvent",
            returnType = "Z",
            parameters = listOf("Landroid/view/MotionEvent;"),
        ).method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        // 2. Hide 3-dot customize button and add button from the Start Page
        // Locates SpeedDialTopLevelManager via unique SharedPreferences change listener signature
        val speedDialManagerMethod = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/content/SharedPreferences;", "Ljava/lang/String;"),
            strings = listOf("show_tab_strip", "show_customize_on_startpage", "show_speeddial_on_startpage"),
        ).method

        // Modifies updateButtonsVisibility on SpeedDialTopLevelManager
        Fingerprint(
            definingClass = speedDialManagerMethod.definingClass,
            returnType = "V",
            parameters = listOf("I"),
            strings = listOf("show_customize_on_startpage", "show_add_on_startpage"),
        ).method.addInstructions(
            0,
            """
                const/16 v0, 0x8
                iget-object v1, p0, ${speedDialManagerMethod.definingClass}->E:Landroid/view/View;
                if-eqz v1, :skip_e
                invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
                :skip_e
                iget-object v1, p0, ${speedDialManagerMethod.definingClass}->F:Landroid/view/View;
                if-eqz v1, :skip_f
                invoke-virtual {v1, v0}, Landroid/view/View;->setVisibility(I)V
                :skip_f
                return-void
            """,
        )

        println("[Clean Speed Dial] Hidden Speed Dial customization buttons and disabled touch interception")
    }
}
