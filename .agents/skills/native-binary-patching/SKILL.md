---
name: native-binary-patching
description: ARM64 ELF and binary asset patching guidelines covering libchrome.so host redirection, multi-candidate offset resolution, companion .so trimming, and Hermes bytecode patching.
---

# Native & Binary Asset Patching Guidelines

## 1. In-Situ Binary String Redirection (`libchrome.so`)

Any binary modification executed inside `rawResourcePatch` targeting ELF libraries (such as `lib/arm64-v8a/libchrome.so`) must adhere to strict bounds and fingerprint verification protocols.

### Production Pattern (`BraveBlockTelemetryPatch.kt`)
Redirection overwrites domain strings in-place with `0.0.0.0` followed by null bytes (`0x00`). The replacement length must exactly match the original string length to preserve ELF string table boundaries and segment offsets:

```kotlin
val soFile = get("lib/arm64-v8a/libchrome.so")
if (!soFile.exists()) {
    return@execute
}

data class HostEntry(
    val offset: Long,
    val hostName: String,
)

val hostEntries = listOf(
    HostEntry(0x001f9329L, "star-randsrv.bsg.brave.com"),
    HostEntry(0x001f935aL, "collector.bsg.brave.com"),
    HostEntry(0x001f9391L, "usage-ping.brave.com"),
)

val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
var writtenHosts = 0

RandomAccessFile(soFile, "rw").use { raf ->
    for (entry in hostEntries) {
        val expectedBytes = entry.hostName.toByteArray(Charsets.US_ASCII)
        val len = expectedBytes.size
        if (entry.offset + len > raf.length()) {
            throw PatchException("Host offset 0x${entry.offset.toString(16)} out of bounds in libchrome.so")
        }
        val buf = ByteArray(len)
        raf.seek(entry.offset)
        raf.readFully(buf)
        if (!buf.contentEquals(expectedBytes)) {
            throw PatchException(
                "Host fingerprint mismatch at 0x${entry.offset.toString(16)}. " +
                    "Expected: ${entry.hostName}, Found: ${String(buf, Charsets.US_ASCII)}",
            )
        }

        // Construct replacement: "0.0.0.0" + null byte + zero padding to original length
        val replacement = ByteArray(len)
        System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
        // Remaining bytes default to 0x00 (null padded)

        raf.seek(entry.offset)
        raf.write(replacement)
        writtenHosts++
    }
}

println("[Block Telemetry] Redirected $writtenHosts / ${hostEntries.size} endpoints to 0.0.0.0 in libchrome.so")
```

---

## 2. Multi-Candidate Offset Resolution

When string table offsets shift slightly between release builds or regional variants, resolve the target offset dynamically from a candidate list rather than hardcoding a single fragile pointer.

### Production Pattern (`libchrome.so` Multi-Candidate Resolution)
```kotlin
val syncOffsets = listOf(0x0031b2b2L, 0x0031b2b5L, 0x0031b34fL)
val targetUrl = "https://endpoint.example.com/v1/telemetry"
val expectedBytes = targetUrl.toByteArray(Charsets.US_ASCII)
val len = expectedBytes.size
val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)

RandomAccessFile(soFile, "rw").use { raf ->
    val validOffset = syncOffsets.firstOrNull { offset ->
        if (offset + len > raf.length()) return@firstOrNull false
        val buf = ByteArray(len)
        raf.seek(offset)
        raf.readFully(buf)
        buf.contentEquals(expectedBytes)
    } ?: throw PatchException(
        "Sync fingerprint mismatch for $targetUrl in libchrome.so",
    )

    val replacement = ByteArray(len)
    System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)

    raf.seek(validOffset)
    raf.write(replacement)
}
```

---

## 3. Companion Native Library Bloat Trimming

When applications package unused third-party native binaries (e.g. AI models, XR runtime, or bundled VPN binaries), zeroing them out via `writeBytes(byteArrayOf())` preserves file entries in the ZIP header while drastically reducing APK footprint.

### Production Pattern (`BraveNativeBloatSlimmerPatch.kt`)
```kotlin
private val EMPTY_STUB_BYTES = byteArrayOf()

private val BLOAT_NATIVE_LIBS = listOf(
    "libimpress_api_jni.so",
    "libwg-go.so",
    "libandroidx.xr.arcore.openxr.so",
    "libarcore_sdk_c.so",
)

val libDir = get("lib/arm64-v8a")
if (libDir.exists() && libDir.isDirectory) {
    BLOAT_NATIVE_LIBS.forEach { libName ->
        val libFile = get("lib/arm64-v8a/$libName")
        if (libFile.exists() && libFile.isFile) {
            val originalSize = libFile.length()
            if (originalSize > 0) {
                libFile.writeBytes(EMPTY_STUB_BYTES)
                savedBytes += (originalSize - libFile.length())
                strippedLibs.add(libName)
            }
        }
    }
}
```

---

## 4. React Native Hermes Bytecode Asset Patching

For React Native Android applications utilizing the Hermes JavaScript engine, the precompiled bundle is stored as a binary asset (`assets/index.android.bundle`). `rawResourcePatch` can parse and mutate Hermes Bytecode (HBC) directly.

### Production Pattern (`HevyUnlockProPatch.kt`)
1. **Magic & Version Verification**: Assert Hermes magic number (`0x1f1903c103bc1fc6L`) and target HBC version (e.g. HBC96 = `96`).
2. **Table Parsing**: Compute 4-byte aligned offsets for function headers, string table, overflow string table, and string storage.
3. **Target Function / Getter Identification**: Locate function index by matching string identifier table entries (e.g. `"isPro"`, `"isPaying"`).
4. **Bytecode Opcode Injection**: Overwrite the target function prologue with forced return instructions:
   - Hermes bytecode opcodes: `LoadConstTrue r0` (`0x78 0x00`) followed by `Ret r0` (`0x5C 0x00`).
   ```kotlin
   private val FORCE_TRUE_PROLOGUE = byteArrayOf(0x78.toByte(), 0x00, 0x5C.toByte(), 0x00)
   ```
5. **Write Back**: Write modified byte array back to `bundleFile.writeBytes(bundleBytes)`.
