# 📱 Compatibility & Architecture Policy

## CPU Architecture Support Policy (ARM64 vs ARMv7a)

> [!NOTE]
> **Architecture Matrix Summary:**
> - **Gboard Lite**: Officially supports both **`arm64-v8a` (64-bit)** and **`armeabi-v7a` (32-bit)**.
> - **Brave Browser & Vivaldi Snapshot**: **`arm64-v8a` (64-bit only)**.

### Why Gboard Lite supports 32-bit:
All Gboard Lite patches in this suite operate strictly on Dalvik/ART DEX bytecode and Android XML resources. They contain **zero native `.so` binary dependencies** and run identically on both 64-bit and 32-bit Android runtimes.

### Why Brave & Vivaldi are strictly 64-bit:
Both Chromium-based browsers depend on surgical byte-level patching of the native ELF binary `lib/arm64-v8a/libchrome.so` (redirecting background telemetry, Crashpad crash uploaders, and DirectMatch endpoints to `0.0.0.0` at hardcoded 64-bit virtual memory and file offsets).

Supporting 32-bit ARM for these browsers would require reverse-engineering and maintaining an entirely separate set of 32-bit ELF binary offsets for a legacy target. **Please do not open issues requesting `armeabi-v7a` support for Brave or Vivaldi.**

---

## APK Variant Requirements

### 🦁 Brave Browser: Why `Bravemonoarm64.apk`?
Always use `Bravemonoarm64.apk` from [Brave GitHub Releases](https://github.com/brave/brave-browser/releases). Do **NOT** use `Bravearm64Universal.apk`, `Bravearm64.apk`, or 32-bit/x86 builds.

- **Bytecode Consistency**: `Bravemonoarm64.apk` is the official 64-bit ARM Monochrome build. Other variants (especially *Universal*) use different R8/ProGuard obfuscation passes and aggressive method inlining, causing Dalvik fingerprint mismatches.
- **Native ARM64 Hooks**: Byte-level offsets in `lib/arm64-v8a/libchrome.so` are calculated strictly against ARM64 Monochrome binaries.

### 🔴 Vivaldi Browser: Why Snapshot & Future Transition to Stable
Always download the official `arm64-v8a` APK directly from the [Vivaldi Android Blog](https://vivaldi.com/blog/android/).

- **Extension Support**: Morphe Patches currently targets **Vivaldi Snapshot** to provide native Desktop Extension support and modern Chromium components.
- **Stable Transition Roadmap**: Vivaldi Snapshot is maintained as the primary target **only until Web Extensions support is enabled by default in the stable release** of Vivaldi Browser (`com.vivaldi.browser`), at which point patch compatibility will transition to the stable channel.
