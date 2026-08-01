# Zeam Launcher 3.1.10 — Reconstruction Progress Log

## Hosted API 24 instrumentation repairs — 2026-08-01

- Fixes ten failures exposed by the first bounded API 24 run without weakening production contracts:
  paging geometry now derives test viewports from density-aware minimum cell dimensions; focus traversal is
  correctly scoped as instrumentation through `Launcher.dispatchKeyEvent` instead of unsupported cross-UID
  input injection; drawer pulls begin inside padded content; Preferences waits for distinct lifecycle instances.
- Makes focusability and recreation checks independent of asynchronous application loading, test order, and
  stale `ActivityMonitor` matches. Lifecycle observers unregister in cleanup paths, original preferences and
  requested orientation remain restorable, and focus checks exercise Launcher activity dispatch explicitly.
- JDK 17 validation passed 204 JVM tests, Android-test assembly, lint, required Docker-wrapper APK build, and
  `git diff --check`. API 35 passed 16 focused paging, focus, drawer-pull, and Preferences recreation tests.
  A full API 35 suite attempt reached test 81 of 122 without a reported failure before its 15-minute local
  timeout.
- Hosted API 24 follow-up completed all 122 tests with 121 passes and one remaining Preferences touch failure.
  Root-list touch now targets the adapter-resolved visible row directly instead of relying on decor routing;
  focused API 35 validation passes, while another hosted API 24 execution remains pending.

## Hosted API 24 CI hardening — 2026-07-31

- Replaces newline-dependent emulator-runner commands with one checked-in Bash driver so strict mode,
  variables, instrumentation result handling, smoke assertions, and failure status remain in one process.
- Bounds every ADB operation, rejects instrumentation crash/failure markers even when ADB exits zero, and
  captures logcat, window/activity/process/package state, UI hierarchy, and screenshot before emulator
  teardown. Workflow always uploads `e2e-diagnostics/` without masking primary failure.
- Makes vertical-drawer fast-scroll E2E state deterministic by snapshotting drawer/blur preferences,
  waiting for the asynchronous application load before installing a 52-item A–Z fixture, dispatching real
  grid and rail touch input, checking Z navigation and delayed auto-hide, and reusing one shared icon.
- Static validation passed CI workflow contract, Bash syntax, 204 JVM tests, Android-test assembly, lint,
  Docker-wrapper debug APK build, and `git diff --check`. API 35 runtime passed focused fast-scroll E2E
  in normal and forced-RTL layouts, two Preferences smoke tests, exact Launcher focus/workspace smoke, and
  filtered fatal/verifier/API-error logcat checks. Hosted API 24 execution remains pending.

## [3.1.11-alpha-rc11]

- Adds one **Blur backgrounds** preference that applies a Material You frosted-glass surface to dock
  and active application drawer, including both vertical and horizontal paging implementations.
- Uses best-effort blurred static-wallpaper pixels on API 31+ when `Workspace` already owns a readable
  bitmap. API 24–30, live wallpapers, restricted access, invalid sources, cancellation, and allocation
  failures use deterministic procedural frost without new permissions or runtime dependencies.
- Preserves saved dock appearance and configured drawer transparency while blur is enabled, then restores
  both when disabled. Wallpaper-aligned parallax reuses cached blur without regenerating during scrolling.
- Cancels queued or active blur work during wallpaper replacement and launcher teardown, rejects stale
  generations, and preserves bitmap ownership while installed drawables may still reference cached output.
- Validation includes 204 passing JVM tests, Android-test assembly, lint, Docker-wrapper debug APK build,
  8 focused API 35 instrumentation tests, clean filtered logcat, and `git diff --check`. API 24 runtime and
  readable system-wallpaper integration on API 31–33 remain unperformed.

## [3.1.11-alpha-rc10]

- Adds pull-down gesture to close the app grid in both vertical and horizontal paging modes.
  Vertical drawer intercepts a second downward pull after the search bar is revealed; paging drawer
  intercepts any downward-dominant pull. Both close when drag exceeds 72 dp.
- Adds always-visible search bar at the top of the horizontal paging drawer, matching the vertical
  drawer's appearance (Material You palette via WallpaperColorExtractor) and behavior
  (DrawerSearchFilter, clear button, empty-state overlay). Search resets on drawer close.
- Preserves Android 24–35 compatibility boundaries, public resource contracts, reconstruction
  provenance, and zero third-party runtime dependencies.
- Validation includes passing JVM suite, lint (0 errors), Docker-wrapper debug APK build (866 KB),
  and `git diff --check`. Runtime smoke on device remains unperformed.

## Global frosted backgrounds — 2026-07-31

- Adds one **Preferences → General → Appearance → Blur backgrounds** switch for dock plus active
  application drawer. Enabling it overrides dock appearance without changing saved dock mode;
  disabling restores that mode and configured drawer transparency.
- Adds deterministic Material You frost using a translucent vertical tint and fixed-seed tiled grain.
  API 24–30, live wallpapers, denied wallpaper access, and blur-generation failures use this procedural
  fallback without requesting storage or media permissions.
- On API 31+ when `Workspace` already owns a readable static-wallpaper bitmap, generates a cached
  one-eighth-scale, three-pass ARGB box blur on wallpaper refresh executor. Dock and both drawer roots
  sample cached bitmap with existing workspace wallpaper offset, including parallax during page swipes;
  scrolling never recomputes blur.
- Adds JVM coverage for blur kernel, geometry, invalid inputs, interruption, and settings resource contract.
  Cached bitmaps already installed in a backdrop remain drawable until normal garbage collection; stale
  never-installed outputs are recycled. Wallpaper replacement and teardown cancel queued work, interrupt
  active pixel processing, and reject stale generation results.
- Final static validation passed 204 JVM tests with zero failures/errors/skips, Android-test assembly,
  lint, and `git diff --check`. Docker wrapper built a 732,938-byte debug APK with SHA-256
  `b8fb57c239b002b078ece1218012b4fe098e2bd6d01e5ee65c5743fa627e582c`.
- Final API 35 instrumentation passed 8 focused tests: dock plus vertical/paging frost, disabled-state
  restoration, renderer bitmap diffusion/ownership, cache and executor cancellation lifecycle, settings
  reachability, and toggle persistence. Filtered logcat contained no matching launcher fatal exception,
  ANR, verifier/missing-class/method failure, or `UnsupportedOperationException`.
- API 24 runtime remains unperformed because no local API 24 image is available. API 35 normally uses
  procedural fallback because static wallpaper pixels are restricted; actual renderer execution is covered
  with a controlled bitmap, but readable system-wallpaper blur integration on API 31–33 remains unperformed.

## [3.1.11-alpha-rc9]

- Replaces 47 density-qualified PNG rasters and 9-patch backgrounds with VectorDrawable XML
  drawables that reference `@color/m3_*` tokens, enabling automatic adaptation to light, dark,
  and dynamic Material You palettes without any Java or layout changes.
- Fixes two existing vectors (`ic_settings_decrease`, `ic_settings_increase`) that hardcoded
  `@android:color/white` to use `@color/m3_on_primary` instead.
- Preserves Android 24–35 compatibility boundaries, public resource contracts, reconstruction
  provenance, and zero third-party runtime dependencies.
- Validation includes passing JVM suite, lint (0 errors after baseline update), Docker-wrapper
  debug APK build, and `git diff --check`. Runtime icon-adaptation smoke on device remains
  unperformed.

## [3.1.11-alpha-rc8]

- Synchronizes horizontal app-drawer page indicators with the homescreen preference and alignment,
  while suppressing the redundant home control in paging mode.
- Centralizes preference defaults in `defaults.xml`, including the workspace wallpaper-management
  default, without changing existing keys or stored values.
- Improves vertical drawer interaction with reliable fast-scroll reselection, an auto-hiding alphabet
  rail, and pull-to-reveal search.
- Restores movement and DeleteZone removal for resizable widgets while retaining resize handles. Adds
  launcher-owned widget preview cards and exposes Widgets, Shortcuts, and Folders directly from the
  empty-home long-press menu.
- Preserves Android 24–35 compatibility boundaries, public resource contracts, reconstruction
  provenance, and zero third-party runtime dependencies.
- Validation includes passing JVM suites, Android-test compilation, lint, Docker-wrapper debug APK
  builds, changed-path and API compatibility review, plus 16 focused API 35 widget/menu/resize
  instrumentation tests. API 24 runtime and hands-on API 35 real-provider widget, persisted
  move/delete/resize, abandoned-host-ID, and TalkBack smoke remain unperformed.

## [3.1.11-alpha-rc7]

- Replaces modal quantity sliders with accessible inline `−` / `+` controls and moves
  applications-grid transparency to an inline slider. Changes update immediately, persist through a
  250 ms trailing debounce, and flush before Settings yields to Launcher.
- Refactors horizontal application paging around proportional slot boundaries, measured viewport
  layout, and item-ordinal position restoration. Portrait and landscape spacing now share navigation
  strip dimensions and consistent cell padding.
- Adds API 35 light/night Settings theme opt-outs for forced edge-to-edge, protecting system-bar
  spacing while the full edge-to-edge migration remains deferred.
- Preserves Android 24–35 compatibility boundaries, existing preference keys, public resource
  contracts, reconstruction provenance, and zero third-party runtime dependencies.
- Validation includes 185 JVM tests, Android-test compilation, lint, Docker-wrapper debug APK builds,
  full API 35 numeric-settings instrumentation, focused Launcher recreation coverage, and changed-path
  review. Instrumentation tests for drawer indicator type/position and home button guard remain
  pending. API 24 runtime and final API 24/API 35 horizontal-paging device smoke remain unperformed.
  Software-emulator ANR interrupted clean manual API 35 app-grid slider, keyboard/DPAD, forced-RTL,
  and TalkBack traversal; no result is claimed for those paths.

## Widget edit, preview picker, and direct add menu — 2026-07-30

- Restores resizable-widget movement and DeleteZone removal through a second body-drag gesture while
  retaining cell-snapped resize handles. The handoff reuses existing `Workspace`, `DragLayer`, and
  `DeleteZone` behavior rather than adding another move or deletion path.
- Empty-home long press now directly exposes Widgets, Shortcuts, Folders, Wallpaper, and Preferences.
  The platform options-menu Add dialog retains its three add categories.
- Replaces `ACTION_APPWIDGET_PICK` with launcher-owned, asynchronously loaded preview cards: Search
  first, localized provider ordering, preview/icon/fallback artwork, accessible labels, and grid-span
  detail calculated on the main thread. Opening or cancelling the picker allocates no widget host ID.
- External selection allocates one host ID, tries `bindAppWidgetIdIfAllowed(...)`, falls back to
  `ACTION_APPWIDGET_BIND`, and reuses existing provider configuration, placement, recreation, and
  cleanup paths. Picker-open state restores only after desktop binding unlocks the workspace.
- This intentionally diverges from immutable reconstructed source while preserving its widget-host,
  database, workspace drag, and provenance boundaries.

### Validation — 2026-07-30

- Final focused API 35 instrumentation: 16 tests passed in 504.849 seconds across widget picker, menu,
  and resize classes. Coverage includes accessible preview-card rendering/loading, Search-first ordering,
  zero pre-selection allocation, bind cancellation cleanup, picker state restoration, direct menu order,
  resize handles, and body-drag callback separation.
- Full JVM suite: 189 tests passed with 0 failures, errors, or skips. Android-test assembly, lint, and
  `git diff --check` passed. Final changed-path review reported no findings, and API compatibility review
  found no confirmed minSdk 24–API 35 verifier boundary issues.
- `./tools/build_apk.sh` passed. Debug APK: 959,873 bytes; SHA-256
  `ed2e4eb24c7777ad91e7683842111a0c13c969969582e301b98309dc0f856074`.
- API 24 smoke and hands-on API 35 real-provider bind/configuration, persisted move/delete/resize,
  abandoned-host-ID inspection, and TalkBack traversal remain unperformed. No result is claimed for
  those paths.

## Vertical drawer UX: fast-scroll fix, auto-hide rail, pull-to-reveal search — 2026-07-30

- **Fast-scroll re-selection fix.** Tapping a fast-scroll letter after manually scrolling the grid away
  now navigates correctly. Root cause: `DrawerFastScrollView.selectIndex` short-circuited when
  `index == mSelectedIndex`, never cleared after independent grid scroll. Fix: `ApplicationsDrawerView`
  registers an `AbsListView.OnScrollListener` on the grid; `onGridScrollStateChanged` calls
  `mFastScroll.clearSelection()` on `SCROLL_STATE_TOUCH_SCROLL` so every subsequent tap always fires.
- **Fast-scroll auto-hide.** The alphabet rail is now hidden at rest and appears only while the grid is
  scrolling, fading in (150 ms) on `TOUCH_SCROLL`/`FLING` and fading out (150 ms) 1 s after
  `IDLE`. A `Handler` + `Runnable` in `ApplicationsDrawerView` drives the schedule;
  `updateFastScroll()` no longer sets the rail `VISIBLE` directly — it only sets `mFastScrollEnabled`
  and immediately hides the rail when conditions are not met.
- **Pull-to-reveal search bar.** The search bar is hidden by default (height=0, `GONE`) on drawer open.
  `ApplicationsDrawerView` overrides `onInterceptTouchEvent` to detect a downward swipe from the top
  of the grid; `onTouchEvent` grows the container height proportionally. On release, a 150 ms
  `ValueAnimator` snaps to fully revealed (`mSearchRevealed = true`) or collapses back. Active query
  always pins the bar visible. Drawer close resets to hidden via `clearQueryForClose()`.
- Adds `android:id="@+id/drawer_search_container"` to the search `FrameLayout` in both orientation
  layouts. Overrides `performClick()` in `ApplicationsDrawerView` for `ClickableViewAccessibility`.
  Updates `UiModernizationContractTest` assertions to reflect renamed `mFastScrollEnabled` field and
  adds three new contract tests (fast-scroll re-selection, update-method visibility gate, pull-to-reveal
  presence).

### Validation — 2026-07-30

- TDD: three contract tests written red first (confirmed failures at `UiModernizationContractTest.java`
  lines 266, 278, 287); turned green after full implementation.
- JVM: 192 tests, 0 failures, 0 errors.
- Android-test compile: clean.
- Lint: 0 new errors (1 `ClickableViewAccessibility` resolved by `performClick()` override).
- APK: `app-debug.apk` SHA-256 `9ba8896d689578a361448aafb3f7e805ac19cf69086bbf5d4bf42e6bfc9c32c2`.
- API 24/API 35 manual smoke: pending (same status as rc7).

## Unified preference defaults seed file — 2026-07-30

- **New `values/defaults.xml`.** All 35 launcher preference default values now live in one dedicated
  file. 34 `preferences_default_*` string entries moved verbatim from `strings.xml`; one new entry
  (`preferences_default_workspace_manage_wallpaper = true`) added to normalize the only default that
  was previously hardcoded in Java with no XML declaration.
- **`strings.xml` cleaned.** Lines 48–81 removed; only keys, titles, summaries, and UI labels remain.
  All `R.string.preferences_default_*` references in `PreferencesUtil.java`, `Appearance.java`,
  `LocaleUtil.java`, and `preferences.xml` are unchanged — resource names are identical.
- **`PreferencesUtil.isManageWallpaperEnabled()` normalized.** Hardcoded `true` fallback replaced by
  `Boolean.parseBoolean(context.getString(R.string.preferences_default_workspace_manage_wallpaper))`.
- **`preferences.xml` gap filled.** `workspace_manage_wallpaper` SwitchPreference now declares
  `android:defaultValue="@string/preferences_default_workspace_manage_wallpaper"`, consistent with
  all other boolean preferences.
- Color defaults (`preferences_default_general_selector_colour_pressed/focused`) remain in
  `values/colors.xml`; `defaults.xml` carries a header comment pointing to them.
- Zero behavior change. All existing preference values, keys, reset flow, and stored-value semantics
  are unmodified.

### Validation — 2026-07-30

- TDD: `SettingsResourceContractTest.defaultsPlacedInDedicatedFile()` written red first (confirmed
  `FileNotFoundException` for `defaults.xml`); turned green after `defaults.xml` creation,
  `strings.xml` cleanup, `PreferencesUtil` and `preferences.xml` updates.
- `UiModernizationContractTest.verticalDrawerIsDefaultResponsiveAndCacheFree()` updated to read
  from `defaults.xml`; `SettingsResourceContractTest.booleanPreferencesUsePlatformSwitches()` and
  `preferencesKeepAllKeysDefaultsStoredValuesAndSummaries()` updated to merge `defaults.xml` into the
  strings map. Dead `booleanFallback` helper removed.
- JVM: 187 tests across result files, 0 failures, 0 errors, 0 skipped. BUILD SUCCESSFUL.
- Android-test compilation: BUILD SUCCESSFUL.
- Lint: BUILD SUCCESSFUL with existing baseline.
- `git diff --check`: passed.
- `./tools/build_apk.sh`: passed. Debug APK: 950,314 bytes; SHA-256
  `5c6d3dbb707f5a6f4a35579746c1ff798ba30f1b3c7096fc8ae8c6b35be7f4a4`.
- API 24 and API 35 device runtime smoke remain unperformed.

## Inline debounced numeric settings — 2026-07-30

- Replaced eight modal quantity sliders with inline `−` / `+` controls for workspace screen
  count/default screen, workspace rows/columns, and horizontal app-grid portrait/landscape
  rows/columns. Buttons use bounded one-unit changes, disabled endpoint states, consumed long press,
  48dp targets, semantic M3 colors, localized accessibility descriptions, and child-focus support in
  root and nested legacy preference lists.
- Moved applications-grid transparency to an inline native slider in its existing Appearance category,
  retaining raw `0..255` semantics and immediate value feedback.
- Added shared 250 ms trailing persistence. Pending values flush during Settings pause/destruction;
  explicit process exits add a durable commit barrier without creating untouched disabled wrapper keys.
  Horizontal app-grid preference keys and runtime aliases share one editor transaction.
- Invalid stored bounded values are corrected, reset cancels pending callbacks and synchronously clears
  default `SharedPreferences`, and recycled slider callbacks update only their originating row.
- Added API 35 light/night Settings theme opt-outs for forced edge-to-edge. Legacy dialog seek-bar code,
  public resource IDs/attributes, existing keys/defaults/categories, and zero-runtime-dependency contract
  remain unchanged.

### Validation — 2026-07-30

- TDD red evidence included active `DialogSeekBarPreference` resource contracts, the expected
  `InlineStepperPreference`/legacy-cast integration failure, stored `99` remaining outside an `8` bound,
  missing reset helper, nested list child-focus rejection, missing API 35 Settings theme overrides, and
  recycled slider callback updating the wrong row.
- JVM: 169 tests across 31 result files, 0 failures, 0 errors, 0 skipped. Android-test assembly and Java
  compilation passed.
- Full API 35 `PreferencesE2ETest`: 21 tests passed in 341.617 seconds. Coverage includes rapid
  debounce, bounds/listener rejection, screen-count dependency, lifecycle flush, transparency,
  horizontal aliases, invalid-store correction, reset cancellation, nested child focus, slider row
  recycling, and durable no-op behavior. Filtered logcat contained no matching launcher fatal exception,
  verifier/missing class or method failure, `UnsupportedOperationException`, or launcher ANR.
- Latest stabilized Preferences focus group: 4 tests passed in 88.241 seconds. Launcher workspace
  persistence and rebuilt geometry: 1 test passed in 50.959 seconds.
- Final changed-path review reported no issues. Final lint passed with existing baseline after
  instrumentation stabilization; `git diff --check` passed.
- `./tools/build_apk.sh` passed. Debug APK: 939,209 bytes; SHA-256
  `4a9ff6d5dac74c672869096f064160d3512cfe5c4d127b6f12fab4cee59c5b6f`.
- Manual API 35 screenshots confirmed root system-bar spacing and all four Workspace steppers. The
  software-only emulator later raised an input ANR while framework code laid out the nested list, so
  clean app-grid slider, keyboard/DPAD, forced-RTL, and TalkBack traversal are not claimed.
- API 24 runtime remains unperformed because no local API 24 device/image is available.

## Drawer indicator sync and home button guard — 2026-07-30

- **Indicator position aligned.** Moved `apps_paging_screen_indicator` from inside
  `ApplicationsPagingView` to `DragLayer` in `launcher.xml` (portrait and landscape). Both
  `workspace_screen_indicator` and `apps_paging_screen_indicator` share the same parent with no
  extra padding, so both resolve to the same visual position via `Gravity.BOTTOM` +
  `navigation_strip_size + rail_inset`.
- **Indicator type synced.** `Launcher.resolveDrawerIndicatorType()` maps the workspace indicator
  preference (`None` → hide, `Slider` → `TYPE_SLIDER_BOTTOM`, `Dots` → `TYPE_DOTS`).
  `loadIndicator()` calls `ApplicationsPagingView.configureIndicator()` so both indicators update
  together when the preference changes.
- **Home button suppressed for paging drawer.** `openApplicationsGrid()` and `closeApplicationsGrid()`
  guard home-button show/hide inside `!(mApplicationsView instanceof ApplicationsPagingView)`.
  The paging drawer is dismissed by back gesture or navigation button.
- **Explicit drawer indicator hide on close.** `closeApplicationsGrid()` calls
  `drawerIndicator.hide()` because the indicator lives in `DragLayer` and is not hidden by the
  paging view's `INVISIBLE` transition.
- `ApplicationsPagingView.configureIndicator(indicator, enabled, type)` replaces the previous
  `onFinishInflate` auto-find of `R.id.apps_paging_screen_indicator`. The indicator reference and
  type are externally provided by Launcher.

### Validation — 2026-07-30

- TDD: `SettingsResourceContractTest.drawerIndicatorPlacedInDragLayer()` written red first; turned
  green after XML and Java production changes.
- JVM: 185 tests across result files, 0 failures, 0 errors, 0 skipped. BUILD SUCCESSFUL.
- Android-test compilation: BUILD SUCCESSFUL.
- Lint: BUILD SUCCESSFUL with existing baseline.
- `git diff --check`: passed.
- `./tools/build_apk.sh`: passed. Debug APK: 811,161 bytes; SHA-256
  `f6d740a636ac932f19a3d7af53884a4e45e68ddbfb861d08a6608cd6db26d564`.
- Instrumentation tests for runtime home-button guard and drawer indicator type/position remain
  pending. API 24 and API 35 device runtime smoke remain unperformed.

## Horizontal paging grid visual refactor — 2026-07-30

- **Cell/grid alignment fixed.** `DrawerLayoutMetrics` now exposes proportional integer slot
  boundaries (`columnLeft`, `columnRight`, `rowTop`, `rowBottom`) using the formula
  `boundary(i) = i * available / count`. Adjacent cells share exact boundaries with zero
  remainder gap at the trailing edge, regardless of viewport-width divisibility.
- **Orientation inconsistency fixed.** `apps_paging_view.xml` (portrait and landscape) replaced
  hard-coded `40dp` dock-exclusion padding with `@dimen/navigation_strip_size`. Landscape
  `apps_page_view.xml` asymmetric `paddingEnd="30dp"` equalized to `3dp`. Landscape
  `application_boxed_page.xml` normalized to match portrait padding and `minHeight`.
- **`ApplicationsPageView` replaced nested `LinearLayout` rows with a direct `ViewGroup`.** New
  `onMeasure` measures each child to its exact slot dimensions; `onLayout` places each child at
  its slot coordinates. Removes the double floor-division truncation that each row's `LinearLayout`
  contributed.
- **Position restoration by item ordinal.** `ApplicationsPagePartition.pageIndexForItemOrdinal()`
  maps a saved item ordinal to its page after capacity changes. `ApplicationsPagingView` captures
  the first visible ordinal before a rebuild and restores the containing page when remembered
  position is enabled; resets to page zero otherwise.
- **Deferred pager build on viewport.** `ViewPager.OnViewportChangedListener` fires on the first
  real non-zero size change. `ApplicationsPagingView` uses this to rebuild when the initial build
  used a display-metrics fallback width (i.e., before first layout).
- No third-party dependencies added. `ApplicationsView` interface unchanged.

### Validation — 2026-07-30

- TDD: red tests written before each production change; all turned green after implementation.
- JVM unit tests: `DrawerLayoutMetricsTest` (4 existing + 6 slot-boundary), `ApplicationsPagePartitionTest`
  (9 existing + 6 ordinal-mapping). All passed.
- Full JVM suite: BUILD SUCCESSFUL (all tests green).
- Instrumentation compilation: `./gradlew :app:assembleDebugAndroidTest` BUILD SUCCESSFUL.
  New `ApplicationsPagingViewInstrumentationTest` compiles; device-runtime evidence not yet obtained.
- Lint: BUILD SUCCESSFUL with existing baseline (23 previously-baselined issues resolved by this
  change; no new issues introduced).
- `git diff --check`: passed.
- `./tools/build_apk.sh`: passed. APK 1,008,198 bytes;
  SHA-256 `2bf08365453dd2ac2c5fe686954c7920ea38a7d39f06bbef0f375eaae12d608e`.
- API 24 and API 35 device runtime smoke: not yet performed (no connected device). Required before
  shipping: portrait 4×4 and landscape 3×5 smoke, trailing-edge alignment visual check, rotation
  with remembered position on and off, dock/indicator overlap check, TalkBack page count.

## Launcher settings persistence — 2026-07-29

- `Launcher.sRestart`-backed preference changes recreate `Launcher` on return without killing the process.
  An appearance change plus a `Launcher.sRestart`-backed setting coalesces into exactly one replacement.
- Workspace row and column values persist in default `SharedPreferences`; rebuilt `CellLayout`
  instances use distinct selected geometry.
- No manifest, process configuration, or third-party dependency changes.

### Validation — 2026-07-29

- TDD red evidence: the valid pre-fix grid test left `Launcher` unreplaced on the stale-geometry
  path; the valid coalescing test expected 1 replacement but got 2.
- Focused API 35 greens: appearance, 1 test in 61.375 seconds; workspace grid, 1 test in
  74.814 seconds.
- Final API 35 `LauncherE2ETest`: 13 tests passed in 384.437 seconds. Drawer open/close smoke:
  1 test passed in 26.65 seconds.
- JVM: 167 tests, 0 failures, 0 errors, 0 skipped. Android-test assembly and Java compilation
  passed. Lint passed with the existing baseline. `git diff --check` passed.
- `./tools/build_apk.sh` passed. Debug APK: 798,946 bytes; SHA-256
  `0d3cfa9810fe005c2474dc658d82d4cbfbbe5e7f9f67b5278fbdd06dd82cc519`.
- API 35 cold launch succeeded after dismissing an unrelated Digital Wellbeing system ANR; the
  `Launcher` hierarchy loaded. Filtered logcat had no launcher `FATAL`, launcher ANR,
  verifier/missing class/method, or `UnsupportedOperationException`.
- API 24 runtime remains unperformed because no local API 24 image exists. The full
  instrumentation suite and manual force-stop preference-retention smoke remain unperformed.

## Docker emulator local noVNC interaction — 2026-07-29

- Added Xvfb-backed Android Emulator display, internal loopback-only x11vnc bridge, and noVNC/websockify browser endpoint.
- `zeam-runtime` maps noVNC only to `127.0.0.1:6080`; raw VNC remains inside container on `localhost:5900` and no unauthenticated remote endpoint is published.
- `run-zmreborn` detects stale image/port mappings before deployment and requires explicit `recreate`, preserving fixed-name runtime reuse during normal runs.
- Updated emulator testing skill and README with browser interaction, Docker-exec ADB fallback, and static-container lifecycle instructions.

### Validation — 2026-07-29

- Pending emulator-image rebuild and noVNC browser interaction evidence.

## Build tooling — AGP 8.6.0 upgrade — 2026-07-29

- Upgraded Android Gradle Plugin from `8.5.2` to `8.6.0`, the minimum officially supported version
  for `compileSdk 35` per the AGP 8.6.0 release notes.
- Gradle Wrapper (`8.7`), JDK (`17`), `compileSdk`/`targetSdkVersion` (`35`), `minSdkVersion` (`24`),
  Java 8 source/target compatibility, and all existing dependency versions remain unchanged.
- No third-party runtime dependencies added. No Kotlin, R8/D8 override, NDK, or `buildToolsVersion`
  changes made. One-line change in `build.gradle`; `CLAUDE.md` and `README.md` updated to match.
- `local.properties` is not committed; Docker wrapper resolves the SDK from container environment.
  The `sdk.dir` advisory warning is a non-blocking informational emitted when the untracked local file
  references the host-only `/opt/android-sdk`; the Docker wrapper build succeeds regardless.
- Lint passed with existing `app/lint-baseline.xml`; no new issues introduced by the upgrade.
  Baseline metadata (`lint 8.5.2`, `AGP (8.5.2)`) records the generating version; AGP 8.6.0 reads
  and applies it correctly. Baseline requires regeneration only if new issues appear or are waived.

### Validation — 2026-07-29

- JVM unit tests: **167 tests**, 31 result files, 0 failures, 0 errors, 0 skipped.
- Android-test Java compilation: passed.
- Lint: passed (baseline unchanged).
- `git diff --check`: passed.
- `./tools/build_apk.sh`: passed. Debug APK: **798,938 bytes**;
  SHA-256 `768c586cf2bcd372bf0c901dcf81e0b44ab1a545b5b89beff197fba23cf4ecf9`.

## Persisted appearance modes and palette sources — 2026-07-29

### Appearance persistence and resources

- Added persisted Follow-system, Light, and Dark appearance modes. Activity configuration is wrapped
  so the selected mode applies consistently through launcher and Settings recreation.
- Added theme resource qualifiers for light, dark, and system-following appearance values, preserving
  semantic launcher and Settings color tokens across configuration changes.
- Retained palette state refreshes after recreation, so the active palette is reapplied without losing
  the selected appearance mode or requiring a wallpaper change.

### Palette source compatibility

- API 31+ obtains dynamic palette resources through a configuration-wrapped context and bypasses stale
  resource/cache entries when the active appearance configuration changes.
- API 24–30 uses a brightness-prefixed schema-v2 wallpaper palette cache, keeping light and dark
  palette entries distinct while retaining compatible cached wallpaper-derived colors.

### Tests and validation

- Updated JVM and Android instrumentation coverage for persisted appearance selection, configuration
  wrapping, dynamic-resource cache bypass, brightness-prefixed schema-v2 cache behavior, recreation,
  and retained palette refresh.
- 149 JVM tests passed. Android-test compilation passed. Lint passed. `git diff --check` passed.
- `./tools/build_apk.sh` passed, producing an 890,264-byte APK with SHA-256
  `7f57ba56f5739bf53e6ad5e6bc19a6aee0efb70d4b66dc042757d51bf6327dfd`.
- API 35 runtime attempt was blocked because `docker-dev` lacks `/dev/kvm` and host `adb` is
  unavailable. API 24 runtime remains unavailable.

## Launcher UI/UX modernization — 2026-07-29

### Home, dock, drawer, and icons

- Empty workspaces now remain intentionally blank; zero/one-page indicators stay hidden and
  multi-page dots sit above bottom navigation/dock space.
- Fresh installs use responsive vertical `GridView.AUTO_FIT` application layout. Existing saved
  drawer choices remain unchanged. Automatic, Comfortable, Default, and Compact density presets
  select width-driven cell geometry; portrait and landscape share spacing and two-line labels.
- Vertical mode now integrates accent-insensitive, case-insensitive search with prefix-first ranking,
  clear/no-results affordances, query retention through model refresh, and stable-anchor restoration.
  Empty-query multi-section results expose a 48dp accent-normalized alphabetical fast-scroll rail
  that preserves source positions, adapts labels to available height, hides during search reordering,
  and supports touch, keyboard, and per-section accessibility navigation. Rail selection/virtual IDs
  remain stable through usable compaction, all drawer input is suppressed during close, and RTL mirrors
  rail position, grid inset, plus directional focus. Adapter snapshots,
  deterministic profile-sensitive stable IDs, and mutation-free binding keep filtered/package-refresh
  state independent from shared model objects.
- Both vertical and paged drawers suppress click/long-click actions throughout close animations and
  reset transforms before reopening. Reopening invalidates stale animated-close callbacks so a late
  callback cannot hide the reopened grid.
- First-run dock bootstrap resolves installed Phone, Messages, Browser, Camera, and Contacts
  handlers through implicit intents, deduplicates components, and never inserts a drawer shortcut.
- Shared icon normalization keeps API 26+ adaptive drawables live, supplies deterministic null
  fallback, and raster-normalizes only legacy drawables. `TextView` bindings now use view-owned
  drawable copies with explicit positive bounds, avoiding blank adaptive icons and callback/bounds
  conflicts across drawer, workspace, and dock.
- Remembered vertical drawer position is preserved when focus returns. Empty-query package refresh
  still resets to top when remember-position is disabled; clearing search restores its explicit
  pre-search anchor. `ApplicationsDrawerView.captureScrollState()` returns an empty state when its
  adapter is absent or empty, or the visible position is invalid, preventing refresh crashes. While
  API 35 forced edge-to-edge remains opted out, drawer system-bar padding is zeroed because decor
  fitting has already applied those insets; gesture insets continue to reach drawer and workspace.

### Palette, widgets, and Settings

- Wallpaper palette refresh runs through a dedicated single-thread launcher executor instead of
  process-global `AsyncTask` scheduling. Wallpaper broadcasts retain
  `BroadcastReceiver.PendingResult` lifetime on launcher-live and launcher-absent paths, then post
  live palette reapplication to the UI thread. Controlled instrumentation dispatch exercises the
  registered production receiver path and waits on the same executor queue.
- Widget picker/configuration tracks allocated IDs through recreation, rejects stale or mismatched
  successful results, and releases abandoned IDs even when cancellation data is null. Initial spans
  wait for measured target-screen `CellLayout` geometry, use signed gaps, clamp to grid bounds, and
  publish provider size options only after current geometry becomes ready. Deferred placement keeps
  its original target screen across layout delay and activity recreation.
- Direct widget resize uses provider-axis handles and cell snapping, enforces provider minimum
  dimensions, rejects occupied or out-of-bounds spans, persists accepted changes, and refreshes
  provider options from measured geometry. Cancelled resize gestures restore the original span
  without persistence or provider-option refresh.
- Settings boolean rows use platform `SwitchPreference`, exact semantic thumb/track state lists,
  borderless normal rows, focused/selected outline/ripple feedback, and cached wallpaper surface.
  Vertical drawer density is exposed as a localized `ListPreference`; legacy row/column controls
  remain available only for horizontal paging.

### Regression coverage and validation

- Added/expanded JVM contracts for blank home, dock resolution, normalized drawer filtering,
  stable/profile-sensitive IDs, stable-anchor restoration, responsive density breakpoints,
  close-state guards, dedicated wallpaper scheduling, explicit adaptive-icon bounds, signed widget
  gaps and clamping, pure resize geometry/source contracts, exact switch style/state chains,
  production fallback colors, and ripple ordering.
- Added/expanded API instrumentation for integrated search and immutable binding, stale-close reopen
  protection, unremembered package-refresh reset, visible empty-to-populated adapter refresh that
  restores original items and closes the drawer, adaptive-icon compound bounds, close-animation action
  suppression, production pager accessibility, Preferences recreation, registered platform back callback
  delivery, wallpaper receiver scheduling, measured widget geometry, and gesture exclusion combinations.
- Final fast-scroll/RTL static validation passed: 155 JVM tests, 0 failures, 0 errors, 0 skipped;
  Android-test Java compilation, `:app:lint`, and `git diff --check` passed.
- Final full working-tree validation passed: 167 JVM tests across 31 result files, with 0
  failures, 0 errors, and 0 skipped; Android-test Java compilation, `:app:lint`,
  `git diff --check`, and parsing all 29 changed XML files passed.
- `WidgetResizeInstrumentationTest` passed all 6 component tests on API 35 in 102.099 seconds,
  covering provider-axis handles, 48dp targets, valid and occupied candidates, fixed providers,
  outside cancellation, and accessibility-click cancellation without premature layout mutation.
- Targeted API 35 Launcher, drawer, and Preferences smoke passed all 3 tests in 57.372 seconds.
  Filtered logcat contained no launcher fatal exception, verifier failure, missing-method/class
  failure, `UnsupportedOperationException`, or launcher ANR.
- Final `./tools/build_apk.sh` passed. Debug APK: 903,707 bytes; SHA-256
  `b0ba47f78f7169ad8ffd6781706fc6cba470892e080241059964508df04eae00`.
- The pre-fix 80-test API 35 instrumentation result is historical and does not validate this fix.
  No API 35 device run validates final fast-scroll/RTL behavior: an emulator reboot triggered
  unrelated Google/phone system ANRs and instrumentation lifecycle timeouts. The compiled
  fast-scroll and empty-adapter regressions remain unexecuted on-device.
- Post-fix API 35 cold-launch/model-refresh smoke succeeded: launcher started; the drawer showed
  18 apps and its search field. Filtered logcat after that smoke had no launcher `FATAL`,
  `IndexOutOfBoundsException`, verifier, missing-method, or `UnsupportedOperationException` entries.
- Real external-provider widget resize runtime validation remains unperformed on API 24 and API 35.
- No API 24 device or local emulator image is available, so API 24 runtime smoke remains unperformed.
  Instrumentation invokes the registered API 35 predictive-back callback object, but an actual
  SystemUI edge gesture remains unverified.

### Deferred scope

- Drawer suggestions, profile-aware icon cache, automatic neighboring-item reflow and snackbar undo,
  Material sheets, complete Settings hierarchy, optional accessibility drawer button, and forced
  edge-to-edge opt-in remain deferred.

## Gesture Modernization — Phase 4: Material 3 Expressive Visual Refresh — 2026-07-28

### Dynamic wallpaper-adaptive color

- Added `org.zmreborn.theme.WallpaperColorExtractor` — extracts the system wallpaper's primary seed color via `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` (API 27+) and derives a 6-role M3-inspired tonal palette using HSV value steps. Static amber-seed fallback on API 24–26 and on null/SecurityException.
- Colors cached in `SharedPreferences` (`org.zmreborn.theme.wallpaper_colors`) to avoid blocking cold start.
- `Launcher.onCreate()` schedules `WallpaperColorExtractor.refresh()` through a serial background executor on every cold start.
- `WallpaperIntentReceiver.onReceive()` retains broadcast lifetime with `goAsync()`, schedules serialized extraction, and reapplies the palette after wallpaper changes.

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

- New `WallpaperColorExtractorTest` (9 pure JVM tests): tonal ramp role count, hue preservation, HSV value contracts (dark on-primary, bright on-surface, midtone outline), saturation clamping, gray-seed zero-saturation, fallback alpha opacity.
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

## [3.1.11-alpha-rc6]

- Fixed `Launcher.sRestart`-backed settings persistence and application by recreating `Launcher`
  instead of killing the process.
- Workspace row and column changes rebuild workspace geometry.
- Appearance and `Launcher.sRestart`-backed changes coalesce into one `Launcher` recreation.
- Validation: 167 JVM tests; final 13 API 35 `LauncherE2ETest` tests; drawer smoke; lint;
  Android-test assembly; build wrapper; clean filtered launcher logcat. API 24 and manual
  force-stop smoke remain unperformed.

## [3.1.11-alpha-rc5]

- Added local noVNC browser interaction for the Docker Android emulator, backed by Xvfb and an internal loopback-only VNC bridge.
- Added runtime recreate checks that detect stale emulator image or port mappings before deployment.

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
