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

## Pro Capabilities: Client-Side Unlocks vs. Server-Side Limitations

The **Unlock Pro** patch operates strictly on client-side JavaScript execution by overriding property getters (`isPro`, `isPaying`, `isInGracePeriod`, `isWithinProOfflineGracePeriod`) within the React Native Hermes Bytecode (`assets/index.android.bundle`).

Because remote database records on `api.hevyapp.com` are not modified, features are divided into client-side gated (fully unlocked) and server-side gated (governed by remote API rules):

### 1. Unlocked Capabilities (Client-Side Gated)

To verify that **Hevy Pro is active and functioning**:

1. **Unlimited Workout Routines (Bypass 4-Routine Cap):**
   - Free accounts are strictly capped at **4 routine templates**.
   - Navigate to the **Workout** tab and create a **5th routine**. On Free, a blocking paywall appears (*"You've reached the 4 routine limit"*). With the patch, you can create, save, and sync unlimited routines.
2. **Routine Folders:**
   - Go to **Workout > Routines** and tap **New Folder**. In Free, folder creation is locked behind Pro. With the patch, you can create and organize folders freely.
3. **Advanced Progress Graphs & Exercise History:**
   - Open any exercise detail view and inspect the progress/analytics charts (e.g. 1RM progression, estimated volume, set distribution). Paywalled charts are unlocked and fully readable.

---

### 2. Server-Side Limitations & Known Errors

| Feature / Behavior | Technical Reason | User Experience / Error |
| :--- | :--- | :--- |
| **Custom Exercises (> 7)** | `POST https://api.hevyapp.com/custom_exercise_template` is strictly validated by the backend database against free tier quotas. | Shows **`Failed to save exercise`** popup dialog. |
| **Profile "Free" Badge** | User profile status is returned directly in backend session JSON payloads. | Profile and settings show "Free" / "Get Hevy Pro". |
| **Deep Server Sync Features** | Server-side background jobs that query account entitlement tables. | Governed by remote database records. |

#### Diagnosing "Failed to save exercise"

> [!WARNING]
> **Why does saving a custom exercise fail with `"Failed to save exercise"`?**
>
> 1. **7 Custom Exercises Backend Quota (Primary Cause):**
>    - Hevy's backend restricts Free accounts to a maximum of **7 custom exercises**.
>    - When tapping **Save** in the *Create Exercise* screen, `CreateExerciseViewModel` invokes `postCustomExerciseTemplate`, which sends an HTTP `POST` to `api.hevyapp.com/custom_exercise_template`.
>    - Since the patch only affects client-side bytecode, the backend recognizes the account as Free. If you already have 7 custom exercises, the server rejects the request.
>    - The app catches the HTTP rejection and displays `createExercise.alert.errorSave` (*"Failed to save exercise"*).
>    - **Resolution:** Delete an unused custom exercise from your exercise library so your account total is below 7, then save the new one.
>
> 2. **Network / Offline Creation:**
>    - Custom exercises are not queued or saved locally in SQLite when offline; the app requires an immediate successful response from `api.hevyapp.com`. If you have no connection or packet loss, saving will fail.
>
> 3. **Stale Session Token:**
>    - If your login token is corrupted or expired, log out and log back in using your Email and Password.


