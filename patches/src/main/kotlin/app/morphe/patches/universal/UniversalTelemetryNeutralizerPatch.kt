package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.disableComponentsWhere
import app.morphe.patches.shared.getAttributeValue
import app.morphe.patches.shared.removeChildren
import app.morphe.patches.shared.removeComponentDiscoveryRegistrarsWhere
import app.morphe.patches.shared.setApplicationMetaData
import app.morphe.patches.shared.stripPermissionsWhere
import org.w3c.dom.Element

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

private val TELEMETRY_PROVIDERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementContentProvider",
    "io.sentry.android.core.SentryInitProvider",
    "io.sentry.android.core.SentryPerformanceProvider",
    "com.facebook.internal.FacebookInitProvider",
    "com.flurry.android.agent.FlurryContentProvider",
    "io.branch.referral.BranchInitProvider",
    "com.appsflyer.internal.platform_extension.PluginInfoContentProvider",
    "com.google.firebase.perf.provider.FirebasePerfProvider",
    "com.facebook.ads.AudienceNetworkContentProvider",
    "com.facebook.FacebookContentProvider",
    "com.vungle.ads.VungleProvider",
    "com.fairtiq.sdk.internal.telemetry.processTime.StartupTimeProvider",
    "com.google.android.gms.ads.MobileAdsInitProvider",
    "com.applovin.sdk.AppLovinInitProvider",
    "com.ironsource.lifecycle.IronsourceLifecycleProvider",
    "com.ironsource.lifecycle.LevelPlayActivityLifecycleProvider",
    "com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider",
    "io.bidmachine.BidMachineInitProvider",
)

private const val FIREBASE_INIT_PROVIDER = "com.google.firebase.provider.FirebaseInitProvider"

private val TELEMETRY_SERVICES = setOf(
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.google.firebase.sessions.SessionLifecycleService",
    "com.appsflyer.internal.service.AFJobSchedulerService",
    "com.fairtiq.sdk.internal.services.tracking.TrackingServiceImpl",
    "com.google.android.gms.ads.AdService",
    "com.applovin.impl.adview.activity.FullscreenAdService",
    "io.hopmonsdk.service.MoneytiserService",
    "com.appsalt.lib.longevity.RewardService",
)

private val TELEMETRY_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.adjust.sdk.AdjustReferrerReceiver",
    "com.appsflyer.SingleInstallBroadcastReceiver",
    "com.appsflyer.MultipleInstallBroadcastReceiver",
    "io.hopmonsdk.support.NetworkStateReceiver",
    "com.appsalt.lib.BootReceiver",
)

private val PUSH_SERVICES = setOf(
    "com.facebook.rti.push.service.FbnsService",
    "com.facebook.rti.pushv2.inapp.InappFbnsService",
    "com.facebook.pushlite.PushLiteFallbackJobService",
    "com.facebook.pushlite.PushLiteGCMJobService",
    "com.facebook.pushlite.PushLiteLollipopJobService",
    "com.facebook.pushlite.tokenprovider.fcm.PushLiteFcmListenerService",
    "com.facebook.pushlite.tokenprovider.fcm.PushLiteFirebaseMessagingService",
    "com.google.firebase.messaging.FirebaseMessagingService",
)

private val PUSH_RECEIVERS = setOf(
    "com.google.firebase.iid.FirebaseInstanceIdReceiver",
    "com.braze.push.BrazePushReceiver",
    "com.braze.BrazeFlushPushDeliveryReceiver",
)

private val GOOGLE_ANALYTICS_SERVICES = setOf(
    "com.google.android.gms.analytics.AnalyticsService",
    "com.google.android.gms.analytics.AnalyticsJobService",
)

private val GOOGLE_ANALYTICS_RECEIVERS = setOf(
    "com.google.android.gms.analytics.AnalyticsReceiver",
)

private val META_ANALYTICS_SERVICES = setOf(
    "com.facebook.analytics2.fabric.onefabric.FFAlarmUploadJobService",
    "com.facebook.analytics2.logger.GooglePlayUploadService",
    "com.facebook.analytics2.logger.legacy.uploader.AlarmBasedUploadService",
    "com.facebook.analytics2.logger.legacy.uploader.Analytics2UploadService",
    "com.facebook.analytics2.logger.legacy.uploader.LollipopUploadService",
    "com.facebook.analytics2.logger.service.LollipopUploadSafeService",
    "com.facebook.delayedworker.DelayedWorkerService",
)

private val META_ANALYTICS_RECEIVERS = setOf(
    "com.facebook.analytics2.fabric.onefabric.OneFabricUploadAlarmReceiver",
    "com.facebook.analytics2.logger.legacy.uploader.HighPriUploadRetryReceiver",
    "com.instagram.analytics.uploadscheduler.AnalyticsUploadAlarmReceiver",
    "com.facebook.delayedworker.DelayedWorkerServiceReceiver",
)

private val CRASH_UPLOAD_SERVICES = setOf(
    "com.facebook.common.errorreporting.memory.service.jobschedulercompat.fbsvc.DumperUploadService",
    "com.facebook.common.errorreporting.memory.service.jobschedulercompat.igsvc.DumperUploadService",
    "com.whatsapp.infra.crash.upload.ExceptionsUploadService",
    "com.whatsapp.infra.perf.profilo.ProfiloUploadService",
)

private val CRASH_DETECTOR_RECEIVERS = setOf(
    "com.facebook.errorreporting.lacrima.detector.broadcast.ProtectedLockScreenBroadcastReceiver",
    "com.facebook.errorreporting.lacrima.detector.broadcast.PublicLockScreenBroadcastReceiver",
    "com.facebook.errorreporting.lacrima.detector.broadcast.SystemShutdownBootBroadcastReceiver",
    "com.facebook.errorreporting.lacrima.detector.broadcast.InternalShutdownBootBroadcastReceiver",
    "com.facebook.errorreporting.lacrima.detector.broadcast.SecureShutdownBootBroadcastReceiver",
    "com.facebook.nobreak.CrashLoop\$LastState",
)

private val DEVICE_ID_PROVIDERS = setOf(
    "libraries.accessv2.src.provider.AccessLibraryContentProvider",
    "com.facebook.katana.provider.AttributionIdProvider",
    "com.facebook.katana.provider.InstallReferrerProvider",
    "com.instagram.contentprovider.InstallReferrerProvider",
    "com.facebook.katana.provider.LastUsedTimestampProvider",
    "com.facebook.messaging.partneranalytics.marketinsights.LastUsedTimestampProvider",
    "com.facebook.fdidlite.FDIDLiteProvider",
    "com.instagram.common.analytics.fdidlite.AsyncInstagramFDIDLiteProvider",
    "com.instagram.common.analytics.phoneid.AsyncInstagramPhoneIdProvider",
    "com.facebook.katana.liteprovider.usdid.UsdidValuesProvider",
    "com.instagram.liteprovider.usdid.UsdidValuesProvider",
    "com.instagram.barcelona.liteprovider.usdid.UsdidValuesProvider",
    "com.facebook.katana.liteprovider.FirstPartyUserValuesLiteProvider",
    "com.instagram.liteprovider.v2.FirstPartyUserValuesLiteProviderV2",
    "com.instagram.barcelona.liteprovider.BarcelonaLiteContentProvider",
    "com.instagram.contentprovider.AsyncFamilyAppsUserValuesProvider",
    "com.facebook.messaging.provider.FamilyAppsUserValuesProvider",
    "com.facebook.messaging.liteprovider.FamilyAppsUserValuesLiteProvider",
    "com.whatsapp.accesslibraryprovider.provider.FamilyAppsUserValuesProvider",
)

private val DEVICE_ID_SERVICES = setOf(
    "com.facebook.secure.usdid.signing.CrossSigningService",
    "com.facebook.messaging.universallinks.receiver.InstallReferrerFetchJobIntentService",
)

private val DEVICE_ID_RECEIVERS = setOf(
    "com.facebook.googleplay.GooglePlayInstallReferrerReceiver",
    "com.instagram.common.analytics.phoneid.InstagramPhoneIdRequestReceiver",
    "com.whatsapp.phoneid.PhoneIdRequestReceiver",
    "com.facebook.secure.usdid.signing.CrossSigningBroadcastReceiver",
)

private val AD_STARTUP_INITIALIZERS = setOf(
    "com.unity3d.services.core.configuration.AdsSdkInitializer",
)

private const val STARTUP_INIT_PROVIDER = "androidx.startup.InitializationProvider"

// AdServices residual config (issue #162):
// <property> and <meta-data> entries whose android:name starts with android.adservices.
private const val AD_SERVICES_PREFIX = "android.adservices."

private fun Element.removeAdServicesConfigWhere(): Int {
    val matches = childrenNamed("property", "meta-data")
        .filter { getAttributeValue(it, "name").startsWith(AD_SERVICES_PREFIX) }
    removeChildren(matches)
    return matches.size
}

private fun Element.removeStartupInitializersWhere(predicate: (String) -> Boolean): Int {
    var removed = 0
    childrenNamed("provider")
        .filter {
            val name = it.getAttribute("android:name").ifBlank { it.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
            name == STARTUP_INIT_PROVIDER
        }
        .forEach { provider ->
            val matches = provider.childrenNamed("meta-data")
                .filter { metaData ->
                    val name = metaData.getAttribute("android:name").ifBlank { metaData.getAttributeNS(ANDROID_XML_NAMESPACE, "name") }
                    predicate(name)
                }
            provider.removeChildren(matches)
            removed += matches.size
        }
    return removed
}

private val OPT_OUT_METADATA = listOf(
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_performance_collection_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    "firebase_performance_logcat_enabled" to "false",
    "firebase_data_collection_default_enabled" to "false",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_deferred_deep_link_enabled" to "false",
    "google_analytics_default_allow_ad_personalization_signals" to "false",
    "google_analytics_automatic_screen_reporting_enabled" to "false",
    "appsflyer_data_collection_enabled" to "false",
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
    "io.sentry.auto-init" to "false",
)

@Suppress("unused")
val universalTelemetryNeutralizerPatch = resourcePatch(
    name = "Block Trackers (Universal)",
    description = "Removes ad IDs, disables tracking components, and adds opt-out flags.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val revokePermissions by booleanOption(
        key = "revokePermissions",
        default = true,
        title = "Ad permissions",
        description = "Removes advertising ID and Privacy Sandbox permissions.",
        required = false,
    )

    val disableProviders by booleanOption(
        key = "disableProviders",
        default = true,
        title = "Tracking providers",
        description = "Disables analytics and measurement content providers.",
        required = false,
    )

    val disableServices by booleanOption(
        key = "disableServices",
        default = true,
        title = "Tracking services",
        description = "Disables background analytics and measurement services.",
        required = false,
    )

    val disableReceivers by booleanOption(
        key = "disableReceivers",
        default = true,
        title = "Tracking receivers",
        description = "Disables install referrer and measurement receivers.",
        required = false,
    )

    val injectOptOutFlags by booleanOption(
        key = "injectOptOutFlags",
        default = true,
        title = "Opt-out flags",
        description = "Injects opt-out flags for analytics and crash reporting SDKs.",
        required = false,
    )

    val disableFirebaseInit by booleanOption(
        key = "disableFirebaseInit",
        default = false,
        title = "Firebase init (may break apps)",
        description = "Disables Firebase initialization provider.",
        required = false,
    )

    val disablePushServices by booleanOption(
        key = "disablePushServices",
        default = false,
        title = "Push services (breaks push!)",
        description = "Disables push delivery services and receivers (breaks push notifications).",
        required = false,
    )

    val disableGoogleAnalytics by booleanOption(
        key = "disableGoogleAnalytics",
        default = true,
        title = "Google Analytics",
        description = "Disables legacy Google Analytics services and receivers.",
        required = false,
    )

    val disableMetaAnalytics by booleanOption(
        key = "disableMetaAnalytics",
        default = true,
        title = "Meta analytics",
        description = "Disables Meta analytics and upload services.",
        required = false,
    )

    val disableCrashDetectors by booleanOption(
        key = "disableCrashDetectors",
        default = true,
        title = "Crash uploaders",
        description = "Disables crash detection and dump upload services.",
        required = false,
    )

    val disableDeviceIdProviders by booleanOption(
        key = "disableDeviceIdProviders",
        default = false,
        title = "Device ID (may break login!)",
        description = "Disables device ID and cross-app identity providers.",
        required = false,
    )

    val disableMlKit by booleanOption(
        key = "disableMlKit",
        default = false,
        title = "ML Kit vision (breaks scanner!)",
        description = "Disables ML Kit vision and scanner components.",
        required = false,
    )

    val disableRemoteConfig by booleanOption(
        key = "disableRemoteConfig",
        default = false,
        title = "Remote config (may break app!)",
        description = "Disables remote config and dynamic feature flag registrars.",
        required = false,
    )

    val disableCrashlyticsRegistrars by booleanOption(
        key = "disableCrashlyticsRegistrars",
        default = false,
        title = "Crashlytics (may break app!)",
        description = "Disables Firebase Crashlytics startup registrars.",
        required = false,
    )

    val disableAdStartupInitializers by booleanOption(
        key = "disableAdStartupInitializers",
        default = false,
        title = "Ad auto-start",
        description = "Removes ad SDK entries from androidx startup initializers.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Block Trackers (Universal)] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        val shouldRevoke = revokePermissions ?: true
        val shouldDisableProviders = disableProviders ?: true
        val shouldDisableServices = disableServices ?: true
        val shouldDisableReceivers = disableReceivers ?: true
        val shouldInjectOptOut = injectOptOutFlags ?: true
        val shouldDisableFirebase = disableFirebaseInit ?: false
        val shouldDisablePush = disablePushServices ?: false
        val shouldDisableGoogleAnalytics = disableGoogleAnalytics ?: true
        val shouldDisableMetaAnalytics = disableMetaAnalytics ?: true
        val shouldDisableCrashDetectors = disableCrashDetectors ?: true
        val shouldDisableDeviceIds = disableDeviceIdProviders ?: false
        val shouldDisableMlKit = disableMlKit ?: false
        val shouldDisableRemoteConfig = disableRemoteConfig ?: false
        val shouldDisableCrashlyticsRegistrars = disableCrashlyticsRegistrars ?: false
        val shouldDisableAdStartup = disableAdStartupInitializers ?: false

        var removedPerms: List<String> = emptyList()
        var disabledProvidersCount = 0
        var disabledServicesCount = 0
        var disabledReceiversCount = 0
        var disabledPushCount = 0
        var disabledPushReceiversCount = 0
        var disabledGaServicesCount = 0
        var disabledGaReceiversCount = 0
        var disabledMetaServicesCount = 0
        var disabledMetaReceiversCount = 0
        var disabledCrashServicesCount = 0
        var disabledCrashReceiversCount = 0
        var disabledDeviceIdProvidersCount = 0
        var disabledDeviceIdServicesCount = 0
        var disabledDeviceIdReceiversCount = 0
        var removedStartupInitCount = 0
        var injectedFlagsCount = 0
        var removedRegistrarsCount = 0
        var removedAdServicesConfigCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.childrenNamed("application").firstOrNull()

            if (shouldRevoke) {
                removedPerms = root.stripPermissionsWhere { it in TRACKING_PERMISSIONS }
                if (application != null) {
                    removedAdServicesConfigCount = application.removeAdServicesConfigWhere()
                }
            }

            if (application != null) {
                if (shouldDisableProviders) {
                    val targetProviders = if (shouldDisableFirebase) {
                        TELEMETRY_PROVIDERS + FIREBASE_INIT_PROVIDER
                    } else {
                        TELEMETRY_PROVIDERS
                    }
                    disabledProvidersCount = application.disableComponentsWhere("provider") { it in targetProviders }
                }

                if (shouldDisableServices) {
                    disabledServicesCount = application.disableComponentsWhere("service") { it in TELEMETRY_SERVICES }
                }

                if (shouldDisableReceivers) {
                    disabledReceiversCount = application.disableComponentsWhere("receiver") { it in TELEMETRY_RECEIVERS }
                }

                if (shouldDisablePush) {
                    disabledPushCount = application.disableComponentsWhere("service") { it in PUSH_SERVICES }
                    disabledPushReceiversCount = application.disableComponentsWhere("receiver") { it in PUSH_RECEIVERS }
                    removedRegistrarsCount += application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("Installations", ignoreCase = true) ||
                            name.contains("Transport", ignoreCase = true) ||
                            name.contains("Iid", ignoreCase = true)
                    }
                }

                if (shouldDisableGoogleAnalytics) {
                    disabledGaServicesCount = application.disableComponentsWhere("service") { it in GOOGLE_ANALYTICS_SERVICES }
                    disabledGaReceiversCount = application.disableComponentsWhere("receiver") { it in GOOGLE_ANALYTICS_RECEIVERS }
                }

                if (shouldDisableMetaAnalytics) {
                    disabledMetaServicesCount = application.disableComponentsWhere("service") { it in META_ANALYTICS_SERVICES }
                    disabledMetaReceiversCount = application.disableComponentsWhere("receiver") { it in META_ANALYTICS_RECEIVERS }
                }

                if (shouldDisableCrashDetectors) {
                    disabledCrashServicesCount = application.disableComponentsWhere("service") { it in CRASH_UPLOAD_SERVICES }
                    disabledCrashReceiversCount = application.disableComponentsWhere("receiver") { it in CRASH_DETECTOR_RECEIVERS }
                }

                if (shouldDisableDeviceIds) {
                    disabledDeviceIdProvidersCount = application.disableComponentsWhere("provider") { it in DEVICE_ID_PROVIDERS }
                    disabledDeviceIdServicesCount = application.disableComponentsWhere("service") { it in DEVICE_ID_SERVICES }
                    disabledDeviceIdReceiversCount = application.disableComponentsWhere("receiver") { it in DEVICE_ID_RECEIVERS }
                }

                if (shouldDisableMlKit) {
                    disabledProvidersCount += application.disableComponentsWhere("provider") { it == "com.google.mlkit.common.internal.MlKitInitProvider" }
                    disabledServicesCount += application.disableComponentsWhere("service") { it == "com.google.mlkit.common.internal.MlKitComponentDiscoveryService" }
                    removedRegistrarsCount += application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name == "com.google.mlkit.vision.barcode.internal.BarcodeRegistrar" ||
                        name == "com.google.mlkit.vision.face.internal.FaceRegistrar" ||
                        name == "com.google.mlkit.vision.text.internal.TextRegistrar" ||
                        name == "com.google.mlkit.vision.common.internal.VisionCommonRegistrar" ||
                        name == "com.google.mlkit.common.internal.CommonComponentRegistrar"
                    }
                }

                if (shouldDisableRemoteConfig) {
                    removedRegistrarsCount += application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("RemoteConfig", ignoreCase = true) ||
                            name.contains("Abt", ignoreCase = true) ||
                            name.contains("DynamicLoading", ignoreCase = true)
                    }
                }

                if (shouldDisableCrashlyticsRegistrars) {
                    removedRegistrarsCount += application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("Crashlytics", ignoreCase = true)
                    }
                }

                if (shouldDisableAdStartup) {
                    removedStartupInitCount = application.removeStartupInitializersWhere { it in AD_STARTUP_INITIALIZERS }
                }

                if (shouldInjectOptOut) {
                    val existingMetaData = application.childrenNamed("meta-data")
                    OPT_OUT_METADATA.forEach { (name, value) ->
                        val existing = existingMetaData.firstOrNull {
                            getAttributeValue(it, "name") == name
                        }
                        if (existing == null || getAttributeValue(existing, "value") != value) {
                            application.setApplicationMetaData(name, value)
                            injectedFlagsCount++
                        }
                    }

                    removedRegistrarsCount += application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("Analytics", ignoreCase = true) ||
                            name.contains("Perf", ignoreCase = true) ||
                            name.contains("Sessions", ignoreCase = true) ||
                            name.contains("Measurement", ignoreCase = true)
                    }
                }
            }
        }

        val totalDisabled = disabledProvidersCount + disabledServicesCount + disabledReceiversCount + disabledPushCount + disabledPushReceiversCount + disabledGaServicesCount + disabledGaReceiversCount + disabledMetaServicesCount + disabledMetaReceiversCount + disabledCrashServicesCount + disabledCrashReceiversCount + disabledDeviceIdProvidersCount + disabledDeviceIdServicesCount + disabledDeviceIdReceiversCount
        if (removedPerms.isEmpty() && totalDisabled == 0 && injectedFlagsCount == 0 && removedRegistrarsCount == 0 && removedStartupInitCount == 0 && removedAdServicesConfigCount == 0) {
            println("[Block Trackers (Universal)] AndroidManifest.xml is already clean (0 tracking elements found).")
            return@execute
        }

        val permNames = removedPerms.map { it.substringAfterLast('.') }.distinct()
        val permNote = if (removedPerms.isNotEmpty()) "revoked ${removedPerms.size} permission(s) (${permNames.joinToString(", ")})" else "0 permissions revoked"
        val initNote = if (removedStartupInitCount > 0) " (+${removedStartupInitCount} startup initializer(s))" else ""
        val regNote = if (removedRegistrarsCount > 0 || removedStartupInitCount > 0) {
            if (removedRegistrarsCount > 0) ", removed $removedRegistrarsCount discovery registrar(s)$initNote"
            else ", removed $removedStartupInitCount startup initializer(s)"
        } else ""

        val categoryBreakdown = listOf(
            "providers" to disabledProvidersCount,
            "services" to disabledServicesCount,
            "receivers" to disabledReceiversCount,
            "push" to (disabledPushCount + disabledPushReceiversCount),
            "ga" to (disabledGaServicesCount + disabledGaReceiversCount),
            "meta" to (disabledMetaServicesCount + disabledMetaReceiversCount),
            "crash" to (disabledCrashServicesCount + disabledCrashReceiversCount),
            "deviceid" to (disabledDeviceIdProvidersCount + disabledDeviceIdServicesCount + disabledDeviceIdReceiversCount),
        ).filter { it.second > 0 }
            .joinToString(", ") { "${it.first}=${it.second}" }
        val componentDetails = if (categoryBreakdown.isNotEmpty()) " ($categoryBreakdown)" else ""

        val adServicesNote = if (removedAdServicesConfigCount > 0) ", removed $removedAdServicesConfigCount AdServices config entries" else ""
        println("[Block Trackers (Universal)] $permNote, disabled $totalDisabled component(s)$componentDetails, injected $injectedFlagsCount opt-out flag(s)$regNote$adServicesNote.")
    }
}
