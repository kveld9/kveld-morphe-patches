# ⌨️ Gboard Lite: Offline Dictionary & Glide Typing Setup

## Target Variant Selection

> [!IMPORTANT]
> Always download the standalone `lite` / `lite_beta` APK (nodpi) from [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/):
> - **Format**: `APK` *(Do **NOT** download `BUNDLE` / split packages)*
> - **Architecture**: `arm64-v8a` or `armeabi-v7a`
> - **Screen DPI**: `nodpi`

---

## Predictive Text & Glide Typing on Fresh Installations

> [!IMPORTANT]
> **Gboard Lite does not bundle language dictionaries, predictive text models, or gesture/glide typing decoding models inside the APK.**
> Unlike the full Gboard APK, Gboard Lite downloads language models on-demand upon first launch via Google's **MDD (Mobile Data Download)** and **Superpacks** subsystems.

If you perform a clean install of Gboard Lite with background sync debloat patches enabled, Gboard will be prevented from downloading the initial dictionary and gesture model pack for your language. This results in an empty suggestion bar, no predictive text, and **Glide Typing (swipe to type) not working**.

### Setup Procedure:

1. **When patching for a clean install (or when adding new languages):**
   - **Temporarily uncheck:**
     - ❌ `Disable MDD Background Sync`
     - ❌ `Disable Superpacks Eager Sync`
     - ❌ `Disable WorkManager`
   - *(Also ensure `Force Incognito Mode` is unchecked if you want personalized learning and history).*

2. **Open Gboard once with an active Internet connection:**
   - Type a few words, test a swipe gesture, or navigate to *Gboard Settings > Languages* so it downloads your language dictionary and gesture pack into local storage (`/data/data/...`).

3. **Re-apply debloat patches (Optional):**
   - Once your language packs are cached locally on device, you can re-patch with `Disable MDD Background Sync`, `Disable Superpacks Eager Sync`, and `Disable WorkManager` to freeze background network traffic and disk polling.
