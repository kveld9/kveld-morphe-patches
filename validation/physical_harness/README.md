# Morphe Patches — Suite de Validación Física en Dispositivos ARM64

Esta suite automatiza la recolección de métricas, eventos de batería, tareas de Background Sync, wake locks de MediaSession y gestos de Pull-To-Refresh mediante ADB en un dispositivo Android ARM64 real.

---

## Estructura del Harness

```
validation/physical_harness/
├── run_harness.py                 # Orquestador principal CLI
├── server/                        # Servidor HTTP local para páginas diagnósticas
│   ├── battery_test.html          # Test 1: Eventos Battery Status API
│   ├── background_sync_test.html  # Test 2: ServiceWorker Sync / PeriodicSync
│   ├── sw.js                      # ServiceWorker de prueba
│   ├── media_test.html            # Test 3: Audio/Video MediaSession WakeLock
│   └── pull_to_refresh_test.html  # Test 4: Contador de reload y scroll DOM
├── scripts/
│   ├── common.py                  # Conexión ADB, dumpsys y port forwarding
│   ├── test_battery.py            # Runner Test 1 (Battery)
│   ├── test_background_sync.py    # Runner Test 2 (Background Sync)
│   ├── test_media_wakelock.py     # Runner Test 3 (Media WakeLock A/B)
│   ├── test_pull_to_refresh.py    # Runner Test 4 (Pull To Refresh)
│   └── compare_results.py         # Comparador Vanilla vs Patched
└── results/                       # Volcados de logcat, dumpsys y métricas
```

---

## Procedimiento Paso a Paso

### Paso 1: Conectar el Teléfono ARM64
1. Habilita **Depuración por USB (USB Debugging)** en las Opciones de Desarrollador de tu dispositivo.
2. Conéctalo por cable USB a la computadora.
3. Verifica que ADB lo reconozca:
   ```bash
   adb devices
   ```

---

### Paso 2: Ejecutar la Prueba en Modo VANILLA
1. Instala el APK Vanilla de Brave 1.93.138:
   ```bash
   adb install -r "apks-ultima-version/BraveMonoarm64 (1).apk"
   ```
2. Ejecuta la suite de pruebas para Vanilla:
   ```bash
   cd validation/physical_harness
   python run_harness.py --mode vanilla
   ```
   *El script iniciará el servidor de diagnóstico en segundo plano, ejecutará los 4 tests secuencialmente y guardará los volcados en `results/vanilla/`.*

---

### Paso 3: Ejecutar la Prueba en Modo PATCHED
1. Parchea e instala el APK de Brave con el bundle `.mpp` compilado (`patches/build/libs/patches-1.3.0.mpp`).
2. Instálalo en el teléfono:
   ```bash
   adb install -r <ruta-al-apk-parcheado.apk>
   ```
3. Ejecuta la suite de pruebas para Patched:
   ```bash
   python run_harness.py --mode patched
   ```
   *El script repetirá exactamente las mismas secuencias y guardará los volcados en `results/patched/`.*

---

### Paso 4: Generar el Informe Comparativo Final
Ejecuta el comparador:
```bash
python run_harness.py --compare
```

El script analizará los logs y dumpsys de ambas ejecuciones y generará el informe final evaluando cada mecanismo con:
* **PASS**
* **FAIL**
* **INCONCLUSIVE**
