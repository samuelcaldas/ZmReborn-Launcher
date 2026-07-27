# ZM Reborn 3.1.11-alpha

Reconstructed Android project for ZM Reborn from the historical Zeam Launcher 3.1.10 source. The reconstruction is based on raw JADX source in `origin/original_source`; the original APK is archived at [`docs/reference/zeam-launcher-3-1-10-en-android.apk`](docs/reference/zeam-launcher-3-1-10-en-android.apk).

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
│       ├── java/org/zmreborn/
│       └── res/
├── app/build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── gradlew
├── CLAUDE.md
├── tools/
│   └── build_apk.sh
└── docs/
    ├── README.md
    ├── CHANGELOG.md
    └── reference/
```

## Toolchain

Validation uses JDK 17, Gradle Wrapper 8.7, Android Gradle Plugin 8.5.2, Android SDK Platform 35, and Build Tools 34.0.0. `adb` and Android Emulator are used for device checks; Docker and KVM are used for validation environments.

## Build

All local debug APK builds use the Docker wrapper:

```sh
./tools/build_apk.sh
```

The wrapper requires Docker context `docker-dev` and image `zeam-docker-dev:android35`. It resolves that tag to the inspected local content-addressed image ID and runs with `--pull=never`, preventing tag changes or network pulls between validation and execution. It mounts the project at `/workspace`, reuses volume `zeam-gradle-cache`, supplies `TZ=America/Sao_Paulo`, and unsets `DOCKER_HOST` so it cannot override the selected context. No host JDK or `ANDROID_SDK_ROOT` is required.

Successful builds print only artifact metadata:

```text
APK: app/build/outputs/apk/debug/app-debug.apk
Bytes: <size>
SHA-256: <digest>
```

Failures print the captured Docker/Gradle diagnostics. Use `./tools/build_apk.sh --help` for optional `DOCKER_CONTEXT`, `DOCKER_IMAGE`, and `GRADLE_CACHE_VOLUME` overrides. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`; do not invoke `assembleDebug` directly for local builds.

## Releases

Debug CI artifacts are not release builds. Signed APK publication uses the protected GitHub Actions workflow described in [`docs/RELEASING.md`](docs/RELEASING.md). It validates semantic tags, app version metadata, changelog notes, signing certificate identity, APK metadata, and checksums before publishing a GitHub Release.

## Install and launch

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
```

On modern Android versions, set ZM Reborn as the device's HOME app through the system HOME-role/default-launcher setup before testing launcher behavior.

## Languages

ZM Reborn supports System default, English, and Brazilian Portuguese (`pt-BR`). Choose **Preferences → General → Language**; ZM Reborn persists the selection and restarts so launcher resources and cached application labels reload consistently. Unsupported or malformed stored language values fall back safely to English.

Language resources are packaged with the base app so in-app switching also works for Android App Bundle installs. Supported languages are currently left-to-right; RTL layout support remains deferred.

## Verification and compatibility

Validation uses `./tools/build_apk.sh` for the debug APK, plus `:app:testDebugUnitTest`, `:app:lint`, and `git diff --check`. Runtime-facing changes require emulator smoke coverage on API 24 and API 35.

`minSdk` 24 supports Android 7.0 and newer. The app has no third-party runtime dependencies; JUnit 4 is test-only. On API 35, static wallpaper bitmap access falls back to the system wallpaper background when platform access is denied.

See [`docs/CHANGELOG.md`](docs/CHANGELOG.md) for detailed reconstruction evidence and known risks.