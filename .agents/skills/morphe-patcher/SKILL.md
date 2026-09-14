---
name: morphe-patcher
description: Architecture, patch typology (bytecodePatch, resourcePatch, rawResourcePatch), universal patches, stringOption DSL, fingerprint resolution, compatibility contracts (Constants.kt), and diagnostic telemetry invariants.
---

# Morphe Patcher Architectural Guidelines

## 1. Patch DSL & Typology

Morphe patches are declared using functional Kotlin builder DSLs provided by `app.morphe.patcher.patch`:

### A. `bytecodePatch`
Primary tool for Dalvik/Smali AST bytecode transformations via dexlib2 fingerprints and instructions.
```kotlin
val myPatch = bytecodePatch(
    name = "Unique Patch Name",
    description = "Concise technical description of the modification.",
    default = true // Whether enabled by default in Morphe Manager
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)
    dependsOn(companionResourcePatch) // Optional dependency execution

    execute {
        // Fingerprinting and instruction insertion
    }
}
```

### B. `resourcePatch`
Used for parsing and transforming decompiled Android resource XML files prior to DEX assembly.
```kotlin
val myResourcePatch = resourcePatch(
    name = "Resource Defaults Patch",
    description = "Overwrites default XML attributes.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val targetFile = get("res/xml").listFiles()
            ?.firstOrNull { it.extension == "xml" && it.readText().contains("target_key") }
            ?: return@execute

        document(targetFile.absolutePath).use { doc ->
            val nodes = doc.getElementsByTagName("SwitchPreference")
            for (i in 0 until nodes.length) {
                val elem = nodes.item(i) as? Element ?: continue
                if (elem.getAttribute("android:key") == "target_key") {
                    elem.setAttribute("android:defaultValue", "true")
                }
            }
        }
    }
}
```

### C. `rawResourcePatch`
Direct byte-level modification of bundled binary shared libraries (`.so`) or uncompressed assets (`assets/index.android.bundle`).
```kotlin
val myNativePatch = rawResourcePatch(
    name = "Native Hardening Patch",
    description = "Direct binary patching of libchrome.so",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_BRAVE)

    execute {
        val soFile = get("lib/arm64-v8a/libchrome.so")
        if (!soFile.exists()) return@execute
        // Validate offsets and mutate bytes via RandomAccessFile
    }
}
```

---

## 2. Compatibility Scope & Universal Patches

Compatibility is configured via `compatibleWith(...)`:

- **Single Target**:
  ```kotlin
  compatibleWith(Constants.COMPATIBILITY_BRAVE)
  ```
- **Multi-Compatibility Varargs**:
  Accepts multiple `Compatibility` contracts for cross-target or dual-package applications (e.g. TikTok Global + TikTok Asia):
  ```kotlin
  compatibleWith(Constants.COMPATIBILITY_TIKTOK, Constants.COMPATIBILITY_TIKTOK_ASIA)
  ```
- **Universal Patches**:
  **Omitting `compatibleWith(...)`** entirely produces a universal patch (e.g. `LocaleResourceSlimmerPatch`, `DpiResourceSlimmerPatch`). Universal patches are offered across all target applications in Morphe Manager and the CLI patcher.

---

## 3. User Configurable Options (`stringOption` DSL)

Patches can expose configurable settings to users in Morphe Manager or CLI using the `stringOption` property delegate:

```kotlin
import app.morphe.patcher.patch.stringOption

val targetLocales by stringOption(
    key = "locales",
    title = "Locales to keep",
    description = "Comma-separated language codes to preserve (e.g. 'en, es, pt, fr, de'). English fallback is always retained.",
    default = "en",
    required = false,
)
```

### Metadata Synchronization Rule
Whenever patch options, descriptions, titles, or defaults are added or modified in Kotlin code, always synchronize the patch catalog before committing:
```bash
./gradlew generatePatchesList
```
This updates the local build catalog and validates schema conformance.

---

## 4. Fingerprint Resolution Strategies

Fingerprints locate target methods across obfuscated versions without hardcoding method names:

1. **String Literals**: Most resilient anchor. Locate methods referencing unique log strings or preference keys.
   ```kotlin
   Fingerprint(
       returnType = "V",
       strings = listOf("brave.origin.package_name_android", "brave.origin.product_id_android")
   )
   ```

2. **Signature & Parameter Filtering**: Match methods by strict parameter and return type signatures.
   ```kotlin
   Fingerprint(
       returnType = "Z",
       parameters = listOf("Lorg/chromium/chrome/browser/profiles/Profile;"),
       strings = listOf("getIsSubscriptionActive profile is null")
   )
   ```

3. **Instruction Filters & Register Sniffing**:
   ```kotlin
   val fp = Fingerprint(
       definingClass = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;",
       returnType = "V",
       parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
       filters = listOf(
           methodCall(definingClass = "Lcom/target/Class;", name = "predicate", returnType = "Z")
       )
   )
   val matchIndex = fp.instructionMatches.first().index
   val targetReg = fp.method.getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA
   ```

---

## 5. Metadata Contracts (`Constants.kt`)

Every patch must reference the shared compatibility object defined centrally in `Constants.kt`. Never inline `Compatibility(...)` objects.

Active targets defined in `Constants.kt`:
1. **Brave**: `Constants.COMPATIBILITY_BRAVE` (`com.brave.browser`)
2. **Gboard Lite**: `Constants.COMPATIBILITY_GBOARD` (`com.google.android.inputmethod.latin`)
3. **Vivaldi**: `Constants.COMPATIBILITY_VIVALDI` (`com.vivaldi.browser`)
4. **Hevy**: `Constants.COMPATIBILITY_HEVY` (`com.hevy`)
5. **TikTok**: `Constants.COMPATIBILITY_TIKTOK` (`com.zhiliaoapp.musically`) & `Constants.COMPATIBILITY_TIKTOK_ASIA` (`com.ss.android.ugc.trill`)

---

## 6. Diagnostic Telemetry Invariants & Harness Compliance

Every patch execution must emit concise, high-signal diagnostic telemetry captured by Morphe Manager / CLI logs (`[WARN] [STDIO]: [...]`). Patches must satisfy the following invariants tested by RE and audit harnesses:

1. **Standardized Prefix**: Every log line must start with the bracketed patch name prefix: `println("[Patch Name] ...")`.
2. **Dynamic Mutation Counter**: Track injected modifications with a local counter:
   ```kotlin
   var patched = 0
   ```
3. **Per-Hook Try/Catch with Notes**: Wrap individual hook/fingerprint blocks in `try/catch` to allow partial degradation without failing the entire patch run:
   ```kotlin
   try {
       // fingerprint resolution and hook injection
       patched++
   } catch (e: Exception) {
       println("[Patch Name] Component note: ${e.message}")
   }
   ```
4. **Consolidated Summary Log**: Emit a quantifiable summary upon completing operations:
   ```kotlin
   println("[Patch Name] Successfully applied $patched hooks across target components.")
   ```
5. **Anti-Spam & Bounded Output**: Never dump thousands of lines or unbounded file trees. Repetitive items must be summarized or bounded to short representative samples (e.g. `.take(6)`).
6. **Failure & Guard Transparency**: If an operation is skipped or safely aborted (e.g. missing targets or preconditions), log an explicit descriptive reason so issues can be immediately diagnosed from user-submitted logs.
7. **Zero Emojis Policy**: Never use emojis in telemetry logs, exceptions, or console output. All logging must use clean, standard ASCII / plain-text formatting (e.g. `[INFO]`, `[WARN]`, `[PASS]`, `[FAIL]`).

---

## 7. Mandatory Verification Gate: In-Situ Morphe Patcher Execution

After completing any change in any patch or adding a new patch:
1. **Zero Fake Completion**: Never declare a task or implementation complete merely based on `./gradlew check` or static checks.
2. **Execute Full-Suite In-Situ Patching**:
   ```bash
   ./gradlew runPatchTest -Papp=<targetApp>
   ```
   Targets: `gboard`, `tiktok`, `brave`, `vivaldi`, `hevy`.
   Or specify an explicit APK:
   ```bash
   ./gradlew runPatchTest -Papk=/path/to/app.apk
   ```
3. **Assert 100% Pass Rate**: Every single patch corresponding to that target app must execute and pass with **zero exceptions, zero fingerprint mismatches, and zero failed patches**.

