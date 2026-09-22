package app.morphe.patches.xiaomi.earbuds

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val xiaomiEarbudsTelemetryResourcePatch = resourcePatch(
    name = "Xiaomi Earbuds Telemetry Manifest Purge",
    description = "Strips advertising, install referrer, and bug report permissions, and disables telemetry providers and receivers in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Xiaomi Earbuds Telemetry Manifest Purge] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val blockedPermissions = setOf(
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.miui.bugreport.permission.DUMP_CACHED_LOG",
        )

        val blockedComponents = setOf(
            "com.google.android.gms.measurement.AppMeasurementService",
            "com.google.android.gms.measurement.AppMeasurementJobService",
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            "com.xiaomi.fitness.feedback.bugreport.FeedBackDumpLogReceiver",
            "com.miui.bugreport.logprovider.DumpLogProvider",
        )

        var removedPermissions = 0
        var disabledComponents = 0

        document(manifestFile.absolutePath).use { doc ->
            // 1. Strip tracking and ad-services permissions
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

            // 2. Disable measurement, analytics, and crash log dump components
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

        println("[Xiaomi Earbuds Telemetry Manifest Purge] Stripped $removedPermissions permissions, disabled $disabledComponents analytics/logging components.")
    }
}

@Suppress("unused")
val xiaomiEarbudsBlockTelemetryPatch = bytecodePatch(
    name = "Xiaomi Earbuds Block Telemetry & Trackers",
    description = "Neutralizes Firebase Analytics, Xiaomi OneTrack, AutoReportHelper, and GlobalReport telemetry and event dispatching.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_XIAOMI_EARBUDS)
    dependsOn(xiaomiEarbudsTelemetryResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. FirebaseAnalytics.logEvent(String, Bundle)V
        Fingerprint(
            definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
            name = "logEvent",
            parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("FirebaseAnalytics.logEvent")
        }

        // 2. OnetrackImpl.reportData(String, Map)V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/statistics/OnetrackImpl;",
            name = "reportData",
            parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("OnetrackImpl.reportData")
        }

        // 3. OneTrackStatImpl.track(String, Map)V
        Fingerprint(
            definingClass = "Lcom/xiaomi/accountsdk/account/stat/OneTrackStatImpl;",
            name = "track",
            parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("OneTrackStatImpl.track")
        }

        // 4. AutoReportHelper.reportConnect(MiEarphoneDeviceInfo, boolean, String)V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/report/AutoReportHelper;",
            name = "reportConnect",
            parameters = listOf("Lcom/mi/earphone/bluetoothsdk/MiEarphoneDeviceInfo;", "Z", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("AutoReportHelper.reportConnect")
        }

        // 5. AutoReportHelper.reportDisConnect(MiEarphoneDeviceInfo, String)V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/device/manager/report/AutoReportHelper;",
            name = "reportDisConnect",
            parameters = listOf("Lcom/mi/earphone/bluetoothsdk/MiEarphoneDeviceInfo;", "Ljava/lang/String;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("AutoReportHelper.reportDisConnect")
        }

        // 6. GlobalReport.reportNoise(DeviceConfigNoiseLevel)V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/report/GlobalReport;",
            name = "reportNoise",
            parameters = listOf("Lcom/mi/earphone/bluetoothsdk/setting/function/DeviceConfigNoiseLevel;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("GlobalReport.reportNoise")
        }

        // 7. GlobalReport.reportWear(DeviceConfigRemindLost)V
        Fingerprint(
            definingClass = "Lcom/mi/earphone/settings/report/GlobalReport;",
            name = "reportWear",
            parameters = listOf("Lcom/mi/earphone/bluetoothsdk/setting/lab/DeviceConfigRemindLost;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("GlobalReport.reportWear")
        }

        // 8. Analytics.track(String, Map)V (Xiaomi Passport Analytics)
        Fingerprint(
            definingClass = "Lcom/xiaomi/passport/ui/onetrack/Analytics;",
            name = "track",
            parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
            returnType = "V",
        ).method.apply {
            addInstructions(0, "return-void")
            hookedMethods.add("Analytics.track")
        }

        println("[Xiaomi Earbuds Block Telemetry] Neutralized ${hookedMethods.size} telemetry, event tracking, and auto-report hooks.")
    }
}
