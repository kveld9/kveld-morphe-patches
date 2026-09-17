package app.morphe.patches.chromium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnNull

@Suppress("unused")
val sensorPrivacyPatch = bytecodePatch(
    name = "Sensor Privacy Guard",
    description = "Neutralizes motion, ambient, and orientation sensor providers to prevent hardware fingerprinting and tracking via Generic Sensor APIs.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE, Constants.COMPATIBILITY_VIVALDI)

    execute {
        var patched = 0
        val hookedMethods = mutableListOf<String>()

        // 1. Neutralize PlatformSensorProvider.hasSensorType(int) -> return false
        try {
            Fingerprint(
                definingClass = "Lorg/chromium/device/sensors/PlatformSensorProvider;",
                name = "hasSensorType",
                returnType = "Z",
                parameters = listOf("I"),
            ).method.apply {
                replaceWithReturnBoolean(false)
                hookedMethods.add("PlatformSensorProvider.hasSensorType")
                patched++
            }
        } catch (e: Exception) {
            println("[Sensor Privacy Guard] PlatformSensorProvider.hasSensorType hook note: ${e.message}")
        }

        // 2. Neutralize PlatformSensor.create(PlatformSensorProvider, int, long) -> return null
        // Note: PlatformSensorProvider.create() must NOT return null as C++ PlatformSensorProviderAndroid
        // binds its JNI receiver to it; returning null triggers SIGSEGV / JNI abort on hasSensorType calls.
        try {
            Fingerprint(
                definingClass = "Lorg/chromium/device/sensors/PlatformSensor;",
                name = "create",
                parameters = listOf("Lorg/chromium/device/sensors/PlatformSensorProvider;", "I", "J"),
            ).method.apply {
                replaceWithReturnNull()
                hookedMethods.add("PlatformSensor.create")
                patched++
            }
        } catch (e: Exception) {
            println("[Sensor Privacy Guard] PlatformSensor.create hook note: ${e.message}")
        }

        println("[Sensor Privacy Guard] Neutralized $patched sensor provider hooks (${hookedMethods.joinToString(", ")}) -> motion/ambient sensor fingerprinting disabled.")
    }
}
