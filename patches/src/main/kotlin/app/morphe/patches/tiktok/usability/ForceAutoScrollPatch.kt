package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val forceAutoScrollPatch = bytecodePatch(
    name = "Force auto-scroll",
    description = "Forces the activation of the native video auto-scroll experiment flag for accounts and regions that lack it due to A/B testing.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0

        // 1. Force native Feed Auto-Scroll A/B experiment flag (fyp_auto_scroll)
        try {
            val fypAutoScrollFp = Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("fyp_auto_scroll"),
            )
            fypAutoScrollFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] fyp_auto_scroll note: ${e.message}")
        }

        // 2. Force FypAutoScrollServiceImpl capability check (LJIILJJIL)
        try {
            val fypServiceFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/feed/platform/panel/autoscroll/FypAutoScrollServiceImpl;",
                name = "LJIILJJIL",
                returnType = "Z",
            )
            fypServiceFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] FypAutoScrollServiceImpl.LJIILJJIL note: ${e.message}")
        }

        // 3. Force tablet / foldable auto-scroll experiment flag (tablet_fyp_auto_scroll)
        try {
            val tabletAutoScrollFp = Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("tablet_fyp_auto_scroll"),
            )
            tabletAutoScrollFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] tablet_fyp_auto_scroll note: ${e.message}")
        }

        // 4. Force search results feed auto-scroll experiment flag (search_auto_scroll)
        try {
            val searchAutoScrollFp = Fingerprint(
                returnType = "Z",
                parameters = emptyList(),
                strings = listOf("search_auto_scroll"),
            )
            searchAutoScrollFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] search_auto_scroll note: ${e.message}")
        }

        // 5. Force panel_auto_scroll check on FeedBottomBarFacadeImpl
        try {
            val feedBottomBarFp = Fingerprint(
                definingClass = "Lcom/ss/android/ugc/aweme/feed/bottombar/FeedBottomBarFacadeImpl;",
                returnType = "Z",
                strings = listOf("panel_auto_scroll"),
            )
            feedBottomBarFp.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] FeedBottomBarFacadeImpl.LJIJJLI note: ${e.message}")
        }

        // 6. Ensure auto-scroll action item is created in the long-press / share menu
        try {
            val createItemFp = Fingerprint(
                strings = listOf("createAutoScrollItem, add auto scroll item to the menu"),
            )
            val createItemMethod = createItemFp.method
            val instructions = createItemMethod.implementation?.instructions?.toList() ?: emptyList()

            val panelStrIdx = instructions.indexOfFirst { ins ->
                ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string == "panel_auto_scroll"
            }
            if (panelStrIdx >= 0) {
                val moveResultIdx = (panelStrIdx until (panelStrIdx + 5)).firstOrNull { idx ->
                    instructions[idx].opcode.name.startsWith("move-result")
                }
                if (moveResultIdx != null) {
                    val reg = (instructions[moveResultIdx] as OneRegisterInstruction).registerA
                    createItemMethod.replaceInstruction(moveResultIdx, "const/4 v$reg, 1")
                }
            }

            val isLoginIdx = instructions.indexOfFirst { ins ->
                ((ins as? ReferenceInstruction)?.reference as? MethodReference)?.name == "isLogin"
            }
            if (isLoginIdx >= 0) {
                val moveResultIdx = (isLoginIdx until (isLoginIdx + 5)).firstOrNull { idx ->
                    instructions[idx].opcode.name.startsWith("move-result")
                }
                if (moveResultIdx != null) {
                    val reg = (instructions[moveResultIdx] as OneRegisterInstruction).registerA
                    createItemMethod.replaceInstruction(moveResultIdx, "const/4 v$reg, 1")
                }
            }
            patched++
        } catch (e: Exception) {
            println("[Force auto-scroll] createAutoScrollItem note: ${e.message}")
        }

        println("[Force auto-scroll] Applied $patched auto-scroll hook(s) -> Native auto-scroll permanently forced.")
    }
}
