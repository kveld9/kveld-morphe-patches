package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnVoid

private const val CREATE_GROUP_BUTTON_ASSEM_CLASS =
    "Lcom/ss/android/ugc/aweme/internalshare/impl/refactor/assem/SharePanelCreateGroupButtonAssem;"
private const val GROUP_CHAT_HINT_ASSEM_CLASS =
    "Lcom/ss/android/ugc/aweme/internalshare/impl/refactor/assem/SharePanelGroupChatHintAssem;"

val cleanSharePanelPatch = bytecodePatch(
    name = "Clean Share Panel",
    description = "Removes clutter from the share panel and direct message dialog, including suggested quick emojis and the 'Send to new group' button.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    val hideQuickEmojis by booleanOption(
        key = "hideQuickEmojis",
        default = true,
        title = "Hide Quick Emojis",
        description = "Removes the horizontal row of suggested quick emojis from the direct share panel.",
        required = false,
    )

    val hideSendToNewGroup by booleanOption(
        key = "hideSendToNewGroup",
        default = true,
        title = "Hide 'Send to New Group'",
        description = "Removes the 'Send to new group' button and hint from the direct share panel.",
        required = false,
    )

    execute {
        if (hideQuickEmojis != true && hideSendToNewGroup != true) {
            println("[Clean Share Panel] Both toggles disabled -> nothing to patch.")
            return@execute
        }

        var patched = 0

        // 1. Feature: Hide suggested quick emojis
        if (hideQuickEmojis == true) {
            // Neutralize emoji provider to return an empty list across all share panel views
            val emojiClinitFp = Fingerprint(
                name = "<clinit>",
                returnType = "V",
                strings = listOf("\uD83E\uDD70", "\uD83D\uDC4D", "\uD83D\uDE02", "\uD83D\uDE0E"),
            )
            val emojiClass = emojiClinitFp.classDef
            val getEmojisMethod = emojiClass.methods.first {
                it.returnType == "Ljava/util/List;" && it.parameters.isEmpty()
            }
            val impl = getEmojisMethod.implementation
            if (impl != null) {
                getEmojisMethod.clearTryBlocks()
                getEmojisMethod.ensureRegisterCount(1)
                getEmojisMethod.removeInstructions(0, impl.instructions.count())
                getEmojisMethod.addInstructions(
                    0,
                    """
                        invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                        move-result-object v0
                        return-object v0
                    """.trimIndent(),
                )
                println("[Clean Share Panel] Hooked ${emojiClass.type}.${getEmojisMethod.name} -> return Collections.emptyList().")
                patched++
            }
        }

        // 2. Feature: Hide "Send to new group" button and hint in the share panel
        if (hideSendToNewGroup == true) {
            // 2a. Hide SharePanelCreateGroupButtonAssem view and neutralize state updates
            val createGroupAssemFp = Fingerprint(
                definingClass = CREATE_GROUP_BUTTON_ASSEM_CLASS,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            )
            val createGroupMethod = createGroupAssemFp.method
            val createGroupImpl = createGroupMethod.implementation
            if (createGroupImpl != null) {
                createGroupMethod.clearTryBlocks()
                createGroupMethod.ensureRegisterCount(2)
                createGroupMethod.removeInstructions(0, createGroupImpl.instructions.count())
                createGroupMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Clean Share Panel] Hooked SharePanelCreateGroupButtonAssem.onViewCreated -> View.GONE.")
                patched++
            }

            Fingerprint(
                definingClass = CREATE_GROUP_BUTTON_ASSEM_CLASS,
                name = "ce",
                returnType = "V",
                parameters = listOf("Z"),
            ).method.replaceWithReturnVoid()
            println("[Clean Share Panel] Hooked SharePanelCreateGroupButtonAssem.ce -> return-void.")
            patched++

            // 2b. Hide SharePanelGroupChatHintAssem view and neutralize state updates
            val groupChatHintFp = Fingerprint(
                definingClass = GROUP_CHAT_HINT_ASSEM_CLASS,
                name = "onViewCreated",
                returnType = "V",
                parameters = listOf("Landroid/view/View;"),
            )
            val groupChatHintMethod = groupChatHintFp.method
            val hintImpl = groupChatHintMethod.implementation
            if (hintImpl != null) {
                groupChatHintMethod.clearTryBlocks()
                groupChatHintMethod.ensureRegisterCount(2)
                groupChatHintMethod.removeInstructions(0, hintImpl.instructions.count())
                groupChatHintMethod.addInstructions(
                    0,
                    """
                        move-object/from16 v0, p1
                        const/16 v1, 0x8
                        invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
                        return-void
                    """.trimIndent(),
                )
                println("[Clean Share Panel] Hooked SharePanelGroupChatHintAssem.onViewCreated -> View.GONE.")
                patched++
            }

            Fingerprint(
                definingClass = GROUP_CHAT_HINT_ASSEM_CLASS,
                name = "ce",
                returnType = "V",
                parameters = listOf("Z"),
            ).method.replaceWithReturnVoid()
            println("[Clean Share Panel] Hooked SharePanelGroupChatHintAssem.ce -> return-void.")
            patched++
        }

        println("[Clean Share Panel] Applied $patched hooks -> share panel cleaned.")
    }
}
