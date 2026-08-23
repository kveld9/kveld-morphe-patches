# Morphe Patches — ARM64 Physical Device Validation Suite

This test suite automates metric collection, battery event tracking, Background Sync job monitoring, MediaSession wake lock assertions, and Pull-To-Refresh gesture validation via ADB on physical ARM64 Android devices.

---

## Harness Structure

```
validation/physical_harness/
├── run_harness.py                 # Primary CLI orchestrator
├── server/                        # Local HTTP diagnostic server
│   ├── battery_test.html          # Test 1: Battery Status API events
│   ├── background_sync_test.html  # Test 2: ServiceWorker Sync / PeriodicSync
│   ├── sw.js                      # Test ServiceWorker
│   ├── media_test.html            # Test 3: Audio/Video MediaSession WakeLock
│   └── pull_to_refresh_test.html  # Test 4: Reload counter and DOM scroll
├── scripts/
│   ├── common.py                  # ADB connection, dumpsys, and port forwarding
│   ├── test_battery.py            # Test 1 Runner (Battery)
│   ├── test_background_sync.py    # Test 2 Runner (Background Sync)
│   ├── test_media_wakelock.py     # Test 3 Runner (Media WakeLock A/B)
│   ├── test_pull_to_refresh.py    # Test 4 Runner (Pull To Refresh)
│   └── compare_results.py         # Vanilla vs Patched comparator
└── results/                       # Logcat, dumpsys, and metrics dumps
```

---

## Step-by-Step Procedure

### Step 1: Connect ARM64 Device
1. Enable **USB Debugging** in Developer Options on your device.
2. Connect it via USB cable to your computer.
3. Verify ADB detects it:
   ```bash
   adb devices
   ```

---

### Step 2: Run Tests in VANILLA Mode
1. Install the Vanilla Brave APK (v1.93.138):
   ```bash
   adb install -r "apks-ultima-version/BraveMonoarm64 (1).apk"
   ```
2. Execute the test suite for Vanilla:
   ```bash
   cd validation/physical_harness
   python run_harness.py --mode vanilla
   ```
   *The script will launch the background diagnostic server, execute all tests sequentially, and save results in `results/vanilla/`.*

---

### Step 3: Run Tests in PATCHED Mode
1. Patch and install the Brave APK using the compiled `.mpp` bundle (`patches/build/libs/patches-*.mpp`).
2. Install it on the device:
   ```bash
   adb install -r <path-to-patched-apk.apk>
   ```
3. Execute the test suite for Patched:
   ```bash
   python run_harness.py --mode patched
   ```
   *The script will repeat the exact test sequence and save results in `results/patched/`.*

---

### Step 4: Generate Comparative Validation Report
Run the comparator:
```bash
python run_harness.py --compare
```

The script will analyze logs and dumpsys outputs from both runs and generate the final report evaluating each mechanism:
* **PASS**
* **FAIL**
* **INCONCLUSIVE**
