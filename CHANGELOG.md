# Changelog

## [1.0.1](https://github.com/kidager/ch.jacem.starclone/compare/v1.0.0...v1.0.1) (2026-10-21)


### Bug Fixes

* **app:** back handled by OnBackPressedCallback, back gestures skip onBackPressed from API 36 ([c108c8c](https://github.com/kidager/ch.jacem.starclone/commit/c108c8cd4ea21170e2d189a35056fe7df945abef))
* **app:** internal components not exported, preference sets copied, explicit locales, exact alarm screen only on Android 12+ ([8969dde](https://github.com/kidager/ch.jacem.starclone/commit/8969ddea7fecdcc0fd12beeb8f167a84aa30aeb3))
* **app:** minimum SDK 24, required by Play automatic protection ([88f198b](https://github.com/kidager/ch.jacem.starclone/commit/88f198b1bbf4e3375fac8a5639eb9e84a45ae4a2))
* **ci:** apktool wrapper fetched from the pinned release tag ([62af2b4](https://github.com/kidager/ch.jacem.starclone/commit/62af2b4b5a0c4f729ef071a28d9caad3f727c03c))
* **i18n:** typos and typography, Chinese cancelled-sync text without a missing argument ([d10ce67](https://github.com/kidager/ch.jacem.starclone/commit/d10ce6789bf651e9e56c6d32c4caf1188711c29c))
* **ui:** primitive Compose state, resources and locale read from the composition ([cbcf9cb](https://github.com/kidager/ch.jacem.starclone/commit/cbcf9cb4aaf58e1e2936f089a6ddc7a193789aab))
* **ui:** resources and locale read through LocalResources and LocalConfiguration ([0dcb898](https://github.com/kidager/ch.jacem.starclone/commit/0dcb8989eb370ba26da8fc60f6df1f6588724f96))
* **updates:** update check and download point at ch.jacem.starclone and its release APK name ([358deac](https://github.com/kidager/ch.jacem.starclone/commit/358deac263fc1e543a0e17ecc48f187100fe868f))
* **updates:** update checker in the rs flavor only, OSS build without the NonFreeNet library ([a1b6f39](https://github.com/kidager/ch.jacem.starclone/commit/a1b6f391a1cb3848cfb5b188ba5e24cea3d2d33d))


### Miscellaneous Chores

* **lint:** baseline regenerated ([8e1fed8](https://github.com/kidager/ch.jacem.starclone/commit/8e1fed8d2871293b2feb3869945448d7dc8a3bb9))
* **lint:** baseline regenerated without the fixed and stale entries ([c6452eb](https://github.com/kidager/ch.jacem.starclone/commit/c6452eb9bba389e5bf7dc85e6c93a453ccf8cc12))


### Code Refactoring

* **app:** version checks below Android 10 removed, FlagsUtil combines FLAG_IMMUTABLE with OR ([68df4f1](https://github.com/kidager/ch.jacem.starclone/commit/68df4f1ff288bd355c628cf5c401b84d6d6b1e33))


### Dependencies

* **app:** Jackson 2.22.3 with annotations 2.22, LocalBroadcastManager declared ([c62d3f8](https://github.com/kidager/ch.jacem.starclone/commit/c62d3f8eef41629a4d87e7f9866bcc024ece289b))
* bump androidx.appcompat:appcompat from 1.6.1 to 1.8.0 ([52e13b1](https://github.com/kidager/ch.jacem.starclone/commit/52e13b1bf41d83ec79efa8c6cf7521066a3d77b9))
* bump androidx.compose:compose-bom from 2024.12.01 to 2026.09.00 ([e097ff4](https://github.com/kidager/ch.jacem.starclone/commit/e097ff41cadf7ea0e4b68546c937181fbcc083fe))
* bump androidx.documentfile:documentfile from 1.0.1 to 1.1.0 ([03d3bea](https://github.com/kidager/ch.jacem.starclone/commit/03d3beadfcfff6b04af43286293dd6cd69041ef7))
* bump androidx.navigation:navigation-compose from 2.8.5 to 2.10.2 ([d9546db](https://github.com/kidager/ch.jacem.starclone/commit/d9546dbaaf02905f1c88f4c86436f11f7be435f7))
* bump androidx.work:work-runtime-ktx from 2.10.0 to 2.12.0 ([67b4706](https://github.com/kidager/ch.jacem.starclone/commit/67b4706505f7630484f0e2c9db8a1fa7b58ac85a))
* bump com.google.android.material:material from 1.12.0 to 1.14.0 ([d31990c](https://github.com/kidager/ch.jacem.starclone/commit/d31990cac87fecb74a6f110c063231cfa242740f))
* bump com.squareup.okhttp3:okhttp from 4.12.0 to 5.5.0 ([48f5493](https://github.com/kidager/ch.jacem.starclone/commit/48f5493cd0d32cde9517e270779822685685e363))
* bump org.jetbrains.kotlinx:kotlinx-coroutines-test ([1517ad9](https://github.com/kidager/ch.jacem.starclone/commit/1517ad9467d4cd46bda988297a166e16a6e2e970))
* update Kotlin to 2.4.21 and Gradle to 9.8.1 ([e679120](https://github.com/kidager/ch.jacem.starclone/commit/e67912086da050591081045ba61e550588ba545f))


### Build System

* Gradle 9.8, AGP 9.4, Kotlin 2.4, JDK 25 and SDK 29/36/37 as in id3 ([e1b4e32](https://github.com/kidager/ch.jacem.starclone/commit/e1b4e32e23e3711e3ed4f187a590cc51699d2763))

## 1.0.0 (2026-10-20)


### Features

* **backup:** add encrypted backup and restore with Compose UI ([f6d823d](https://github.com/kidager/ch.jacem.starclone/commit/f6d823db520be3e23d60d91361ff0176935e0437))
* **backup:** encrypt rclone.conf in Android Auto Backup ([96a1408](https://github.com/kidager/ch.jacem.starclone/commit/96a1408d83ae080f0b6998d46f325ef2f0dfa812))
* **compose:** migrate about screens and filter to Compose ([3bff37a](https://github.com/kidager/ch.jacem.starclone/commit/3bff37a414b735a655e9e5f18603008fd9713fe3))
* **compose:** migrate main screens to Jetpack Compose with Material 3 ([7ddc5bf](https://github.com/kidager/ch.jacem.starclone/commit/7ddc5bf7fb2c5c0012d4ac086599a9c948464801))
* **explorer:** add in-app transfer progress indicator ([20b849f](https://github.com/kidager/ch.jacem.starclone/commit/20b849fe42245fcaedd3b837e40aac4e62b626a4))
* **file-picker:** migrate FilePicker to Kotlin and Compose ([4d2a64e](https://github.com/kidager/ch.jacem.starclone/commit/4d2a64e583059099a4ac9941fb9bfeb68cb3a77b))
* **onboarding:** move import config from popup to onboarding slide ([9c9cdd4](https://github.com/kidager/ch.jacem.starclone/commit/9c9cdd43f3cd46f18acb2882348192b0b030cd43))
* **remote-config:** migrate provider list and config form to Compose ([ab423ab](https://github.com/kidager/ch.jacem.starclone/commit/ab423ab9fb67d34e9a3941c0582687de4ce28431))
* **remotes:** map all remote types to specific icons ([475af92](https://github.com/kidager/ch.jacem.starclone/commit/475af928d991ca2694f2dea4cb54c03e69c8bc6f))
* **sharing:** migrate SharingActivity to Kotlin with Compose remote list ([176964e](https://github.com/kidager/ch.jacem.starclone/commit/176964e5b7411585c0394bf3256040c75e14f993))
* **tasks:** add long press to open options on task and trigger cards ([13a6295](https://github.com/kidager/ch.jacem.starclone/commit/13a6295e66743121360b96ecbed9612b312df204))
* **tasks:** add remote-to-remote sync and copy directions ([5bf24d7](https://github.com/kidager/ch.jacem.starclone/commit/5bf24d760a804b87e65c8c00f7cf8f1ab0d47206))
* **ui:** add processes page, dialog components, dynamic colors, and nav improvements ([a51fb8b](https://github.com/kidager/ch.jacem.starclone/commit/a51fb8b3b964de6ec99522f6161d6dd75922028f))


### Bug Fixes

* **backup:** use rclone config encryption set for config encryption ([16bb953](https://github.com/kidager/ch.jacem.starclone/commit/16bb953e1d997a52f649e43325a023f7222bfffd))
* **ci:** NDK install no longer fails on the yes broken pipe ([4f81b83](https://github.com/kidager/ch.jacem.starclone/commit/4f81b8392d63289f2ce78c3983d3dadae4f0a82d))
* **cleanup:** restore XML layouts still referenced by active code ([90b7aa1](https://github.com/kidager/ch.jacem.starclone/commit/90b7aa128fbd1a876f16c1d06cd27a356adfa18e))
* **cloud-backup:** fix encryption cancel, age key UX, and default folder ([bc5a016](https://github.com/kidager/ch.jacem.starclone/commit/bc5a016bbe6267fd23875b2a42ed0f66a2fe5d08))
* **core:** recycle parcels, add lazy key, fix typo, and use proper logging ([70f090d](https://github.com/kidager/ch.jacem.starclone/commit/70f090db901a1bbd8046226f607de7e15bfba26c))
* **explorer:** remove path-based LazyColumn keys to prevent duplicate key crash ([c46cabc](https://github.com/kidager/ch.jacem.starclone/commit/c46cabcce90b608d971ceb2605c7f198db918ba3))
* **import:** backup config before import and restore on cancel ([16ae4e2](https://github.com/kidager/ch.jacem.starclone/commit/16ae4e23de2cd1fb02c91201475fcc4df4835a3b))
* **logs:** make detail dialog scrollable ([2a260cf](https://github.com/kidager/ch.jacem.starclone/commit/2a260cf532b5f67f6209349335939f502ca3ef0b))
* **logs:** split rclone stderr into individual log entries ([0ada868](https://github.com/kidager/ch.jacem.starclone/commit/0ada868d1d76af4afedae8a21c46ba23e218b142))
* **nav:** persist selected tab across Activity recreation ([dc91b2e](https://github.com/kidager/ch.jacem.starclone/commit/dc91b2e181b47d5b79eb8b9883eefc753678b85b))
* **notifications:** show filename instead of 0 count on fast transfers ([45deb9a](https://github.com/kidager/ch.jacem.starclone/commit/45deb9a7c0b751507606b5b534b85aa3d8181637))
* **remoteconfig:** clear menu before inflating to prevent duplicates ([95fc13f](https://github.com/kidager/ch.jacem.starclone/commit/95fc13f362c8c06743aa0ebe2c81e30c6ddf95ab))
* **settings:** use Compose fragments in SettingsActivity ([8dae4d5](https://github.com/kidager/ch.jacem.starclone/commit/8dae4d5b243d6366734aca830b654d0ed6559610))
* **tasks:** make delete button red in task and trigger option sheets ([083dade](https://github.com/kidager/ch.jacem.starclone/commit/083dade0978ab5ee7034959f9cf25ce812515071))
* **tasks:** use map lookup for triggers and fix toggle persistence ([0631d3e](https://github.com/kidager/ch.jacem.starclone/commit/0631d3ebf505c87383b324e7683bcc6cf01a6d48))
* **tasks:** use referential equality for trigger toggle reactivity ([59520b6](https://github.com/kidager/ch.jacem.starclone/commit/59520b6f1d580ddeb5cfefbf157296e1da9c1685))
* **ui:** address multiple UI issues across Compose screens ([a04de51](https://github.com/kidager/ch.jacem.starclone/commit/a04de5146ece6ca1e25fa6360065eedbbf20d374))
* **ui:** improve trigger time picker, clickable triggers, and remotes refresh ([2bfd467](https://github.com/kidager/ch.jacem.starclone/commit/2bfd467a64c0755a0b0a35c5b8f39d023d5902d9))
* **ui:** sync bottom nav state, add password toggle and remote long press ([c722e29](https://github.com/kidager/ch.jacem.starclone/commit/c722e297430fa6086a77f82485421fb00879395d))
* **workers:** check rclone exit code and capture stderr errors ([47483b5](https://github.com/kidager/ch.jacem.starclone/commit/47483b5f8f6087f4ef445b264aa86fce83885fd3))
* **workers:** throttle notifications and log errors individually ([a6b04ea](https://github.com/kidager/ch.jacem.starclone/commit/a6b04ea3b8a5e05f6aeb41a74680ffb75cb505cc))


### Performance Improvements

* **compose:** add contentType to LazyColumn items for recycling ([5924f1c](https://github.com/kidager/ch.jacem.starclone/commit/5924f1c20958f9feac135981bfbd4960c1461878))
* **rclone:** add resource limits and drive-acknowledge-abuse flag ([d753236](https://github.com/kidager/ch.jacem.starclone/commit/d7532369eab1a79a7891c65f78b63ce69afcf985))


### Miscellaneous Chores

* **build:** reduce rclone binary size and add release signing config ([f4a6714](https://github.com/kidager/ch.jacem.starclone/commit/f4a6714f2118f607eaee34c2c80990ea91bbee36))
* **build:** refine proguard rules for release builds ([437ff1e](https://github.com/kidager/ch.jacem.starclone/commit/437ff1ed8b1a8e381c1f76f1da27f99fc1530ba4))
* **cleanup:** remove dead code from Compose migration ([d78bf44](https://github.com/kidager/ch.jacem.starclone/commit/d78bf440786377e6d4d1b52cc5e40cc83f062d94))
* **cleanup:** remove unused XML menu resources ([a8dc48b](https://github.com/kidager/ch.jacem.starclone/commit/a8dc48bce5b81ad27998b07999717ad99442b10d))
* **deps:** update dependencies and bump safdav SDK to 35 ([8ba950c](https://github.com/kidager/ch.jacem.starclone/commit/8ba950ca456c183609e78aa3c5ebd8dc5c46dc32))
* **lint:** baseline updated with the current lint errors ([95233b6](https://github.com/kidager/ch.jacem.starclone/commit/95233b6ad911c215b40deac7252be301e5407ad4))
* **release:** release-please config and Dependabot as in the other Android repos ([cdb7611](https://github.com/kidager/ch.jacem.starclone/commit/cdb7611658a8a401414244b63a2eacac08512cbb))
* unknown fixes ([db20efa](https://github.com/kidager/ch.jacem.starclone/commit/db20efa030ac0c68d05a22c9c2f3e4e0f292f357))


### Code Refactoring

* **branding:** replace Round Sync references with StarClone ([591cae7](https://github.com/kidager/ch.jacem.starclone/commit/591cae726f1517c66a0ccdc93a1a00eec2e32e3b))
* **cleanup:** remove legacy XML dialogs, fragments, and settings ([65aa0b8](https://github.com/kidager/ch.jacem.starclone/commit/65aa0b8c0a742eece6462f22cf207adc24b4f7f3))
* **compose:** migrate settings, sharing, backup, and folder picker to Compose ([9696eb3](https://github.com/kidager/ch.jacem.starclone/commit/9696eb382a206f052bc4b08071de1e68e4700f6f))
* **explorer:** consolidate file actions into single bottom sheet ([7fa6846](https://github.com/kidager/ch.jacem.starclone/commit/7fa684632639f65e8483fb0ecc477e662840c5f0))
* **namespace:** rename ca.pkay.rcloneexplorer to ch.jacem.starclone ([7de65a7](https://github.com/kidager/ch.jacem.starclone/commit/7de65a7756e36e310711992298b3f64a1a09ee01))
* **namespace:** rename de.felixnuesse to ch.jacem.starclone ([374cb02](https://github.com/kidager/ch.jacem.starclone/commit/374cb020e5f4c95486981238ed9d066c3575a63f))
* **rebrand:** rename extract package to core and rebrand docs for StarClone ([a31e51d](https://github.com/kidager/ch.jacem.starclone/commit/a31e51d453f4705a660c0054c4f746a5174f925e))
* **tasks:** migrate task and trigger screens to Compose ([b084b17](https://github.com/kidager/ch.jacem.starclone/commit/b084b179504265f5a3690ae5198e9c242ca7c2fb))
* **toolbar:** migrate XML toolbar to per-screen Compose StarCloneTopBar ([c6cf845](https://github.com/kidager/ch.jacem.starclone/commit/c6cf845a8f7aab59ca216bd0e34ab767988ccc75))


### Dependencies

* **ci:** update every action to its latest major version ([b24819b](https://github.com/kidager/ch.jacem.starclone/commit/b24819b0bc2d6d36f0a9290dec2246783cb7d324))
* **rclone:** upgrade rclone from v1.71.0 to v1.73.0 ([c3d16fc](https://github.com/kidager/ch.jacem.starclone/commit/c3d16fc7c3eecbc056b75352e664466c885d5bf4))


### Tests

* **backup:** add unit tests for crypto and config parsing ([0ff6377](https://github.com/kidager/ch.jacem.starclone/commit/0ff637742f29f1b573715a9ee013033a7c7f1cc4))
* **items:** add FileItem unit tests ([25c0851](https://github.com/kidager/ch.jacem.starclone/commit/25c0851c3bf76390301041d194c82c2ac30eb02e))


### Continuous Integration

* cancel superseded lint, translation and dependency checks ([e1db136](https://github.com/kidager/ch.jacem.starclone/commit/e1db136bb274a17254ef926896acc2ef7b130e27))
* **deps:** dependency check on main and PRs only, not on tags ([cc7324e](https://github.com/kidager/ch.jacem.starclone/commit/cc7324e9bfbcf6716e711302ce31d7565481e21b))
* **release:** add release-please and dependabot config ([6054a67](https://github.com/kidager/ch.jacem.starclone/commit/6054a675950b295dd3af81a5dbb39d96a1bb25d9))
* **release:** kidager-bot release flow with Play internal, promote and APK labels, cached rclone ([5fccf63](https://github.com/kidager/ch.jacem.starclone/commit/5fccf632b332942806087d6b97b044498e2db29c))
* **release:** no build or gate run on closed PRs (kidager-bot relabels the merged release PR) ([08a443c](https://github.com/kidager/ch.jacem.starclone/commit/08a443c05a93a5e656196b52fe33c6b9f39bdaea))
* **release:** signed build only once the release PR has the build label, gated by the release-build check ([653d787](https://github.com/kidager/ch.jacem.starclone/commit/653d7873e4d50eb5db6a4a91d6c64bad8a31ec1e))
