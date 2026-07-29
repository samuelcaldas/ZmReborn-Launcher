# ZM Reborn UI state reference

## Scope and evidence

This document describes current working-tree behavior for ZM Reborn 3.1.11-alpha-rc4. Source and automated contracts prove implementation structure; they do not replace device smoke tests or screenshots.

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

Widget add flow now tracks pending `AppWidgetHost` ID across activity results and saved-instance state.

Implemented lifecycle behavior:

- picker/configuration cancellation releases allocated ID, including null result intents;
- successful null configuration result can recover tracked ID;
- stale or mismatched successful results are rejected without adopting an unrelated returned ID;
- invalid provider results release ID;
- successful placement clears pending tracking;
- failed placement releases ID;
- deferred placement state and insertion behavior survive activity recreation;
- delayed placement remains bound to the original target screen rather than the later current page.

Initial widget span comes from provider minimum dimensions only after target `CellLayout` geometry has completed layout. Horizontal and vertical spans use their own cell size and signed gap, clamp to available grid bounds, and avoid off-by-one or overflow errors.

Widget provider options are updated from persisted span pixel dimensions during initial bind and rebind, also waiting for measured target-screen geometry when needed. Numeric `widget_span` resources remain for public/provenance compatibility but launcher logic no longer shows that dialog.

Long-pressing a resizable provider widget opens a direct full-screen, cell-snapped resize overlay. Focusable 48dp handles appear only on axes supported by the provider. The overlay enforces provider minimum dimensions with documented fallbacks, grid bounds, and exact target-cell occupancy without moving neighboring items. Invalid placements show explicit unavailable text. Releasing a valid resize persists the new cell and span, then refreshes provider options using measured geometry. Back, outside touch, and lifecycle cancellation remove the overlay without mutating the widget.

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

Legacy row/column controls remain enabled only for horizontal paging. Vertical auto-fit mode disables fixed-column controls because runtime width determines column count. Drawer density is persisted through a vertical-only `ListPreference`; changing it restarts launcher so responsive geometry is reapplied consistently.

Legacy popup, sheet, and navigation redesign remain deferred; externally-owned remote UI is not claimed as themed.

## Insets, gestures, and back

`compat/WindowInsetsCompat`, `GestureExclusionCompat`, and `BackGestureCompat` isolate newer platform APIs from API 24 class verification.

Launcher propagates gesture insets to workspace, drag layer, and drawer, and provides system-bar metadata to Dock. Because forced edge-to-edge remains opted out, decor fitting already positions drawer content outside system bars; drawer system-bar padding stays zero to avoid duplicate top/bottom spacing. Left/right system-back edges remain available. Registered API 33+ platform predictive-back callback object invocation/delivery closes drawer, then folder; API 34 adds reversible progress preview.

`values-v35/styles.xml` still opts out of forced edge-to-edge enforcement. Full edge-to-edge migration remains deferred until runtime evidence covers all launcher surfaces.

## Automated coverage

Focused JVM/resource/source coverage includes:

- `CellLayoutSpanTest` — span ceiling, gaps, clamping, overflow, invalid geometry;
- `WallpaperColorExtractorTest` — HSV role contracts and production fallback opacity;
- `DrawerSearchFilterTest`, `DrawerScrollStateTest`, `DrawerDensityPolicyTest`, and `ApplicationsAdapterContractTest` — normalized ranking, immutable filtering, stable restoration/fallback, responsive density breakpoints, and deterministic profile-sensitive IDs;
- `SettingsResourceContractTest` — switch semantics, drawer-density persistence, settings geometry, ripple root/color, persistence/resource contracts, and widget source contracts;
- `UiModernizationContractTest` — blank home, resolved dock handlers, integrated immutable drawer search, responsive/close-safe drawer, explicit icon bounds, dedicated wallpaper executor, and receiver dispatch;
- existing drawer, token, compatibility, persistence, localization, and public-resource suites.

API 35 instrumentation coverage includes:

- `ApplicationIconE2ETest` — deterministic null fallback plus API 26+ adaptive-icon identity, explicit compound bounds, and layer-safe treatments;
- `WallpaperPaletteE2ETest` — live palette reapplication plus controlled production receiver scheduling and cached-role refresh;
- `ApplicationsDrawerE2ETest` — workspace rendering during drawer exit, close-animation action suppression through decor-root pointer dispatch, stale-close reopen protection, search query retention, immutable binding, unremembered refresh reset, and visible empty-adapter recovery that restores original items and closes the drawer;
- `PredictiveBackE2ETest` — registered platform predictive-back callback object invocation/delivery, handler previews, and top/bottom gesture exclusions;
- `LauncherE2ETest` — measured production widget-span clamping and pre-measure geometry rejection;
- `WidgetResizeInstrumentationTest` — provider-axis handle selection, 48dp handle geometry, valid
  and occupied candidates, fixed-provider behavior, outside cancellation, and accessibility-click
  cancellation without premature layout mutation;
- `PreferencesE2ETest` — real activity recreation and `ListPreference` rehydration;
- accessibility coverage for the production horizontal pager and indicator page-count semantics;
- existing launcher, drawer, focus, inset, Settings, localization, and public-resource tests.

Latest fresh build and runtime evidence is recorded in `docs/CHANGELOG.md`. API 24 and API 35 runtime coverage remain distinct from compilation and JVM results.

## Runtime evidence and remaining validation

Fresh API 35 evidence is recorded in `docs/CHANGELOG.md`. Six widget-resize component tests and three targeted Launcher, drawer, and Preferences smoke tests passed on API 35 with clean filtered launcher logcat. This component coverage does not exercise a real external widget provider. The precise empty-adapter recovery regression compiles, but a complete post-fix API 35 instrumentation rerun remains blocked by unrelated Google/phone system ANRs and instrumentation lifecycle timeouts after emulator reboot. No performed result substitutes for API 24, real-provider, or SystemUI gesture validation.

Remaining runtime gaps:

- API 24 smoke is unperformed because no API 24 device or local emulator image is available;
- instrumentation invokes the retained registered predictive-back callback object, but an actual
  SystemUI edge gesture is unverified;
- hands-on widget picker/configuration with a real external provider, provider rendering after
  orientation, real-provider resize validation on API 24 and API 35, and abandoned-ID inspection
  remain unperformed;
- automatic neighbor reflow, snackbar undo, and other explicitly deferred features have no runtime
  claim.

No unperformed device result is implied by this document.
