# Zeam Launcher 3.1.10 — Reconstruction Progress Log

## Docker Emulator Runtime — 2026-07-27

- Added `tools/Dockerfile.emulator` extending `zeam-docker-dev:android35` with Android Emulator and API 35 Google APIs x86_64 system image.
- Added `tools/emulator-entrypoint.sh` to create and start a headless AVD with KVM acceleration, SwiftShader GPU, 1080×1920 display, and ADB port 5555.
- Added README instructions for building, running, connecting to, installing APK on, capturing screenshots from, and stopping Docker emulator.
- RC2 runtime screenshot sweep remains in progress; visual findings will be recorded in `docs/captures/` after emulator boot.

---

## Full Validation Review — 2026-07-26

Completed task #15: Comprehensive validation of tasks #8–#14 redesign changes.

**JVM Test Suite Results:**
- Fixed 3 pre-existing test failures before validation:
  - `LocalizationResourcesTest#portugueseStringsMatchTranslatableBaseStrings` — Added missing Portuguese translations for `accessibility_app_installed` and `accessibility_app_updated`.
  - `LocalizationResourcesTest#portugueseStringsPreserveFormatArguments` — Same resolution.
  - `UiTokenContractTest#dimensionsArePositiveAndKeepTouchAndRailContracts` — Removed unused `label_line_spacing` value (1.1) from dimens.xml that lacked a unit suffix.
- Full suite: **87 tests passed, 0 failures** (100% success rate).

**Lint Analysis:**
- Baseline: 302 errors (pre-existing); new errors detected: 28.
- Fixed 8 new errors:
  - `NewApi` (Folder.java:126): Guarded `GridView.getNumColumns()` behind API 11 check with fallback value.
  - `WrongConstant` (Dock.java:526, 534): Replaced raw integers (4, 0) with `View.INVISIBLE`, `View.VISIBLE` constants.
  - `ViewConstructor` (SignalRailView.java:8): Added standard Android view constructors for framework inflation.
  - `TextFields` (number_picker.xml:11): Added `android:inputType="number"` to EditText.
  - `RtlHardcoded` (ScreenIndicator.java:192, launcher.xml:15, widget_search.xml:44): Replaced `Gravity.RIGHT` with `Gravity.END`; added `layout_marginStart`/`layout_marginEnd` for RTL support.
- Remaining 20 errors are design-token UnusedResources, PluralsCandidate, and Overdraw warnings; these are intentional design system resources or false positives.

**Build and Assembly:**
- APK assembly: **Successful**.
  - Path: `app/build/outputs/apk/debug/app-debug.apk`
  - Size: 751,952 bytes
  - SHA-256: `8ee4765dfb7cc614e92024e1adad089a5ef16214611c06d68c1671f04361d191`
- Android test assembly (`assembleDebugAndroidTest`): **Successful**.

**Brand Verification:**
- `python3 tools/verify_brand_identity.py`: **Passed** (`ZM Reborn identity verification passed`).

**Code Quality:**
- `git diff --check`: **Passed** (no trailing whitespace or formatting issues).
- Diff review of tasks #8–#14: Package refactor `org.zeam` → `org.zmreborn` complete and verified; resource updates correct; no fabricated APKs or screenshots staged; no `.claude/` content staged.

**Runtime Safety Validation:**
- Folder.java API guard: API 11+ uses `getNumColumns()`; fallback default 4 columns on API 8–10.
- No new unguarded APIs introduced; minSdk 8 compatibility preserved.
- Zero new runtime dependencies; generated R used throughout; public resource IDs stable.
- Preference keys, defaults, and persistence integrity maintained.
- Database/workspace/dock persistence contracts verified via public resource test suite.

**Localization Parity:**
- Portuguese and English string sets now synchronized (both 109 translatable strings after fixes).
- Format argument structure verified for plural-capable strings.

**Documentation Updates:**
- `UI_STATE.md`: Reflects current implementation of tasks #8–#14 features (Signal Rail, screens indicator, drawer state, focus routes, accessibility, branding).
- `README.md`: Updated for package rename, Docker wrapper, and build instructions.
- `CHANGELOG.md`: This section documents task #15 validation results.

**Constraints Verified:**
- minSdk 8, targetSdk 35, Java 8 ✓
- Zero runtime dependencies ✓
- Generated R, public IDs ✓
- Package `org.zmreborn`, provider authority `org.zmreborn.provider` ✓
- Database/workspace/dock/folder/preferences persistence intact ✓
- No commits, pushes, tags, or releases made ✓

**Deliverables:**
- APK metadata and hash recorded above.
- JVM test count: 87 passed / 0 failed.
- Lint delta: 28 new errors detected, 8 fixed, 20 deferred (design tokens/style).
- Android test assembly successful.
- Brand identity verified.
- All pre-validation fixes committed to working tree.

---

## Focus, Accessibility, and Folder Polish — 2026-07-26

Completed task #12: DPAD/keyboard focus, accessibility, large-font handling, folder actions, Signal Rail, and launcher-owned dialog styling.

- Added deterministic focus navigation routes: workspace → dock → drawer → home button; desk navigation restores focus context on close.
- Implemented DPAD keyboard handling in Launcher, Workspace, Dock, and Folder classes with edge detection and focus routing.
- Enhanced accessibility descriptions for drawer state, folder item counts, page indicators, and app install status; descriptions use API-8-safe patterns.
- Added large-font safe layouts with two-line ellipsized labels, density-based minimum heights (label_min_height=44dp), and safe viewport math.
- Applied glass and amber theme to launcher-owned dialogs: folder rename, create app-list folder, delete confirmation, app-list folder actions.
- Added Signal Rail placeholder structure below folder title for potential future paging indicator integration.
- Introduced new accessibility contract test and extended folder/drawer E2E tests for focus, keyboard navigation, and state descriptions.
- Preserved zero runtime dependencies, minSdk 8, targetSdk 35, generated R, public resources, and database/workspace/dock persistence contracts.
- Static validation: `git diff --check` passed; no trailing whitespace or formatting issues. Device smoke testing remains pending.

## Applications Drawer Load States — 2026-07-26

- Added shared loading, ready, empty, error, close, and retry states for grid and paging application drawers.
- Added generation validation and cancellation so stale application loads cannot replace current results or folder projections.
- Added focused JVM coverage for load-generation freshness and drawer instrumentation contract assertions for the shared overlay.
- Focused Docker unit tests passed; device validation remains pending.

## ZM Reborn Rebrand — 2026-07-26

Static validation completed for current working-tree rebrand changes:

- Brand verifier and deterministic icon byte verification passed.
- Full `docker-dev` rerun passed `assembleDebug`, `testDebugUnitTest`, `lint`, and `assembleDebugAndroidTest`; 34 JVM tests passed with 0 failures, errors, or skips.
- Regenerated lint baseline decreased from 430 to 324 findings, with no issue-ID count increases.
- APK badging reports package `org.zmreborn`, label `ZM Reborn`, `versionCode 113`, and `versionName 3.1.11-alpha`; packaged process, provider, and test-target identities validated.
- Reviewer-found `DialogSeekBarPreference` package namespace regression was fixed to `res-auto` and covered by a regression test.
- Package and signing identity changes make this build clean-install-only; it cannot upgrade historical Zeam APK installations.
- Updated launcher icon and visual palette for the ZM Reborn identity.
- Updated current documentation and CI/release automation for ZM Reborn, including package-aware instrumentation, smoke checks, artifact names, and release metadata.
- API 10/API 35 runtime validation remains pending because no attached device or installed emulator/system images are available.

## Launcher System-Bar Bounds Fix — 2026-07-26

- Traced covered launcher controls to Android 15 forced edge-to-edge behavior after targeting API 35. Launcher activity still referenced the platform wallpaper theme directly, so project theme could not provide an API-specific compatibility policy.
- Routed Launcher through a dedicated project `LauncherTheme` inheriting `Theme.Wallpaper.NoTitleBar`, preserving the exact API 8–34 platform wallpaper-window behavior and fullscreen preference semantics.
- Added a `values-v35` theme override with `windowOptOutEdgeToEdgeEnforcement=true`. API 8–34 retain legacy window-managed content bounds, including devices with physical navigation buttons; API 35 once again reserves status and navigation bar space.
- Added JVM resource-contract coverage plus instrumentation assertions against legacy visible-window bounds on API 8–22 and root system-bar insets on API 23+. Instrumentation also drives fullscreen off → on → off, checks window flags and bounds after each transition, then restores the exact prior preference state.
- Verified resource packaging: compiled `style/LauncherTheme` contains the API 35 opt-out entry and Launcher manifest resolves that style. Unit tests, debug app build, Android test build, brand verification, and `git diff --check` pass.
- On-device API 35 confirmation remains pending because no attached device, emulator binary, or installed system image is available.

## Docker APK Build Wrapper — 2026-07-26

- Added `tools/build_apk.sh` as the required local debug APK build entrypoint.
- Wrapper validates Docker, `docker-dev`, `zeam-docker-dev:android35`, and project prerequisites before starting Gradle. It unsets `DOCKER_HOST`, resolves the tag to its inspected content-addressed image ID, forbids pulls, mounts the shared Gradle cache, and sets `TZ=America/Sao_Paulo`.
- Successful builds suppress Docker/Gradle noise and print only APK path, byte size, and SHA-256. Failed Docker preflights and Gradle builds return captured diagnostics.
- Documented wrapper in `README.md` and required its use in project `CLAUDE.md`; direct local `assembleDebug` use is retired.
- Verified shell syntax, help output, rejected-argument handling, Docker preflight diagnostics, and an actual wrapper build. Successful output contained only APK path, byte size, and SHA-256.

## Status

Zeam Launcher 3.1.10 has been reconstructed from JADX source. Current development branch: `main`.

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

## App-List Drawer Geometry and Accessibility — 2026-07-19

Refined drawer rendering without changing drawer mode meaning, app launch behavior, or app-list folder semantics:

- Swapped paging geometry to measured content bounds instead of window-frame-derived rows and heights, so drawer pages stay bounded on narrow and landscape layouts.
- Added Android-free page-partition helpers and regression tests for zero items, exact capacity, overflow, and invalid requested dimensions.
- Kept drawer folder tiles intact while improving application tile labels to 13sp, two-line wrapping, and end-ellipsis treatment for long localized names.
- Added full accessibility content descriptions for drawer items and made uninstall state instance-scoped so paging and grid adapters no longer share a global uninstall flag.
- Hardened paging long-press, empty-list handling, and current-page clamping so remembered positions do not wander past available pages.
- Added instrumentation coverage for drawer inflation and open/close round-trips; connected device/emulator validation was not run in this environment because no SDK installation was available.

## UI Capture Tracking — 2026-07-19

Added revision-linked capture tracking under `docs/captures/`:

- Documented naming, device metadata, surface checklist, and screenshot commands.
- Recorded current revision `68d3786` and required homescreen, drawer, folder, and settings captures.
- No runtime PNGs were fabricated: capture remains pending because no Android device/emulator is attached and `/opt/android-sdk` is unavailable in the current environment.

## Drawer Safe-Area Runtime Validation — 2026-07-19

- Built and installed current `main` source in Docker on the API 35 emulator (`320 × 640`, portrait).
- Added visible-window inset handling to vertical and paged drawer renderers, preserving existing layout padding while reserving status/navigation bar space.
- Added close-control margin handling so the drawer close button remains above the navigation bar in portrait and landscape.
- Added unit coverage proving status/navigation inset padding reduces available drawer height before row sizing.
- Hardened Preferences list-summary binding for unset or mismatched stored values; API 35 root Preferences now opens without the prior `SettingsSummaryBinder` null-value crash.
- Initial hierarchy showed app tiles at y=0 and close control through y=640; fixed hierarchy reports app tiles beginning at y=24 and close control ending at y=616.
- Verified captures: `docs/captures/2026-07-19-api35-portrait-app-drawer-safe-area.png` and `docs/captures/2026-07-19-api35-portrait-preferences-root.png`.
- API 35 instrumentation suite passed on connected Docker emulator: 9 tests, including drawer inflation/open-close, launcher flows, and Preferences flows.
- Landscape, vertical-grid, folder, large-font, and API 10 captures remain pending; see [`docs/captures/2026-07-19-current-state.md`](captures/2026-07-19-current-state.md).

## GitHub Release Automation — 2026-07-18

Added protected signed APK release automation without publishing a new tag or release:

- Added `.github/workflows/release.yml` with semantic-tag validation, changelog/version checks, protected signing, APK certificate and metadata verification, checksums, provenance attestation, and GitHub Release publication gates.
- Added [`docs/RELEASING.md`](RELEASING.md) covering environment secrets, dry runs, publication, consumer verification, and rollback boundaries.
- Debug CI artifacts remain separate from signed GitHub Release assets.

## Signed Alpha Candidate Validation — 2026-07-19

Validated current source as a signed alpha candidate in the `zeam-emu` container:

- `:app:lint`, `:app:testDebugUnitTest`, and `:app:assembleRelease` passed after correcting two release-blocking lint defects.
- APK metadata is `org.zeam`, `versionCode 113`, and `versionName 3.1.11-alpha`.
- APK signature verification passed v1/JAR and v2 APK Signature Scheme checks.
- APK SHA-256: `9472f2f29611a489ac119745b77fb9748250f8935825c9b4314d371e446857bb`.
- Alpha uses a new signing identity and must be installed as a clean alpha; it is not an upgrade-compatible build for the historical APK.
- Initial GitHub dry run `29707324433` stopped at the old changelog release-heading gate; no tag or GitHub Release was created.

## [3.1.11-alpha]

First alpha candidate from current launcher source:

- Signed release APK uses `versionCode 113` and `versionName 3.1.11-alpha`.
- Release validation covers lint, JVM tests, APK metadata, v1/v2 signatures, certificate identity, checksum, and provenance gates.
- Alpha is signed with a new key and is not upgrade-compatible with the historical APK.
