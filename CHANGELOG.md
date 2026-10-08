## [2.6.0-experimental.4](https://github.com/kveld9/kveld-morphe-patches/compare/v2.6.0-experimental.3...v2.6.0-experimental.4) (2026-10-08)

### Bug Fixes

* **universal:** pre-filter immutable dex scan before materializing mutable defs ([96070c0](https://github.com/kveld9/kveld-morphe-patches/commit/96070c0d9501a1b89403eda23b23df9b0bcdfecb))

## [2.6.0-experimental.3](https://github.com/kveld9/kveld-morphe-patches/compare/v2.6.0-experimental.2...v2.6.0-experimental.3) (2026-10-08)

### Bug Fixes

* **tiktok:** preserve stock translations in Video Fit to restore centering ([c69c334](https://github.com/kveld9/kveld-morphe-patches/commit/c69c33429d487550b0119de56d00df645ad263b6))
* **universal:** process full hosts blocklist without entry cap ([db29807](https://github.com/kveld9/kveld-morphe-patches/commit/db29807a20d45a4393fffd525f6d5511ab18693d))

## [2.6.0-experimental.2](https://github.com/kveld9/kveld-morphe-patches/compare/v2.6.0-experimental.1...v2.6.0-experimental.2) (2026-10-08)

### New Features

* **universal:** add opt-in hosts blocklist patch for dex URL literals ([fa1e0ab](https://github.com/kveld9/kveld-morphe-patches/commit/fa1e0ab5e35e46b93f484994c4c9db344c14f6e6))

## [2.6.0-experimental.1](https://github.com/kveld9/kveld-morphe-patches/compare/v2.5.0...v2.6.0-experimental.1) (2026-10-08)

### Bug Fixes

* **brave:** give Block Telemetry single ownership of P3A/Stats/WDP XML defaults ([2d31b64](https://github.com/kveld9/kveld-morphe-patches/commit/2d31b64c68fa02d4d6702ddad628083d1be93514))
* **chromium:** strip Mercado Libre ad-recommendation fragment params ([52bf2a9](https://github.com/kveld9/kveld-morphe-patches/commit/52bf2a9ff1a0618bf96da87db54e0f68a82b432c))
* **gboard:** add type-safety guards to preference reflection fields ([7b8e668](https://github.com/kveld9/kveld-morphe-patches/commit/7b8e66866648b50a9a716871ccb8eec9bbcb5a11))
* **gboard:** correct title/summary field mapping in settings i18n ([779a11a](https://github.com/kveld9/kveld-morphe-patches/commit/779a11a11e8dbe5bbb3cbe86e66eb38cd21f2802))
* **gboard:** default Disable Play Services Integration to off ([3d4b1a1](https://github.com/kveld9/kveld-morphe-patches/commit/3d4b1a10e85801fb098324ca3749a1403676d26d))
* **gboard:** default Hide IME navigation bar to off to preserve switcher buttons ([af18328](https://github.com/kveld9/kveld-morphe-patches/commit/af18328209e648a0eb1eefd315b3c986974b3b4b))
* **gboard:** drive Hide Number Hints via enable_number_row flag ([ee14b2a](https://github.com/kveld9/kveld-morphe-patches/commit/ee14b2a037c1b410c1930750998ca32cfc4922d8))
* **gboard:** eliminate language flicker when opening settings ([4e1306b](https://github.com/kveld9/kveld-morphe-patches/commit/4e1306b0c522a1c6af8e908fdf473f45e2d97e98))
* **gboard:** keep Hide Number Hints as unnamed Enhancements sub-patch ([1e9a610](https://github.com/kveld9/kveld-morphe-patches/commit/1e9a61095ffb31eed8e2fc427ceba2415322f226))
* **gboard:** recenter letters when number hints are hidden ([bfc153e](https://github.com/kveld9/kveld-morphe-patches/commit/bfc153e32c06d8d42adde7b705a4ba4e00cb3021))
* **gboard:** recenter main label when hiding number hints ([00028da](https://github.com/kveld9/kveld-morphe-patches/commit/00028da1a53296547a791c659664659f0e87ef1e))
* **gboard:** revert Hide Number Hints flag override ([1c5e678](https://github.com/kveld9/kveld-morphe-patches/commit/1c5e67817e89dd4141a232dec14d8f55bdb6b273))
* **shared:** overwrite existing android attributes in manifest helpers ([b108df9](https://github.com/kveld9/kveld-morphe-patches/commit/b108df95b0198fb9910b46c14a4ac2c2fa2ff03e))
* **tiktok:** block combined push permission popup at manager entry ([b92c512](https://github.com/kveld9/kveld-morphe-patches/commit/b92c512ece32f4093182a97f4dde074ff9e22775))
* **tiktok:** block find-contacts dialog pipeline at LX/0v5r entry ([53a0078](https://github.com/kveld9/kveld-morphe-patches/commit/53a0078f1606c19736291c1756a284beacb360eb))
* **tiktok:** block Friends-tab swipe suggestion BigCards ([7387c1b](https://github.com/kveld9/kveld-morphe-patches/commit/7387c1b2471fc560664bbf718e785657d5291102))
* **tiktok:** block GPPPA 2SV sheet at campaign entry ([32cbae6](https://github.com/kveld9/kveld-morphe-patches/commit/32cbae6b89eb31bc92cf738a399033d21e66e57b))
* **tiktok:** block viewer-history sheet alternate trigger LX/0OOs.invoke ([6f8a028](https://github.com/kveld9/kveld-morphe-patches/commit/6f8a02837ea4017fa80d65ee953d4078fb71a739))
* **tiktok:** cancel single location fixes and force location status off ([1a5913b](https://github.com/kveld9/kveld-morphe-patches/commit/1a5913b8d68d01f7b22a9e225936f3ccee394135))
* **tiktok:** collapse BigCards post-bind to avoid uninitialized parent crash ([a071394](https://github.com/kveld9/kveld-morphe-patches/commit/a0713945a4b7d7e03efe55c7663d34dd0b5456ff))
* **tiktok:** continue operator rewrite on call sites without move-result ([353b242](https://github.com/kveld9/kveld-morphe-patches/commit/353b24291e975e2e73c80b5020ec46552b07b24c))
* **tiktok:** default Enable Live Search to off ([6be257b](https://github.com/kveld9/kveld-morphe-patches/commit/6be257bc58d37218f410027aef08b7de5d5243ff))
* **tiktok:** derive VideoItemParams register by parameter index in VM hooks ([1d6427b](https://github.com/kveld9/kveld-morphe-patches/commit/1d6427b2e6cd320caa6b6ed4069cc9b2ae96ed28))
* **tiktok:** drop dead story-cell block from Video Fit ([0235fd9](https://github.com/kveld9/kveld-morphe-patches/commit/0235fd9c360e092361d85998e1ec9566fd4ee93d))
* **tiktok:** extend popups suppressor to viewer history, push guide and GPPPA 2SV sheets ([b77f36e](https://github.com/kveld9/kveld-morphe-patches/commit/b77f36e086358f0fff8ce6892516b9966226d88d))
* **tiktok:** filter framework query results without removing invokes ([43d3476](https://github.com/kveld9/kveld-morphe-patches/commit/43d347659f0570b90551d875ffad6e4106ae8349))
* **tiktok:** handle boxed Float translate fields in Video Fit ([06d5c99](https://github.com/kveld9/kveld-morphe-patches/commit/06d5c991fec0194a7a5be27c7d3aedc02ebbe14e))
* **tiktok:** hook AB-helper gates for 2x speed lock instead of consumers ([969d4b5](https://github.com/kveld9/kveld-morphe-patches/commit/969d4b55da3602946dc0e0c1d85d09f6313af926))
* **tiktok:** insert AB-gate return wrappings in descending index order ([f77d679](https://github.com/kveld9/kveld-morphe-patches/commit/f77d6795773f3eca0125a91de459cd217607a9cb))
* **tiktok:** keep df_music_dsp in studio creation de-bloat ([82cb70a](https://github.com/kveld9/kveld-morphe-patches/commit/82cb70aa5806798d29744cb398e150f9d36513ca))
* **tiktok:** log AB-gate callees when 2x-lock lookup misses ([54db320](https://github.com/kveld9/kveld-morphe-patches/commit/54db3209ff17a5de0beada25984ce0e77f360e82))
* **tiktok:** remove redundant enabled toggle from camera mic indicator patch ([c37c8bb](https://github.com/kveld9/kveld-morphe-patches/commit/c37c8bba0429c09d7a0fad307f440f1ac11caf16))
* **tiktok:** resolve feed container size in Video Fit to stop thumbnail shrink ([5696de9](https://github.com/kveld9/kveld-morphe-patches/commit/5696de9dd192a017446a21545c2a3f326cf2358b))
* **tiktok:** resolve undecodable dimensions from parent Video ([ac78c2b](https://github.com/kveld9/kveld-morphe-patches/commit/ac78c2b821cf492219db18173f6fc34d6935435b))
* **tiktok:** simplify double tap patch to disable-only without comments redirect ([7e159ce](https://github.com/kveld9/kveld-morphe-patches/commit/7e159ce6ab2d595de6d786e7924798819ae9abde))
* **tiktok:** suppress friend suggestion swipe cards at list source ([7ae633d](https://github.com/kveld9/kveld-morphe-patches/commit/7ae633d64c3835fc3221a2ddc57e983ad16dec4b))
* **tiktok:** suppress inbox tab badge change events at LX/0ALx entry ([6e3ce83](https://github.com/kveld9/kveld-morphe-patches/commit/6e3ce83cad3bb9882c3168f1417c2bcdddfc5db1))
* **tiktok:** suppress typing-triggered sticker strip in direct message declutter ([7d2912d](https://github.com/kveld9/kveld-morphe-patches/commit/7d2912dd315ec8a383d2ee08bff02b37ae9a53f9))
* **tiktok:** use API 23-compatible atomic decrement in camera-mic hook ([e46f80f](https://github.com/kveld9/kveld-morphe-patches/commit/e46f80f95c799d9f81eecbca3d7382fb6b01b253))
* **tiktok:** use collision-free scratch register in AB-gate return wrapping ([98b896f](https://github.com/kveld9/kveld-morphe-patches/commit/98b896f6d2c70af033e24245b5a2124e92363f35))

### New Features

* **brave:** update target to 1.97.56 and align fingerprints/offsets ([6527f4e](https://github.com/kveld9/kveld-morphe-patches/commit/6527f4e85ca61b5dc0e7a6827fed3c5e55919a66))
* **chromium:** add Disable Content Capture patch for Brave ([10ae3e9](https://github.com/kveld9/kveld-morphe-patches/commit/10ae3e98ccbc6c7de8773f6e0c85ae9e2d826fc7))
* **gboard:** add Clipboard in Incognito toggle to force incognito ([a2b476a](https://github.com/kveld9/kveld-morphe-patches/commit/a2b476a7e9cb143b76f331feed06f05b7094caf9))
* **gboard:** add Hide IME navigation bar toggle to zero bottom inset ([8b8681d](https://github.com/kveld9/kveld-morphe-patches/commit/8b8681d4f5c271d03a122ac5e613e724d175b078))
* **gboard:** add Hide Number Hints toggle ([66b7c49](https://github.com/kveld9/kveld-morphe-patches/commit/66b7c49a1f7e25af0b386c20a643962e3bfd5839))
* **gboard:** add Hide Number Hints toggle ([22d86d0](https://github.com/kveld9/kveld-morphe-patches/commit/22d86d0f70d2ad94fb6ebc614e1b4452d8a744fe))
* **nokoprint:** add Skip Welcome Dialog patch ([3e76eb8](https://github.com/kveld9/kveld-morphe-patches/commit/3e76eb86b1b0ceb20cbda32958e02d48dc430eb8))
* **tiktok:** accept fixed resolution ceilings in download quality preference ([f63ce31](https://github.com/kveld9/kveld-morphe-patches/commit/f63ce315339304c935cf114dc0df818e9a0718bd))
* **tiktok:** add author region and warning skip to publish-date patch ([74f6e11](https://github.com/kveld9/kveld-morphe-patches/commit/74f6e118151788a1f068f05d41523b6c5714dcad))
* **tiktok:** add Avoid ByteVC2 Software Decoding toggle ([bd5c571](https://github.com/kveld9/kveld-morphe-patches/commit/bd5c571915774adb6f7cf1c2b04c996b11ecb25a))
* **tiktok:** add Camera and Microphone Indicator ([f688dc8](https://github.com/kveld9/kveld-morphe-patches/commit/f688dc8b7ca25ed021085f544a8ed24f69b6204e))
* **tiktok:** add comment send fix and popup-ad block to Comment Customizer ([e4841c3](https://github.com/kveld9/kveld-morphe-patches/commit/e4841c3d0e93e51a1831f0b30269fbf3d014cf12))
* **tiktok:** add custom host option to Clean Share URL ([f4f5625](https://github.com/kveld9/kveld-morphe-patches/commit/f4f56259c6e3b8f7ecf194e0935a552b49186785))
* **tiktok:** add do-not-translate language exclusions to Comment Customizer ([944e058](https://github.com/kveld9/kveld-morphe-patches/commit/944e05801541b1d9f03c29b954c35422e601ebba))
* **tiktok:** add double-tap redirect mode ([05b9530](https://github.com/kveld9/kveld-morphe-patches/commit/05b953012eb0d3c945263b635931c64660a2ba15))
* **tiktok:** add download quality preference and watermark toggle ([90c8f15](https://github.com/kveld9/kveld-morphe-patches/commit/90c8f15b8025c0a9a5d8adc7952987c4bee109c2))
* **tiktok:** add Enable Live Search patch ([0738ae4](https://github.com/kveld9/kveld-morphe-patches/commit/0738ae44c219cb1d996c82e2822d08040c54e9d8))
* **tiktok:** add Feed Content Filter patch ([a05712a](https://github.com/kveld9/kveld-morphe-patches/commit/a05712ae45ea819a15381e7f36a79fc72b9f5275))
* **tiktok:** add Hide Feedback Buttons toggle to feed declutter ([0a58255](https://github.com/kveld9/kveld-morphe-patches/commit/0a582559527210022ee2f637bf40f7a1eb5afd47))
* **tiktok:** add Hide Suggested Accounts patch ([1492b6d](https://github.com/kveld9/kveld-morphe-patches/commit/1492b6d322f7a2c1645e8e4c460a4c6c52d3c61d))
* **tiktok:** add hold-and-slide 2x speed lock option to Playback Speed ([2312f9e](https://github.com/kveld9/kveld-morphe-patches/commit/2312f9ea92517454e90d442416a984a58e1c69f7))
* **tiktok:** add Non-Personalized Search patch ([4affc38](https://github.com/kveld9/kveld-morphe-patches/commit/4affc38def6d93841a47a38d84859deae948276d))
* **tiktok:** add Remember Clear Display patch ([6441f4c](https://github.com/kveld9/kveld-morphe-patches/commit/6441f4c4ed6e190b078a9e979b03babc044e7a54))
* **tiktok:** add Show Author Region standalone patch ([4de5406](https://github.com/kveld9/kveld-morphe-patches/commit/4de54060234e1ff41991b2641025132046e2a1b6))
* **tiktok:** add Skip Content Warnings standalone patch ([aeb6262](https://github.com/kveld9/kveld-morphe-patches/commit/aeb6262f432a3d4aca03f474bbeca4bff8ca0e65))
* **tiktok:** add undecodable-video guard to Video Quality Governor ([9d8afa5](https://github.com/kveld9/kveld-morphe-patches/commit/9d8afa57c754bad0c924531a65c4edddc9ea113f))
* **tiktok:** add Video Fit mode ([3564e82](https://github.com/kveld9/kveld-morphe-patches/commit/3564e82948ec27326cd59ffe42ae8006d8ead25c))
* **tiktok:** add video-body long-press modes ([5616ab1](https://github.com/kveld9/kveld-morphe-patches/commit/5616ab1034e4912e1d89a3f55c70bbc7b627c5d5))
* **tiktok:** add voice and speech engine de-bloat ([726a967](https://github.com/kveld9/kveld-morphe-patches/commit/726a967f0c1054fef2424bd3ce3834c0f5190dd1))
* **tiktok:** centralize new TikTok extension descriptors ([70d5d70](https://github.com/kveld9/kveld-morphe-patches/commit/70d5d70c29ba4a1d70537bacb7c2182914f58177))
* **tiktok:** enforce governor on codec playAddr variants and log ladder floor ([37d86d4](https://github.com/kveld9/kveld-morphe-patches/commit/37d86d4bb1acc30fa2c3ce326e87c7d92dafdb8b))
* **tiktok:** extend live suite de-bloat with voip and rtm runtimes ([6f6b168](https://github.com/kveld9/kveld-morphe-patches/commit/6f6b1687e74604ff643a365459541c969c9e13ab))
* **tiktok:** extend SIM Region Selector to operator and CellIdentity spoofing ([b4366bb](https://github.com/kveld9/kveld-morphe-patches/commit/b4366bb947a3b07a3305b729e62d3d81c7836583))
* **tiktok:** extend studio creation de-bloat with camera/music DF, encoders, CutSame ([977d3c6](https://github.com/kveld9/kveld-morphe-patches/commit/977d3c60db8547fc2d18a253f05fb7bb960beb18))
* **tiktok:** extend studio creation de-bloat with LiteRT AI runtimes ([a0b6ad1](https://github.com/kveld9/kveld-morphe-patches/commit/a0b6ad1ac0466d4debc9b3c076e23e55a5a98f2b))
* **tiktok:** extend Video Quality Governor to detail playback path ([183e2c7](https://github.com/kveld9/kveld-morphe-patches/commit/183e2c778834f6b1319ea07297497b613103443b))
* **tiktok:** intercept framework data queries in Device Privacy Guard ([86362a7](https://github.com/kveld9/kveld-morphe-patches/commit/86362a7500dddcc91da3585747738d4a03ef9eb2))
* **universal:** add crash detectors toggle to telemetry neutralizer ([43c2fae](https://github.com/kveld9/kveld-morphe-patches/commit/43c2fae106f68c2644689ddd30acafc0aad50495))
* **universal:** add device-ID providers toggle to telemetry neutralizer ([4fc89da](https://github.com/kveld9/kveld-morphe-patches/commit/4fc89daee704fb9b8271e251a2cad46eede38483))
* **universal:** add Facebook SDK opt-out flags to telemetry neutralizer ([4f8be1c](https://github.com/kveld9/kveld-morphe-patches/commit/4f8be1cc28fb0225ba394d6da8ea36dbe402a46c))
* **universal:** add Google Analytics legacy toggle to telemetry neutralizer ([a37ec2d](https://github.com/kveld9/kveld-morphe-patches/commit/a37ec2d981f319cea08e0ff28452f8bb1afeae3f))
* **universal:** add Meta Analytics pipeline toggle to telemetry neutralizer ([db4e48f](https://github.com/kveld9/kveld-morphe-patches/commit/db4e48f6d9cb17519bf5de2d9c369d38ac80af96))
* **universal:** add opt-in push services toggle to telemetry neutralizer ([872ac39](https://github.com/kveld9/kveld-morphe-patches/commit/872ac394c4669927fc1a05ed5301c83171addb85))
* **universal:** cover AdMob and mediation SDKs in telemetry neutralizer ([1bfe5f6](https://github.com/kveld9/kveld-morphe-patches/commit/1bfe5f687b0dd087e50c664ce23a500e229b4b1c))
* **universal:** default DPI form-factor stripping toggles to on ([5357d1d](https://github.com/kveld9/kveld-morphe-patches/commit/5357d1dbff3d6bd1b462045e39d76a167c102c91))
* **universal:** extend telemetry coverage to IID, MLKit and third-party SDKs ([75d093f](https://github.com/kveld9/kveld-morphe-patches/commit/75d093ff9d7f780f9707481c5f674497cd2a23dd))
* **universal:** prune RemoteConfig registrars and ad startup initializers ([60ba017](https://github.com/kveld9/kveld-morphe-patches/commit/60ba0173168f4b0544a673a70690eb0b37eaef01))

## [2.5.0](https://github.com/kveld9/kveld-morphe-patches/compare/v2.4.0...v2.5.0) (2026-10-06)

### Bug Fixes

* **chromium:** prevent share intent cleaner bypass at return points ([c938129](https://github.com/kveld9/kveld-morphe-patches/commit/c938129ea57e708a6787721d0bd63087db81b3d7))
* **gboard:** complete haptics decoupling for custom keypress vibration ([58c68eb](https://github.com/kveld9/kveld-morphe-patches/commit/58c68eb82f96569394550d4c92ed649e3781c56e))
* **gboard:** hide framework IME navigation bar in zero bottom inset ([02dc380](https://github.com/kveld9/kveld-morphe-patches/commit/02dc38093b315625393ab590f6b1c43242fa0d3a))
* **gboard:** isolate feature flag overrides from sibling flags ([c2721c9](https://github.com/kveld9/kveld-morphe-patches/commit/c2721c9ee70168249de4879b0c3f0ec4b6ed404b))
* **gboard:** prevent system haptic status override bypass at return points ([29f43e7](https://github.com/kveld9/kveld-morphe-patches/commit/29f43e7abc31413b2404f5b516201d1816bca4e9))
* **tiktok:** disable create and publish button toggle by default ([e20b8f1](https://github.com/kveld9/kveld-morphe-patches/commit/e20b8f12e15b2662134c407336a566a41aa71565))
* **tiktok:** disable direct message declutter by default ([51e4609](https://github.com/kveld9/kveld-morphe-patches/commit/51e4609c2a0713b9d760310bf30e56f85f44b911))
* **tiktok:** disable feed interface declutter by default and englishize descriptions ([e250b04](https://github.com/kveld9/kveld-morphe-patches/commit/e250b04de0cec9cb66df9f8a5cd9225f32242737))
* **tiktok:** disable feed long-press actions by default ([6c9fea0](https://github.com/kveld9/kveld-morphe-patches/commit/6c9fea09dcb987b640968a8bf3c27fad1c64c811))
* **tiktok:** disable inbox story and status tray hiding by default ([d48f46a](https://github.com/kveld9/kveld-morphe-patches/commit/d48f46a365747df5030d0e2781f4d1d9ab394a5c))
* **tiktok:** drop ineffective share panel gap workaround ([846d497](https://github.com/kveld9/kveld-morphe-patches/commit/846d497098b956e4a6b70f24ca3ae340d38b23c8))
* **tiktok:** enable hide seen videos patch by default ([821e16e](https://github.com/kveld9/kveld-morphe-patches/commit/821e16ee46f7a013d9e8fc2a6b21016e690e1068))
* **tiktok:** filter AI content in Friends feeds at network boundary ([46b319e](https://github.com/kveld9/kveld-morphe-patches/commit/46b319e3efa0745a6f3172edb88f6f10bae5a255))
* **tiktok:** filter feed bloat in Friends feeds at network boundary ([fbbabf3](https://github.com/kveld9/kveld-morphe-patches/commit/fbbabf305e84bd515ff0d75b23ffea985d108397))
* **tiktok:** harden system font redirection, webview response, and auth loading flow ([b17f5c8](https://github.com/kveld9/kveld-morphe-patches/commit/b17f5c80a634390764da7806d1b73e015dafcfe7))
* **tiktok:** neutralize cold startup task in update prompt suppressor ([d99c9b1](https://github.com/kveld9/kveld-morphe-patches/commit/d99c9b1a1b213cbe2faec144aaa92fb37267486a))
* **tiktok:** persist profile banner by forcing allowListValue in snapshot ([7a59e65](https://github.com/kveld9/kveld-morphe-patches/commit/7a59e65c2e1c69c4bc5a455b04c4087e0222957a))
* **tiktok:** prevent bitrate filter bypass at return points ([6fb7967](https://github.com/kveld9/kveld-morphe-patches/commit/6fb7967dd171511bd04adbbc2b4440f4c1d5cd31))
* **tiktok:** prevent crash in direct messages by hiding forward button via view binding ([af67460](https://github.com/kveld9/kveld-morphe-patches/commit/af67460802b996387f89ac9e94355cbf0046f4c9))
* **tiktok:** prevent feed ad filter bypass at return points ([2a0401d](https://github.com/kveld9/kveld-morphe-patches/commit/2a0401d2352b7b57c858afb25a0f8948a46965be))
* **tiktok:** prevent feed bloat filter bypass at return points ([a0ec01c](https://github.com/kveld9/kveld-morphe-patches/commit/a0ec01c58b91eafbf223236f16ef2485e478550b))
* **tiktok:** prevent feed live stream filter bypass at return points ([bd03814](https://github.com/kveld9/kveld-morphe-patches/commit/bd0381496a3b2e177368a9300abea08300c14fba))
* **tiktok:** prevent first video auto-pause hook bypass ([33a9d7a](https://github.com/kveld9/kveld-morphe-patches/commit/33a9d7a38cdd0f9fecc60ad5649a808a6d52dc18))
* **tiktok:** prevent offline sheet text hook bypass ([231d1f6](https://github.com/kveld9/kveld-morphe-patches/commit/231d1f6b70b4cce8ff3c5106c674c2ab7d605960))
* **tiktok:** prevent popular lives ab param filter bypass ([4e16b8f](https://github.com/kveld9/kveld-morphe-patches/commit/4e16b8f092ec3ef154849e76c0b83efd8b28343b))
* **tiktok:** prevent seen video filter bypass in fetchFeedList ([d3f1a8f](https://github.com/kveld9/kveld-morphe-patches/commit/d3f1a8f40e3782d28a8b581020433d75e96d5f65))
* **tiktok:** prevent share panel filter bypass in constructor ([be392d3](https://github.com/kveld9/kveld-morphe-patches/commit/be392d32dd560887d0a9794c606519b642b81e2e))
* **tiktok:** prevent shop anchor filter bypass at return points ([a4db9b6](https://github.com/kveld9/kveld-morphe-patches/commit/a4db9b651df267418921b51471ce93c602f59d31))
* **tiktok:** prevent suggested search ab param filter bypass ([a0e74e5](https://github.com/kveld9/kveld-morphe-patches/commit/a0e74e597cb917a29c9c6dcb7e781b7ea3d72bcf))
* **tiktok:** restore dropped onViewCreated calls in feed interface declutter ([87d043b](https://github.com/kveld9/kveld-morphe-patches/commit/87d043b377579a378a0c5e46def2d2df7520e82a))
* **tiktok:** skip DASH and ByteVC2 renditions when picking download stream ([8f69dbf](https://github.com/kveld9/kveld-morphe-patches/commit/8f69dbf0ff3c232b0cff28ea5169cca40f4b6bfd))
* **tiktok:** standardize streak mascot description in hide inbox promos patch ([6ba314f](https://github.com/kveld9/kveld-morphe-patches/commit/6ba314f140aa93ecc288e599cf2bec9899c7ddce))
* **tiktok:** standardize suggested searches telemetry log to english ([54bea27](https://github.com/kveld9/kveld-morphe-patches/commit/54bea278487ecdd8e4531831fbc4602459edd198))
* **tiktok:** suppress preshown banner and typing suggestions in direct message declutter ([901d671](https://github.com/kveld9/kveld-morphe-patches/commit/901d67170efe68bcf53af61016ee0700406eea9b))
* **universal:** correct screen brightness governor description to reflect window layout parameters scope ([a0b6991](https://github.com/kveld9/kveld-morphe-patches/commit/a0b6991c56a48a6456cc56428a43d7f03775781c))

### New Features

* **gboard:** add clip character limit slider ([77b4a53](https://github.com/kveld9/kveld-morphe-patches/commit/77b4a53c659155789fd058fb36a67cdf3f70beab))
* **gboard:** add disable cloud backup patch ([0353379](https://github.com/kveld9/kveld-morphe-patches/commit/0353379136b6258e151825e9cab2915a7b66bedb))
* **gboard:** add disable play services integration patch ([306f7d1](https://github.com/kveld9/kveld-morphe-patches/commit/306f7d17dcc704f8de9eab21e3f6cdc38ae7073a))
* **gboard:** add dynamic device language localization for settings menu ([dea11db](https://github.com/kveld9/kveld-morphe-patches/commit/dea11dbf9f8d66201843810ea9c60cd6656a7acb))
* **gboard:** add modern keypress haptics toggle ([c16ec72](https://github.com/kveld9/kveld-morphe-patches/commit/c16ec725ea146169ff07d81f5eaf4aa8dcfbe0a8))
* **gboard:** remove exported web debug bridge provider ([6515ffa](https://github.com/kveld9/kveld-morphe-patches/commit/6515ffa6fddb5f3f9142b1696c3f5062629e3872))
* **shared:** add universal screen brightness governor patch ([7f4cceb](https://github.com/kveld9/kveld-morphe-patches/commit/7f4ccebe1ffc27a1b0b156fcf9b9a17fba2d5da2))
* **tiktok:** add Direct Message Declutter patch ([f965c4f](https://github.com/kveld9/kveld-morphe-patches/commit/f965c4fd3c6b11415233d5ddea5fc40fa1e58ec8))
* **tiktok:** add disable HDR video playback patch ([2933eb5](https://github.com/kveld9/kveld-morphe-patches/commit/2933eb5566369ab355d471a8b2c8b7272a2d832b))
* **tiktok:** add fix spotify login patch ([8994589](https://github.com/kveld9/kveld-morphe-patches/commit/89945897c167c2471bdf193ff2762788ba986ef6))
* **tiktok:** add friends feed strict mutuals patch ([a8b4f2a](https://github.com/kveld9/kveld-morphe-patches/commit/a8b4f2ac2454f039a003713d64d883216ec7c2c5))
* **tiktok:** add hide comment quick actions toggle to comment customizer ([fb2744a](https://github.com/kveld9/kveld-morphe-patches/commit/fb2744af762529b4280d7efd5fb676783f05848c))
* **tiktok:** add hide friends tab avatar preview option to navigation declutter ([8f22ce0](https://github.com/kveld9/kveld-morphe-patches/commit/8f22ce0ef4926637a780caf9c4102f5c92bdd4f2))
* **tiktok:** add hide full screen button toggle to feed interface declutter ([6c87c5b](https://github.com/kveld9/kveld-morphe-patches/commit/6c87c5b13f6ef9e9fc37d6722b1944fadd9fb232))
* **tiktok:** add hide inbox notification badge option ([ae21f64](https://github.com/kveld9/kveld-morphe-patches/commit/ae21f64114296a139b7df5740311e01b74706f25))
* **tiktok:** add hideCommentSurveys option to comment customizer ([93b507f](https://github.com/kveld9/kveld-morphe-patches/commit/93b507f49406729062af94d02b59eecf6548ebdc))
* **tiktok:** add hideStoryRings option to comment customizer ([645ca55](https://github.com/kveld9/kveld-morphe-patches/commit/645ca5562deee9468fc73df55df3c11e418b25d4))
* **tiktok:** add popups and prompts suppressor patch ([01d60b3](https://github.com/kveld9/kveld-morphe-patches/commit/01d60b30fec81cb4154745260c09900df09dcc3c))
* **tiktok:** add system font patch ([032e371](https://github.com/kveld9/kveld-morphe-patches/commit/032e3713003f07523cc659f9ed906f1dc941cf61))
* **tiktok:** hide try effect and sticker reply suggestions in DMs ([3c6bf76](https://github.com/kveld9/kveld-morphe-patches/commit/3c6bf76509a84d37d0b477c03418e7e46e6549a0))
* **tiktok:** neutralize post-video surveys and community cards in feed bloat blocker ([7aea111](https://github.com/kveld9/kveld-morphe-patches/commit/7aea111ae54389cb4d2e1a0f1fa21c514b38eb99))
* **tiktok:** support hiding gallery button in redesigned direct messages ([025a66d](https://github.com/kveld9/kveld-morphe-patches/commit/025a66d1b9fca8670ef52cc27f47f89445a9b9a7))

### Improvements

* **brave:** search preference XML first when locating switches ([9c73ba8](https://github.com/kveld9/kveld-morphe-patches/commit/9c73ba81cd56b414797a24ce8053a71c4c00e5c3))
* **shared:** merge shared extension once instead of per patch ([247308a](https://github.com/kveld9/kveld-morphe-patches/commit/247308a2310031e47affd12605f2341880ef2292))
* **shared:** skip protected trees in APK junk cleaner ([10ecfb3](https://github.com/kveld9/kveld-morphe-patches/commit/10ecfb3aa8e10c221e15f118859e1f9dedcaa0e9))
* **tiktok:** cut per-method cost of unanchored fingerprints ([9aad059](https://github.com/kveld9/kveld-morphe-patches/commit/9aad059ab039a3f9187ab285d32a268cf50a234e))

## [2.4.0](https://github.com/kveld9/kveld-morphe-patches/compare/v2.3.0...v2.4.0) (2026-10-03)

### Bug Fixes

* **ci:** support dynamic channel in gboard apkmirror link generator ([a1b42fa](https://github.com/kveld9/kveld-morphe-patches/commit/a1b42fa87aa5ff0bdbeb86d6d32122660493c04c))
* **gboard:** add aapt workaround for connectionless stylus handwriting attribute ([78abfda](https://github.com/kveld9/kveld-morphe-patches/commit/78abfda79c7d30813c47516553e0f2da2203057f))
* **gboard:** correct opcode insertion index for more_pill_keys feature flag ([3a696fd](https://github.com/kveld9/kveld-morphe-patches/commit/3a696fde8d9c759b39b5547e4336278b4d7836ea))
* **gboard:** disable cursor trackpad by default to prevent web view flicker ([c7ddbd7](https://github.com/kveld9/kveld-morphe-patches/commit/c7ddbd7ba32dd4299790d14aa31395a1cc0f69fd))
* **gboard:** eliminate register clobbering and add hot-path caching to decouple haptics ([f1b4223](https://github.com/kveld9/kveld-morphe-patches/commit/f1b422331d11df82ce7396806da0d007623f3aae))
* **gboard:** use more_pill_keys feature flag for key shape selection ([3719ae6](https://github.com/kveld9/kveld-morphe-patches/commit/3719ae6ec8b94f1ebb3c486337bddc6dbbc98c83))
* **hevy:** recalculate hermes bundle sha-1 footer hash on pro unlock ([ac0c671](https://github.com/kveld9/kveld-morphe-patches/commit/ac0c6712b476f046126e9154ed196d6bac51c675))
* **shared:** prevent asset scale corruption and protect launcher icons in dpi resource slimmer ([#67](https://github.com/kveld9/kveld-morphe-patches/issues/67)) ([271f06e](https://github.com/kveld9/kveld-morphe-patches/commit/271f06e702befac0dafb4b96858d1af00eabb89e))
* **shared:** qualify android xml namespace attributes in ManifestXml helpers ([d4c9aab](https://github.com/kveld9/kveld-morphe-patches/commit/d4c9aab4ce4bb5b959b67f5e0b296b536bb68f7a))
* **tiktok:** eliminate empty gap under share panel send button ([192b1ba](https://github.com/kveld9/kveld-morphe-patches/commit/192b1bab76c962cd197ea2aba984e02228a7a8d1))
* **tiktok:** prevent register collision in publish tab visibility hook ([53c4d3c](https://github.com/kveld9/kveld-morphe-patches/commit/53c4d3c32b9ecb309b4a4b35d3698f94bd965920))
* **tiktok:** restore circle to search and recent apps preview ([c5d2f93](https://github.com/kveld9/kveld-morphe-patches/commit/c5d2f9392f1b65e59a92b364fb874e2269e9ca5c))
* **tiktok:** suppress see translation button in hide video descriptions ([9962117](https://github.com/kveld9/kveld-morphe-patches/commit/99621170d69c8a04f4870db19765378a9587a281))

### New Features

* **brave:** add Disable Tab Auto-Minimization patch ([0940c43](https://github.com/kveld9/kveld-morphe-patches/commit/0940c43efaaf2dfaf2b47c4cff86caf7c967bc30))
* **brave:** update target to 1.96.61 and align native offsets ([575d864](https://github.com/kveld9/kveld-morphe-patches/commit/575d8647fe58be8a86eae5020a1ac65e3edaae42))
* **gboard:** add Voice Typing in Incognito patch ([cfcfda8](https://github.com/kveld9/kveld-morphe-patches/commit/cfcfda856efe007d51868967f605dd12777fe664))
* **gboard:** declutter root settings menu by stripping privacy, about and help headers ([9100049](https://github.com/kveld9/kveld-morphe-patches/commit/9100049cb93590722faa084183eaedd457a9980f))
* **gboard:** decouple keyboard vibration from system touch feedback ([d5d2d79](https://github.com/kveld9/kveld-morphe-patches/commit/d5d2d795d03c79424ca77b8b912424c6ed198394))
* **gboard:** implement Gboard Enhancements master suite and in-app settings ([0dbb53e](https://github.com/kveld9/kveld-morphe-patches/commit/0dbb53e2873227e80e70a17aad41ef76600c9d7d))
* **shared:** add Disable Firebase Telemetry universal patch ([16a670d](https://github.com/kveld9/kveld-morphe-patches/commit/16a670de6d2ada3c793a39cd853c2d3f5de85f1d))
* **shared:** add DOM manipulation helper extensions for AndroidManifest.xml ([ada638c](https://github.com/kveld9/kveld-morphe-patches/commit/ada638c088488c0cba3cd86b44e134b024636871))
* **shared:** add multi-package arsc traversal to locale resource slimmer ([f19f6ee](https://github.com/kveld9/kveld-morphe-patches/commit/f19f6ee2517eeaf05455e8bf23376d3cbd9f3d22))
* **shared:** add non-phone ui mode stripping and multi-package arsc traversal to dpi resource slimmer ([0bd4b03](https://github.com/kveld9/kveld-morphe-patches/commit/0bd4b03731093c537ccd2ae07eb957b1da07bdff))
* **shared:** implement universal screen timeout enforcer patch ([071392f](https://github.com/kveld9/kveld-morphe-patches/commit/071392fa298f329b7354345315b63cf2dc064ef4))
* **shared:** implement universal screenshot protection bypass patch ([634bf2c](https://github.com/kveld9/kveld-morphe-patches/commit/634bf2c30f81e3a814466ce04bf57bae3e20f6bd))
* **tiktok:** add feed interface declutter patch ([ef9bf0d](https://github.com/kveld9/kveld-morphe-patches/commit/ef9bf0dd73fb040832b7edba63d50e13e747fbdf))
* **tiktok:** add hide create button option to navigation declutter ([5fbe3fd](https://github.com/kveld9/kveld-morphe-patches/commit/5fbe3fd5b41000092963e18c8c5db5bc10398efe))
* **tiktok:** add hide music cover disc option to feed interface declutter ([27df3a7](https://github.com/kveld9/kveld-morphe-patches/commit/27df3a7d53cdb149c6211270408290fbe24ac46c))
* **tiktok:** add hide playlist bottom bar option to feed interface declutter ([ac8e468](https://github.com/kveld9/kveld-morphe-patches/commit/ac8e468ee7b86ec4b9fb1b440b49666b9853d20d))
* **tiktok:** add hide save button option to feed interface declutter ([e1fe07c](https://github.com/kveld9/kveld-morphe-patches/commit/e1fe07c69fe8287148bbf7aa5423439557848f83))
* **tiktok:** enable profile banner by default and sync documentation ([4e6442d](https://github.com/kveld9/kveld-morphe-patches/commit/4e6442d7e721e5a354137968b630a3d869f9b653))
* **tiktok:** implement profile banner unlock patch ([50ecff6](https://github.com/kveld9/kveld-morphe-patches/commit/50ecff6068837c7f248ee2e59420ef569652246a))

### Code Refactoring

* **gboard:** consolidate voice typing in incognito and aapt workaround into gboard enhancements ([c732b0c](https://github.com/kveld9/kveld-morphe-patches/commit/c732b0c6a3d6f6c4b1f86a947c43c663c7efb587))
* **gboard:** deduplicate byte scanning helper and synchronize options documentation ([1aa075d](https://github.com/kveld9/kveld-morphe-patches/commit/1aa075d2b962eb99a6f34a68c62b129e550c57ca))
* **shared:** consolidate firebase telemetry neutralization into universal telemetry neutralizer ([ae0108d](https://github.com/kveld9/kveld-morphe-patches/commit/ae0108d474d3c4dd85c02e86ffb85ad99d1794a6))

## [2.3.0](https://github.com/kveld9/kveld-morphe-patches/compare/v2.2.0...v2.3.0) (2026-10-01)

### Bug Fixes

* **gboard:** hook internal ergonomic and navigation metrics to eliminate bottom chin ([6328ab3](https://github.com/kveld9/kveld-morphe-patches/commit/6328ab3fac749951515383c868d91c10429888ec))

### New Features

* **brave:** update target to 1.96.60 and align native offsets ([b9c8869](https://github.com/kveld9/kveld-morphe-patches/commit/b9c8869a88541733ec56cb6d66efdf35431f5bb2))
* **gboard:** add zero bottom inset patch to eliminate gesture navigation chin ([c591c01](https://github.com/kveld9/kveld-morphe-patches/commit/c591c01ddc6cc9aa1f35609c9b083b00965802e5))
* **gboard:** consolidate experimental flags into feature flags patch ([6b0a6ca](https://github.com/kveld9/kveld-morphe-patches/commit/6b0a6ca19ee2e8f36084b2b51bc7ba37ef834828))
* **gboard:** consolidate signature bypass, launcher trampoline, and phenotype resilience into core integrity patch ([994d168](https://github.com/kveld9/kveld-morphe-patches/commit/994d168d5e405e09a14017b4cea315876cb4ff0d))
* **gboard:** update target to 18.4.1.985164140 and align obfuscated targets ([af42cfa](https://github.com/kveld9/kveld-morphe-patches/commit/af42cfab146aff347b6ff10fc31ba9b3eaa65bd9))
* **nokoprint:** update target to 5.28.6 and align obfuscated targets ([c6b6396](https://github.com/kveld9/kveld-morphe-patches/commit/c6b63966674892252f4e5029f9ad1fe54b5f48c8))
* **shared:** implement universal privacy permissions stripper patch ([8f18fc0](https://github.com/kveld9/kveld-morphe-patches/commit/8f18fc0ef5aba259a602d42b32537d3efe7f5c6f))
* **tiktok:** add clean share panel patch ([a4a0fec](https://github.com/kveld9/kveld-morphe-patches/commit/a4a0fec987232a33ae4371fa4cf50b04726ff1ae))
* **tiktok:** add dragging thumbnail preview to show seekbar patch ([6864d71](https://github.com/kveld9/kveld-morphe-patches/commit/6864d710a826d7af8bdc0860eb89e4b00043b498))
* **tiktok:** add hide inbox promos and alerts patch ([fda0bb5](https://github.com/kveld9/kveld-morphe-patches/commit/fda0bb501410215db2ab8a4e376f84a4ef931ac2))
* **tiktok:** consolidate comment enhancements into comment customizer patch ([6e1ed0d](https://github.com/kveld9/kveld-morphe-patches/commit/6e1ed0d028b020ec921ab5e8fd45befa839c0425))
* **tiktok:** consolidate feed navigation and header declutter patches ([9c6ce3f](https://github.com/kveld9/kveld-morphe-patches/commit/9c6ce3fb46d3ba451125ecab8d20fad2ef716176))
* **tiktok:** decouple asia variant and enforce single global target ([bac43bc](https://github.com/kveld9/kveld-morphe-patches/commit/bac43bc04d56d3a21b453efc4c254ee069c4b1ce))
* **tiktok:** disable feed long-press action gestures ([9f1dfc7](https://github.com/kveld9/kveld-morphe-patches/commit/9f1dfc72a3739c4e836ccd044f470fd5e48b6b70))
* **tiktok:** hide inbox story and status tray ([7a5b82e](https://github.com/kveld9/kveld-morphe-patches/commit/7a5b82e313db6d7e027fe5cbb4a3f479f56b2915))
* **tiktok:** implement disable post-download share dialog patch (default: false) ([7ab8546](https://github.com/kveld9/kveld-morphe-patches/commit/7ab85465e9f15cc923f5c0fc6db0246d1b875819))
* **tiktok:** suppress in-feed search recommendations and interest cards ([ebc94bf](https://github.com/kveld9/kveld-morphe-patches/commit/ebc94bf2d8149a6de1f40b2e8fe0782ae3e83576))

## [2.2.0](https://github.com/kveld9/kveld-morphe-patches/compare/v2.1.0...v2.2.0) (2026-09-30)

### New Features

* **gboard:** add hide incognito icon toggle to force incognito mode patch ([f81f9ac](https://github.com/kveld9/kveld-morphe-patches/commit/f81f9acc4c5d5eff33c04d608283db65abe970cc))
* **gboard:** update target to 18.3.2.977415014 and align obfuscated targets ([191cfcc](https://github.com/kveld9/kveld-morphe-patches/commit/191cfcce7d411be9e3b998d49f9b9dd714b310b3))
* **tiktok:** add create group and story toggles and fix promote filter in custom share sheet ([940d66f](https://github.com/kveld9/kveld-morphe-patches/commit/940d66f48bd932e949f1c4641fa7d1ddd941b919))
* **tiktok:** update target to 47.1.4 and align obfuscated targets ([de5b141](https://github.com/kveld9/kveld-morphe-patches/commit/de5b1418eb2223e6f069528a79fe8a54bc3840b1))

### Code Refactoring

* **tiktok:** remove redundant custom hidden keys from share sheet patch ([a971ae8](https://github.com/kveld9/kveld-morphe-patches/commit/a971ae8cf83a58664d5265feb47542c43be5ff9a))

## [2.1.0](https://github.com/kveld9/kveld-morphe-patches/compare/v2.0.0...v2.1.0) (2026-09-29)

### Bug Fixes

* **brave:** neutralize armv8.0 cpu feature constructor traps in libchrome ([98201c2](https://github.com/kveld9/kveld-morphe-patches/commit/98201c27bef7ff641330784f2417063fe4ef9fd1))
* **brave:** resolve android 16 arm64 bti trap and enforce native library extraction ([2224cdb](https://github.com/kveld9/kveld-morphe-patches/commit/2224cdb1afef63b358f0a535044821eb6e9a5a2b))
* **nokoprint:** resolve infinite progress dialog hang when adding printers ([47fed43](https://github.com/kveld9/kveld-morphe-patches/commit/47fed434581ae4f8f3b432d6f31f77ee5c77881a))
* **tiktok:** align obfuscated privacy client targets and prune obsolete hooks in device privacy guard patch ([282c95b](https://github.com/kveld9/kveld-morphe-patches/commit/282c95b8384d432994cd5d8bf5594794523dc03e))
* **tiktok:** decouple quality governor lifecycle, resolution ceilings, and DASH manifests ([8ca1161](https://github.com/kveld9/kveld-morphe-patches/commit/8ca1161273fd414b7528da1b389a71e1b4071cf3))
* **tiktok:** eliminate cold and warm background resume splash and topview ads ([7fa4796](https://github.com/kveld9/kveld-morphe-patches/commit/7fa479680d13b7effb5adf3de1d0ddb12a5b5b9f))
* **tiktok:** eliminate obsolete MainPageFragment hook in SkipFirstLaunchOnboarding ([e5ec9c2](https://github.com/kveld9/kveld-morphe-patches/commit/e5ec9c2d20b957ea3f2bc21a68c0dc0cbd6704c1))
* **tiktok:** enforce fail-fast fingerprint resolution in SkipFirstLaunchOnboarding ([e7f7fb4](https://github.com/kveld9/kveld-morphe-patches/commit/e7f7fb4fedcd6c5c254be2c9ba5344bfac0343eb))
* **tiktok:** enforce fail-fast resolution capping hooks in VideoQualityGovernor ([658608c](https://github.com/kveld9/kveld-morphe-patches/commit/658608c59d70352928c172cc8ffd777c82a86946))
* **tiktok:** enforce register allocation and clear try blocks in InstantColdStart ([fe5f7cf](https://github.com/kveld9/kveld-morphe-patches/commit/fe5f7cf4f70d0ab6162feb3a1a072c46df3aa070))
* **tiktok:** expand hideRepost key set to include live repost channel variants ([68bc856](https://github.com/kveld9/kveld-morphe-patches/commit/68bc856d9d287584a88b80761e05ed68da95bfcf))
* **tiktok:** expand tracking parameter filtering and enforce fail-fast in clean share url ([e46107d](https://github.com/kveld9/kveld-morphe-patches/commit/e46107dcfed03a12a1fe6d74e5203976ff0d3bff))
* **tiktok:** harden feed ad filter reflection type guards ([afd4060](https://github.com/kveld9/kveld-morphe-patches/commit/afd40608a499546b68774e9a77880227b67ed383))
* **tiktok:** isolate share sheet panel mutations and escape string options ([470fb30](https://github.com/kveld9/kveld-morphe-patches/commit/470fb300039c6ffa6c9b3df8e9592ab08c109407))
* **tiktok:** preserve repost channel in custom share sheet essentials ([ace8225](https://github.com/kveld9/kveld-morphe-patches/commit/ace82254e1b5db31b10e1bef3a83f4854e9dc8c8))
* **tiktok:** prevent video ID collision and preserve distinct stream download mapping (fixes [#58](https://github.com/kveld9/kveld-morphe-patches/issues/58)) ([1a83be8](https://github.com/kveld9/kveld-morphe-patches/commit/1a83be8d5bff462669ce8e98efea44b18a4b9f7c))
* **tiktok:** replace share sheet whitelist with granular boolean toggles ([5c8ba5e](https://github.com/kveld9/kveld-morphe-patches/commit/5c8ba5ea654ce98c07d5514285f1c2b9bd079bfd)), closes [#59](https://github.com/kveld9/kveld-morphe-patches/issues/59)
* **tiktok:** resolve class cast exception by dynamically hooking video frame rate opt in display refresh rate governor patch ([3356eaf](https://github.com/kveld9/kveld-morphe-patches/commit/3356eaf2fdc67d01a8364e8995c411ca6ce81c27))
* **tiktok:** resolve null pointer exception in aweme video control seekbar initialization ([0fc11fd](https://github.com/kveld9/kveld-morphe-patches/commit/0fc11fda03fe00f1e881c97db44c7be79fd66258))
* **tiktok:** resolve share item reflection key per instance ([3e88ee0](https://github.com/kveld9/kveld-morphe-patches/commit/3e88ee05489f7320673cf6704e90af6dac0e115a))
* **tiktok:** support shifted account user service getters in mandatory login bypass hook ([3439f1f](https://github.com/kveld9/kveld-morphe-patches/commit/3439f1ff21419776299206dcc7827634535a2ca1))
* **tiktok:** suppress settings redirect prompts and onboarding triggers for blocked permissions ([086b25a](https://github.com/kveld9/kveld-morphe-patches/commit/086b25ad5d58031444f39434de2338d1aa3bfef0))
* **tiktok:** update aweme stats api and history targets in disable watch history recording patch ([7a06f6a](https://github.com/kveld9/kveld-morphe-patches/commit/7a06f6a280dedacf705c130cef586baa782edff7))
* **tiktok:** update bottom tab protocol hooks in hide community tab patch ([9596583](https://github.com/kveld9/kveld-morphe-patches/commit/9596583fc86cc17c7caa0d89fef853917758b0fb))
* **tiktok:** update feed anchor filter targets in hide tiktok shop anchors patch ([73cd253](https://github.com/kveld9/kveld-morphe-patches/commit/73cd253cdeef7cf3d1b0ccb8dea2995b4e2ebef0))
* **tiktok:** update feed avatar default assem hooks and helper in hide avatar follow button patch ([b14eefc](https://github.com/kveld9/kveld-morphe-patches/commit/b14eefc8d1356f2959c5fb26491b1ccfc4c7afc5))
* **tiktok:** update feed avatar live assem hooks in disable avatar live status patch ([83253d4](https://github.com/kveld9/kveld-morphe-patches/commit/83253d4b1d389942d32134f03bf8a517d8380ce4))
* **tiktok:** update live outer service hooks in hide top live entrance patch ([af41b2d](https://github.com/kveld9/kveld-morphe-patches/commit/af41b2dc1de264e83715ceedff087bd4f0d7cb30))
* **tiktok:** update player controller hooks and dynamic panel discovery in auto pause first video patch ([db5bfb2](https://github.com/kveld9/kveld-morphe-patches/commit/db5bfb29034fbdeef8794d8677887126fdc50bb6))
* **tiktok:** update search bar assem target hooks in hide feed search bar patch ([b4f5e4e](https://github.com/kveld9/kveld-morphe-patches/commit/b4f5e4e15b1c55a18644da57c16c4bb88d5e8611))
* **tiktok:** update search filter hooks in hide search popular lives patch ([a083c6f](https://github.com/kveld9/kveld-morphe-patches/commit/a083c6f84913bc8db263a9f8d989b839c1d9539f))
* **tiktok:** update search history repository target in disable search history recording patch ([7d29bb1](https://github.com/kveld9/kveld-morphe-patches/commit/7d29bb1223412f75041f2b6be50287a9c2601c5c))
* **tiktok:** update search intermediate and guess query hooks in hide suggested searches patch ([d227b9e](https://github.com/kveld9/kveld-morphe-patches/commit/d227b9eee5d9bc952065bfcc594d6e9d21830692))
* **tiktok:** update smart service and client ai targets in client ai governor patch ([793aeb4](https://github.com/kveld9/kveld-morphe-patches/commit/793aeb4b8b433d64a388682a945fff5a9aa4a542))
* **tiktok:** update splash ad service and real time splash manager targets in instant cold start patch ([caf2530](https://github.com/kveld9/kveld-morphe-patches/commit/caf253027dcb9a55c76aaf180df3bc112329025b))

### New Features

* **shared:** bump tiktok target version to 47.1.3 and update compatibility docs ([83e8d24](https://github.com/kveld9/kveld-morphe-patches/commit/83e8d240969b7fcd2f676de19e04b17a22c188fd))
* **tiktok:** add comment sort controls patch ([2751e0e](https://github.com/kveld9/kveld-morphe-patches/commit/2751e0ef8ff27a3ab555c98962863de2e2eb2fa8))
* **tiktok:** add disable search video autoplay patch ([7af1162](https://github.com/kveld9/kveld-morphe-patches/commit/7af1162bc3f190ceddc1a1001f5187a8a3f7f158))
* **tiktok:** add hide nearby feed tab patch ([ea84d65](https://github.com/kveld9/kveld-morphe-patches/commit/ea84d65ee6dac84f88ba2267189b3a4c33981cd5))
* **tiktok:** add hide seen videos patch ([027b385](https://github.com/kveld9/kveld-morphe-patches/commit/027b385cd8d6a2deb4a53fa5ccaf6a078c61bb6c))
* **tiktok:** add resume video after scroll patch ([265cd07](https://github.com/kveld9/kveld-morphe-patches/commit/265cd0713d2eb8cc2c32c32f0d74116172255745))
* **tiktok:** add stop video looping patch ([709a3ad](https://github.com/kveld9/kveld-morphe-patches/commit/709a3add8e766319f88d25a139a1a1c4d05b2464))
* **tiktok:** block sponsored ads in search video feeds ([f0dfa68](https://github.com/kveld9/kveld-morphe-patches/commit/f0dfa68104a40746473fc91d628a34d59deae295))
* **tiktok:** increase default custom offline videos limit to 1000 ([988887e](https://github.com/kveld9/kveld-morphe-patches/commit/988887e7e5146920e28836f57f5b192dc768169f))

### Code Refactoring

* **hevy:** standardize patch titles and align documentation metadata ([93549e3](https://github.com/kveld9/kveld-morphe-patches/commit/93549e322224e3420e2bce949211e420fa40f83b))
* **tiktok:** focus disable story feed indicators patch on avatar story rings and prune obsolete story tags ([8d77540](https://github.com/kveld9/kveld-morphe-patches/commit/8d7754071ca3e781ce4381cb88de56b18159d2b0))
* **xiaomi:** strip redundant title prefixes and enhance patch descriptions ([de37cc2](https://github.com/kveld9/kveld-morphe-patches/commit/de37cc2b5dd92145609a94506a26a80f59878983))

## [2.0.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.46.0...v2.0.0) (2026-09-26)

### ⚠ BREAKING CHANGES

* **vivaldi:** drop Vivaldi Browser target application and all 8 associated patches

### Bug Fixes

* **brave:** remove destructive elf bti and manifest extraction patches ([1785a21](https://github.com/kveld9/kveld-morphe-patches/commit/1785a219dff9491cc6ec7bbcef653d8d4947b5e2))
* **brave:** update arm32 libchrome telemetry offsets for 1.96.59 (closes [#50](https://github.com/kveld9/kveld-morphe-patches/issues/50)) ([98d702f](https://github.com/kveld9/kveld-morphe-patches/commit/98d702f5f077697ae231b2b08904059424058b4b))
* **harness:** fix split version code override, attribute resolution, and build-tools compatibility ([58565f2](https://github.com/kveld9/kveld-morphe-patches/commit/58565f2e5c0ddc60d16c2b9f43758b5d33a573d5))
* **nokoprint:** resolve driver download freeze and startup crash regressions ([c7118d2](https://github.com/kveld9/kveld-morphe-patches/commit/c7118d24e5af84d3f159193f741ac250ed8e35e1))
* **tiktok:** filter recom_search cards in suggested searches patch ([c34b779](https://github.com/kveld9/kveld-morphe-patches/commit/c34b7794696bd7bd071df2b20d05e40f7ba27de6))
* **tiktok:** filter trending_rank_live cards in popular lives search patch ([3a4c6b3](https://github.com/kveld9/kveld-morphe-patches/commit/3a4c6b3222ccb757299df9c41f251723dc87143a))
* **tiktok:** suppress aweme story model in disable story feed indicators patch ([c9fce75](https://github.com/kveld9/kveld-morphe-patches/commit/c9fce755030b0711318a83e86565e7ec972b3826))
* **tiktok:** suppress lynx ab parameters and schema queries in hide popular lives search patch ([cbb625e](https://github.com/kveld9/kveld-morphe-patches/commit/cbb625e668b5f70e24c87df1f2a255343949ec4c))
* **tiktok:** suppress lynx ab parameters and schema queries in hide suggested searches patch ([b54d095](https://github.com/kveld9/kveld-morphe-patches/commit/b54d0955b9e532adedc878faed768814f56aeb9e))

### New Features

* **gboard:** add strip permissions patch with configurable manifest toggles ([e53fc66](https://github.com/kveld9/kveld-morphe-patches/commit/e53fc6670cc0390d62e78b74b090b6cd6f0e424b))
* **tiktok:** add configurable options to hide shop tab and video anchors ([e1d93e9](https://github.com/kveld9/kveld-morphe-patches/commit/e1d93e940119274eb9b8191c85acdade6daf61b6))
* **tiktok:** replace stem and community tabs patch with hide community tab ([1eba714](https://github.com/kveld9/kveld-morphe-patches/commit/1eba7142e7f308192f13e2be978833b6bcbdf8c0))
* **tiktok:** separate screen capture bypass into dedicated patch ([541a62f](https://github.com/kveld9/kveld-morphe-patches/commit/541a62f180f7671bb88d3036d36f2400f1e030ca))
* **vivaldi:** drop vivaldi browser target and patches ([0103e8f](https://github.com/kveld9/kveld-morphe-patches/commit/0103e8f45db564b765428c5491d0295a000ce93c))

### Code Refactoring

* **brave:** demote bti and native extraction patches to anonymous internal invariants ([288e61c](https://github.com/kveld9/kveld-morphe-patches/commit/288e61c3405f6659851def6ed72e4b78d539c4bf))
* **gboard:** consolidate telemetry, primes, appdoctor and tenor tracking into block telemetry patch ([6f42fd5](https://github.com/kveld9/kveld-morphe-patches/commit/6f42fd5cc0ddf135dd039ffcfaa02560e09dd0f5))
* **gboard:** consolidate workmanager, mdd and superpacks into disable background sync patch ([9dfe539](https://github.com/kveld9/kveld-morphe-patches/commit/9dfe539ffd19bcc3d557e9b7b0fd036a215e05f8))
* **gboard:** decouple contacts permission stripping from offline only patch ([2b9bfd9](https://github.com/kveld9/kveld-morphe-patches/commit/2b9bfd9bead2a9ce391192908ac34f41b2f4de4e))
* **harness:** add 16kb zipalign signing and max version code support ([9ca61d3](https://github.com/kveld9/kveld-morphe-patches/commit/9ca61d361db3339f25d63f3c0c870d97ebe10cf1))
* **harness:** remove vivaldi pipeline and migration validator contracts ([ca2c09f](https://github.com/kveld9/kveld-morphe-patches/commit/ca2c09fadb68356aedfa060812c950fe0d0a071b))
* **nokoprint:** consolidate ad patches and streamline catalog names ([5d5715a](https://github.com/kveld9/kveld-morphe-patches/commit/5d5715acead329efec59933f7dcc8c68c36c582d))

## [1.46.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.45.0...v1.46.0) (2026-09-25)

### Bug Fixes

* **brave:** neutralize ARM64 BTI flag to prevent startup crash on Android 16 ([f799f29](https://github.com/kveld9/kveld-morphe-patches/commit/f799f2927bccde164bc30ef04a7b0b9255f27ed7))
* **ci:** prevent html tag truncation in telegram release notifications ([b758dd4](https://github.com/kveld9/kveld-morphe-patches/commit/b758dd46b1450105810f301801bed975590f593f))
* **tiktok:** prevent false positives in ai remix detection ([45e4eb9](https://github.com/kveld9/kveld-morphe-patches/commit/45e4eb9457a397e73b1be584b516eb332d07baf5))

### New Features

* **tiktok:** add custom share sheet patch ([4a7e2b8](https://github.com/kveld9/kveld-morphe-patches/commit/4a7e2b8b5d2ce5603bcef744630dd193b30bafe6))

### Code Refactoring

* **tiktok:** tune default patch selection and standardize metadata ([d9a0538](https://github.com/kveld9/kveld-morphe-patches/commit/d9a05381cb53eee20cc00da086c4ea4c84eef13b))

## [1.45.0](https://github.com/kveld9/kveld-morphe-patches/compare/v1.44.0...v1.45.0) (2026-09-25)

### Bug Fixes

* **brave:** update block telemetry native host offsets for 1.96.59 ([44adde5](https://github.com/kveld9/kveld-morphe-patches/commit/44adde5dd7ed1f605df14078f655ae07ea271ed8))
* **brave:** update brave origin fingerprints and packaging rule for 1.96.59 ([397c289](https://github.com/kveld9/kveld-morphe-patches/commit/397c289d2a506c802d512b4fb64dd41f63333376))
* **brave:** update suppress in-app promos fingerprints for 1.96.59 ([155ccad](https://github.com/kveld9/kveld-morphe-patches/commit/155ccad4fbbe105eb4a4b016eab7b456cb6dd703))
* **chromium:** remove return type constraint from clipboard fingerprint ([41d3789](https://github.com/kveld9/kveld-morphe-patches/commit/41d37892116fd39d6a4e1e852106f126a0622918))
* **gboard:** parse digits resiliently for clipboard enhancement options ([6f43c4e](https://github.com/kveld9/kveld-morphe-patches/commit/6f43c4e8a19857fb9449daa95f3e1e29ecd75a27))
* **hevy:** suppress subscription grace period warning banners ([176a4c3](https://github.com/kveld9/kveld-morphe-patches/commit/176a4c3ac10428d6110fdf4b794e87e02661a8db))
* **tiktok:** block suggested friend cards and bloat in friends feed ([8f4895e](https://github.com/kveld9/kveld-morphe-patches/commit/8f4895e98d21b8563c9b204a3ff4539d3c71dfc8))
* **tiktok:** enforce video download and playback resolution ceilings ([bec8b94](https://github.com/kveld9/kveld-morphe-patches/commit/bec8b94343c7371fbeeb2a9e341107b9fb6ae20f))
* **tiktok:** neutralize invasive permissions and prompts via bytecode defense without breaking launcher icons ([8e28a75](https://github.com/kveld9/kveld-morphe-patches/commit/8e28a759e9c1e9c001a7e4f702ffaa7ac959f282))

### New Features

* **brave:** add suppress in-app promos and surveys patch ([43ad539](https://github.com/kveld9/kveld-morphe-patches/commit/43ad539aa414625d87e7ec0ce70a7684fb3725cd))
* **brave:** add top sites toggle and modernize clean new tab page patch ([2b6fab5](https://github.com/kveld9/kveld-morphe-patches/commit/2b6fab5fe5b465ed751443e762ed5a4fd7f5a1fb))
* **brave:** bump target version to 1.96.59 ([974f4e4](https://github.com/kveld9/kveld-morphe-patches/commit/974f4e46350b90da0e48fdc56b3e34194f794cfc))
* **nokoprint:** update target to v5.28.4 and harden privacy suite ([abdb961](https://github.com/kveld9/kveld-morphe-patches/commit/abdb961245d4ba660935f205c21759b0fdbb067f))
* **tiktok:** add auto-pause first video patch ([c7e2908](https://github.com/kveld9/kveld-morphe-patches/commit/c7e2908fb8ddb774cd68244ac8387c63daea79b2))
* **tiktok:** add disable comment suggested emojis patch ([3c0a246](https://github.com/kveld9/kveld-morphe-patches/commit/3c0a246a535d4cbff9363714bf6e552a00157d24))
* **tiktok:** add Disable double tap to like patch ([0a2dfc8](https://github.com/kveld9/kveld-morphe-patches/commit/0a2dfc82c125fd625fbfb42679b6fb20096ba4a1)), closes [#55](https://github.com/kveld9/kveld-morphe-patches/issues/55)
* **tiktok:** add Disable Search History Recording patch ([ded97f7](https://github.com/kveld9/kveld-morphe-patches/commit/ded97f75b694d9a609045e6ec10c42085a793787))
* **tiktok:** add Disable Story Feed Indicators patch ([1fd44f3](https://github.com/kveld9/kveld-morphe-patches/commit/1fd44f3117b9ec7a37aba8e29ce378bab38098bb))
* **tiktok:** add Disable Watch History Recording patch ([3011b7b](https://github.com/kveld9/kveld-morphe-patches/commit/3011b7b3b3f31ed5844f7b0aecb92247182262ff))
* **tiktok:** add Enable Voice Comments patch ([c1bfbff](https://github.com/kveld9/kveld-morphe-patches/commit/c1bfbff2de13ebcf8217625c2eb3ce0b0d338a61))
* **tiktok:** add Force auto-scroll patch ([8a82466](https://github.com/kveld9/kveld-morphe-patches/commit/8a82466a879debb73e38989bfc764a5ce9d40133))
* **tiktok:** add Hide AI-Generated Content patch ([67e9a4f](https://github.com/kveld9/kveld-morphe-patches/commit/67e9a4f5f67637826b816984bb77d44244ceeeeb))
* **tiktok:** add Hide Feed Search Bar patch ([0cd1a87](https://github.com/kveld9/kveld-morphe-patches/commit/0cd1a877ae7a5fca34fef44540c8fbe4ed5a94e2))
* **tiktok:** add Hide Popular Lives In Search patch ([d556ed6](https://github.com/kveld9/kveld-morphe-patches/commit/d556ed6ace33301651b09bbd676f0624787bda79))
* **tiktok:** add Hide Suggested Searches patch ([4528bc3](https://github.com/kveld9/kveld-morphe-patches/commit/4528bc3f0fd44bc406848b56a3f045f5c44c8315))
* **tiktok:** add patch to disable profile photo LIVE status and force profile navigation ([a8c12f7](https://github.com/kveld9/kveld-morphe-patches/commit/a8c12f7a7facaafe3f83171f32ce51f97eeb9bc4))
* **tiktok:** add patch to hide profile photo follow button ([9d545ee](https://github.com/kveld9/kveld-morphe-patches/commit/9d545ee8741a6e1912cd4b559649c816e42d00a0))
* **tiktok:** add patch to hide STEM and community tabs ([f55d1a2](https://github.com/kveld9/kveld-morphe-patches/commit/f55d1a28bb39e9acbe9637976ab5abf8126498a6))
* **tiktok:** add patch to hide top-left LIVE button ([f62d462](https://github.com/kveld9/kveld-morphe-patches/commit/f62d4623a804d3dcb0f7c572bd4f1bcd6a6063da))

### Code Refactoring

* **brave:** demote native extraction patch to internal packaging invariant ([15af474](https://github.com/kveld9/kveld-morphe-patches/commit/15af474c59b5fbf4628429657f2b354a6ac94e4c))
* **tiktok:** prune obsolete permission dispatcher and use range invokes in Device Privacy Guard ([4946079](https://github.com/kveld9/kveld-morphe-patches/commit/4946079266d5ac96679ba34e3c766705c0a7b083))

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
