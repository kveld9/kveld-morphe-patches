package app.morphe.patches.tiktok.usability

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.getReference
import app.morphe.patches.shared.replaceWithReturnBoolean
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val profileBannerPatch = bytecodePatch(
    name = "Enable Profile Banner",
    description = "Unlocks the profile banner and its editing tools.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_TIKTOK)

    execute {
        var patched = 0

        // 1. Hook ProfileBackgroundExp gate (LX/0OSK;->LIZJ(Z)Z) -> true
        val expFp = Fingerprint(
            strings = listOf("ProfileBackgroundExp", "profile_bg_in_allow_list"),
        )
        val expClass = expFp.classDef
        val gateMethod = expClass.methods.first { m ->
            m.returnType == "Z" && m.parameterTypes.size == 1 && m.parameterTypes[0] == "Z"
        }
        gateMethod.replaceWithReturnBoolean(true)
        println("[Enable Profile Banner] Hooked ${expClass.type}->${gateMethod.name}(Z)Z -> true (ProfileBackgroundExp gate enabled).")
        patched++

        // 2. Hook Snapshot constructor so allowListValue (abValue) is always 1
        // LX/0OSK;->LIZ()LX/0OSM; returns a snapshot instance passed to ProfilePlatformViewModel
        // to populate the Retrofit query 'profile_bg_in_allow_list'. Forcing the snapshot's
        // allowListValue to 1 prevents the TikTok backend from stripping the profile background.
        val snapshotMethod = expClass.methods.first { m ->
            m.parameterTypes.isEmpty() && m.returnType.startsWith("L")
        }
        val snapshotClass = mutableClassDefBy(snapshotMethod.returnType)
        val snapshotConstructor = snapshotClass.methods.first { m ->
            m.name == "<init>" && m.parameterTypes.size == 2 && m.parameterTypes[1] == "I"
        }
        snapshotConstructor.addInstructions(
            0,
            """
                const/4 p2, 0x1
            """.trimIndent(),
        )
        println("[Enable Profile Banner] Hooked ${snapshotClass.type}-><init>(Ljava/lang/String;I)V -> forced allowListValue (p2) to 1.")
        patched++

        // 3. Hook ProfileBackgroundComponent: replace forced A/B gate with real banner discriminator across all visibility sites
        val bgComponentType = "Lcom/ss/android/ugc/profile/platform/business/background/ProfileBackgroundComponent;"
        val bgComponentClass = mutableClassDefBy(bgComponentType)

        val discriminators = bgComponentClass.methods.filter { m ->
            m.returnType == "Z" && m.parameterTypes.isEmpty() &&
                m.implementation?.instructions?.any { ins ->
                    ins.getReference<MethodReference>()?.name == "hasBackground"
                } == true
        }
        check(discriminators.size == 1) { "Expected exactly one hasBackground() discriminator in $bgComponentType, found ${discriminators.size}" }
        val discriminator = discriminators.single()

        var hooked = 0
        for (method in bgComponentClass.methods) {
            val instructions = method.implementation?.instructions?.toList() ?: continue
            for (i in 0 until instructions.size - 4) {
                val ins0 = instructions[i]
                if (ins0.opcode != Opcode.INVOKE_STATIC) continue
                val ref0 = ins0.getReference<MethodReference>() ?: continue
                if (ref0.parameterTypes != listOf("Z") || ref0.returnType != "Z") continue

                val ins1 = instructions[i + 1]
                if (ins1.opcode != Opcode.MOVE_RESULT) continue

                val ins2 = instructions[i + 2]
                if (ins2.opcode != Opcode.IF_EQZ) continue

                val ins3 = instructions[i + 3]
                if (ins3.opcode != Opcode.CONST_4) continue
                if ((ins3 as? NarrowLiteralInstruction)?.narrowLiteral != 0) continue

                val ins4 = instructions[i + 4]
                if (ins4.opcode != Opcode.INVOKE_STATIC) continue
                val ref4 = ins4.getReference<MethodReference>() ?: continue
                if (ref4.parameterTypes != listOf("I", "Landroid/widget/FrameLayout;")) continue

                method.replaceInstruction(i, "invoke-virtual/range {p0 .. p0}, $bgComponentType->${discriminator.name}()Z")
                println("[Enable Profile Banner] Hooked $bgComponentType->${method.name}()V -> forced A/B gate replaced with ${discriminator.name}()Z banner discriminator.")
                hooked++
                patched++
            }
        }
        check(hooked > 0) { "No banner visibility site found in $bgComponentType" }

        if (patched == 0) {
            println("[Enable Profile Banner] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Enable Profile Banner] Applied $patched hook(s) -> profile banner enabled.")
        }
    }
}
