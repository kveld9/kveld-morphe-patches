package app.morphe.patches.nokoprint

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val nokoPrintTelemetryResourcePatch = resourcePatch(
    name = "NokoPrint Telemetry Manifest Purge",
    description = "Strips advertising and install referrer permissions, and disables Google Firebase/Measurement components in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[NokoPrint Telemetry Manifest Purge] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val blockedPermissions = setOf(
            "com.google.android.gms.permission.AD_ID",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE",
        )

        val blockedComponents = setOf(
            "com.google.android.gms.measurement.AppMeasurementService",
            "com.google.android.gms.measurement.AppMeasurementJobService",
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            "com.google.firebase.provider.FirebaseInitProvider",
            "com.google.firebase.sessions.SessionLifecycleService",
        )

        var removedPermissions = 0
        var disabledComponents = 0

        document(manifestFile.absolutePath).use { doc ->
            // 1. Remove tracking & ads permissions
            val usesPermissions = doc.getElementsByTagName("uses-permission")
            val permsToRemove = mutableListOf<Element>()
            for (i in 0 until usesPermissions.length) {
                val elem = usesPermissions.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name in blockedPermissions) {
                    permsToRemove.add(elem)
                }
            }
            permsToRemove.forEach {
                it.parentNode?.removeChild(it)
                removedPermissions++
            }

            // 2. Disable measurement & analytics components
            val tags = listOf("service", "receiver", "provider")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val elem = elements.item(i) as? Element ?: continue
                    val name = elem.getAttribute("android:name")
                    if (name in blockedComponents) {
                        elem.setAttribute("android:enabled", "false")
                        disabledComponents++
                    }
                }
            }
        }

        println("[NokoPrint Telemetry Manifest Purge] Stripped $removedPermissions permissions, disabled $disabledComponents analytics components.")
    }
}

@Suppress("unused")
val nokoPrintBlockTelemetryPatch = bytecodePatch(
    name = "NokoPrint Block Telemetry & Trackers",
    description = "Neutralizes Firebase Analytics and Google Measurement tracking and event dispatching.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)
    dependsOn(nokoPrintTelemetryResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Stub FirebaseAnalytics.a(String, Bundle)V (logEvent)
        Fingerprint(
            definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
            name = "a",
            parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("FirebaseAnalytics.a(logEvent)")
        }

        // 2. Stub FirebaseAnalytics.b(Bundle) default parameters
        Fingerprint(
            definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
            name = "b",
            parameters = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("FirebaseAnalytics.b(setDefaultEventParameters)")
        }

        // 3. Stub AppMeasurement.logEventInternal
        Fingerprint(
            definingClass = "Lcom/google/android/gms/measurement/AppMeasurement;",
            name = "logEventInternal",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Landroid/os/Bundle;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("AppMeasurement.logEventInternal")
        }

        // 4. Stub com.nokoprint.App.B(Throwable)V (Crashlytics reporting)
        Fingerprint(
            definingClass = "Lcom/nokoprint/App;",
            name = "B",
            parameters = listOf("Ljava/lang/Throwable;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("App.B(crashlyticsReport)")
        }

        // 5. Stub com.nokoprint.App.C(Throwable, String)V (Crashlytics reporting)
        Fingerprint(
            definingClass = "Lcom/nokoprint/App;",
            name = "C",
            parameters = listOf("Ljava/lang/Throwable;", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("App.C(crashlyticsLog)")
        }

        println("[NokoPrint Block Telemetry] Neutralized ${hookedMethods.size} telemetry dispatch methods.")
    }
}
