package app.morphe.patches.nokoprint

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

@Suppress("unused")
val nokoPrintAdActivitiesSlimmerPatch = resourcePatch(
    name = "NokoPrint Ad Activities Slimmer",
    description = "Disables over 100 third-party ad mediation activities, internal web browsers, and debuggers registered in AndroidManifest.xml.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_NOKOPRINT)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[NokoPrint Ad Activities Slimmer] AndroidManifest.xml not found - skipping.")
            return@execute
        }

        val adComponentPrefixes = listOf(
            "com.applovin.",
            "com.mbridge.msdk.",
            "com.facebook.",
            "com.unity3d.",
            "com.ironsource.",
            "com.fyber.inneractive.",
            "com.vungle.ads.",
            "com.chartboost.sdk.",
            "com.inmobi.ads.",
            "com.amazon.device.ads.",
            "com.amazon.aps.",
            "com.appbrain.",
            "net.pubnative.lite.",
            "com.smaato.sdk.",
            "com.google.android.gms.ads.",
            "com.bytedance.sdk.",
            "sg.bigo.ads.",
            "com.ogury.",
            "com.moloco.sdk.",
            "com.pubmatic.sdk.",
            "com.tappx.sdk.",
        )

        var disabledCount = 0

        document(manifestFile.absolutePath).use { doc ->
            val tags = listOf("activity", "service", "receiver")
            for (tag in tags) {
                val elements = doc.getElementsByTagName(tag)
                for (i in 0 until elements.length) {
                    val comp = elements.item(i) as? Element ?: continue
                    val name = comp.getAttribute("android:name")
                    if (adComponentPrefixes.any { name.startsWith(it) }) {
                        comp.setAttribute("android:enabled", "false")
                        disabledCount++
                    }
                }
            }
        }

        println("[NokoPrint Ad Activities Slimmer] Disabled $disabledCount ad mediation and debug components.")
    }
}
