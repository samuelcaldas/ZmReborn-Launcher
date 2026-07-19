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
- No on-device locale rendering was available during this feature pass. Broader launcher interaction flows remain manually validated from prior validation.

## Dependencies

Zeam has zero app or runtime third-party dependencies. Build validation uses Android Gradle Plugin and JUnit 4 for test-only code.

## Multilingual Support — 2026-07-17

Added persisted in-app language selection for System default, English, and Brazilian Portuguese (`pt-BR`):

- Extracted remaining user-visible Java strings and changed dynamic formats to indexed Android placeholders.
- Marked preference keys, defaults, and entry values non-translatable. Localized display-entry arrays remain separate, preserving existing stored values such as `Slider`, `Dark`, `Center`, `Start`, and `Medium`.
- Added `LocaleUtil` with API 8–16 `Resources.updateConfiguration` handling and an API 17+ configuration-context bridge. Locale wrapping occurs before application, launcher, and Preferences resources inflate.
- Persisted language changes synchronously before process restart. Empty values select System default; malformed or unsupported values normalize safely to English.
- Included complete Brazilian Portuguese strings and display arrays under `values-pt-rBR`.
- Restricted packaged locales to English and Brazilian Portuguese while disabling App Bundle language splits so in-app selection always has local resources.
- Added JUnit tests for normalization, parsing, locale fingerprints, translation parity, placeholder parity, array parity, and stable metadata.
- API 8 remains compile/minimum-SDK compatible but runtime-untested because no API 8 image was available.
- Supported locales are left-to-right. RTL layout work remains deferred; no partial RTL declaration or mirroring was added.

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

## Homescreen UI/UX Refinement — 2026-07-17

Refined the homescreen styling, folders, indicator timing, delete feedback, accessibility semantics, and empty workspace tip:
- **Refined Visual Theme Colors**: Introduced `zeam_slate` (#121a21), `zeam_glass` (#d9121a21), `zeam_fog` (#eaf0f3), `zeam_steel` (#b8c2c8), `zeam_amber` (#f2b64a), and `zeam_ember` (#d95c4f) in `colors.xml` and applied them to workspace icons, app grid backgrounds, and custom indicator selectors.
- **Typography & Spacing Refinements**: Changed workspace icon label sizes from `12dp` to `12sp` to support system accessibility text scaling. Refined folder solid background with a clean 1dp border, and increased folder action image buttons touch target width to `48dp` for better usability.
- **Signature Page Indicator**: Standardized active indicator state with a sleek Amber dash and inactive state with a Steel dot using vector shape resources. Adjusted auto-hide duration to 600ms for improved visual readability.
- **Accessibility Labels**: Programmatically injected content descriptions for drawer close buttons, folder rename/close controls, trashcan delete target, voice search button, and desktop shortcuts.
- **Drag & Delete Feedback**: Integrated dynamic `zeam_ember` highlight color overlay and updated accessibility readouts on `DeleteZone` only when dragging hover arming is reached.
- **Empty Workspace Tip**: Center-aligned a non-intrusive textual guidance tip ("Long press to add shortcuts & widgets") visible only when the homescreen workspace is completely empty of shortcuts/widgets.

## Folder Ledger and Safe Drawer Layout — 2026-07-19

Added separate app-list folder organization while preserving existing homescreen folder persistence:

- App-list folders store launcher component names in dedicated tables; workspace `favorites` rows remain unchanged.
- Both vertical scrolling and horizontal paging drawers render folder tiles, with folder contents resolved from current installed applications.
- App-list folders support create, rename, delete, and membership selection from drawer-native dialogs; deleting a folder returns its applications to the loose list.
- Folder panels use bounded, scrollable layouts and safe measured geometry so narrow, short, portrait, and landscape screens do not clip headers, controls, or icons.
- Drawer preferences remain the source of requested rows and columns; runtime sizing clamps invalid or oversized values and rebuilds after orientation or drawer-mode changes.
- Added unit and instrumentation coverage for projection, persistence, geometry, folder navigation, orientation changes, and settings regression paths; instrumentation sources compile successfully.
- Connected instrumentation and API 10/API 35 manual smoke were not run in this environment because no Android device or emulator was attached.

## GitHub Release Automation — 2026-07-18

Added protected signed APK release automation without publishing a new tag or release:

- Added `.github/workflows/release.yml` with semantic-tag validation, changelog/version checks, protected signing, APK certificate and metadata verification, checksums, provenance attestation, and GitHub Release publication gates.
- Added [`docs/RELEASING.md`](RELEASING.md) covering environment secrets, dry runs, publication, consumer verification, and rollback boundaries.
- Debug CI artifacts remain separate from signed GitHub Release assets.

