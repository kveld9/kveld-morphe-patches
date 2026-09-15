## [1.29.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.28.1...v1.29.0) (2026-09-15)

### New Features

* **hevy:** bump target version to 3.1.14 and support APKM in test harness ([470f49a](https://github.com/kveld9/kveld-morphe-patches/commit/470f49a9b101a4859e887d36189754a23f893f77))

## [1.28.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.28.0...v1.28.1) (2026-09-15)

### Bug Fixes

* **docs:** correct APKMirror download badge URLs for Gboard and TikTok ([8baddef](https://github.com/kveld9/kveld-morphe-patches/commit/8baddeff14f744b82fc0122b8ece5cc737c8bc16))

## [1.28.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.27.2...v1.28.0) (2026-09-15)

### New Features

* **vivaldi:** update target version to 8.2.4147.77 and resolve libchrome offsets ([be8a275](https://github.com/kveld9/kveld-morphe-patches/commit/be8a2750455db88ce3b85033ed2dcc91e9091b34))

## [1.27.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.27.1...v1.27.2) (2026-09-15)

### Bug Fixes

* **tiktok:** resolve ART SIGSEGV on feed loading and video playback ([06f59be](https://github.com/kveld9/kveld-morphe-patches/commit/06f59be19c7fa8469134473512e5660228136e71)), closes [#31](https://github.com/kveld9/kveld-morphe-patches/issues/31)

## [1.27.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.27.0...v1.27.1) (2026-09-14)

### Bug Fixes

* **tiktok:** correct register indices and speed persistence bounds ([05307bb](https://github.com/kveld9/kveld-morphe-patches/commit/05307bb2de40a4134048553b505c22ff687478a7))

## [1.27.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.26.1...v1.27.0) (2026-09-14)

### New Features

* **tiktok:** add playback speed persistence patch ([7f65ac3](https://github.com/kveld9/kveld-morphe-patches/commit/7f65ac35991dedb61370d2cd26910af38ee794e8)), closes [#25](https://github.com/kveld9/kveld-morphe-patches/issues/25)

## [1.26.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.26.0...v1.26.1) (2026-09-14)

### Bug Fixes

* **tiktok:** fix watermark removal and stream redirection ([42fab99](https://github.com/kveld9/kveld-morphe-patches/commit/42fab99d5e6c0ec9000c45c728821e2dc3267b5b))

## [1.26.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.25.2...v1.26.0) (2026-09-14)

### New Features

* **gboard:** add clipboard enhancements patch ([2a13c2e](https://github.com/kveld9/kveld-morphe-patches/commit/2a13c2e5dd5eacdee4f6c19893f3b5b293318129))

## [1.25.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.25.1...v1.25.2) (2026-09-14)

### Bug Fixes

* **brave:** resolve ArrayIndexOutOfBoundsException in BraveBlockTelemetryPatch ([3888c2b](https://github.com/kveld9/kveld-morphe-patches/commit/3888c2b01ea8e1e353f2e27345ddfea27af84831)), closes [#27](https://github.com/kveld9/kveld-morphe-patches/issues/27)

### Code Refactoring

* strip emojis across codebase and enforce strict prohibition in tooling and governance ([7ccd0b8](https://github.com/kveld9/kveld-morphe-patches/commit/7ccd0b8760532f6bc5c5cf795326fe69681b4b5c))

## [1.25.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.25.0...v1.25.1) (2026-09-14)

### 🐛 Bug Fixes

* **vivaldi:** resolve label index out of bounds in startup performance patch ([4260d59](https://github.com/kveld9/kveld-morphe-patches/commit/4260d594d549a43962dfbd8339b5057668c41eb6))

## [1.25.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.24.0...v1.25.0) (2026-09-14)

### ✨ New Features

* **test:** enforce in-situ morphe patcher verification gate across all targets ([9da9c51](https://github.com/kveld9/kveld-morphe-patches/commit/9da9c51b6589d39d53295fa1ea44119c4ddde02f))

## [1.24.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.23.1...v1.24.0) (2026-09-14)

### ✨ New Features

* **tiktok:** pin target to v46.9.3 and decouple 16 independent patches ([80d30c1](https://github.com/kveld9/kveld-morphe-patches/commit/80d30c1630b757b95e40dea98655d6a01b4675fd))

### ♻️ Code Refactoring

* **patches:** add diagnostic skip logging and migrate label instructions ([cfaad44](https://github.com/kveld9/kveld-morphe-patches/commit/cfaad444e3dc58d2bdd7ee10061804c261ef92b0))

## [1.23.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.23.0...v1.23.1) (2026-09-13)

### 🐛 Bug Fixes

* **tiktok:** add multi-version fallbacks for cold start and fresco memory governor ([4b86116](https://github.com/kveld9/kveld-morphe-patches/commit/4b8611610d8e7a9e19665fbf6b3a3069325342de))

## [1.23.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.22.2...v1.23.0) (2026-09-13)

### ✨ New Features

* **tiktok:** implement universal feed ad blocker patch ([b2b2a45](https://github.com/kveld9/kveld-morphe-patches/commit/b2b2a45353a325128d447cf4384621e6b0faca72)), closes [#23](https://github.com/kveld9/kveld-morphe-patches/issues/23)

## [1.22.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.22.1...v1.22.2) (2026-09-13)

### 🐛 Bug Fixes

* **tiktok:** add v46.x+ fingerprint compatibility with backward fallback ([b3de916](https://github.com/kveld9/kveld-morphe-patches/commit/b3de9162e106f7a6ac77f8793d70434088401bfb))

## [1.22.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.22.0...v1.22.1) (2026-09-13)

### ♻️ Code Refactoring

* **harness:** eradicate hardcoded identifiers and harden validation toolchain ([57c23e5](https://github.com/kveld9/kveld-morphe-patches/commit/57c23e5d5d543c1ea6c5884ac743072230b48ee9))

## [1.22.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.21.2...v1.22.0) (2026-09-12)

### ✨ New Features

* **tiktok:** integrate TikLite patch suite and validation harness ([f3006d1](https://github.com/kveld9/kveld-morphe-patches/commit/f3006d1730ed4b685e9d71d5ce7fb3e524d3010d))

## [1.21.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.21.1...v1.21.2) (2026-09-12)

### ♻️ Code Refactoring

* **brave:** resolve boolean field dynamically in startup performance patch ([858068e](https://github.com/kveld9/kveld-morphe-patches/commit/858068e7fb1176786ce32be9e5055a685ee782c9))

## [1.21.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.21.0...v1.21.1) (2026-09-12)

### 🐛 Bug Fixes

* **vivaldi:** neutralize donation and search engine bottom sheet prompt ([ca96f0f](https://github.com/kveld9/kveld-morphe-patches/commit/ca96f0ff342795beddb1a25265aaaae1c04a0723)), closes [#22](https://github.com/kveld9/kveld-morphe-patches/issues/22)

## [1.21.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.20.1...v1.21.0) (2026-09-11)

### ✨ New Features

* **brave:** update target to v1.95.101 and align libchrome offsets ([f88b2ef](https://github.com/kveld9/kveld-morphe-patches/commit/f88b2efd15a4a78692087aaa5f1f48ed0643bfea))

## [1.20.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.20.0...v1.20.1) (2026-09-11)

### 🐛 Bug Fixes

* **vivaldi:** clarify APKM bundle requirement and update issue templates ([1944b0b](https://github.com/kveld9/kveld-morphe-patches/commit/1944b0b2e47593f0bb5a976deae4fcd109b7361c))

## [1.20.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.19.0...v1.20.0) (2026-09-11)

### ✨ New Features

* **hevy:** enhance pro unlocking, neutralize play billing, and add auth guide ([bace76c](https://github.com/kveld9/kveld-morphe-patches/commit/bace76cf8204ee0244a8e8fbed820c0e5c1c65f5))

## [1.19.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.18.0...v1.19.0) (2026-09-11)

### ✨ New Features

* **hevy:** add pro unlock, telemetry blocking, and resource optimization patches ([2dde79c](https://github.com/kveld9/kveld-morphe-patches/commit/2dde79cc5a1b3c4d3f2f2d2a8f70cad69388312e))
* **shared:** promote locale slimmer to universal and add apk junk cleaner ([a9127bb](https://github.com/kveld9/kveld-morphe-patches/commit/a9127bbc2123a9a6d5b52aaaf0516ae186d18f97))

## [1.18.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.17.0...v1.18.0) (2026-09-10)

### ✨ New Features

* **vivaldi:** add support for Vivaldi Browser Stable v8.2.4147.58 ([b81c003](https://github.com/kveld9/kveld-morphe-patches/commit/b81c003c07840789313aa6a66399bd025572a0ef))

## [1.17.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.16.0...v1.17.0) (2026-09-10)

### ✨ New Features

* **gboard:** update compatibility to Gboard Lite v18.2.4 ([7d52471](https://github.com/kveld9/kveld-morphe-patches/commit/7d52471f94bb918b7522231501280c67e86b84ce))

## [1.16.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.15.0...v1.16.0) (2026-09-09)

### 🐛 Bug Fixes

* **docs:** fix Gboard Lite supported versions table formatting in README ([6a087f4](https://github.com/kveld9/kveld-morphe-patches/commit/6a087f4558155c1ee3f2497a48649c07e92fb6e0))

### ✨ New Features

* **gboard:** add armeabi-v7a and lite_release compatibility ([83c8bb8](https://github.com/kveld9/kveld-morphe-patches/commit/83c8bb8844109c5364dadf55ad8f36440b17ad76))

## [1.15.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.14.0...v1.15.0) (2026-09-09)

### ✨ New Features

* **patches:** add DPI Resource Slimmer patch ([f98d7ce](https://github.com/kveld9/kveld-morphe-patches/commit/f98d7ce0091ef14fd7d684bcc2f99bd67db7edb7)), closes [#16](https://github.com/kveld9/kveld-morphe-patches/issues/16)

## [1.14.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.13.1...v1.14.0) (2026-09-09)

### ✨ New Features

* **vivaldi:** update target to v8.2.4147.50 and update native offsets ([927a551](https://github.com/kveld9/kveld-morphe-patches/commit/927a5511d977cf14331b25b223729fc009ccaec9))

## [1.13.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.13.0...v1.13.1) (2026-09-07)

### 🐛 Bug Fixes

* **patches:** ensure safe en-US fallback in locale slimmer to prevent startup crashes ([af6e495](https://github.com/kveld9/kveld-morphe-patches/commit/af6e4951b02a65af1f9b123cf81a2995287294b1))

## [1.13.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.12.0...v1.13.0) (2026-09-05)

### ✨ New Features

* **brave:** update target to v1.94.121 and align libchrome offsets ([fe413fd](https://github.com/kveld9/kveld-morphe-patches/commit/fe413fd7b1c02e4518faa553cd6733eb222e33f1))

## [1.12.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.11.0...v1.12.0) (2026-09-03)

### ✨ New Features

* **vivaldi:** update patches for v8.2.4147.28 and align libchrome offsets ([b37b01d](https://github.com/kveld9/kveld-morphe-patches/commit/b37b01d935425e3df752a07d0b1f83cdb5c55945))

## [1.11.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.10.1...v1.11.0) (2026-09-02)

### ✨ New Features

* **brave:** update patches for v1.94.119 and validate on physical arm64 device (3e1fc19)

## [1.10.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.10.0...v1.10.1) (2026-08-30)

### 🐛 Bug Fixes

* **brave:** disable native bloat and locale slimmers by default ([9874434](https://github.com/kveld9/kveld-morphe-patches/commit/98744343ba53641e30dedbc3bc7de34c08a64606))

## [1.10.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.9.0...v1.10.0) (2026-08-30)

### ✨ New Features

* **vivaldi:** prevent persistent tab restoration on startup ([a34673e](https://github.com/kveld9/kveld-morphe-patches/commit/a34673e8b12fb4b99e8b41ad8ab0aef6d19e1b1a))

## [1.9.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.8.0...v1.9.0) (2026-08-29)

### ✨ New Features

* update Vivaldi Browser Snapshot target to v8.2.4145.4 ([9fe9ce4](https://github.com/kveld9/kveld-morphe-patches/commit/9fe9ce4c68b7488a2be6a95e99881ce9b51601cc))

## [1.8.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.7.0...v1.8.0) (2026-08-29)

### ✨ New Features

* support Brave v1.94.117 and Gboard Lite v18.1.3 ([fb6a47e](https://github.com/kveld9/kveld-morphe-patches/commit/fb6a47e93510d8ca61043a8279898d9db457f6fe))

## [1.7.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.6.0...v1.7.0) (2026-08-27)

### ✨ New Features

* add Vivaldi Browser support, asset slimmers, and dynamic diagnostic telemetry ([9f83f8c](https://github.com/kveld9/kveld-morphe-patches/commit/9f83f8c846e8c39c3209cc4953567637866d4b84))

## [1.6.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.5.0...v1.6.0) (2026-08-24)

### ✨ New Features

* **gboard:** remove incomplete Free Cursor 2D Trackpad patch ([2a9e760](https://github.com/kveld9/kveld-morphe-patches/commit/2a9e7602266229f1cb36ae6110f57e11dbd0b0b0))

## [1.5.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.4.0...v1.5.0) (2026-08-23)

### ✨ New Features

* **brave:** add Background Sync, Battery Optimization, and Disable Pull-to-Refresh patches (fe89f48)

## [1.4.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.3.0...v1.4.0) (2026-08-22)

### ✨ New Features

* **gboard:** add Clone Gboard patch, tune recommended defaults, and fix Lottie crash ([9e5a4b3](https://github.com/kveld9/kveld-morphe-patches/commit/9e5a4b36dd7d5f807054a027643d70736de2dcd6))

## [1.3.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.2.0...v1.3.0) (2026-08-22)

### ✨ New Features

* **patches:** add new slimming patches and update Brave support to v1.93.138 ([690339f](https://github.com/kveld9/kveld-morphe-patches/commit/690339f78009eed2d7afb9ff0f7d1ae24c016fab))

## [1.2.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.1.0...v1.2.0) (2026-08-20)

### ✨ New Features

* **gboard:** add modular patch suite for Gboard Lite v18.0.3 ([4c9351e](https://github.com/kveld9/kveld-morphe-patches/commit/4c9351e6b8af7014cd60922997b75561f30fc0ee))

## [1.1.0](https://github.com/kveld9/brave-patches/compare/v1.0.1...v1.1.0) (2026-08-20)

### ✨ New Features

* target Brave v1.93.137 and add GitHub releases source note ([c4fa325](https://github.com/kveld9/brave-patches/commit/c4fa3257f674ecf6eb3a8ee47a9f7edb96ebc21b))

## [1.0.1](https://github.com/kveld9/brave-patches/compare/v1.0.0...v1.0.1) (2026-08-20)

### 🚀 Updated App Support

* update Brave support to v1.93.137 ([4445496](https://github.com/kveld9/brave-patches/commit/4445496720efe316358f1c566450ddaa335dcc00))

## 1.0.0 (2026-08-20)

### ✨ New Features

* initial Brave patches suite ([1f5a92c](https://github.com/kveld9/brave-patches/commit/1f5a92cc2d7af2d83b2847b08e98d9f3f703d4fa))
