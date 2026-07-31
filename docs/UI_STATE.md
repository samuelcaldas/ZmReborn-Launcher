# ZM Reborn UI state reference

## Scope and evidence

This document describes current working-tree behavior for ZM Reborn 3.1.11-alpha-rc7. Source and automated contracts prove implementation structure; they do not replace device smoke tests or screenshots.

Project constraints:

- Legacy Java/View launcher architecture remains in place.
- `minSdk` 24, `targetSdk` 35, `compileSdk` 35.
- Zero third-party application/runtime dependencies.
- `original_source` remains immutable reconstruction provenance.
- Generated `R` and existing public resource/database contracts remain authoritative.

## Home shell

### Empty workspace

`Launcher.updateWorkspaceEmptyTip()` always hides `workspace_empty_tip`. Empty home pages are intentional blank states; no persistent instruction overlays wallpaper.

### Page indicator

`ScreenIndicator`:

- stays hidden for zero or one logical page;
- suppresses accessibility page announcements when only one page exists;
- reveals and auto-hides only for multiple pages;
- places dot indicators at bottom above dock/navigation spacing;
- retains bottom Signal Rail compatibility.

### First-run dock

Fresh dock bootstrap resolves installed default handlers through implicit intents for:

- Phone;
- Messages;
- Browser;
- Camera;
- Contacts.

Unresolved or duplicate components are skipped. Bootstrap does not add an application-drawer item. Legacy `dirty` bootstrap markers are deleted without recreating that item, so user removal is not reversed on later launches.

Dock background choices now map to cached wallpaper surface roles, except explicit transparent and legacy grey-bar choices. Dock icons no longer use reflection rendering.

## Application drawer

### Default and geometry

Fresh installations default to vertical scrolling. Existing stored drawer choices remain unchanged.

`ApplicationsGridView` uses:

- `GridView.AUTO_FIT`;
- Automatic, Comfortable, Default, and Compact density presets;
- 360/600/840dp width classes for Automatic density;
- shared portrait/landscape spacing and two-line wrapping labels;
- orientation-specific safe padding without fixed portrait/landscape column counts;
- top reset after adapter replacement when remember-position is disabled.

Horizontal paging remains available for existing users and advanced compatibility. Paging cell constraints use `drawer_cell_min_width` and `drawer_cell_min_height` resources rather than raw pixels.

### Paging drawer page indicator

`apps_paging_screen_indicator` lives in `DragLayer` alongside `workspace_screen_indicator`, not inside `ApplicationsPagingView`. Both indicators share the same parent container with no extra padding offset, so their visual position (`Gravity.BOTTOM` + `navigation_strip_size + rail_inset`) resolves to the same pixel row.

Indicator type follows the workspace page indicator preference (`workspace_screen_indicator`):

- **None** — drawer indicator hidden;
- **Slider** — drawer shows `TYPE_SLIDER_BOTTOM` bar, same as homescreen;
- **Dots** — drawer shows `TYPE_DOTS`, same as homescreen.

`Launcher.loadIndicator()` calls `ApplicationsPagingView.configureIndicator()` whenever the preference changes, so both indicators update together. The drawer indicator is hidden explicitly on drawer close because it lives in `DragLayer` and is not swept away with `ApplicationsPagingView`'s `INVISIBLE` state.

The home/close `ImageButton` (`home_button`) is not shown when the paging drawer is active; the back gesture or navigation button closes the paging drawer. The home button remains for the vertical `ApplicationsDrawerView` only.

### State and motion

Grid and paging implementations reset alpha, scale, translation, and animation state before opening or closing. Animated close hides the drawer only after exit animation completion and disables grid, search, clear, and fast-scroll input during that interval. Legacy drawing-cache enablement, no-op scroll workarounds, and explicit `System.gc()` calls are absent.

Loading, ready, empty, and error states remain launcher-owned and shared by both drawer implementations. `ApplicationsDrawerView.captureScrollState()` returns an empty state when the adapter is absent or empty, or its visible position is invalid. Reopening during an animated close invalidates the stale close callback, preventing a late callback from hiding the reopened grid.

### Search and identity

Vertical mode includes an integrated search field and 48dp clear action. Filtering is case-insensitive and accent-insensitive, ranks prefix matches before contains matches, preserves source order within each rank, and shows a dedicated no-results state. Active queries survive package/model refresh. Clearing search restores the pre-search stable-item anchor and pixel offset; empty-query refresh still resets to top when remember-position is disabled.

Application and folder rows expose deterministic stable adapter IDs derived from current profile serial plus component, intent, title, or folder identity. Drawer and adapter list boundaries snapshot submitted collections, and icon binding does not mutate shared model icon/filter state.

An empty-query result with multiple normalized A–Z sections exposes a 48dp alphabetical fast-scroll rail. It preserves source positions, folds accents into their base letter, groups non-Latin/numeric/untitled labels under `#`, compacts labels for short windows, and hides while search changes result ordering. Selected reachable sections retain their identity through compaction, and virtual accessibility IDs remain letter-stable. Touch drag, keyboard directional navigation, and per-section virtual accessibility actions all move the grid to the selected source position. RTL locales mirror rail position, grid inset, and directional traversal.

Suggestions and profile-aware icon caching are not implemented.

### Icons

`Utilities.normalizeApplicationIcon()` is the shared application-icon path:

- null icons use deterministic `ic_launcher_application` fallback;
- API 26+ adaptive icons remain live `AdaptiveIconDrawable` instances;
- adaptive foreground/background layers and device mask remain intact;
- legacy drawables are raster-normalized;
- application icons bound to `TextView` receive view-owned drawable copies and explicit positive
  bounds, including adaptive icons whose color layers do not provide usable intrinsic dimensions;
  model/drawer/workspace/dock instances therefore do not compete for one drawable callback;
- uninstall and disabled treatments handle non-bitmap drawables without unsafe casts.

API 26 class references are isolated in `compat/AdaptiveIconCompat.Api26`, behind an SDK gate.

## Appearance and wallpaper-derived palette

### Appearance selection

**Preferences → General → Theme** provides Follow system, Light, and Dark. The chosen value persists as `application_appearance`.

`LocaleUtil` composes the selected night mode with the locale configuration, changing only `Configuration.UI_MODE_NIGHT_MASK` and preserving every non-night configuration bit. Changed theme selection follows explicit recreation paths in both `Launcher` and Settings, so locale and appearance configuration apply together.

### Frosted backgrounds

**Preferences → General → Appearance → Blur backgrounds** controls dock and active application drawer
with one switch. Enabled state temporarily overrides dock background rendering but never changes saved dock
mode. Disabling restores that mode plus configured application-grid transparency.

All supported versions render a Material-derived translucent gradient with deterministic fixed-seed grain.
API 24–30 always use this procedural frost. Live wallpaper, platform-restricted wallpaper access, invalid
wallpaper geometry, and generation/allocation failures also use frost without requesting storage or media
permissions.

On API 31+ only, readable static wallpaper bitmap already owned by `Workspace` can provide actual blurred
pixels. `WallpaperBackdropRenderer` downsamples full wallpaper to one eighth linear size, applies three
sliding-window ARGB box-blur passes on shared wallpaper refresh executor, and caches result by source bitmap
identity. Drawables upscale and crop cached result using same workspace wallpaper X/Y offset as normal
wallpaper rendering. Workspace scrolling invalidates dock and drawer surfaces for parallax alignment but does
not regenerate blur. Wallpaper replacement and launcher teardown invalidate generation token, cancel queued
work, and interrupt active pixel processing. Cache invalidation drops ownership without recycling a bitmap
that an installed backdrop can still draw; stale never-installed render results are recycled immediately.

### Role resources and cache

Semantic M3 role resources have light and night variants; platform Settings and dialogs use matching platform themes.

On API 31+, role resources resolve through system dynamic color and wallpaper palette caching is bypassed. On API 24–30, `theme/WallpaperColorExtractor` reads `WallpaperManager.getWallpaperColors()` on API 27+ and derives an HSV tonal ramp; API 24–26, unavailable colors, and denied access use the static amber fallback. Schema-v2 launcher-private `SharedPreferences` cache entries isolate palettes by light/dark brightness:

- primary;
- on-primary;
- surface;
- surface variant;
- on-surface;
- outline.

On API 24–30, Launcher applies the matching cached roles immediately and serializes extraction during startup and wallpaper changes through a dedicated single-thread executor. A live Launcher wallpaper callback reapplies cached roles on the UI thread while retaining pagination and user-folder rails, including drawer surfaces and labels, `Dock`, open workspace `Folder` instances, and rename-dialog palettes. Broadcast handling uses `goAsync()` on both live-launcher and launcher-absent paths so process lifetime covers extraction; test synchronization uses a queue barrier on that same executor rather than process-global `AsyncTask` state.

Settings reads its current themed surface when the Preferences activity is created. Cached values do not replace compiled `R.color` resources; static semantic role resources remain fallback styling.

Legacy popup, sheet, and navigation redesign remain deferred. Externally-owned remote UI is not claimed as themed.

## Widgets

Empty-home long press now exposes **Widgets**, **Shortcuts**, **Folders**, **Wallpaper**, and **Preferences** directly. The platform options-menu **Add** action retains its compact Widgets/Shortcuts/Folders dialog.

Widget selection uses a launcher-owned preview-card dialog rather than `ACTION_APPWIDGET_PICK`. Search is always first; installed providers follow in localized label order. Preview resolution uses provider preview image, provider icon, then launcher widget fallback. Provider metadata and drawables load on a cancellable background executor, while target-grid span calculation and view publication remain on the main thread. Opening, loading, dismissing, or rotating this dialog allocates no `AppWidgetHost` ID; picker-open state and target cell restore after desktop binding completes.

External provider selection allocates one ID, tries `bindAppWidgetIdIfAllowed(...)`, and falls back to `ACTION_APPWIDGET_BIND` when user approval is required. Search selection reuses the launcher-owned Search widget path without allocating an ID.

Implemented lifecycle behavior:

- bind/configuration cancellation releases allocated ID, including null result intents;
- successful null configuration result can recover tracked ID;
- stale or mismatched successful results are rejected without adopting an unrelated returned ID;
- invalid provider results release ID;
- successful placement clears pending tracking;
- failed placement releases ID;
- deferred placement state and insertion behavior survive activity recreation;
- delayed placement remains bound to the original target screen rather than the later current page.

Initial widget span comes from provider minimum dimensions only after target `CellLayout` geometry has completed layout. Horizontal and vertical spans use their own cell size and signed gap, clamp to available grid bounds, and avoid off-by-one or overflow errors.

Widget provider options are updated from persisted span pixel dimensions during initial bind and rebind, also waiting for measured target-screen geometry when needed. Numeric `widget_span` resources remain for public/provenance compatibility but launcher logic no longer shows that dialog.

Long-pressing a resizable provider widget opens a direct full-screen, cell-snapped edit overlay. Focusable 48dp handles appear only on axes supported by the provider. Handle gestures enforce provider minimum dimensions with documented fallbacks, grid bounds, and exact target-cell occupancy without moving neighboring items. Invalid placements show explicit unavailable text. Releasing a valid resize persists the new cell and span, then refreshes provider options using measured geometry. After selection, a second press-and-drag on the widget body removes the overlay and hands the original workspace item to the existing `Workspace`/`DragLayer` flow, enabling movement and DeleteZone removal without a parallel deletion path. Back, outside touch, and lifecycle cancellation remove the overlay without mutating the widget.

Automatic neighbor reflow and snackbar undo remain deferred.

## Settings

Boolean settings use platform `SwitchPreference` while preserving existing keys, defaults, listeners, and `SharedPreferences` persistence.

Settings styling includes:

- semantic switch thumb/track state colors;
- 48dp minimum switch touch geometry;
- M3 role text/category colors;
- cached wallpaper surface background;
- transparent normal preference rows;
- primary outline and surface-variant fill for focused/selected rows;
- framework-owned row click/focus behavior.

Eight count settings use inline `−` and `+` controls: workspace screen count/default screen, workspace rows/columns, and horizontal app-grid portrait/landscape rows/columns. Each action changes one unit, disables at its configured bound, consumes long press without repeating, and exposes a 48dp keyboard-focusable action with localized current-value/bound descriptions. Nested preference dialogs allow descendant focus so DPAD/keyboard navigation can reach these controls.

Applications-grid transparency remains a raw `0..255` value but now uses an inline native slider in **Applications Grid → Appearance**. Numeric labels update immediately. Steppers and slider use a 250 ms trailing persistence window; `onPause()`/`onDestroy()` flush pending values before Settings yields to Launcher, while explicit process-restart paths add a durable commit barrier. Reset cancels callbacks then clears default `SharedPreferences` synchronously, preventing queued writes from restoring deleted settings.

Horizontal app-grid controls remain enabled only for horizontal paging. Vertical auto-fit mode disables fixed row/column controls because runtime width determines geometry. Each accepted horizontal change writes its preference key and authoritative runtime alias in one editor transaction. Untouched disabled wrapper keys are not created by durable flushes.

`Launcher.sRestart`-backed preference changes clear pending restart state and automatically recreate `Launcher` on return. Appearance plus `Launcher.sRestart`-backed changes coalesce into one activity recreation. Workspace row and column values remain in default `SharedPreferences`; rebuilt `CellLayout` instances use selected geometry. Drawer density remains a vertical-only `ListPreference`.

Legacy popup, sheet, and navigation redesign remain deferred; externally-owned remote UI is not claimed as themed.

## Insets, gestures, and back

`compat/WindowInsetsCompat`, `GestureExclusionCompat`, and `BackGestureCompat` isolate newer platform APIs from API 24 class verification.

Launcher propagates gesture insets to workspace, drag layer, and drawer, and provides system-bar metadata to Dock. Because forced edge-to-edge remains opted out, decor fitting already positions drawer content outside system bars; drawer system-bar padding stays zero to avoid duplicate top/bottom spacing. Left/right system-back edges remain available. Registered API 33+ platform predictive-back callback object invocation/delivery closes drawer, then folder; API 34 adds reversible progress preview.

API 35 light/night Launcher and Settings themes opt out of forced edge-to-edge enforcement. Full edge-to-edge migration remains deferred until runtime evidence covers all launcher surfaces.

## Automated coverage

Focused JVM/resource/source coverage includes:

- `CellLayoutSpanTest` — span ceiling, gaps, clamping, overflow, invalid geometry;
- `WallpaperColorExtractorTest`, `ArgbBoxBlurTest`, `WallpaperBackdropRendererTest`, `WallpaperBackdropAlignmentTest`, and `WallpaperBlurGenerationTest` — HSV role contracts, deterministic and interruptible multi-pass ARGB blur, downsample geometry, invalid-input rejection, workspace offset alignment, and stale-generation rejection;
- `DrawerSearchFilterTest`, `DrawerScrollStateTest`, `DrawerDensityPolicyTest`, and `ApplicationsAdapterContractTest` — normalized ranking, immutable filtering, stable restoration/fallback, responsive density breakpoints, and deterministic profile-sensitive IDs;
- `SettingsResourceContractTest` and `SystemBarResourceContractTest` — switch semantics, eight inline steppers, inline transparency slider, 250 ms debounce, reset/storage contracts, settings geometry, semantic control colors, logical padding, API 35 theme opt-outs, and preserved public/widget source contracts;
- `UiModernizationContractTest` — blank home, resolved dock handlers, integrated immutable drawer search, responsive/close-safe drawer, explicit icon bounds, dedicated wallpaper executor, and receiver dispatch;
- existing drawer, token, compatibility, persistence, localization, and public-resource suites.

API 35 instrumentation coverage includes:

- `ApplicationIconE2ETest` — deterministic null fallback plus API 26+ adaptive-icon identity, explicit compound bounds, and layer-safe treatments;
- `WallpaperPaletteE2ETest` — live palette reapplication plus controlled production receiver scheduling and cached-role refresh;
- `BlurBackgroundInstrumentationTest`, `WallpaperBackdropRendererInstrumentationTest`, and `WallpaperBlurLifecycleInstrumentationTest` — procedural frost on dock plus vertical/paging drawers, disabled-state dock/alpha restoration, unchanged saved dock preference, actual bitmap diffusion/ownership, safe cache invalidation, and queued executor cancellation;
- `ApplicationsDrawerE2ETest` — workspace rendering during drawer exit, close-animation action suppression through decor-root pointer dispatch, stale-close reopen protection, search query retention, immutable binding, unremembered refresh reset, and visible empty-adapter recovery that restores original items and closes the drawer;
- `PredictiveBackE2ETest` — registered platform predictive-back callback object invocation/delivery, handler previews, and top/bottom gesture exclusions;
- `LauncherE2ETest` — `Launcher.sRestart`-backed preference persistence, automatic and coalesced
  appearance/`Launcher.sRestart`-backed `Launcher` recreation, workspace row/column geometry rebuild, measured
  production widget-span clamping, and pre-measure geometry rejection;
- `WidgetResizeInstrumentationTest` — provider-axis handle selection, 48dp handle geometry, valid
  and occupied candidates, fixed-provider behavior, outside cancellation, and accessibility-click
  cancellation without premature layout mutation;
- `PreferencesE2ETest` — real activity recreation, `ListPreference` rehydration, immediate inline numeric updates, trailing debounce, lifecycle flush, bounds, listener rejection, app-grid alias transactions, storage reset, clamped-value correction, recycled slider rows, durable no-op behavior, and nested child focus;
- accessibility coverage for the production horizontal pager and indicator page-count semantics;
- existing launcher, drawer, focus, inset, Settings, localization, and public-resource tests.

Latest fresh build and runtime evidence is recorded in `docs/CHANGELOG.md`. API 24 and API 35 runtime coverage remain distinct from compilation and JVM results.

## Runtime evidence and remaining validation

Fresh API 35 evidence is recorded in `docs/CHANGELOG.md`. Full `PreferencesE2ETest` coverage passed
21 tests, including real inline controls, debounce, lifecycle persistence, alias transactions, reset,
row recycling, and nested focus. Focused `LauncherE2ETest` coverage also passed workspace persistence,
activity recreation, and rebuilt grid geometry. Filtered full-suite logcat contained no matching
launcher fatal exception, launcher ANR, verifier/missing class or method failure, or
`UnsupportedOperationException`. Fresh blur coverage passed 8 focused API 35 tests across both drawer
implementations, renderer pixels, bitmap/cache ownership, executor cancellation, settings reachability, and
preference persistence. API 24 runtime and readable system-wallpaper blur integration on API 31–33 remain
unperformed; API 35 normally exercises procedural fallback because wallpaper pixels are restricted.

Manual API 35 screenshots confirmed Settings root system-bar spacing and rendered Workspace rows with
all four inline steppers. Software-only emulator load later produced an input ANR while Android framework
code laid out the nested preference list, so this attempt does not claim clean hands-on app-grid slider,
keyboard/DPAD, or complete Settings traversal. Automated instrumentation remains separate evidence and
does not convert this interrupted manual check into a pass.

Remaining runtime gaps:

- API 24 smoke is unperformed because no API 24 device or local emulator image is available;
- clean manual API 35 traversal of app-grid transparency, keyboard/DPAD activation, forced RTL, and
  TalkBack descriptions remains unperformed after the software-emulator ANR;
- instrumentation invokes the retained registered predictive-back callback object, but an actual
  SystemUI edge gesture is unverified;
- hands-on external-provider selection, bind approval/configuration, provider rendering after
  orientation, real-provider body move/DeleteZone removal and resize validation on API 24 and API 35,
  and abandoned-ID inspection remain unperformed; focused API 35 instrumentation covers preview-card
  loading, Search selection, zero pre-selection allocation, bind cancellation cleanup, picker state
  restoration, resize-handle behavior, and body-drag handoff callback only;
- automatic neighbor reflow, snackbar undo, and other explicitly deferred features have no runtime
  claim.

No unperformed device result is implied by this document.
