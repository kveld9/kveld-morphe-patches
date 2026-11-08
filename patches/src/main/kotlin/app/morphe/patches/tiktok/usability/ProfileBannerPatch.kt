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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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

        // 3. Hook ProfileBackgroundComponent.zj()V: replace forced A/B gate with real banner discriminator
        val bgComponentType = "Lcom/ss/android/ugc/profile/platform/business/background/ProfileBackgroundComponent;"
        val bgComponentClass = mutableClassDefBy(bgComponentType)
        check(bgComponentClass.methods.any { it.name == "x22" && it.returnType == "Z" && it.parameterTypes.isEmpty() }) {
            "Class $bgComponentType is missing discriminator method x22()Z"
        }

        val zjMethod = bgComponentClass.methods.firstOrNull { m ->
            m.name == "zj" && m.returnType == "V" && m.parameterTypes.isEmpty()
        }
        check(zjMethod != null) {
            "Method zj()V not found in class $bgComponentType"
        }

        val instructions = zjMethod.implementation?.instructions?.toList()
        check(instructions != null) {
            "Method zj()V in class $bgComponentType has no implementation"
        }

        val gateCallSites = instructions.mapIndexedNotNull { index, ins ->
            if (ins.opcode != Opcode.INVOKE_STATIC) return@mapIndexedNotNull null
            val ref = ins.getReference<MethodReference>() ?: return@mapIndexedNotNull null
            if (ref.definingClass == "LX/0O6A;" &&
                ref.name == "LJFF" &&
                ref.parameterTypes == listOf("Z") &&
                ref.returnType == "Z"
            ) {
                index
            } else {
                null
            }
        }
        check(gateCallSites.size == 1) {
            "Expected exactly 1 call to LX/0O6A;->LJFF(Z)Z in $bgComponentType->zj()V, found ${gateCallSites.size}"
        }
        val gateIndex = gateCallSites.first()

        check(gateIndex + 4 < instructions.size) {
            "Not enough instructions after LX/0O6A;->LJFF(Z)Z in $bgComponentType->zj()V for shape guard (need 4, have ${instructions.size - gateIndex - 1})"
        }

        val moveResult = instructions[gateIndex + 1]
        check(moveResult.opcode == Opcode.MOVE_RESULT && moveResult is OneRegisterInstruction) {
            "Expected MOVE_RESULT (OneRegisterInstruction) at index ${gateIndex + 1} in $bgComponentType->zj()V, found ${moveResult.opcode}"
        }

        val ifEqz = instructions[gateIndex + 2]
        check(ifEqz.opcode == Opcode.IF_EQZ) {
            "Expected IF_EQZ at index ${gateIndex + 2} in $bgComponentType->zj()V, found ${ifEqz.opcode}"
        }

        val constZero = instructions[gateIndex + 3]
        check(constZero.opcode == Opcode.CONST_4 && (constZero as? NarrowLiteralInstruction)?.narrowLiteral == 0) {
            "Expected CONST_4 with narrowLiteral 0 at index ${gateIndex + 3} in $bgComponentType->zj()V, found ${constZero.opcode}"
        }

        val visibilityInvoke = instructions[gateIndex + 4]
        val visibilityRef = visibilityInvoke.getReference<MethodReference>()
        check(
            visibilityInvoke.opcode.name.startsWith("INVOKE") &&
                visibilityRef != null &&
                visibilityRef.name == "LJLLL" &&
                visibilityRef.parameterTypes == listOf("I", "Landroid/widget/FrameLayout;")
        ) {
            "Expected invoke with reference LJLLL(I, Landroid/widget/FrameLayout;) at index ${gateIndex + 4} in $bgComponentType->zj()V, found ${visibilityRef?.name}(${visibilityRef?.parameterTypes?.joinToString()})"
        }

        zjMethod.replaceInstruction(gateIndex, "invoke-virtual/range {p0 .. p0}, $bgComponentType->x22()Z")
        println("[Enable Profile Banner] Hooked $bgComponentType->zj()V -> A/B gate LJFF(Z)Z replaced with x22()Z discriminator.")
        patched++

        if (patched == 0) {
            println("[Enable Profile Banner] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Enable Profile Banner] Applied $patched hook(s) -> profile banner enabled.")
        }
    }
}
