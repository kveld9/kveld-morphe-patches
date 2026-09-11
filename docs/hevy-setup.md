# 🏋️ Hevy: Setup & Authentication Guide

## Target Variant Selection

> [!IMPORTANT]
> Download the official release bundle from [APKMirror (Hevy - Gym Log Workout Tracker)](https://www.apkmirror.com/apk/hevy/hevy-gym-log-workout-tracker/):
> - **Format**: `BUNDLE` (APKM / Split APKs)
> - **Architecture**: `arm64-v8a`
> - **Target Version**: `3.1.13`

Morphe Manager automatically extracts and merges the necessary split modules (`base.apk`, `split_config.arm64_v8a.apk`, device DPI, and locale splits) into a unified installable APK during patching.

---

## Authentication: Google Sign-In vs. Email Login

> [!WARNING]
> **Google Sign-In will fail with `"Error al iniciar sesión con Google"` (or `DEVELOPER_ERROR 10` / `SIGN_IN_FAILED 12500`).**
> **You must authenticate using Email and Password.**

### Root Cause
Google Play Services OAuth 2.0 enforces cryptographic certificate verification. When Google Sign-In is triggered:
1. Google Play Services queries Android's `PackageManager` for the caller's signing certificate SHA-1 fingerprint.
2. The Google OAuth authentication backend verifies whether the SHA-1 matches the official production keystore registered in Hevy's Google Developer Console.
3. Because Morphe Manager must re-sign modified APKs with a local private keystore, Google Play Services detects the certificate mismatch and rejects the authentication request.

### Solution: Email & Password Authentication

Hevy's native backend API (`api.hevyapp.com`) authenticates email and password credentials directly without relying on Google Play Services signature checks. Cloud synchronization, user profiles, workout history, and Pro features operate normally through this channel.

#### A. If your account was originally created with Google:
1. On the initial login screen, tap **`¿Tienes una cuenta? Iniciar sesión`** (or **`Log In`**).
2. Tap **`¿Olvidaste tu contraseña?`** (or **`Forgot password?`**).
3. Enter your Google account email address.
4. Open the password reset email sent by Hevy and set a dedicated password for your account.
5. Return to the app and log in using your **email and newly created password**.

#### B. If creating a new account:
- Tap **`Registrarse con correo electrónico`** (or **`Sign up with email`**) instead of the Google button.

---

## Patch Capabilities & Features

| Patch | Description | Recommended |
| :--- | :--- | :---: |
| **Unlock Pro** | Unlocks local Hevy Pro capabilities (unlimited workout routines, routine folders, advanced graphs, and local analytics) via Hermes Bytecode (HBC96) Pro getter overrides. | ✅ Yes |
| **Block Hevy Telemetry & Trackers** | Disables Sentry native SDK crash reporting, Adjust attribution queues, Facebook AppEvents telemetry, Branch referral links, and WearOS background sync. | ✅ Yes |
| **Hevy Battery Optimization** | Neutralizes background WorkManager alarms, periodic job schedulers, Firebase wakeups, and DataTransport schedulers to eliminate idle battery drain. | ✅ Yes |
| **Hevy Resource Slimmer** | Strips embedded onboarding MP4 tutorial video and heavy photo editor stickers/textures, reducing APK size by ~12 MB. | ✅ Yes |
| **Universal Slimmers & Cleaners** | `APK Junk Cleaner`, `DPI Resource Slimmer`, and `PNG Asset Optimizer` further reduce APK size and optimize resources. | ✅ Yes |

---

## Verifying Pro Capabilities (Free Label vs. Unlocked Features)

> [!NOTE]
> **Why does the Profile or Settings screen still say "Free" or show "Get Hevy Pro"?**
> The account badge in your profile is populated directly by Hevy's backend servers (`api.hevyapp.com`), reflecting your remote subscription status in their database. The patch modifies client-side JavaScript execution in Hermes Bytecode (Pro property getters `isPro`, `isPaying`, `isInGracePeriod`) rather than remote server records.

To verify that **Hevy Pro is active and functioning**:

1. **Unlimited Workout Routines (Bypass 4-Routine Cap):**
   - Free accounts are strictly capped at **4 routine templates**.
   - Navigate to the **Workout** tab and create a **5th routine**. On Free, a blocking paywall appears (*"You've reached the 4 routine limit"*). With the patch, you can create and save unlimited routines.
2. **Routine Folders:**
   - Go to **Workout > Routines** and tap **New Folder**. In Free, folder creation is locked behind Pro. With the patch, you can create and organize folders freely.
3. **Advanced Progress Graphs & Exercise History:**
   - Open any exercise detail view and inspect the progress/analytics charts (e.g. 1RM progression, estimated volume). Paywalled charts are unlocked and fully readable.

