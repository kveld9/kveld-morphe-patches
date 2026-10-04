package app.morphe.patches.shared

import app.morphe.patcher.patch.bytecodePatch

/**
 * Merges the shared extension into the target dex exactly once.
 *
 * Patches must `dependsOn(sharedExtensionPatch)` instead of calling `extendWith(...)` themselves:
 * each `extendWith` triggers a full ClassMerger pass over the extension, so declaring it in every
 * patch merged the same classes dozens of times per run (profiling showed >50% of CPU time).
 */
internal val sharedExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}
