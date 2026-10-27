package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.clearTryBlocks
import app.morphe.patches.shared.ensureRegisterCount
import app.morphe.patches.shared.replaceWithReturnBoolean
import app.morphe.patches.shared.replaceWithReturnInt
import app.morphe.patches.shared.replaceWithReturnNull
import app.morphe.patches.shared.replaceWithReturnVoid
import app.morphe.patches.shared.sharedExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method

private class AdTarget(
    val descriptor: String,
    val methodNames: Set<String> = emptySet(),
    val methodPrefixes: Set<String> = emptySet(),
) {
    fun matches(name: String): Boolean =
        name in methodNames || methodPrefixes.any { name.startsWith(it) }
}

private val MINTEGRAL_PREFIXES = setOf("load", "preLoad", "preload", "bidLoad")

private val STANDARD_TARGETS = listOf(
    // AppLovin MAX
    AdTarget("Lcom/applovin/mediation/ads/MaxAdView;", setOf("loadAd")),
    AdTarget("Lcom/applovin/mediation/ads/MaxInterstitialAd;", setOf("loadAd")),
    AdTarget("Lcom/applovin/mediation/ads/MaxRewardedAd;", setOf("loadAd")),
    AdTarget("Lcom/applovin/mediation/ads/MaxAppOpenAd;", setOf("loadAd")),
    AdTarget("Lcom/applovin/mediation/nativeAds/MaxNativeAdLoader;", setOf("loadAd")),

    // Meta Audience Network
    AdTarget("Lcom/facebook/ads/InterstitialAd;", setOf("loadAd")),
    AdTarget("Lcom/facebook/ads/AdView;", setOf("loadAd")),
    AdTarget("Lcom/facebook/ads/RewardedVideoAd;", setOf("loadAd")),
    AdTarget("Lcom/facebook/ads/RewardedInterstitialAd;", setOf("loadAd")),
    AdTarget("Lcom/facebook/ads/NativeAdBase;", setOf("loadAd")),

    // Unity Ads
    AdTarget("Lcom/unity3d/ads/UnityAds;", setOf("load")),
    AdTarget("Lcom/unity3d/services/banners/BannerView;", setOf("load")),
    AdTarget("Lcom/unity3d/ads/InterstitialAd;", setOf("load")),
    AdTarget("Lcom/unity3d/ads/RewardedAd;", setOf("load")),
    AdTarget("Lcom/unity3d/ads/BannerAd;", setOf("load")),

    // ironSource / LevelPlay
    AdTarget("Lcom/unity3d/mediation/interstitial/LevelPlayInterstitialAd;", setOf("loadAd")),
    AdTarget("Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;", setOf("loadAd")),
    AdTarget("Lcom/unity3d/mediation/banner/LevelPlayBannerAdView;", setOf("loadAd")),
    AdTarget("Lcom/unity3d/ironsourceads/interstitial/InterstitialAdLoader;", setOf("loadAd")),
    AdTarget("Lcom/unity3d/ironsourceads/rewarded/RewardedAdLoader;", setOf("loadAd")),
    AdTarget("Lcom/unity3d/ironsourceads/banner/BannerAdLoader;", setOf("loadAd")),

    // Pangle
    AdTarget("Lcom/bytedance/sdk/openadsdk/api/interstitial/PAGInterstitialAd;", setOf("loadAd")),
    AdTarget("Lcom/bytedance/sdk/openadsdk/api/reward/PAGRewardedAd;", setOf("loadAd")),
    AdTarget("Lcom/bytedance/sdk/openadsdk/api/banner/PAGBannerAd;", setOf("loadAd")),
    AdTarget("Lcom/bytedance/sdk/openadsdk/api/open/PAGAppOpenAd;", setOf("loadAd")),
    AdTarget("Lcom/bytedance/sdk/openadsdk/api/nativeAd/PAGNativeAd;", setOf("loadAd")),

    // Vungle / Liftoff
    AdTarget("Lcom/vungle/ads/BaseAd;", setOf("load")),
    AdTarget("Lcom/vungle/ads/InterstitialAd;", setOf("load")),
    AdTarget("Lcom/vungle/ads/RewardedAd;", setOf("load")),
    AdTarget("Lcom/vungle/ads/BannerAd;", setOf("load")),
    AdTarget("Lcom/vungle/ads/NativeAd;", setOf("load")),

    // InMobi
    AdTarget("Lcom/inmobi/ads/InMobiInterstitial;", setOf("load")),
    AdTarget("Lcom/inmobi/ads/InMobiBanner;", setOf("load")),
    AdTarget("Lcom/inmobi/ads/InMobiNative;", setOf("load")),

    // Mintegral
    AdTarget("Lcom/mbridge/msdk/out/MBInterstitialHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidInterstitialHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBRewardVideoHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidRewardVideoHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBannerView;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidBannerView;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBSplashHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidSplashHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBNativeHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidNativeHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBNativeAdvancedHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidNativeAdvancedHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBNewInterstitialHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/out/MBBidNewInterstitialHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/interstitialvideo/out/MBInterstitialVideoHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/interstitialvideo/out/MBBidInterstitialVideoHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/splash/out/MBSplashHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/splash/out/MBBidSplashHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/advanced/out/MBNativeAdvancedHandler;", methodPrefixes = MINTEGRAL_PREFIXES),
    AdTarget("Lcom/mbridge/msdk/advanced/out/MBBidNativeAdvancedHandler;", methodPrefixes = MINTEGRAL_PREFIXES),

    // Chartboost
    AdTarget("Lcom/chartboost/sdk/ads/Interstitial;", setOf("cache")),
    AdTarget("Lcom/chartboost/sdk/ads/Rewarded;", setOf("cache")),
    AdTarget("Lcom/chartboost/sdk/ads/Banner;", setOf("cache")),
)

private const val ADMOB_GATE_CLASS = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;"

private val ADMOB_STATIC_REDIRECT_CLASSES = listOf(
    "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
    "Lcom/google/android/gms/ads/rewarded/RewardedAd;",
    "Lcom/google/android/gms/ads/rewardedinterstitial/RewardedInterstitialAd;",
    "Lcom/google/android/gms/ads/appopen/AppOpenAd;",
    "Lcom/google/android/gms/ads/admanager/AdManagerInterstitialAd;",
    "Lcom/google/android/gms/ads/admanager/AdManagerAppOpenAd;",
)

private val ADMOB_PLAIN_STUB_TARGETS = listOf(
    AdTarget("Lcom/google/android/gms/ads/BaseAdView;", setOf("loadAd")),
    AdTarget("Lcom/google/android/gms/ads/AdView;", setOf("loadAd")),
    AdTarget("Lcom/google/android/gms/ads/admanager/AdManagerAdView;", setOf("loadAd")),
    AdTarget("Lcom/google/android/gms/ads/AdLoader;", setOf("loadAd", "loadAds")),
)

private fun getLastParameterRegister(method: Method): String {
    var reg = 0
    for (i in 0 until method.parameters.size - 1) {
        val type = method.parameters[i].type
        reg += if (type == "J" || type == "D") 2 else 1
    }
    return "p$reg"
}

private fun redirectAdMobLoadMethod(method: MutableMethod): Boolean {
    val impl = method.implementation ?: return false
    val callbackReg = getLastParameterRegister(method)
    val regNum = callbackReg.removePrefix("p").toIntOrNull() ?: return false
    method.clearTryBlocks()
    method.ensureRegisterCount(maxOf(impl.registerCount, regNum + 1))
    method.removeInstructions(0, impl.instructions.count())
    method.addInstructions(
        0,
        """
            invoke-static {$callbackReg}, Lcom/kveld9/morphe/extension/AdLoadBlocker;->notifyLoadFailed(Ljava/lang/Object;)V
            return-void
        """.trimIndent(),
    )
    return true
}

private fun isAdMobStaticLoadMethod(method: Method): Boolean =
    method.name == "load" &&
        AccessFlags.STATIC.isSet(method.accessFlags) &&
        method.implementation != null &&
        method.parameters.isNotEmpty()

private fun BytecodePatchContext.neutralizeAdMobStaticClass(descriptor: String): Int {
    val mutableClass = mutableClassDefByOrNull(descriptor) ?: return 0
    var count = 0
    for (method in mutableClass.methods) {
        if (isAdMobStaticLoadMethod(method) && redirectAdMobLoadMethod(method)) {
            count++
        }
    }
    return count
}

private fun BytecodePatchContext.neutralizeAdMobTargets(): Int {
    if (classDefByOrNull(ADMOB_GATE_CLASS) == null) return 0
    var count = 0
    for (descriptor in ADMOB_STATIC_REDIRECT_CLASSES) {
        count += neutralizeAdMobStaticClass(descriptor)
    }
    for (target in ADMOB_PLAIN_STUB_TARGETS) {
        count += neutralizeTarget(target)
    }
    return count
}

private fun neutralizeMethod(method: MutableMethod): Boolean {
    if (method.implementation == null) return false
    when (method.returnType) {
        "V" -> method.replaceWithReturnVoid()
        "Z" -> method.replaceWithReturnBoolean(false)
        "I", "S", "B", "C" -> method.replaceWithReturnInt(0)
        else -> method.replaceWithReturnNull()
    }
    return true
}

private fun processTargetClass(mutableClass: MutableClass, target: AdTarget): Int {
    var count = 0
    for (method in mutableClass.methods) {
        if (target.matches(method.name) && neutralizeMethod(method)) {
            count++
        }
    }
    return count
}

private fun BytecodePatchContext.neutralizeTarget(target: AdTarget): Int {
    val mutableClass = mutableClassDefByOrNull(target.descriptor) ?: return 0
    return processTargetClass(mutableClass, target)
}

private fun BytecodePatchContext.neutralizeStandardTargets(): Int {
    var count = 0
    for (target in STANDARD_TARGETS) {
        count += neutralizeTarget(target)
    }
    return count
}

@Suppress("unused")
val universalRemoveAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Blocks common third-party ad networks from loading ads in any app.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)

    execute {
        var neutralizedCount = 0

        neutralizedCount += neutralizeStandardTargets()
        neutralizedCount += neutralizeAdMobTargets()

        if (neutralizedCount == 0) {
            println("[Remove ads] Skipped: no supported ad SDK found.")
        } else {
            println("[Remove ads] Neutralized $neutralizedCount ad loader method(s)/call site(s).")
        }
    }
}
