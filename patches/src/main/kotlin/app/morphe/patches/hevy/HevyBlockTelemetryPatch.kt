package app.morphe.patches.hevy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val hevyTelemetryResourcePatch = resourcePatch(
    name = "Hevy Telemetry Manifest Purge",
    description = "Strips tracking/advertising permissions and disables Sentry providers and WearOS background sync in AndroidManifest.xml.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Block Hevy Telemetry] AndroidManifest.xml not found - skipping manifest purge.")
            return@execute
        }

        val blockedPermissions = setOf(
            "com.google.android.gms.permission.AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
            "android.permission.ACCESS_ADSERVICES_TOPICS",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "android.permission.READ_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        )

        val blockedProviders = setOf(
            "io.sentry.android.core.SentryInitProvider",
            "io.sentry.android.core.SentryPerformanceProvider",
        )

        var removedPermissions = 0
        var disabledComponents = 0

        document(manifestFile.absolutePath).use { doc ->
            // 1. Remove tracking & ads permissions
            val usesPermissions = doc.getElementsByTagName("uses-permission")
            val toRemove = mutableListOf<Element>()
            for (i in 0 until usesPermissions.length) {
                val elem = usesPermissions.item(i) as? Element ?: continue
                val name = elem.getAttribute("android:name")
                if (name in blockedPermissions) {
                    toRemove.add(elem)
                }
            }
            toRemove.forEach {
                it.parentNode?.removeChild(it)
                removedPermissions++
            }

            // 2. Disable Sentry content providers
            val providers = doc.getElementsByTagName("provider")
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as? Element ?: continue
                val name = provider.getAttribute("android:name")
                if (name in blockedProviders) {
                    provider.setAttribute("android:enabled", "false")
                    disabledComponents++
                }
            }

            // 3. Disable WearOS background service and Adjust install referrer receiver
            val services = doc.getElementsByTagName("service")
            for (i in 0 until services.length) {
                val service = services.item(i) as? Element ?: continue
                val name = service.getAttribute("android:name")
                if (name == "com.hevy.services.WearListenerService") {
                    service.setAttribute("android:enabled", "false")
                    disabledComponents++
                }
            }

            val receivers = doc.getElementsByTagName("receiver")
            for (i in 0 until receivers.length) {
                val receiver = receivers.item(i) as? Element ?: continue
                val name = receiver.getAttribute("android:name")
                if (name == "com.adjust.sdk.AdjustReferrerReceiver") {
                    receiver.setAttribute("android:enabled", "false")
                    disabledComponents++
                }
            }
        }

        println("[Block Hevy Telemetry] Stripped $removedPermissions permissions and disabled $disabledComponents tracking components in AndroidManifest.xml")
    }
}

@Suppress("unused")
val hevyBlockTelemetryPatch = bytecodePatch(
    name = "Block Hevy Telemetry & Trackers",
    description = "Neutralizes Sentry crash reporting, Adjust attribution, Facebook AppEvents, Branch referral tracking, and WearOS background sync.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_HEVY)
    dependsOn(hevyTelemetryResourcePatch)

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Sentry React Native initialization stub (resolve Boolean.TRUE and return)
        val sentryFp = Fingerprint(
            definingClass = "Lio/sentry/react/RNSentryModuleImpl;",
            name = "initNativeSdk",
        )
        sentryFp.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-interface {p2, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """.trimIndent(),
        )
        hookedMethods.add("RNSentryModuleImpl.initNativeSdk")

        // 2. Adjust SDK queuing stubs
        val adjustAddFp = Fingerprint(
            definingClass = "Lcom/adjust/sdk/PackageHandler;",
            name = "addPackage",
        )
        adjustAddFp.method.addInstructions(0, "return-void")
        hookedMethods.add("PackageHandler.addPackage")

        val adjustSendFp = Fingerprint(
            definingClass = "Lcom/adjust/sdk/PackageHandler;",
            name = "sendFirstPackage",
        )
        adjustSendFp.method.addInstructions(0, "return-void")
        hookedMethods.add("PackageHandler.sendFirstPackage")

        // 3. Facebook AppEvents flush
        val fbFlushFp = Fingerprint(
            definingClass = "Lcom/facebook/appevents/AppEventQueue;",
            name = "flush",
        )
        fbFlushFp.method.addInstructions(0, "return-void")
        hookedMethods.add("AppEventQueue.flush")

        // 4. Amplitude Context Provider ad tracking stubs
        val amplitudeContextFp = Fingerprint(
            definingClass = "Lcom/amplitude/reactnative/AndroidContextProvider;",
            name = "isLimitAdTrackingEnabled",
        )
        amplitudeContextFp.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        hookedMethods.add("AndroidContextProvider.isLimitAdTrackingEnabled")

        val amplitudePrefetchFp = Fingerprint(
            definingClass = "Lcom/amplitude/reactnative/AndroidContextProvider;",
            name = "prefetch",
        )
        amplitudePrefetchFp.method.addInstructions(0, "return-void")
        hookedMethods.add("AndroidContextProvider.prefetch")

        // 5. Branch Metrics referral tracking stub
        val branchFp = Fingerprint(
            definingClass = "Lio/branch/referral/Branch;",
            name = "isTrackingDisabled",
        )
        branchFp.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
        hookedMethods.add("Branch.isTrackingDisabled")

        // 6. Amplitude React Native Bridge stub (drops legacy event tracking)
        val amplitudeFp = Fingerprint(
            definingClass = "Lcom/amplitude/reactnative/AmplitudeReactNativeModule;",
            name = "getLegacyEvents",
        )
        amplitudeFp.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-interface {p3, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """.trimIndent(),
        )
        hookedMethods.add("AmplitudeReactNativeModule.getLegacyEvents")

        println("[Block Hevy Telemetry] Successfully hooked ${hookedMethods.size} telemetry methods: ${hookedMethods.joinToString(", ")}")
    }
}
