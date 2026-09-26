package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnVoid
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

val disableAvatarLiveStatusPatch = bytecodePatch(
    name = "Disable Profile Photo LIVE Status",
    description = "Removes the pulsing LIVE ring and badge from creator avatars in the feed and ensures clicking navigates strictly to the user profile instead of launching the live stream.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)

    execute {
        var patched = 0
        val wrapClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarAssemWrap;"
        val liveAssemClass = "Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarLiveAssem;"

        // 1. Discover and hook the author LIVE status validator (e.g. X.09A7.LIZIZ(Aweme, User)Z)
        var liveCheckClass = "LX/09A7;"
        var liveCheckMethod = "LIZIZ"

        val wrapFp = Fingerprint(definingClass = wrapClass)
        for (m in wrapFp.classDef.methods) {
            val insns = m.implementation?.instructions ?: continue
            for (insn in insns) {
                val ref = (insn as? ReferenceInstruction)?.reference?.toString() ?: continue
                if (ref.contains("(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Lcom/ss/android/ugc/aweme/profile/model/User;)Z")) {
                    val parts = ref.split("->")
                    if (parts.size == 2) {
                        liveCheckClass = parts[0]
                        liveCheckMethod = parts[1].substringBefore("(")
                        break
                    }
                }
            }
            if (liveCheckClass != "LX/09A7;") break
        }

        Fingerprint(
            definingClass = liveCheckClass,
            name = liveCheckMethod,
            returnType = "Z",
            parameters = listOf(
                "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
                "Lcom/ss/android/ugc/aweme/profile/model/User;",
            ),
        ).method.replaceWithReturnBoolean(false)
        println("[Disable Profile Photo LIVE Status] Hooked $liveCheckClass->$liveCheckMethod() -> false (all authors treated as non-live).")
        patched++

        // 2. Hook FeedAvatarAssemWrap.Yr()Z -> return false (prevents attaching FeedAvatarLiveAssem)
        Fingerprint(
            definingClass = wrapClass,
            returnType = "Z",
            parameters = emptyList(),
            custom = { m, _ ->
                m.implementation?.instructions?.any {
                    (it as? ReferenceInstruction)?.reference?.toString()?.contains("getCurrentProfileUserRoomId") == true
                } ?: false
            },
        ).method.replaceWithReturnBoolean(false)
        println("[Disable Profile Photo LIVE Status] Hooked FeedAvatarAssemWrap room check -> false.")
        patched++

        // 3. Hook FeedAvatarLiveAssem.ur()Z -> return false
        Fingerprint(
            definingClass = liveAssemClass,
            returnType = "Z",
            parameters = emptyList(),
            custom = { m, _ ->
                m.implementation?.instructions?.any {
                    (it as? ReferenceInstruction)?.reference?.toString()?.contains("getCurrentProfileUserRoomId") == true
                } ?: false
            },
        ).method.replaceWithReturnBoolean(false)
        println("[Disable Profile Photo LIVE Status] Hooked FeedAvatarLiveAssem.ur() -> false.")
        patched++

        // 4. Hook FeedAvatarLiveAssem.Ar(ZZ)V -> return-void (suppresses live entrance animations)
        Fingerprint(
            definingClass = liveAssemClass,
            returnType = "V",
            parameters = listOf("Z", "Z"),
        ).method.replaceWithReturnVoid()
        println("[Disable Profile Photo LIVE Status] Hooked FeedAvatarLiveAssem.Ar(ZZ) -> return-void.")
        patched++

        // 5. Hook FeedAvatarLiveAssem.onBind/z4(Object)V -> return-void (suppresses live UI binding and click redirection)
        Fingerprint(
            definingClass = liveAssemClass,
            returnType = "V",
            parameters = listOf("Ljava/lang/Object;"),
        ).method.replaceWithReturnVoid()
        println("[Disable Profile Photo LIVE Status] Hooked FeedAvatarLiveAssem.(Object)V -> return-void.")
        patched++

        println("[Disable Profile Photo LIVE Status] Successfully applied $patched hook(s).")
    }
}
