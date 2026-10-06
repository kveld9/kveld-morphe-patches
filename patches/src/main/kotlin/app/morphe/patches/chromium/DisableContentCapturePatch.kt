package app.morphe.patches.chromium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.replaceWithReturnBoolean

@Suppress("unused")
val disableContentCapturePatch = bytecodePatch(
    name = "Disable Content Capture",
    description = "Stops Chromium from streaming on-screen page text and URLs to the Android ContentCapture system service (Android System Intelligence).",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        // shouldCapture is JNI-only (@CalledByNative) and gates native content capture.
        Fingerprint(
            definingClass = "Lorg/chromium/components/content_capture/OnscreenContentProvider;",
            name = "shouldCapture",
            returnType = "Z",
            parameters = listOf("Ljava/lang/String;"),
        ).method.replaceWithReturnBoolean(false)

        println("[Disable Content Capture] Forced OnscreenContentProvider.shouldCapture -> false; page content no longer dispatched to ContentCapture.")
    }
}
