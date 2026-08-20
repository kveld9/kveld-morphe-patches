# Morphe Patcher Architectural Guidelines

## 1. Patch DSL & Types

Morphe patches are declared using functional Kotlin builder DSLs provided by `app.morphe.patcher.patch`:

### A. `bytecodePatch`
Primary tool for Dalvik/Smali AST bytecode transformations.
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
Direct byte-level modification of bundled binary shared libraries (`.so`) or uncompressed assets.
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

## 2. Fingerprint Resolution Strategies

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

## 3. Metadata Contracts (`Constants.kt`)

Every patch must reference the shared compatibility object:
```kotlin
object Constants {
    const val BRAVE_TARGET_VERSION = "1.93.137"
    const val BRAVE_PACKAGE_NAME = "com.brave.browser"

    val COMPATIBILITY_BRAVE = Compatibility(
        name = "Brave Private Web Browser, VPN",
        packageName = BRAVE_PACKAGE_NAME,
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF4500,
        targets = listOf(
            AppTarget(
                version = BRAVE_TARGET_VERSION,
                description = "Download v1.93.137 from github.com/brave/brave-browser/releases"
            )
        )
    )
}
```
