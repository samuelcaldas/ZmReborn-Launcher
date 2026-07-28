# Zeam Launcher 3.1.10 — Reconstruction Progress Log

## Gesture Modernization — Phase 4: Material 3 Expressive Visual Refresh — 2026-07-28

### Dynamic wallpaper-adaptive color

- Added `org.zmreborn.theme.WallpaperColorExtractor` — extracts the system wallpaper's primary seed color via `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` (API 27+) and derives a 6-role M3-inspired tonal palette using HSL lightness steps. Static amber-seed fallback on API 24–26 and on null/SecurityException.
- Colors cached in `SharedPreferences` (`org.zmreborn.theme.wallpaper_colors`) to avoid blocking cold start.
- `Launcher.onCreate()` calls `WallpaperColorExtractor.refresh()` on every cold start.
- `WallpaperIntentReceiver.onReceive()` calls `WallpaperColorExtractor.refresh()` on wallpaper change, so accent colors update within one launch cycle.

### M3 color role tokens

- Added 13 static M3 role tokens to `res/values/colors.xml`: `m3_primary` (#fff2b64a), `m3_on_primary`, `m3_primary_container`, `m3_on_primary_container`, `m3_surface` (#ff121a21), `m3_on_surface`, `m3_surface_variant`, `m3_on_surface_variant`, `m3_outline`, `m3_outline_variant`, `m3_error`, `m3_on_error`, `m3_ripple_primary` (#33f2b64a).
- Semantic aliases (`window_background`, `gesture_color`, `uncertain_gesture_color`, `bubble_dark_background`, `appwidget_error_color`, `snag_callout_color`, `preferences_default_general_selector_colour_*`) remapped to M3 roles.
- Original 6 primitive tokens (`zm_reborn_*`) preserved as fallback layer.

### Shape, elevation, and ripple

- Added 5-tier shape scale to `dimens.xml`: `shape_corner_extra_small`=4dp, `small`=6dp, `medium`=8dp, `large`=12dp, `extra_large`=16dp.
- Added elevation tier scale: `elevation_surface`=0dp, `elevation_dock`=4dp, `elevation_folder`=6dp, `elevation_drawer_header`=2dp.
- `Dock.onFinishInflate()` now calls `setElevation(elevation_dock)`.
- `Folder.onFinishInflate()` now calls `setElevation(elevation_folder)`.
- `ApplicationsPagingView.onFinishInflate()` now calls `setElevation(elevation_drawer_header)`.
- `res/drawable/settings_preference_selector.xml` replaced `<selector>` with `<ripple android:color="@color/m3_ripple_primary">`, preserving `state_selected`/`state_focused` items with M3 surface-variant fill and primary stroke.

### Typography

- Added `TextAppearance.ZmReborn.Headline` style (sans-serif-medium, 20sp, letterSpacing 0.00) to `styles.xml`.
- Added `text_size_headline=20sp` to `dimens.xml`.
- Added `android:letterSpacing` to all existing `TextAppearance.ZmReborn.*` roles per M3 spec.

### M3 Expressive motion

- `res/anim/apps_scale_in.xml`: enter — decelerate, 450ms scale (0.85→1), 400ms alpha, `pivotY=70%`.
- `res/anim/apps_scale_out.xml`: exit — accelerate, 200ms scale (1→0.9), 200ms alpha, `pivotY=70%`.

### Tests and validation

- New `WallpaperColorExtractorTest` (9 pure JVM tests): tonal ramp role count, hue preservation, lightness contracts (dark on-primary, bright on-surface, midtone outline), saturation clamping, gray-seed zero-saturation, fallback alpha opacity.
- Expanded `UiTokenContractTest`: M3 token presence assertion (`m3ColorRoleTokensPresent`), shape tier ordering invariants, `text_size_headline` value assertion, Headline style in typography scale.
- Updated `SettingsResourceContractTest`: selector accent reference updated from `zm_reborn_amber` to `m3_primary` (semantically equivalent, both #fff2b64a).
- Updated `UiTokenContractTest.assertPositiveDimensions` to allow `0dp` (covers `elevation_surface`).
- Lint baseline updated to include intentionally programmatic-only resources (M3 role tokens, shape tier, Headline style).
- Build: `assembleDebug` — 0 errors, 3 deprecation warnings. APK 757,782 bytes, SHA-256 `1545749ffa260620e9a512050b40d1af593edd5366222bd6d5df4668b3f89255`.
- Tests: all pass (prior 100 + 9 new `WallpaperColorExtractorTest` + expanded contract tests).
- Lint: 0 errors (317 filtered by baseline).
- `git diff --check`: clean.



- Changed fresh-install default for `preferences_default_action_swipe_up` from `"1"` (None)
  to `"2"` (Open applications), matching Pixel launcher convention.
- Changed fresh-install default for `preferences_default_action_swipe_down` from `"2"`
  (Open applications) to `"1"` (None).
- Existing users' `SharedPreferences` values are unaffected; defaults only apply when no saved
  value exists. The configurable action-binding system is preserved intact.

### Validation — 2026-07-28

- `./tools/build_apk.sh` passes. APK 753,480 bytes,
  SHA-256: `017a7c0e3b3f1c9a75e1f10540a73c2e9d8c47c8e8ed4de73d9368b9af2a7a50`.
- `:app:testDebugUnitTest` passes (100 tests, 0 failures). No test hardcoded the old default
  numeric values; all swipe-preference tests reference resource keys only.
- `:app:lint` passes (0 errors, 0 warnings outside baseline).
- `git diff --check` passes.



- Registers `BackGestureCompat` from `Launcher#onCreate` and unregisters its callback in
  `onDestroy`. API 33+ routes through `OnBackInvokedDispatcher`; API 24–32 keep existing
  key-event routing without loading newer framework classes.
- Consolidated callback and legacy-key handling through one Launcher-owned close sequence:
  applications drawer → open folder → existing preview dismissal → home-screen no-op. This
  preserves prior launcher behavior while allowing predictive-back callback delivery.
- API 34 callback progress now previews closure on the active drawer or folder using reversible
  alpha and scale transforms. Cancel and commit reset transforms before any view is hidden.
- `Workspace` now applies API 29+ exclusion rectangles only for an edge whose configured swipe
  action is **Open applications**. Unbound or differently bound actions receive an empty list,
  leaving horizontal system-back edges available to Android. The implementation never excludes
  left or right back edges.
- Added JVM null-boundary coverage for `BackGestureCompat`, plus API instrumentation coverage
  for drawer/folder close, home-screen no-op, preview cancellation, and workspace exclusions.

### Validation status — 2026-07-28

- `git diff --check` passes.
- `./tools/build_apk.sh` passes. APK: `app/build/outputs/apk/debug/app-debug.apk`,
  753,480 bytes, SHA-256: `e7549880ecd246e9d38e193ca3615dd5d95e5e9a6c654f9c2bf9016ded0b3497`.
- `:app:testDebugUnitTest` passes. 100 tests, 0 failures, 0 errors. Includes
  `BackGestureCompatTest` (3 JVM null-boundary tests), `GestureExclusionCompatTest`, and
  `WindowInsetsCompatTest`.
- `:app:lint` passes. 0 errors, 0 warnings outside baseline.
- Build image: `cimg/android:2025.12` (CircleCI verified publisher, tagged locally as
  `zeam-docker-dev:android35`). Contains `platforms;android-35`, `build-tools;35.0.1`,
  JDK 21. Build ran with `--no-daemon` under `docker-dev` context.
- API 35 gestural-nav instrumentation (`PredictiveBackE2ETest`) remains pending: requires
  a running emulator. No pass or fail claim is made for on-device callback delivery.

## Gesture Modernization — Phase 1: Compatibility Bridges & Inset Plumbing — 2026-07-28

- Added `org.zmreborn.compat` package: `WindowInsetsCompat`, `GestureExclusionCompat`,
  `BackGestureCompat`. Each isolates its API 29/30/33/34 platform class references inside
  dedicated nested static classes (`Api29`, `Api30`, `Api33`, `Api34`) so those classes are
  only loaded and verified when actually invoked behind a matching `SDK_INT` guard, keeping
  the outer, universally-loaded class free of any bytecode reference unavailable at `minSdk` 24.
- Wired real `WindowInsets` consumption through `Launcher#applySystemInsets`, propagating
  system-bar and gesture insets to `Workspace`, `DragLayer`, `Dock`, and the Applications
  drawer (`ApplicationsPagingView`/`ApplicationsGridView` via the `ApplicationsView` interface).
- Added `android:enableOnBackInvokedCallback="true"` to the manifest ahead of Phase 2's
  predictive-back gesture; `BackGestureCompat` is implemented but not yet wired into `Launcher`.
- Marked `values-v35/styles.xml`'s edge-to-edge opt-out as transitional; it stays in place
  until Phase 2's predictive-back and inset handling are verified on an API 35 smoke pass.
- Fixed 21 new lint errors (`NewApi`, `InlinedApi`, `UnusedAttribute`) surfaced by the new
  compat classes: added `@TargetApi` to each version-scoped nested class and
  `tools:targetApi="33"` to the manifest's `<application>` element, and moved
  `android.graphics.Insets` field access out of `WindowInsetsCompat`'s always-loaded outer
  class into its `Api29`/`Api30` nested classes.

### Testing

- Added `WindowInsetsCompatTest`, `GestureExclusionCompatTest` (unit, covering the
  API-gated null/no-op fallback paths reachable under this project's unmocked-stub-jar JVM
  test environment).
- Extended `LauncherE2ETest` with a system-inset-wiring regression test asserting `Dock`
  receives insets from the root `DecorView` listener and content stays inside system-bar bounds.
  API 35 testing caught that the legacy edge-to-edge opt-out consumed insets before `DragLayer`;
  moving the listener to `DecorView` while chaining its normal `onApplyWindowInsets` fixed it.
- API 35 instrumentation passes all 10 `LauncherE2ETest` cases; the targeted inset test also
  passes with gestural navigation active. Filtered logcat shows no fatal, verifier,
  missing-method, or `UnsupportedOperationException` entries.
- `./tools/build_apk.sh`, `:app:testDebugUnitTest`, `:app:lint`, and `git diff --check` pass.

## [3.1.11-alpha-rc4]

Material 3 Expressive visual refresh: wallpaper-adaptive color, M3 role tokens, elevation on Dock/Folder/ApplicationsPagingView, RippleDrawable feedback, shape tier scale, M3 Expressive motion (450ms enter / 200ms exit, pivotY=70%), Headline typography tier with M3 letter-spacing.

## [3.1.11-alpha-rc3]

### Bug Fixes

- Fixed horizontal Applications paging by sizing every page to the current `ViewPager`
  viewport, recalculating widths after size changes, and keeping indicator geometry aligned
  with the actual scroll width.
- Restored touch navigation in Preferences by leaving row click/focus ownership with the
  framework `ListView`; selected-state styling preserves DPAD feedback without intercepting taps.
- Standardized launcher typography on semantic category, label, body, title, symbol, and
  display roles using the platform sans-serif family and `sp`-backed size tokens.
- Fixed `NullPointerException` crash on API 35 in Applications grid and Action bindings
  preference screens caused by numeric `<array>` entry values being resolved as integer
  resource types by `TypedArray.getTextArray()`, returning null `CharSequence` elements.
  Converted affected arrays to `<string-array>` to guarantee string interpretation.
- Fixed Preferences inaccessible from options menu on tall displays: menu panel row 1
  now shows Add | Preferences | Wallpaper instead of Add | Wallpaper | Search.
- Fixed misleading icon on Add action in long-press context dialog: was showing orange
  shortcut arrow (`ic_launcher_shortcut`); now shows standard plus icon (`ic_menu_add`).
- Fixed `getResources().getColor()` deprecation warnings in `Launcher.java`,
  `Preferences.java`, `ColourPickerPreference.java`, and `SettingsPreference.java`;
  replaced with `Context.getColor()` throughout.
- Fixed "Scroll reset to.." label in Dock preferences: removed spurious trailing ellipsis.

### CI

- Fixed e2e emulator job using `api-level: 23` when `minSdk` is 24; install now succeeds.
- Regenerated lint baseline after fixing 20 code-level issues (RTL hardcoded attributes,
  `ObsoleteSdkInt`, `LabelFor`, redundant margin attributes across 10 layout files).

### Testing

- Added regression coverage for horizontal page scroll range, touch-opening nested
  Preference screens, Settings row interaction ownership, and typography token/style contracts.
- API 35 emulator smoke confirmed horizontal drawer transition from page 1 to page 2,
  touch-opened General/Workspace/Applications grid/Action bindings/Dock screens, and no
  fatal, verifier, missing-method, or `UnsupportedOperationException` log entries.
- Targeted API 35 instrumentation passed all three new regression tests. API 24 runtime
  smoke remains pending because no local API 24 emulator image is available.
- Added Docker emulator runtime (`tools/Dockerfile.emulator`, `tools/emulator-entrypoint.sh`)
  with API 35 Google APIs x86_64 AVD, KVM acceleration, SwiftShader GPU.
- Added emulator APK test skill and driver (`driver.sh`) for headless screenshot sweeps
  and crash verification across all launcher surfaces.

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
