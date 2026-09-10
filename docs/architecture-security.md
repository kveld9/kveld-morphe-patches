# 🛡️ Architecture & Security Notes

## Brave Browser: Privacy Scanner False Positives

> [!NOTE]
> Component scanners such as App Manager or Exodus Privacy may flag Google Play Billing and Google ML Kit components as "trackers" inside Brave. These are false positives caused by generic package name signatures.

When inspecting Brave with package analysis tools, the following components may be highlighted:

| Component | Origin / Library | Actual Function | Privacy & Telemetry Impact |
| :--- | :--- | :--- | :--- |
| `com.android.billingclient.api.ProxyBillingActivity`<br>`com.android.billingclient.api.ProxyBillingActivityV2` | **Google Play Billing** (`billingclient`) | Handles user-initiated in-app subscriptions (Brave Leo AI Premium, Brave VPN). | **None (Transactional only).** These are trampoline UI activities for Google Play checkout sheets. They collect zero browsing analytics or telemetry. Stripping them breaks subscription handling and triggers `ActivityNotFoundException`. |
| `com.google.mlkit.common.internal.MlKitInitProvider`<br>`com.google.mlkit.common.internal.MlKitComponentDiscoveryService` | **Google ML Kit** (`mlkit.common`) | Powers local, on-device OCR and vision features (e.g. camera-based credit card scanning in Autofill and QR code scanning). | **None (On-device execution).** Flagged by Exodus because ML Kit uses the Firebase component dependency injector (`CommonComponentRegistrar`). All model operations run strictly local to the device. |

---

## Genuine Telemetry Neutralization

Genuine Brave telemetry is fully neutralized by the **Block Brave Telemetry** patch:
- **P3A (Privacy-Preserving Product Analytics)**: Preference getters forced to return `false` in Dalvik bytecode (`PrefService.e`).
- **Brave Stats & Web Discovery Project (WDP)**: Reporting loops disabled and all 7 telemetry endpoints redirected to `0.0.0.0` in `libchrome.so`.
- **Crashpad & Minidump Uploads**: Upload hooks aborted before dispatch (`MinidumpUploadServiceImpl`, `ChromeMinidumpUploadJobService`) and endpoints zeroed in native binary.
- **Variations Seed Fetching**: Blocked before HTTP socket creation (`IOException("Blocked by Morphe")`).
