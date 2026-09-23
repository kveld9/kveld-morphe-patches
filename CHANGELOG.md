## [1.44.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.43.0...v1.44.0) (2026-09-23)

### Bug Fixes

* **gboard:** retain ACCESS_NETWORK_STATE to prevent Cronet startup crash ([d0e8c11](https://github.com/kveld9/kveld-morphe-patches/commit/d0e8c11ed8fda7dc6a6d3826491c40103ea67004))

### New Features

* **gboard:** add enable bluetooth microphone patch ([3591653](https://github.com/kveld9/kveld-morphe-patches/commit/359165313891f0b87d0d33db1be22d3848eafe97))
* **gboard:** add enable cursor trackpad patch ([ea459c4](https://github.com/kveld9/kveld-morphe-patches/commit/ea459c442a13ada17903fd7c00a4672a4121446a))
* **gboard:** add enable dismiss suggestions button patch ([8af2bc8](https://github.com/kveld9/kveld-morphe-patches/commit/8af2bc8a318bfe0ab169de624ce7ce74f18030dc))
* **gboard:** add enable emoji scale setting patch ([92dd78a](https://github.com/kveld9/kveld-morphe-patches/commit/92dd78a3a91dd05235b8c682124501509e41a075))
* **gboard:** add enable grammar checker patch ([c350226](https://github.com/kveld9/kveld-morphe-patches/commit/c3502268dfd6836f1a15d8149e4ba3e0e373e643))
* **gboard:** add phenotype flag resilience patch ([cf3e824](https://github.com/kveld9/kveld-morphe-patches/commit/cf3e824d8d3aa46433f9dec94604e913b320fe84))
* **gboard:** add top toolbar item count patch ([393d1f7](https://github.com/kveld9/kveld-morphe-patches/commit/393d1f7db8a10019192b44ff71be65299f185ae3))
* **patches:** add background sync and jobscheduler purge patch ([639415e](https://github.com/kveld9/kveld-morphe-patches/commit/639415e1570270139be4702370c9597fa8f680f2))
* **patches:** add universal native binary trimmer patch ([a5be255](https://github.com/kveld9/kveld-morphe-patches/commit/a5be255613fc5415da85d6b691981405822a8e04))
* **patches:** add universal telemetry neutralizer patch ([7688f2c](https://github.com/kveld9/kveld-morphe-patches/commit/7688f2cb64ce2e28197679d8339fb59af812dd55))
* **patches:** add universal webp asset optimizer patch ([7f45964](https://github.com/kveld9/kveld-morphe-patches/commit/7f4596415708c9d66b52548dad302f39d3de6f8e))

## [1.43.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.42.2...v1.43.0) (2026-09-23)

### Bug Fixes

* **brave:** enforce native library extraction in manifest ([#49](https://github.com/kveld9/kveld-morphe-patches/issues/49)) ([476bbdb](https://github.com/kveld9/kveld-morphe-patches/commit/476bbdb3c931666e7913e893d2d3ac2cbad76f66))
* **shared:** preserve network state by default and bind xml namespace in universal offline patch ([79853fd](https://github.com/kveld9/kveld-morphe-patches/commit/79853fd47cac5f091bb3a9a058e168d7ca233f75))
* **tiktok:** display offline video counter label before download starts ([#40](https://github.com/kveld9/kveld-morphe-patches/issues/40)) ([48b52f6](https://github.com/kveld9/kveld-morphe-patches/commit/48b52f657250b84206586d79575325ef421b382c))
* **tiktok:** eliminate manifest resource patch in device privacy guard to preserve launcher icons (closes [#51](https://github.com/kveld9/kveld-morphe-patches/issues/51)) ([bb7eb8b](https://github.com/kveld9/kveld-morphe-patches/commit/bb7eb8bbb29009de8e95d20362474ede73081381))
* **tiktok:** enforce H.264 stream selection for video downloads and preserve photo mode ([31cb017](https://github.com/kveld9/kveld-morphe-patches/commit/31cb0172793518fa08daa87ca43037b309f1d8b0))

### New Features

* **brave:** support ARMv7a architecture across native patches (closes [#50](https://github.com/kveld9/kveld-morphe-patches/issues/50)) ([d0b4c3c](https://github.com/kveld9/kveld-morphe-patches/commit/d0b4c3c5d9edb5bdcb05ab99495610c732a5afb1))
* **gboard:** add offline only patch with manifest purge and bytecode neutralization ([371fc3d](https://github.com/kveld9/kveld-morphe-patches/commit/371fc3dbcc38bd0719bcd1c6b9f8344117a1405c))
* **nokoprint:** implement modular cleanup and optimization patch suite ([29df8d4](https://github.com/kveld9/kveld-morphe-patches/commit/29df8d42b81ec1ac66827e989c860ca88cd0c73b))
* **shared:** declare NokoPrint target version and compatibility contracts ([31197cd](https://github.com/kveld9/kveld-morphe-patches/commit/31197cd837659cffa78d398cdf4d8f1eb993a49b))
* **shared:** declare Xiaomi Earbuds target version and compatibility contracts ([7aa8452](https://github.com/kveld9/kveld-morphe-patches/commit/7aa8452cb08c090090df038ab29510a313789522))
* **shared:** implement universal offline mode patch stripping network permissions ([7c37c44](https://github.com/kveld9/kveld-morphe-patches/commit/7c37c4493acfe7cc410b0c767f66a50163e080bf))
* **tiktok:** add comment auto-translation patch (closes [#48](https://github.com/kveld9/kveld-morphe-patches/issues/48)) ([20413aa](https://github.com/kveld9/kveld-morphe-patches/commit/20413aafb656bf4c844a84f74828eec0a75649af))
* **xiaomi:** implement modular privacy, audio, and debloat patch suite for Xiaomi Earbuds ([c9e3a89](https://github.com/kveld9/kveld-morphe-patches/commit/c9e3a89308ac3c08a1dcb512d4e6cfd71f9d1bc1))

### Code Refactoring

* **harness:** apply static audit hardening for elf parsing and readme generator ([81dbb48](https://github.com/kveld9/kveld-morphe-patches/commit/81dbb48b4dd6116d02a30dfc88f4ad0322e6fb24))
* **scripts:** update target synchronization regexes for tabular layout ([3353a35](https://github.com/kveld9/kveld-morphe-patches/commit/3353a354b7c58341416c8d360f3d911eba7e1445))
* **shared:** convert universal offline mode options to native boolean toggles ([efa0d7b](https://github.com/kveld9/kveld-morphe-patches/commit/efa0d7b59aaba6f37e3b2090b58e69ec46354eea))
* **tiktok:** clarify bytecode-only privacy model in device privacy guard description ([641e64d](https://github.com/kveld9/kveld-morphe-patches/commit/641e64d702600db7c7d46602b35612a9324f7697))

## [1.42.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.42.1...v1.42.2) (2026-09-21)

### Bug Fixes

* **tiktok:** restore fresco animation frame cache to fix animated sticker lag in comments ([8cc4534](https://github.com/kveld9/kveld-morphe-patches/commit/8cc45347e6b26dcc15fb67f329520894b45f9b2f))

## [1.42.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.42.0...v1.42.1) (2026-09-21)

### Bug Fixes

* **tiktok:** resolve shifted bytecode targets and prune obsolete hooks for v47.0.3 ([5c60743](https://github.com/kveld9/kveld-morphe-patches/commit/5c60743908079d09c06ca2a62c56bc76792764e2))

## [1.42.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.41.1...v1.42.0) (2026-09-20)

### Bug Fixes

* **chromium:** sanitize clipboard via ClipboardImpl and expand Mercado Libre domains ([35703ae](https://github.com/kveld9/kveld-morphe-patches/commit/35703ae06904862a5a25d319ff14a85548529324))
* **hevy:** drop legacy 3.1.13 target to enforce single latest version support ([8461789](https://github.com/kveld9/kveld-morphe-patches/commit/846178933bc782d8930db5e2d44d3675f6a7c3d5))
* **tiktok:** persist playback speed across search and profile feeds (closes [#46](https://github.com/kveld9/kveld-morphe-patches/issues/46)) ([5ef3eaf](https://github.com/kveld9/kveld-morphe-patches/commit/5ef3eaf3755a24434a7590da3e9cc80806768237))
* **tiktok:** prevent Following feed load failure in feed bloat blocker ([4f71b01](https://github.com/kveld9/kveld-morphe-patches/commit/4f71b0195f47fc4dd90268ef063e87b85cb90827)), closes [#42](https://github.com/kveld9/kveld-morphe-patches/issues/42)
* **tiktok:** suppress secondary story view dispatch and profile analytics leakage ([ed23dfa](https://github.com/kveld9/kveld-morphe-patches/commit/ed23dfaa86e809b98855f2ee4d7e7c18920147b1))
* **tiktok:** unblock modern share panel download action for stories ([a015c4a](https://github.com/kveld9/kveld-morphe-patches/commit/a015c4abc1eb74d59aeaebe0cfb96b9dbf342b8b))
* **tiktok:** unblock story download button in share panel ([62ab730](https://github.com/kveld9/kveld-morphe-patches/commit/62ab73046842da4a667a33f85385c79e9229ace5))

### New Features

* **brave:** update target version to 1.95.102 and align libchrome telemetry offsets ([e7e2a26](https://github.com/kveld9/kveld-morphe-patches/commit/e7e2a269357ef87360d22a8f5647ad8c7a73c8d4))
* **brave:** update target version to 1.95.104 and align libchrome telemetry offsets ([81e1cd6](https://github.com/kveld9/kveld-morphe-patches/commit/81e1cd6260282e515d5c9059cb7918dc5ebcee2c))
* **shared:** bump tiktok target version to 47.0.3 and synchronize contracts ([55fc658](https://github.com/kveld9/kveld-morphe-patches/commit/55fc6581d668f77a84be54c737a3275ec86f3fbb))
* **tiktok:** add custom offline videos download limit patch ([4bce4c5](https://github.com/kveld9/kveld-morphe-patches/commit/4bce4c53831bfd1957666038bae9dfdb218abc93))
* **tiktok:** update bytecode patches and shifted targets for v47.0.3 ([a4e203b](https://github.com/kveld9/kveld-morphe-patches/commit/a4e203b1e0b098bbb44ddc89274445774eb5a45f))
* **tooling:** add contributors synchronization and issue audit automation ([88eb94f](https://github.com/kveld9/kveld-morphe-patches/commit/88eb94f3be6d9df0a0523477e6bb1177344a0916))
* **vivaldi:** update target version to 8.2.4147.93 and resolve libchrome offsets ([9b52955](https://github.com/kveld9/kveld-morphe-patches/commit/9b5295599a43cda9956cb1012dcb64ed3033e33b))

### Code Refactoring

* **harness:** remove ghost mode patch contract ([3dcf7f9](https://github.com/kveld9/kveld-morphe-patches/commit/3dcf7f9a58a92d7d40ba2519f8259978a18f3728))
* **harness:** remove story reference from media enhancements contract ([e237213](https://github.com/kveld9/kveld-morphe-patches/commit/e2372133413eafb8ef60c012c5171cc745654cb2))
* **tiktok:** remove ghost mode and trim story download hooks ([5c03d97](https://github.com/kveld9/kveld-morphe-patches/commit/5c03d9718f4856fe449deee7f9b4712578e879ce))

## [1.41.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.41.0...v1.41.1) (2026-09-18)

### Bug Fixes

* **tiktok:** resolve unauthenticated profile navigation playback freeze ([be3cf7c](https://github.com/kveld9/kveld-morphe-patches/commit/be3cf7c4a9ada6164f20d6e32c6b31e6a2acdc3e))

## [1.41.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.40.0...v1.41.0) (2026-09-18)

### Bug Fixes

* **tiktok:** clamp refresh rate to display peak and fix static hook register ([8dbf732](https://github.com/kveld9/kveld-morphe-patches/commit/8dbf73221ab1cf2e0a92411716cbadf9c85d2085))
* **tiktok:** implement reactive call-site skipping and typing suppression for ghost mode ([250f722](https://github.com/kveld9/kveld-morphe-patches/commit/250f72232195240c8b010a1228b1252a26c63e30))

### New Features

* **shared:** add CFG register liveness analysis and bytecode call-site helpers ([8b96aa3](https://github.com/kveld9/kveld-morphe-patches/commit/8b96aa38aa863de421862f28344a7503a3608c73))

### Code Refactoring

* **harness:** support targeted patch filtering in runPatchTest ([fbf47ef](https://github.com/kveld9/kveld-morphe-patches/commit/fbf47ef761ad13d3328bad6c2fc675300436960d))

## [1.40.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.39.0...v1.40.0) (2026-09-18)

### New Features

* **shared:** add TikTok privacy and refresh rate extension hook constants ([af0e943](https://github.com/kveld9/kveld-morphe-patches/commit/af0e943622763f1bce87291d470b08e922a08681))
* **tiktok:** add display refresh rate governor patch ([0809d22](https://github.com/kveld9/kveld-morphe-patches/commit/0809d224cf708af334643e75af6049b5e6f4a09a))
* **tiktok:** add ghost mode patch for anonymous profile and story browsing ([dd72e1b](https://github.com/kveld9/kveld-morphe-patches/commit/dd72e1b4af7d81055590a6bc6ef7dffe3ce13dd3))
* **tiktok:** decouple download quality ceiling and unblock story downloads ([0258045](https://github.com/kveld9/kveld-morphe-patches/commit/0258045a13c7d7e8cb843b7372bf7d5dfb21d912))
* **tiktok:** harden device privacy guard against package scanning, contacts access, and sensor fingerprinting ([ec5bab1](https://github.com/kveld9/kveld-morphe-patches/commit/ec5bab13de4d319b1654ac7f72ab3a9825c98c1e))

## [1.39.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.38.4...v1.39.0) (2026-09-18)

### Bug Fixes

* **tiktok:** restore seekbar by targeting Aweme.getVideoControl ([633c238](https://github.com/kveld9/kveld-morphe-patches/commit/633c23813c1c6a414918bd1fb28ce3a9866f1f6e)), closes [#39](https://github.com/kveld9/kveld-morphe-patches/issues/39)

### New Features

* **chromium:** strip fragment tracking and add mercadolibre support ([bdcd9db](https://github.com/kveld9/kveld-morphe-patches/commit/bdcd9db4f484dbde8730de4dc2176912b220b467))

## [1.38.4](https://github.com/kveld9/kveld-morphe-patches/compare/v1.38.3...v1.38.4) (2026-09-17)

### Bug Fixes

* **tiktok:** ensure publish date is visible across feed cards ([4c70235](https://github.com/kveld9/kveld-morphe-patches/commit/4c70235782db56e81a676c83bdf0ea1ff521329e))

## [1.38.3](https://github.com/kveld9/kveld-morphe-patches/compare/v1.38.2...v1.38.3) (2026-09-17)

### Bug Fixes

* **vivaldi:** neutralize default browser prompts and remove redundant background media patch ([22c2970](https://github.com/kveld9/kveld-morphe-patches/commit/22c2970eaab824af5f9106273bab86534813af62))

## [1.38.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.38.1...v1.38.2) (2026-09-17)

### Bug Fixes

* **chromium:** preserve PlatformSensorProvider JNI receiver stability ([c71ef43](https://github.com/kveld9/kveld-morphe-patches/commit/c71ef43cf6212e82d3ca21c9d0e6b8aaab5e40ec))

## [1.38.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.38.0...v1.38.1) (2026-09-17)

### Bug Fixes

* **vivaldi:** harden bytecode register allocation, promo handlers, and split compatibility ([509d2ac](https://github.com/kveld9/kveld-morphe-patches/commit/509d2ac1af554af03ef2b7ad62d77332577de483))

### Code Refactoring

* **chromium:** decouple shared browser patches and extension runtime from brave ([783cede](https://github.com/kveld9/kveld-morphe-patches/commit/783cedea464b99967e7a7cfb351eef9d7b00ed34))
* **extension:** isolate ambiguous link tracking parameters to target domains ([1f304fa](https://github.com/kveld9/kveld-morphe-patches/commit/1f304fa55ae415a489e37a52588b9677b376a616))

## [1.38.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.37.0...v1.38.0) (2026-09-17)

### New Features

* **patches:** enable sensor privacy and clean share url for vivaldi ([3077171](https://github.com/kveld9/kveld-morphe-patches/commit/30771712f5d4d9243679d6f563496bf60eb62abb))
* **vivaldi:** add background media, telemetry blocking, and UI debloat patches ([4c1f36b](https://github.com/kveld9/kveld-morphe-patches/commit/4c1f36b99e8c59b0217bfa56f3c6ec0e7696c823))

## [1.37.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.36.1...v1.37.0) (2026-09-17)

### New Features

* **brave:** add clean new tab page, sensor privacy, and link tracking sanitizer ([192c1b1](https://github.com/kveld9/kveld-morphe-patches/commit/192c1b1e1ec7af7a934f81baf87a6b0c18c070d3))

## [1.36.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.36.0...v1.36.1) (2026-09-17)

### Bug Fixes

* **tiktok:** resolve comment copy fingerprint matching and guard one-tap auth ([a038068](https://github.com/kveld9/kveld-morphe-patches/commit/a038068d17dff29686eadb6e83c99927d45f8ddd))

## [1.36.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.35.0...v1.36.0) (2026-09-17)

### New Features

* **tiktok:** add google login fix, seekbar restore, publish date, and clean comment copy ([34f13fc](https://github.com/kveld9/kveld-morphe-patches/commit/34f13fccc71b3b1a0395484a10ac3571a2a774e2))

## [1.35.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.34.0...v1.35.0) (2026-09-17)

### New Features

* **tiktok:** bypass mandatory login and skip first-launch onboarding ([31ed012](https://github.com/kveld9/kveld-morphe-patches/commit/31ed0122fb4c4b81a14e1005b8d5938dc66609f2))
* **tiktok:** default SIM region selector spoof target to CH ([b44a749](https://github.com/kveld9/kveld-morphe-patches/commit/b44a74951fd89cd4f22e0cf9ab48faa4e4bb7178))

## [1.34.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.33.1...v1.34.0) (2026-09-16)

### New Features

* **tiktok:** bypass FLAG_SECURE and purge invasive permissions ([98c66e4](https://github.com/kveld9/kveld-morphe-patches/commit/98c66e46b557977234ee599c166841c4ef496809))
* **tiktok:** redirect external links to system browser ([6e5d210](https://github.com/kveld9/kveld-morphe-patches/commit/6e5d210e0537b2e020a2dffc7cf46168908a8bc0))
* **tiktok:** strip proprietary TTWebView engine and manifest ([56b7c49](https://github.com/kveld9/kveld-morphe-patches/commit/56b7c492dfe9c25488f4715bdf242db3e50a99cb))

## [1.33.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.33.0...v1.33.1) (2026-09-16)

### Bug Fixes

* **tiktok:** neutralize Tako AI feed action bar triggers and router services ([2e6cd87](https://github.com/kveld9/kveld-morphe-patches/commit/2e6cd87e5622b11064a6fc5afa0e69c8da326cdf))

## [1.33.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.32.0...v1.33.0) (2026-09-16)

### New Features

* **tiktok:** add video quality governor patch with configurable ceilings ([3540b62](https://github.com/kveld9/kveld-morphe-patches/commit/3540b625b7c1ce3a0ca592afd2db260babd14d53))

## [1.32.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.31.2...v1.32.0) (2026-09-16)

### New Features

* **tiktok:** expand client AI governor to neutralize Tako AI and search clutter ([d885f41](https://github.com/kveld9/kveld-morphe-patches/commit/d885f410037a82bb019fab98ff6c5bbfa35c02fa))

## [1.31.2](https://github.com/kveld9/kveld-morphe-patches/compare/v1.31.1...v1.31.2) (2026-09-16)

### Bug Fixes

* **gboard:** clear try blocks to prevent VerifyError in clipboard hooks ([c1a5d22](https://github.com/kveld9/kveld-morphe-patches/commit/c1a5d226f6408d78d27dee667e41d5688f2359d0))

## [1.31.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.31.0...v1.31.1) (2026-09-16)

### Bug Fixes

* **tiktok:** preserve libbytenn to avoid dlopen failure in native dependencies ([6de816c](https://github.com/kveld9/kveld-morphe-patches/commit/6de816c83a1c60f0f5a1c5e00adce140585900ca))

## [1.31.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.30.1...v1.31.0) (2026-09-16)

### New Features

* **tiktok:** add privacy guards, block floating ad pendants, and harden live stream filtering ([eaba502](https://github.com/kveld9/kveld-morphe-patches/commit/eaba502c52bbc83c9df556b0af2e0a8d1060a1a1)), closes [#33](https://github.com/kveld9/kveld-morphe-patches/issues/33)

## [1.30.1](https://github.com/kveld9/kveld-morphe-patches/compare/v1.30.0...v1.30.1) (2026-09-16)

### Bug Fixes

* **tiktok:** harden feed bloat blocker and follow feed live stream decoupling ([37ce07e](https://github.com/kveld9/kveld-morphe-patches/commit/37ce07e9613957456dfdde1e3d37a6b3ad1b325e))

## [1.30.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.29.0...v1.30.0) (2026-09-15)

### New Features

* **tiktok:** add feed bloat blocker and neutralize screenshot share panel ([2487de2](https://github.com/kveld9/kveld-morphe-patches/commit/2487de2ce66c7b332237461c42add316300e7781))

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
