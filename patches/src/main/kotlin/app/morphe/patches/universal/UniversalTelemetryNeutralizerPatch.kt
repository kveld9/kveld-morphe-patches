package app.morphe.patches.universal

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.childrenNamed
import app.morphe.patches.shared.disableComponentsWhere
import app.morphe.patches.shared.removeComponentDiscoveryRegistrarsWhere
import app.morphe.patches.shared.setApplicationMetaData
import app.morphe.patches.shared.stripPermissionsWhere

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
    "com.google.mlkit.common.internal.MlKitInitProvider",
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
    "com.google.mlkit.common.internal.MlKitComponentDiscoveryService",
    "com.fairtiq.sdk.internal.services.tracking.TrackingServiceImpl",
    "com.google.android.gms.ads.AdService",
    "com.applovin.impl.adview.activity.FullscreenAdService",
)

private val TELEMETRY_RECEIVERS = setOf(
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
    "com.adjust.sdk.AdjustReferrerReceiver",
    "com.appsflyer.SingleInstallBroadcastReceiver",
    "com.appsflyer.MultipleInstallBroadcastReceiver",
    "com.google.firebase.iid.FirebaseInstanceIdReceiver",
    "com.braze.push.BrazePushReceiver",
    "com.braze.BrazeFlushPushDeliveryReceiver",
    "com.facebook.AuthenticationTokenManager\$CurrentAuthenticationTokenChangedBroadcastReceiver",
    "com.facebook.CurrentAccessTokenExpirationBroadcastReceiver",
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
    name = "Universal Telemetry Neutralizer",
    description = "Strips advertising and Privacy Sandbox permissions, disables analytics ContentProviders and telemetry background services (Firebase, Sentry, Adjust, AppsFlyer, DataTransport), prunes ComponentDiscovery registrars, and injects telemetry opt-out metadata. Includes an optional toggle to disable push notification services.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val revokePermissions by booleanOption(
        key = "revokePermissions",
        default = true,
        title = "Revoke Advertising & Tracking Permissions",
        description = "Remove AD_ID, Privacy Sandbox attribution/topics/audiences, and Play Install Referrer permissions from AndroidManifest.xml.",
        required = false,
    )

    val disableProviders by booleanOption(
        key = "disableProviders",
        default = true,
        title = "Disable Telemetry ContentProviders",
        description = "Disable analytics ContentProviders (Google Measurement, Sentry, Facebook, Flurry, Branch, AppsFlyer).",
        required = false,
    )

    val disableServices by booleanOption(
        key = "disableServices",
        default = true,
        title = "Disable Telemetry Background Services",
        description = "Disable Google Measurement, Google DataTransport, Firebase Sessions, and AppsFlyer background job services.",
        required = false,
    )

    val disableReceivers by booleanOption(
        key = "disableReceivers",
        default = true,
        title = "Disable Telemetry Receivers",
        description = "Disable install referrer and analytics measurement broadcast receivers (Adjust, AppsFlyer, Google Measurement).",
        required = false,
    )

    val injectOptOutFlags by booleanOption(
        key = "injectOptOutFlags",
        default = true,
        title = "Inject Telemetry Opt-Out Flags & Prune Registrars",
        description = "Inject meta-data opt-out entries into application tag (Firebase Analytics, Crashlytics, Performance, AppsFlyer, Sentry, Facebook SDK) and prune Firebase discovery registrars.",
        required = false,
    )

    val disableFirebaseInit by booleanOption(
        key = "disableFirebaseInit",
        default = false,
        title = "Disable Firebase Init Provider",
        description = "Disable com.google.firebase.provider.FirebaseInitProvider. Default is false to prevent issues in apps that depend on Firebase Auth or push notifications.",
        required = false,
    )

    val disablePushServices by booleanOption(
        key = "disablePushServices",
        default = false,
        title = "Disable Push Notification Services",
        description = "Disable Meta Fbns, PushLite, and Firebase Cloud Messaging services. WARNING: this breaks push notifications; enable only to fully silence background push delivery.",
        required = false,
    )

    val disableGoogleAnalytics by booleanOption(
        key = "disableGoogleAnalytics",
        default = true,
        title = "Disable Google Analytics Services",
        description = "Disable legacy Google Analytics background services and receivers (distinct from Firebase AppMeasurement, which is covered by the telemetry toggles above).",
        required = false,
    )

    val disableMetaAnalytics by booleanOption(
        key = "disableMetaAnalytics",
        default = true,
        title = "Disable Meta Analytics Upload Pipeline",
        description = "Disable Meta Analytics2/OneFabric upload services, Instagram upload scheduler receiver, and deferred analytics worker components.",
        required = false,
    )

    val disableCrashDetectors by booleanOption(
        key = "disableCrashDetectors",
        default = true,
        title = "Disable Crash Detectors & Dump Upload",
        description = "Disable Lacrima lock-screen/shutdown crash detectors, crash-loop state trackers, and background crash-dump upload services. User-initiated bug reports are untouched.",
        required = false,
    )

    val disableDeviceIdProviders by booleanOption(
        key = "disableDeviceIdProviders",
        default = false,
        title = "Disable Device-ID & Cross-App Identity Providers",
        description = "Disable attribution, FDID/PhoneId/USDiD, and FamilyApps cross-app identity providers plus referrer and cross-signing components. WARNING: may break login, account switching, and deferred deep links; enable only to fully silence device-identity collection.",
        required = false,
    )

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Universal Telemetry Neutralizer] Skipped: AndroidManifest.xml not found.")
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

        var removedPerms: List<String> = emptyList()
        var disabledProvidersCount = 0
        var disabledServicesCount = 0
        var disabledReceiversCount = 0
        var disabledPushCount = 0
        var disabledGaServicesCount = 0
        var disabledGaReceiversCount = 0
        var disabledMetaServicesCount = 0
        var disabledMetaReceiversCount = 0
        var disabledCrashServicesCount = 0
        var disabledCrashReceiversCount = 0
        var disabledDeviceIdProvidersCount = 0
        var disabledDeviceIdServicesCount = 0
        var disabledDeviceIdReceiversCount = 0
        var injectedFlagsCount = 0
        var removedRegistrarsCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            val application = root.childrenNamed("application").firstOrNull()

            if (shouldRevoke) {
                removedPerms = root.stripPermissionsWhere { it in TRACKING_PERMISSIONS }
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

                if (shouldInjectOptOut) {
                    OPT_OUT_METADATA.forEach { (name, value) ->
                        application.setApplicationMetaData(name, value)
                    }
                    injectedFlagsCount = OPT_OUT_METADATA.size

                    removedRegistrarsCount = application.removeComponentDiscoveryRegistrarsWhere { name ->
                        name.contains("Analytics", ignoreCase = true) ||
                            name.contains("Crashlytics", ignoreCase = true) ||
                            name.contains("Perf", ignoreCase = true) ||
                            name.contains("Sessions", ignoreCase = true) ||
                            name.contains("MlKit", ignoreCase = true) ||
                            name.contains("MLKit", ignoreCase = true) ||
                            name.contains("Vision", ignoreCase = true) ||
                            name.contains("Barcode", ignoreCase = true) ||
                            name.contains("Face", ignoreCase = true) ||
                            name.contains("Text", ignoreCase = true) ||
                            name.contains("Iid", ignoreCase = true) ||
                            name.contains("DynamicLoading", ignoreCase = true) ||
                            name.contains("Transport", ignoreCase = true) ||
                            name.contains("Installations", ignoreCase = true)
                    }
                }
            }
        }

        val totalDisabled = disabledProvidersCount + disabledServicesCount + disabledReceiversCount + disabledPushCount + disabledGaServicesCount + disabledGaReceiversCount + disabledMetaServicesCount + disabledMetaReceiversCount + disabledCrashServicesCount + disabledCrashReceiversCount + disabledDeviceIdProvidersCount + disabledDeviceIdServicesCount + disabledDeviceIdReceiversCount
        if (removedPerms.isEmpty() && totalDisabled == 0 && injectedFlagsCount == 0 && removedRegistrarsCount == 0) {
            println("[Universal Telemetry Neutralizer] AndroidManifest.xml is already clean (0 tracking elements found).")
            return@execute
        }

        val permNames = removedPerms.map { it.substringAfterLast('.') }.distinct()
        val permNote = if (removedPerms.isNotEmpty()) "revoked ${removedPerms.size} permission(s) (${permNames.joinToString(", ")})" else "0 permissions revoked"
        val regNote = if (removedRegistrarsCount > 0) ", removed $removedRegistrarsCount discovery registrar(s)" else ""
        println("[Universal Telemetry Neutralizer] $permNote, disabled $totalDisabled component(s), injected $injectedFlagsCount opt-out flag(s)$regNote.")
    }
}
