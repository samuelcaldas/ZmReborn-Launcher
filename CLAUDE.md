# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch and Provenance

- Work locally on `main`.
- `original_source` is the immutable raw JADX provenance baseline. Preserve reconstructed behavior, provenance, and traceability against it.
- Keep the original APK only at `docs/reference/zeam-launcher-3-1-10-en-android.apk`.
- Do not commit generated APKs or build output.

## Toolchain and Commands

- Use JDK 17, Gradle Wrapper 8.7, Android Gradle Plugin 8.5.2, SDK 35, Build Tools 34.0.0, and `minSdk` 8.
- Always build local debug APKs with `./tools/build_apk.sh`; never invoke `assembleDebug` directly.
- The wrapper owns Docker context `docker-dev`, resolves image `zeam-docker-dev:android35` to its inspected local image ID, forbids pulls, mounts the Gradle cache, provides the Android SDK, and returns concise build output.

```sh
./tools/build_apk.sh
./gradlew :app:lint --no-daemon
git diff --check
```

Install and launch the debug build:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
```

Run unit tests or a single test:

```sh
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:testDebugUnitTest --tests 'org.zmreborn.FastXmlSerializerTest' --no-daemon
```

## Architecture and Runtime Flow

- `LauncherApplication` owns the shared `LauncherModel`; `Launcher` coordinates activity lifecycle and launcher views.
- `LauncherModel` asynchronously loads applications and favorites, then persists workspace changes.
- `LauncherProvider` owns the SQLite favorites/workspace database and its initial seeding.
- `Workspace` and `CellLayout` render the desktop; `DragLayer` and drag-drop interfaces route item movement.
- The app drawer uses grid and paging implementations; preferences and receivers trigger model/UI reloads.
- Widget, dynamic-receiver, and wallpaper compatibility bridges preserve behavior from API 8 through API 35.
- Manifest `<queries>` package visibility plus launcher, provider, and receiver registrations are runtime-critical.

## Constraints

- Keep zero third-party app/runtime dependencies. Use Android SDK and Java APIs unless explicitly approved otherwise.
- Use generated `R`; never restore JADX `C0041R` or frozen numeric resource IDs.
- Avoid direct bytecode references to APIs unavailable at `minSdk` 8; use focused compatibility bridges.
- Preserve fail-fast behavior and specific exception handling at system boundaries.

## Validation and Documentation

- Run relevant build, lint, and `git diff --check` validation after changes.
- Every new feature must add or update automated unit tests covering its expected behavior.
- Every fixed defect must add or update an automated regression test covering the failure path.
- Runtime-facing changes also require relevant instrumentation tests and API 10/API 35 manual smoke coverage.
- For runtime changes, smoke-test API 10 and API 35: install/launch, app drawer, Preferences, and filtered logcat for fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`.
- API 8 is preserved by `minSdk` but remains untested because no system image was available.
- Update `docs/CHANGELOG.md` for reconstruction, build, compatibility, or emulator-validation changes; keep README build and compatibility instructions current.
