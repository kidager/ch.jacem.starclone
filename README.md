# StarClone - Rclone for Android

[![license: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/kidager/rclone-android/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/kidager/rclone-android?include_prereleases)](https://github.com/kidager/rclone-android/releases/latest)

A cloud file manager for Android, powered by [rclone](https://rclone.org/).

## Features

- **File Management** (list, view, download, upload, move, rename, delete files and folders)
- **Streaming** (stream media files, serve files and directories over FTP, HTTP, WebDAV or DLNA)
- **Integration** (access local storage devices and share files to store them on a remote)
- **Many cloud storage providers** (all via rclone config import, some with built-in UI setup)
- **Material 3 Design** (dynamic colors, dark theme)
- **All architectures** (ARM, ARM64, x86 and x64 devices, Android 7+)
- **Storage Access Framework (SAF)** for SD card and USB device access
- **Task Management** with scheduled sync, copy, and move operations
- **Remote-to-Remote** sync and copy support

## Installation

Grab the [latest release](https://github.com/kidager/rclone-android/releases/latest) APK and install it on your phone.

| CPU architecture | Where to find | APK identifier |
|:---|:--|:---:|
|ARM 32 Bit | older devices | `armeabi-v7a` |
|**ARM 64 Bit** | **most devices** | `arm64-v8a` |
|Intel/AMD 32 Bit | some TV boxes and tablets | `x86` |
|Intel/AMD 64 Bit | some emulators | `x86_64` |

If you don't know which version to pick, use the `universal` APK.

## Developing

You should first make sure you have:

- Go 1.24+ installed and in your PATH
- Java 17+ installed and in your PATH
- Android SDK command-line tools installed OR the NDK version specified in `gradle.properties`

You can then build the app from Android Studio or from CLI:

```sh
# Debug build
./gradlew assemblePlaystoreDebug

# Install on connected device
./gradlew app:installPlaystoreDebug
```

## Libraries

- [rclone](https://github.com/rclone/rclone) - The engine behind StarClone. See https://rclone.org/donate/ to support rclone.
- [Jetpack AndroidX](https://developer.android.com/license)
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Glide](https://github.com/bumptech/glide) - Image loading and caching
- [AppIntro](https://github.com/AppIntro/AppIntro) - Onboarding slides

## Contributing

See [CONTRIBUTING](./CONTRIBUTING.md).

## License

This app is released under the terms of the [GPLv3 license](./LICENSE).

## About

StarClone is a fork of [Round Sync](https://github.com/newhinton/Round-Sync) by Felix Nuesse, which is a fork of [RCX](https://github.com/x0b/rcx) by x0b, which is a fork of [rcloneExplorer](https://github.com/patrykcoding/rcloneExplorer) by Patryk Kaczmarkiewicz.
