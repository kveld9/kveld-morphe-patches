package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val xiaomiEarbudsAntiTamperBypassPatch = bytecodePatch(
    name = "Xiaomi Earbuds Anti-Tamper Bypass",
    description = "Bypasses Xposed and hook detection, neutralizes VPN and ADB security checks, bypasses root and emulator detection, and hardens WebView JavaScript bridges.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Bypass Xposed, Substrate, and memory map hook detection (g9.a)
        val g9AClass = "Lg9/a;"
        Fingerprint(
            definingClass = g9AClass,
            name = "a",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.a.a(Context)")
        }

        Fingerprint(
            definingClass = g9AClass,
            name = "b",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.a.b(mapsCheck)")
        }

        Fingerprint(
            definingClass = g9AClass,
            name = "c",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.a.c(pkgCheck)")
        }

        Fingerprint(
            definingClass = g9AClass,
            name = "d",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.a.d(stackCheck)")
        }

        // 2. Bypass Root Detection (g9.b.i)
        Fingerprint(
            definingClass = "Lg9/b;",
            name = "i",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.b.i(rootCheck)")
        }

        // 3. Neutralize VirtualApp / sandbox check (g9.c.e)
        Fingerprint(
            definingClass = "Lg9/c;",
            name = "e",
            parameters = listOf(),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.c.e(virtualAppCheck)")
        }

        // 4. Bypass Emulator Detection in g9.g
        val g9GClass = "Lg9/g;"
        Fingerprint(
            definingClass = g9GClass,
            name = "c",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.g.c(emulatorFiles)")
        }

        Fingerprint(
            definingClass = g9GClass,
            name = "d",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.g.d(emulatorProps)")
        }

        Fingerprint(
            definingClass = g9GClass,
            name = "f",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("g9.g.f(qemuCheck)")
        }

        // 5. Neutralize SensorHelper environment checks
        val sensorHelperClass = "Lcom/xiaomi/verificationsdk/internal/SensorHelper;"

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "A",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.A(substrateCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "C",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.C(vpnCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "D",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.D(xposedCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "t",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.t(emulatorOperatorCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "u",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.u(adbCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "y",
            parameters = listOf(),
            returnType = "I",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("SensorHelper.y(rootCheck)")
        }

        Fingerprint(
            definingClass = sensorHelperClass,
            name = "onSensorChanged",
            parameters = listOf("Landroid/hardware/SensorEvent;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("SensorHelper.onSensorChanged")
        }

        // 6. Bypass root and emulator detection in fitness common utils
        val fitnessUtilsEClass = "Lcom/xiaomi/fitness/common/utils/e;"

        Fingerprint(
            definingClass = fitnessUtilsEClass,
            name = "z",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("e.z(emulatorCheck)")
        }

        Fingerprint(
            definingClass = fitnessUtilsEClass,
            name = "F",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z",
        ).method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0
                    return v0
                """.trimIndent(),
            )
            hookedMethods.add("e.F(rootCheck)")
        }

        // 6. Harden WebView JavaScript bridges against sensitive data leakage
        val jsbClasses = listOf(
            "PassportJsbMethodGetDeviceInfo" to "Lcom/xiaomi/passport/jsb/method_impl/PassportJsbMethodGetDeviceInfo;",
            "PassportJsbMethodGetSIMInfo" to "Lcom/xiaomi/passport/jsb/method_impl/PassportJsbMethodGetSIMInfo;",
            "PassportJsbMethodGetSystemInfo" to "Lcom/xiaomi/passport/jsb/method_impl/PassportJsbMethodGetSystemInfo;",
            "PassportJsbMethodListenInboxSms" to "Lcom/xiaomi/passport/jsb/method_impl/PassportJsbMethodListenInboxSms;",
        )

        for ((name, definingClass) in jsbClasses) {
            Fingerprint(
                definingClass = definingClass,
                name = "invoke",
                parameters = listOf(
                    "Lcom/xiaomi/passport/webview/PassportJsbWebView;",
                    "Lorg/json/JSONObject;",
                ),
                returnType = "Lcom/xiaomi/passport/jsb/PassportJsbMethodResult;",
            ).method.apply {
                addInstructions(
                    0,
                    """
                        new-instance v0, Lorg/json/JSONObject;
                        invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V
                        new-instance v1, Lcom/xiaomi/passport/jsb/PassportJsbMethodResult;
                        invoke-direct {v1, v0}, Lcom/xiaomi/passport/jsb/PassportJsbMethodResult;-><init>(Lorg/json/JSONObject;)V
                        return-object v1
                    """.trimIndent(),
                )
                hookedMethods.add("$name.invoke")
            }
        }

        println("[Xiaomi Earbuds Anti-Tamper Bypass] Neutralized anti-tamper, root/emulator checks, and JS bridges across ${hookedMethods.size} targets.")
    }
}
