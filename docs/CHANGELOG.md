# Zeam Launcher 3.1.10 — Reconstruction Progress Log

## Status

Zeam Launcher 3.1.10 has been reconstructed from JADX source. Current development branch: `dev`.

## Reconstruction Milestones

| Commit | Milestone |
|---|---|
| `e438eeb` | Moved decompiled sources into the Android Gradle module layout. |
| `58a498f` | Set up the Gradle Android project and corrected decompiled `R` class references. |
| `bdc4366` | Fixed JADX decompilation artifacts that blocked the resource build. |
| `9e8b865` | Fixed remaining JADX decompilation bugs that blocked `javac` compilation. |

## Pending Runtime-Validation Fixes

The following runtime-validation changes are present as pending working-tree changes:

- Reconstructed `Launcher.readConfiguration`, `Launcher.writeConfiguration`, and `Launcher.loadIndicator`.
- Reconstructed `XmlUtils.beginDocument` and `XmlUtils.nextElement`.
- Reconstructed `LauncherModel.DesktopItemsLoader.loadWorkspace` with exact cursor item-type and container routing, cleanup, synchronization, and UI binding.
- Corrected `updateShortcutLabels` cursor lifetime so locale refreshes process every favorite before closing the cursor.
- Added a reflection bridge for `AppWidgetManager.bindAppWidgetIdIfAllowed` versus legacy `bindAppWidgetId`, eliminating the API 10 verifier warning.
- Removed obsolete `SYSTEM_TOOLS` permission groups so installation succeeds on API 35.
- Added a reflection bridge for dynamic receiver registration using `RECEIVER_NOT_EXPORTED` on modern Android while preserving `minSdkVersion 8` compatibility.
- Added a `MAIN`/`LAUNCHER` package-visibility query so the API 35 app drawer populates.
- Added a static-wallpaper `SecurityException` fallback to the system or window wallpaper background on API 35.

## Build Evidence — 2026-07-17

`./gradlew assembleDebug --no-daemon` completed successfully.

| Artifact | Evidence |
|---|---|
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| APK size | 558560 bytes |
| SHA-256 | `5927f78c561415b1c0e6ece0110f99f42e82464d6b4fa26fb9892510c3f402a4` |
| Build warning | Android Gradle Plugin 8.5.2 reports testing through `compileSdk 34`; this project compiles against `compileSdk 35`. The build still succeeds. |

## Emulator Validation Matrix

| Platform | Image | Validation | Result |
|---|---|---|---|
| API 10 / Android 2.3.3 | x86; oldest Google system image available | Install, explicit `Launcher` activity launch, workspace loading, app drawer, Preferences, and logcat review | Final APK installed on a clean emulator. The explicit Launcher activity displayed in 918 ms. Loader completed. `apps_grid` was `VISIBLE` in a view-server dump. Preferences opened successfully. Logcat contained no Zeam fatal exception, `UnsupportedOperationException`, verifier, or missing-method lines. |
| API 35 / Android 15 | x86_64 | Install, HOME-role selection, launcher display timing, populated app drawer, Preferences, and fatal-exception review | Final APK installed successfully. Zeam was selected as the actual HOME role and displayed in 426 ms. UI Automator verified populated drawer entries including Calendar, Camera, Chrome, Gmail, and Settings. Preferences resumed successfully. No fatal exceptions occurred. |

## Known Limitations

- Starting API 10 with an explicit component plus the `HOME` category caused a hang between the modern emulator tooling and the ancient guest. The final API 10 validation launched the explicit component without the `HOME` category.
- API 8 remains untested because no API 8 system image is available.
- The widget add/bind path has not been manually exercised.
- Custom static-wallpaper bitmap drawing falls back on API 35 because storage permission is unavailable; the system or window wallpaper background is used instead.
- No automated UI tests exist.

## Dependencies

Zeam has zero app or runtime third-party dependencies. The Android Gradle Plugin is the only build dependency.

## Regression Checklist

- [ ] Run `./gradlew assembleDebug --no-daemon` successfully.
- [ ] Confirm no `Method-not-decompiled` stubs remain.
- [ ] Confirm no `C0041R` references remain.
- [ ] Install and launch on API 10.
- [ ] Install and launch on API 35.
- [ ] Verify the app drawer on API 10 and API 35.
- [ ] Verify Preferences on API 10 and API 35.
- [ ] Review logcat for fatal exceptions, `UnsupportedOperationException`, verifier failures, and missing-method errors.

## CI/CD and JVM Test Harness Implementation — 2026-07-17

A GitHub Actions pipeline and JUnit unit test suite were added:
- GitHub Actions CI workflow config at `.github/workflows/ci.yml` triggers on push/PR for `dev` and `main` branches. It validates the code formatting, compiles the codebase, runs unit tests, builds the debug APK, and verifies the build output.
- JUnit 4 JVM-based test cases implemented at `app/src/test/java/org/zeam/FastXmlSerializerTest.java` (basic serialization, character escaping, XML attribute/entity encoding, and unsupported methods exceptions) and `app/src/test/java/org/zeam/XmlUtilsTest.java` (primitive type conversion, default handling, hex parsing, and null input boundaries).
- Local Gradle task execution validation successfully passes all lint checks, compiler verification, and JUnit tests within the `zeam-emu` container.

