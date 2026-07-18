# Zeam Launcher 3.1.10

Reconstructed Android project for Zeam Launcher 3.1.10. The reconstruction is based on raw JADX source in `origin/original_source`; the original APK is archived at [`docs/reference/zeam-launcher-3-1-10-en-android.apk`](docs/reference/zeam-launcher-3-1-10-en-android.apk).

## Project layout

Before reconstruction, the project was a flat, non-Gradle layout:

```text
sources/org/zeam/*.java
resources/
├── AndroidManifest.xml
├── res/
└── META-INF/
```

Current layout:

```text
.
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/org/zeam/
│       └── res/
├── app/build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── gradlew
├── CLAUDE.md
└── docs/
    ├── README.md
    ├── CHANGELOG.md
    └── reference/
```

## Toolchain

Validation uses JDK 17, Gradle Wrapper 8.7, Android Gradle Plugin 8.5.2, Android SDK Platform 35, and Build Tools 34.0.0. `adb` and Android Emulator are used for device checks; Docker and KVM are used for validation environments.

## Build

Set `ANDROID_SDK_ROOT` to the installed Android SDK, then build:

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
./gradlew assembleDebug --no-daemon
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install and launch

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zeam -c android.intent.category.LAUNCHER 1
```

On modern Android versions, set Zeam as the device's HOME app through the system HOME-role/default-launcher setup before testing launcher behavior.

## Languages

Zeam supports System default, English, and Brazilian Portuguese (`pt-BR`). Choose **Preferences → General → Language**; Zeam persists the selection and restarts so launcher resources and cached application labels reload consistently. Unsupported or malformed stored language values fall back safely to English.

Language resources are packaged with the base app so in-app switching also works for Android App Bundle installs. Supported languages are currently left-to-right; RTL layout support remains deferred.

## Verification and compatibility

Validation targets `assembleDebug`, `:app:testDebugUnitTest`, `:app:lint`, and `git diff --check`. No emulator or device was available for this feature pass, so on-device locale rendering remains pending.

`minSdk` 8 remains preserved. The app has no third-party runtime dependencies; JUnit 4 is test-only. On API 35, static wallpaper bitmap access falls back to the system wallpaper background when platform access is denied.

See [`docs/CHANGELOG.md`](docs/CHANGELOG.md) for detailed reconstruction evidence and known risks.