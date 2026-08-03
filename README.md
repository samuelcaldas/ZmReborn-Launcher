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
│   ├── build_apk.sh
│   ├── Dockerfile.emulator
│   └── emulator-entrypoint.sh
└── docs/
    ├── README.md
    ├── CHANGELOG.md
    ├── UI_STATE.md
    ├── uiux.md
    └── reference/
```

## Toolchain

Validation uses JDK 17, Gradle Wrapper 8.7, Android Gradle Plugin 8.6.0, Android SDK Platform 35, and Build Tools 34.0.0. `adb` and Android Emulator are used for device checks; Docker and KVM are used for validation environments.

## Build

All local debug APK builds use the Docker wrapper:

```sh
./tools/build_apk.sh
```

The wrapper requires Docker context `docker-dev` and image `zeam-docker-dev:android35`. It resolves that tag to the inspected local content-addressed image ID and runs with `--pull=never`, preventing tag changes or network pulls between validation and execution. It mounts the project at `/workspace`, runs the build with container `HOME` and `GRADLE_USER_HOME` set to `/root`, reuses volume `zeam-gradle-cache` at `/root/.gradle`, and keeps the generated debug signing identity in `zeam-android-user-home` at `/root/.android`. Stable debug signing allows separately assembled instrumentation APKs to match the app signature. The wrapper supplies `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`, `TZ=America/Sao_Paulo`, and unsets `DOCKER_HOST` so it cannot override the selected context. No host JDK or `ANDROID_SDK_ROOT` is required.

Successful builds print only artifact metadata:

```text
APK: app/build/outputs/apk/debug/app-debug.apk
Bytes: <size>
SHA-256: <digest>
```

Failures print the captured Docker/Gradle diagnostics. Use `./tools/build_apk.sh --help` for optional `DOCKER_CONTEXT`, `DOCKER_IMAGE`, `GRADLE_CACHE_VOLUME`, `ANDROID_USER_HOME_VOLUME`, and `JAVA_HOME_IN_CONTAINER` overrides. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`; do not invoke `assembleDebug` directly for local builds.

## Releases

Debug CI artifacts are not release builds. Signed APK publication uses the protected GitHub Actions workflow described in [`docs/RELEASING.md`](docs/RELEASING.md). It validates semantic tags, app version metadata, changelog notes, signing certificate identity, APK metadata, and checksums before publishing a GitHub Release.

## Install and launch

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
```

On modern Android versions, set ZM Reborn as the device's HOME app through the system HOME-role/default-launcher setup before testing launcher behavior.

## Languages and preferences

ZM Reborn supports System default, English, and Brazilian Portuguese (`pt-BR`). Choose **Preferences → General → Language**; ZM Reborn persists the selection and restarts so launcher resources and cached application labels reload consistently. Unsupported or malformed stored language values fall back safely to English.

Choose **Preferences → General → Theme** for Follow system, Light, or Dark. The persisted `application_appearance` selection is composed with the locale configuration by `LocaleUtil`, changing only night-mode bits and preserving all other configuration bits. Launcher and Settings use recreation paths to apply a changed selection.

**Preferences → General → Appearance → Blur backgrounds** applies one frosted-glass style to dock and active app drawer. API 31+ uses best-effort static-wallpaper blur when `Workspace` already has readable pixels; API 24–30, live wallpaper, and restricted wallpaper access use deterministic Material-tinted frost without new permissions. Disabling restores saved dock style and drawer transparency.

Language resources are packaged with the base app so in-app switching also works for Android App Bundle installs. Supported languages are currently left-to-right; RTL layout support remains deferred.

## Verification and compatibility

Validation uses `./tools/build_apk.sh` for the debug APK, plus `:app:testDebugUnitTest`, `:app:lint`, and `git diff --check`. Runtime-facing changes require hosted API 24 plus KVM-backed Docker API 35 and API 36 evidence.

`minSdk` 24 supports Android 7.0 and newer. The app has no third-party runtime dependencies; JUnit 4 is test-only. On API 35, static wallpaper bitmap access falls back to the system wallpaper background when platform access is denied.

`Launcher` consumes real `WindowInsets` via `org.zmreborn.compat` (`WindowInsetsCompat`, `GestureExclusionCompat`, `BackGestureCompat`). Gesture insets reach `Workspace`, `DragLayer`, and the Applications drawer; Dock receives system-bar metadata. While forced edge-to-edge remains opted out, normal decor fitting already keeps drawer content outside system bars, so drawer system-bar padding stays zero to avoid duplicate spacing. Registered API 33+ platform predictive-back callback object invocation/delivery closes the drawer then an open folder; API 34 adds a reversible scale/fade preview. Workspace only excludes its matching top or bottom edge when that configured swipe opens applications, preserving Android's left/right system-back edges otherwise. API 29/30/33/34 platform classes are isolated in nested, version-gated classes so pre-`minSdk` devices never verify against them. `values-v35/styles.xml` still opts out of forced edge-to-edge enforcement; this remains transitional until API 36 runtime and hands-on gesture evidence covers the complete launcher surface.

Visual theming uses semantic light/night M3 role tokens (`m3_primary`, `m3_surface`, `m3_surface_variant`, `m3_outline`, etc.) and platform Settings/dialog themes. API 31+ resolves system dynamic-color resources and bypasses the wallpaper palette cache. API 24–30 uses `org.zmreborn.theme.WallpaperColorExtractor`: API 27+ reads the system wallpaper seed and derives an HSV tonal palette, while API 24–26 uses a static amber-seed fallback; schema-v2 cache entries are isolated by light/dark brightness. Extraction runs off the launcher main path on a dedicated single-thread executor. Cached palette reapplication retains drawer pagination and user-folder rails, and reapplies drawer surfaces and labels, `Dock`, open folders, and rename-dialog palettes. Settings reads its current themed surface when created. Cached roles do not replace compiled color resources. Settings boolean controls use platform switches with semantic state colors and borderless normal rows.

Fresh installations use a gesture-first vertical application drawer with responsive `GridView.AUTO_FIT` columns. Vertical mode includes accent-insensitive search, prefix-first ranking, stable query/scroll restoration, and Automatic/Comfortable/Default/Compact density presets. Empty-query multi-section results expose a 48dp, accent-normalized alphabetical fast-scroll rail; it compacts on short windows, hides while search reorders results, supports touch, keyboard, and per-section accessibility actions, and mirrors rail inset/focus traversal in RTL locales. Horizontal paging remains advanced compatibility. Empty workspaces remain blank, one-page indicators stay hidden, and first-run dock entries resolve installed Phone, Messages, Browser, Camera, and Contacts handlers without adding a drawer button. Adaptive icons remain live drawables; legacy and null icons use shared normalization and fallback handling. Empty-home long press directly lists Widgets, Shortcuts, Folders, Wallpaper, and Preferences; the options-menu Add dialog remains available. Launcher-owned widget selection shows Search first plus localized provider preview cards, declares package visibility for receiver-only widget providers, allocates no host ID until external provider selection, routes pointer activation through the picker list, and closes the picker before binding or configuration. Missing or inaccessible configuration activities release pending host state instead of crashing Launcher. An instrumentation-only external provider verifies Add-dialog and provider-card pointer input, exact-provider binding, configuration-only ID-specific rendered content, host-view insertion, placement, persistence-row creation, and cleanup without entering the production APK. Separate instrumentation verifies deferred-placement cancellation during cleanup and rollback when test-added bind authority cannot be verified. Widgets derive initial spans from measured cell geometry. Long-pressing a resizable provider widget opens a full-screen, cell-snapped edit overlay with focusable 48dp handles only on provider-supported axes. Handles resize within provider minimums, bounds, and exact occupancy; a subsequent body drag enters the existing workspace move/DeleteZone flow. Back, outside touch, and lifecycle cancellation remove the overlay without mutation. Automatic neighbor reflow and snackbar undo remain deferred. API 24/API 35/API 36 hands-on third-party bind, move/delete, and resize validation remains pending.

## Docker emulator testing

One parameterized image supports API 35 and Android 16/API 36 Google APIs x86_64 AVDs. Both runtime images use local base `zeam-docker-dev:android35`; API 36 is device-runtime evidence for current target-35 APK, not target-SDK-36 migration evidence.

```sh
# API 35
env -u DOCKER_HOST docker --context docker-dev build \
    -f tools/Dockerfile.emulator \
    -t zeam-docker-emulator:android35 \
    .

# Android 16 / API 36
env -u DOCKER_HOST docker --context docker-dev build \
    -f tools/Dockerfile.emulator \
    --build-arg API_LEVEL=36 \
    --build-arg SYSTEM_IMAGE='system-images;android-36;google_apis;x86_64' \
    -t zeam-docker-emulator:android36 \
    .
```

Image build preflights and verifies exact requested system image. Runtime driver validates API/image/container/AVD identity, rejects unsafe or overflowing port values before arithmetic, mounts repository read-only at `/workspace`, and exposes only API-qualified artifact directory read-write at `/artifacts`. Daemon mount probes also write only under that artifact directory. noVNC remains loopback-only; raw VNC stays container-internal.

Full Docker instrumentation requires Docker daemon host to expose usable `/dev/kvm`; driver probes that boundary before APK build and builds both matching APKs through approved wrapper:

```sh
# API 35
KVM_DEVICE=/dev/kvm bash .claude/skills/run-zmreborn/driver.sh test

# Android 16 / API 36
API_LEVEL=36 KVM_DEVICE=/dev/kvm \
    bash .claude/skills/run-zmreborn/driver.sh test
```

API 36 defaults to container `zeam-runtime-api36`, AVD `zeam_avd_api36`, host ADB port `5556`, noVNC port `6081`, and diagnostics under `.android-emulator-artifacts/api36/e2e-diagnostics-api36/`. Driver verifies `ro.build.version.sdk=36`, runs full instrumentation including widget insertion E2E, relaunches Launcher, verifies focus/workspace hierarchy, and rejects filtered runtime failure markers. `INSTRUMENTATION_TEST_CLASS` selects a diagnostic class or method but does not replace full-suite evidence.

Fresh local native Docker/KVM API 35 and Android 16/API 36 runs each passed the full selector-unset instrumentation suite: all 133 tests, `INSTRUMENTATION_CODE: -1`, exact Launcher focus/workspace smoke, and the bounded filtered-logcat gate. This is local runtime evidence; no hosted API 36 run is claimed. The first API 36 full run exposed a test-fixture `appwidget --user current` grant issue; its focused RED failed, then the fixture was changed to resolve `am get-current-user`, validate the numeric ID, and pair grant/revoke through one captured user-bound token. Focused E2E and full runs passed after hardening. This does not claim hands-on real third-party approval, configuration, move/delete, resize, or actual SystemUI gesture coverage.

Manual deployment remains available. Without `KVM_DEVICE`, manual runtime may use slow software acceleration; automated `test` probes Docker daemon-host KVM and never accepts that fallback.

```sh
bash .claude/skills/run-zmreborn/driver.sh deploy
API_LEVEL=36 KVM_DEVICE=/dev/kvm \
    bash .claude/skills/run-zmreborn/driver.sh deploy
```

ADB and noVNC publish only on Docker daemon-host loopback. For remote `docker-dev`, create an SSH tunnel before opening browser: `ssh -N -L 6080:127.0.0.1:6080 docker-dev` for API 35 or `ssh -N -L 6081:127.0.0.1:6081 docker-dev` for API 36, then open matching local noVNC URL. Use Docker-exec ADB; do not expose raw emulator ADB to network. Entrypoint configures 2048 MB RAM, SwiftShader GPU, 1080×1920 px, and 420 dpi before starting Xvfb, local x11vnc, noVNC, and emulator.

See [`docs/CHANGELOG.md`](docs/CHANGELOG.md) for detailed reconstruction evidence and known risks.