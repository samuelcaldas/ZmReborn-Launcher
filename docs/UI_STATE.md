# ZM Reborn UI state reference

## Reading this document

This is a current working-tree reference for ZM Reborn 3.1.11-alpha. It separates:

- **Confirmed** — behavior, resources, tests, or structure present in the current source tree.
- **Planned or pending** — active reconstruction work, missing runtime evidence, or explicitly deferred coverage.

All source references point to current working-tree line ranges. [`docs/CHANGELOG.md`](CHANGELOG.md) is an evidence log, not implementation proof. No screenshots or current device results are claimed here.

## Snapshot and product identity

| Item | Current identity |
|---|---|
| Product | ZM Reborn 3.1.11-alpha reconstruction of Zeam Launcher 3.1.10. [`README.md:1-3`](../README.md#L1-L3), [`app/build.gradle:32-38`](../app/build.gradle#L32-L38) |
| Audience and job | Fast home launcher for workspace and dock navigation, app drawer access, shortcuts, widgets, folders, search, wallpaper, and preferences. Confirmed by the launcher surface and menu/action wiring in [`app/src/main/java/org/zmreborn/Launcher.java:90-101`](../app/src/main/java/org/zmreborn/Launcher.java#L90-L101), [`app/src/main/res/xml/preferences.xml:3-72`](../app/src/main/res/xml/preferences.xml#L3-L72), and the launcher layout. |
| Package/runtime | `org.zmreborn`; application process resolves to `org.zmreborn.core`; provider authority uses `${applicationId}.provider`. [`app/build.gradle:29-38`](../app/build.gradle#L29-L38), [`app/src/main/AndroidManifest.xml:27-49`](../app/src/main/AndroidManifest.xml#L27-L49) |
| Launcher role | `Launcher` is exported with `MAIN`, `HOME`, `MONKEY`, `DEFAULT`, and `LAUNCHER` intent categories. [`app/src/main/AndroidManifest.xml:29-36`](../app/src/main/AndroidManifest.xml#L29-L36) |
| Provenance | Reconstruction from raw JADX source in `origin/original_source`; original APK retained at [`docs/reference/zeam-launcher-3-1-10-en-android.apk`](reference/zeam-launcher-3-1-10-en-android.apk). [`README.md:1-3`](../README.md#L1-L3) |
| Working-tree caveat | This reference describes the implementation currently present in the working tree, including reconstructed and uncommitted behavior. It does not assert parity with the historical APK, a screenshot, or an attached device. |

## Palette, type, spacing, and timing tokens

### Palette

The semantic aliases are defined before the concrete ZM Reborn tokens in [`app/src/main/res/values/colors.xml:3-19`](../app/src/main/res/values/colors.xml#L3-L19).

| Token | Value | Current semantic use |
|---|---|---|
| `zm_reborn_slate` | `#ff121a21` | Window, grid, settings, and state-overlay background. |
| `zm_reborn_glass` | `#d9121a21` | Bubble/dock glass and Signal Rail track. |
| `zm_reborn_fog` | `#ffeaf0f3` | Primary light text and labels. |
| `zm_reborn_steel` | `#ffb8c2c8` | Secondary text, uncertain gesture state, and empty-workspace hint. |
| `zm_reborn_amber` | `#fff2b64a` | Gesture/snag callout, focused/pressed selectors, and active Signal Rail. |
| `zm_reborn_ember` | `#ffd95c4f` | Delete filter and widget-error color. |

Semantic aliases: `window_background` and `grid_dark_background` → slate; `bubble_dark_background` → glass; `delete_color_filter` and `appwidget_error_color` → ember; `snag_callout_color`, `gesture_color`, `preferences_default_general_selector_colour_pressed`, and `preferences_default_general_selector_colour_focused` → amber; `uncertain_gesture_color` → steel. [`app/src/main/res/values/colors.xml:3-12`](../app/src/main/res/values/colors.xml#L3-L12)

### Type and control spacing

- Workspace labels use `14sp`, fog text, end ellipsis, centered gravity, two lines, and 5dp horizontal padding. [`app/src/main/res/values/styles.xml:3-13`](../app/src/main/res/values/styles.xml#L3-L13)
- Portrait workspace icon treatment uses 4dp top padding, 3dp left/right margins, 13dp top margin, 8dp bottom margin, and 5dp drawable padding. Landscape uses 2dp top padding, 10dp left/right margins, and 3dp drawable padding. [`app/src/main/res/values/styles.xml:14-27`](../app/src/main/res/values/styles.xml#L14-L27)
- Search button padding is 10dp left/right, 7dp top, and 9dp bottom. [`app/src/main/res/values/styles.xml:28-32`](../app/src/main/res/values/styles.xml#L28-L32)
- Drawer `application_boxed_grid` labels use `13sp`, two lines, `end` ellipsis, and 5dp/2dp vertical/horizontal padding. [`app/src/main/res/layout-port/application_boxed_grid.xml:2`](../app/src/main/res/layout-port/application_boxed_grid.xml#L2)
- Settings title/summary labels use `16sp`/`13sp`; folder titles use `17sp`; widget/search and state surfaces add `14sp`/`16sp` labels. [`app/src/main/res/layout/settings_preference.xml:22-35`](../app/src/main/res/layout/settings_preference.xml#L22-L35), [`app/src/main/res/layout-port/user_folder.xml:2-4`](../app/src/main/res/layout-port/user_folder.xml#L2-L4), [`app/src/main/res/layout/widget_search.xml:22-45`](../app/src/main/res/layout/widget_search.xml#L22-L45), [`app/src/main/res/layout-port/launcher.xml:17-22`](../app/src/main/res/layout-port/launcher.xml#L17-L22)

### Dimensions

All values below are current resource tokens from [`app/src/main/res/values/dimens.xml:3-32`](../app/src/main/res/values/dimens.xml#L3-L32).

| Group | Values |
|---|---|
| Base spacing | `space_4` = `4dp`; `space_8` = `8dp`; `space_12` = `12dp`; `space_16` = `16dp`; `space_24` = `24dp`. |
| Touch and drawer minimums | `minimum_touch_target` = `48dp`; drawer minimum cell = `48dp × 64dp`. |
| Dock cells | Portrait = `53dp × 56dp`; landscape = `54dp × 58dp`. |
| Folder/settings controls | `folder_action_size` = `48dp`; `settings_row_height` = `48dp`. |
| Surface geometry | `panel_corner_radius` = `8dp`; `selector_corner_radius` = `6dp`. |
| Signal Rail and indicator | Rail `2dp`; active rail `3dp`; dots height `20dp`; rail inset `4dp`. |
| Drawer/folder/dialog | Drawer horizontal/vertical padding = `12dp`/`8dp`; folder spacing = `8dp`; dialog spacing = `16dp`. |
| Navigation | `navigation_strip_size` = `57dp`. |

### Timing

`duration_fast` = `120ms`, `duration_short` = `180ms`, `duration_medium` = `240ms`, and `duration_long` = `600ms`. [`app/src/main/res/values/integers.xml:3-6`](../app/src/main/res/values/integers.xml#L3-L6) `UiTokenContractTest` asserts the exact palette, dimensions, touch/rail contracts, and timings. [`app/src/test/java/org/zmreborn/UiTokenContractTest.java:16-55`](../app/src/test/java/org/zmreborn/UiTokenContractTest.java#L16-L55)

## Portrait and landscape shell geometry

The qualified launcher layouts share a full-screen `DragLayer`, seven declared workspace `CellLayout` includes, a match-parent application-drawer stub, home control, dock stub, delete target, indicator, empty-workspace tip, and drawer state overlay. [`app/src/main/res/layout-port/launcher.xml:2-24`](../app/src/main/res/layout-port/launcher.xml#L2-L24), [`app/src/main/res/layout-land/launcher.xml:2-24`](../app/src/main/res/layout-land/launcher.xml#L2-L24)

| Orientation | Shell arrangement | Dock resource |
|---|---|---|
| Portrait | Home button, dock, and delete target are bottom-oriented; indicator, empty tip, and overlay remain DragLayer children. [`app/src/main/res/layout-port/launcher.xml:12-24`](../app/src/main/res/layout-port/launcher.xml#L12-L24) | Bottom horizontal dock with `53dp × 56dp` cells and a `HorizontalScrollView`. [`app/src/main/res/layout-port/dockbar.xml:2-5`](../app/src/main/res/layout-port/dockbar.xml#L2-L5) |
| Landscape | Home button, dock, and delete target are right-oriented; indicator, empty tip, and overlay remain DragLayer children. [`app/src/main/res/layout-land/launcher.xml:12-24`](../app/src/main/res/layout-land/launcher.xml#L12-L24) | Right vertical dock with `54dp × 58dp` cells and a `ScrollView`. [`app/src/main/res/layout-land/dockbar.xml:2-5`](../app/src/main/res/layout-land/dockbar.xml#L2-L5) |

The drawer stub is match-parent and is inflated into either `apps_grid_view` or `apps_paging_view`; the overlay is match-parent, slate-backed, clickable, and focusable. [`app/src/main/res/layout-port/launcher.xml:12-24`](../app/src/main/res/layout-port/launcher.xml#L12-L24), [`app/src/main/java/org/zmreborn/Launcher.java:654-718`](../app/src/main/java/org/zmreborn/Launcher.java#L654-L718)

The layout declares `defaultScreen="2"` and seven possible included cells, but runtime screen count is not a fixed seven-screen contract. `Workspace` reads the configured count/default screen, and Preferences constrains number of screens and default screen to `1..7`. [`app/src/main/res/layout-port/launcher.xml:3-10`](../app/src/main/res/layout-port/launcher.xml#L3-L10), [`app/src/main/res/layout-land/launcher.xml:3-10`](../app/src/main/res/layout-land/launcher.xml#L3-L10), [`app/src/main/java/org/zmreborn/Workspace.java:118-151`](../app/src/main/java/org/zmreborn/Workspace.java#L118-L151), [`app/src/main/res/xml/preferences.xml:12-16`](../app/src/main/res/xml/preferences.xml#L12-L16), [`app/src/main/res/values/strings.xml:48-49`](../app/src/main/res/values/strings.xml#L48-L49)

## Core flows

### Launch, workspace, and wallpaper

Confirmed launch sequence: `Launcher.onCreate` initializes the widget host and display metrics, checks locale state, suggests wallpaper dimensions, inflates `R.layout.launcher`, calls `setupViews`, registers observers, restores state, and starts loaders. [`app/src/main/java/org/zmreborn/Launcher.java:194-220`](../app/src/main/java/org/zmreborn/Launcher.java#L194-L220) `setupViews` selects the drawer implementation, inflates the dock, binds the home/delete controls, wires `DragLayer`, `Workspace`, dock, and indicator. [`app/src/main/java/org/zmreborn/Launcher.java:654-718`](../app/src/main/java/org/zmreborn/Launcher.java#L654-L718) `startLoaders` starts application and workspace loading. [`app/src/main/java/org/zmreborn/Launcher.java:365-374`](../app/src/main/java/org/zmreborn/Launcher.java#L365-L374)

Wallpaper behavior is split between the launcher, workspace, and chooser:

- `Launcher.setWallpaperDimension` suggests a desired wallpaper size using the display orientation and a two-screen span. [`app/src/main/java/org/zmreborn/Launcher.java:486-491`](../app/src/main/java/org/zmreborn/Launcher.java#L486-L491)
- `Workspace.setWallpaper` handles live wallpapers, managed static wallpaper, API-denied drawable access, window background selection, invalidation, and relayout. `setDrawWallpaper` and `setScrollWallpaper` apply the corresponding Preferences choices. [`app/src/main/java/org/zmreborn/Workspace.java:1285-1323`](../app/src/main/java/org/zmreborn/Workspace.java#L1285-L1323)
- `WallpaperChooser` enumerates packaged wallpaper resources, previews thumbnails, enables/disables the set action, cancels stale thumbnail work, and calls `WallpaperManager.setResource`; I/O and security failures are logged. [`app/src/main/java/org/zmreborn/WallpaperChooser.java:42-128`](../app/src/main/java/org/zmreborn/WallpaperChooser.java#L42-L128)
- The chooser surface is slate-backed, has a gallery, an image preview, and a minimum-touch set button. [`app/src/main/res/layout/wallpaper_chooser.xml:2-40`](../app/src/main/res/layout/wallpaper_chooser.xml#L2-L40)

### Search

Search input can come from the search widget, the launcher search action, or keyboard handling. `Search` forwards ordinary clicks to `Launcher.onSearchRequested`, starts voice search when available, reports a voice-search-unavailable toast otherwise, and forwards key events to the launcher. [`app/src/main/java/org/zmreborn/Search.java:80-180`](../app/src/main/java/org/zmreborn/Search.java#L80-L180) `Launcher.startSearch` closes the drawer, uses the current-screen search widget when present, otherwise opens the system search dialog; it stops search through `SearchManager` and the widget on cancellation. [`app/src/main/java/org/zmreborn/Launcher.java:1375-1419`](../app/src/main/java/org/zmreborn/Launcher.java#L1375-L1419) The widget surface exposes a focusable text control and focusable voice button, each with a `48dp` minimum touch size. [`app/src/main/res/layout/widget_search.xml:11-45`](../app/src/main/res/layout/widget_search.xml#L11-L45)

### Screen indicator and Signal Rail

`Launcher.loadIndicator` selects no indicator, a bottom Signal Rail, or dots from Preferences, enables auto-hide, sets item count from the workspace, and indicates the current screen. [`app/src/main/java/org/zmreborn/Launcher.java:798-833`](../app/src/main/java/org/zmreborn/Launcher.java#L798-L833) `ScreenIndicator` owns the dots-or-rail choice, progress, visibility, auto-hide scheduling, and content description. [`app/src/main/java/org/zmreborn/ScreenIndicator.java:14-171`](../app/src/main/java/org/zmreborn/ScreenIndicator.java#L14-L171) `SignalRailView` is the integrated rail renderer: glass track, amber active segment, `2dp`/`3dp` thickness, and `4dp` inset. [`app/src/main/java/org/zmreborn/SignalRailView.java:19-108`](../app/src/main/java/org/zmreborn/SignalRailView.java#L19-L108)

The rail/dots indicator auto-hides after `duration_long` = `600ms`, reveals on movement, and publishes `accessibility_page_indicator` with current page and total pages. [`app/src/main/java/org/zmreborn/ScreenIndicator.java:88-171`](../app/src/main/java/org/zmreborn/ScreenIndicator.java#L88-L171), [`app/src/main/res/values/integers.xml:3-6`](../app/src/main/res/values/integers.xml#L3-L6)

### Dock and drag

Dock long press closes the drawer if needed, creates a `DockDragTransaction`, and starts a `DragLayer` drag. [`app/src/main/java/org/zmreborn/Dock.java:204-218`](../app/src/main/java/org/zmreborn/Dock.java#L204-L218) `DragLayer` captures the dragged view, routes motion to drop targets, arms edge scrolling after `600ms`, and dispatches accepted/rejected drops. [`app/src/main/java/org/zmreborn/DragLayer.java:94-166`](../app/src/main/java/org/zmreborn/DragLayer.java#L94-L166), [`app/src/main/java/org/zmreborn/DragLayer.java:244-408`](../app/src/main/java/org/zmreborn/DragLayer.java#L244-L408)

Dock acceptance, external insertion, dock-to-dock staging, item removal, reorder persistence, and completion handling are implemented in [`app/src/main/java/org/zmreborn/Dock.java:63-130`](../app/src/main/java/org/zmreborn/Dock.java#L63-L130), [`app/src/main/java/org/zmreborn/Dock.java:301-338`](../app/src/main/java/org/zmreborn/Dock.java#L301-L338), and [`app/src/main/java/org/zmreborn/Dock.java:342-401`](../app/src/main/java/org/zmreborn/Dock.java#L342-L401), with transaction and result-listener types in [`DockDragTransaction.java:3-59`](../app/src/main/java/org/zmreborn/DockDragTransaction.java#L3-L59) and [`DropResultListener.java:5-7`](../app/src/main/java/org/zmreborn/DropResultListener.java#L5-L7). Task **#10 Dock/DragLayer remains active**, so this code is confirmed implementation, not a claim that the task is complete.

Home action and drawer open/close are controlled by `Launcher.openApplicationsGrid`/`closeApplicationsGrid`. Opening hides the dock, locks the workspace, brings the drawer forward, updates the home-button description, hides the workspace indicator, and renders the current application state. Closing reverses those changes and hides the state overlay. [`app/src/main/java/org/zmreborn/Launcher.java:2618-2669`](../app/src/main/java/org/zmreborn/Launcher.java#L2618-L2669) `performAction` maps the configured home/swipe/double-tap actions, including drawer toggle and return-to-default-screen behavior. [`app/src/main/java/org/zmreborn/Launcher.java:2710-2773`](../app/src/main/java/org/zmreborn/Launcher.java#L2710-L2773)

## Application drawer: grid, paging, and load states

### Surface selection and geometry

`Launcher.setupViews` selects the vertical `ApplicationsGridView` for type `1`, horizontal `ApplicationsPagingView` for type `2`, and paging as the fallback. [`app/src/main/java/org/zmreborn/Launcher.java:654-671`](../app/src/main/java/org/zmreborn/Launcher.java#L654-L671) The grid surface is slate-backed, defaults to four columns in its portrait XML, and declares next-focus targets toward the drawer/home controls. [`app/src/main/res/layout-port/apps_grid_view.xml:2`](../app/src/main/res/layout-port/apps_grid_view.xml#L2) The paging surface contains a `ViewPager` and a paging indicator. [`app/src/main/res/layout-port/apps_paging_view.xml:2-5`](../app/src/main/res/layout-port/apps_paging_view.xml#L2-L5)

Grid behavior confirmed in [`ApplicationsGridView.java:208-270`](../app/src/main/java/org/zmreborn/ApplicationsGridView.java#L208-L270): loading disables actions and reports loading, application delivery installs an adapter, empty/error disable actions and report their states, and `clearState` re-enables the view and reports ready. Click launches an application or opens an app-list folder; long press starts a drawer drag or app-list-folder action. [`app/src/main/java/org/zmreborn/ApplicationsGridView.java:125-165`](../app/src/main/java/org/zmreborn/ApplicationsGridView.java#L125-L165)

Paging behavior confirmed in [`ApplicationsPagingView.java:88-165`](../app/src/main/java/org/zmreborn/ApplicationsPagingView.java#L88-L165): loading/empty/error/ready use the same state callbacks, measured width/height feed `DrawerLayoutMetrics`, and pages are created from rows and columns. Metrics use measured content bounds, padding, and `48dp` minimum cell dimensions. [`app/src/main/java/org/zmreborn/ApplicationsPagingView.java:167-176`](../app/src/main/java/org/zmreborn/ApplicationsPagingView.java#L167-L176) Page contents are partitioned by `rows × columns`; empty lists produce no pages and each non-empty page is a bounded sublist. [`app/src/main/java/org/zmreborn/ApplicationsPagingView.java:178-193`](../app/src/main/java/org/zmreborn/ApplicationsPagingView.java#L178-L193), [`app/src/main/java/org/zmreborn/ApplicationsPagePartition.java:7-28`](../app/src/main/java/org/zmreborn/ApplicationsPagePartition.java#L7-L28)

Drawer labels use `13sp`, two lines, and end ellipsis in the boxed grid/page layouts. [`app/src/main/res/layout-port/application_boxed_grid.xml:2`](../app/src/main/res/layout-port/application_boxed_grid.xml#L2), [`app/src/main/res/layout-port/application_boxed_page.xml:2`](../app/src/main/res/layout-port/application_boxed_page.xml#L2)

### Loading state machine and model generation

Launcher state constants are ready, loading, empty, and error. The shared state overlay is hidden when the drawer is closed or ready; loading and empty expose close; error exposes retry and close. [`app/src/main/java/org/zmreborn/Launcher.java:102-105`](../app/src/main/java/org/zmreborn/Launcher.java#L102-L105), [`app/src/main/java/org/zmreborn/Launcher.java:376-427`](../app/src/main/java/org/zmreborn/Launcher.java#L376-L427) Retry starts a fresh application load; close exits the overlay and restores the normal drawer mode. [`app/src/main/java/org/zmreborn/Launcher.java:677-685`](../app/src/main/java/org/zmreborn/Launcher.java#L677-L685), [`app/src/main/java/org/zmreborn/Launcher.java:2067-2082`](../app/src/main/java/org/zmreborn/Launcher.java#L2067-L2082) The overlay layout is the match-parent frame and centered message/retry/close stack. [`app/src/main/res/layout-port/launcher.xml:18-24`](../app/src/main/res/layout-port/launcher.xml#L18-L24)

`LauncherModel.loadApplications` starts a new generation, marks the view loading, cancels any previous task, optionally delivers a valid cache, queries `MAIN`/`LAUNCHER` activities through `PackageManager`, removes the launcher package, sorts labels, loads app-list-folder records, projects folders into the app list, and publishes only a current generation. [`app/src/main/java/org/zmreborn/LauncherModel.java:68-206`](../app/src/main/java/org/zmreborn/LauncherModel.java#L68-L206) `ApplicationsLoadGeneration` increments the generation and accepts only the current value. [`app/src/main/java/org/zmreborn/ApplicationsLoadGeneration.java:5-13`](../app/src/main/java/org/zmreborn/ApplicationsLoadGeneration.java#L5-L13)

Confirmed consequences:

- A fresh load cancels the old task before starting work.
- A stale result is ignored before view or cache replacement.
- A current null/error result enters error; a current empty result enters empty; a current non-empty result installs data and enters ready.
- Ready enables actions, hides the overlay, and requests first-item focus. [`app/src/main/java/org/zmreborn/Launcher.java:388-461`](../app/src/main/java/org/zmreborn/Launcher.java#L388-L461)
- Application discovery is constrained by the manifest package-visibility query for `MAIN`/`LAUNCHER`. [`app/src/main/AndroidManifest.xml:21-26`](../app/src/main/AndroidManifest.xml#L21-L26)

Open/close behavior is independent of the state result: opening brings the selected implementation forward and renders loading/empty/error/ready; closing hides the implementation, restores dock/workspace state, and clears the overlay. [`app/src/main/java/org/zmreborn/Launcher.java:2618-2669`](../app/src/main/java/org/zmreborn/Launcher.java#L2618-L2669)

## Folders

### Workspace folders

Workspace folders use `Folder`, `UserFolder`, and `FolderIcon`. `Folder` wires content clicks, long-press drag, rename, close, and accessibility descriptions for rename/close actions; closing requests focus on the current workspace cell. [`app/src/main/java/org/zmreborn/Folder.java:29-119`](../app/src/main/java/org/zmreborn/Folder.java#L29-L119) `UserFolder` accepts application/shortcut drops, adds dropped applications to its adapter, and persists the folder container through `LauncherModel.addOrMoveItemInDatabase`; opening requests focus. [`app/src/main/java/org/zmreborn/UserFolder.java:20-63`](../app/src/main/java/org/zmreborn/UserFolder.java#L20-L63) `FolderIcon` exposes the folder title as content description, switches open/closed icon during drag-over, and persists additions to the folder container. [`app/src/main/java/org/zmreborn/FolderIcon.java:25-71`](../app/src/main/java/org/zmreborn/FolderIcon.java#L25-L71)

Launcher folder clicks close/reopen as needed, inflate a `UserFolder`, bind the model, place it over the folder screen, and close the drawer. [`app/src/main/java/org/zmreborn/Launcher.java:1959-2031`](../app/src/main/java/org/zmreborn/Launcher.java#L1959-L2031) Folder interaction and accessibility exist, but task **#12 focus/folders polish is pending**; this reference does not call folders complete.

### App-list folders

App-list folders are drawer projections, not workspace favorites. Launcher provides open, create, rename, delete, and multi-select dialogs. [`app/src/main/java/org/zmreborn/Launcher.java:1215-1359`](../app/src/main/java/org/zmreborn/Launcher.java#L1215-L1359) `AppListFolderStore` persists folder metadata and component assignments through the provider, validates titles and component names, and replaces folder contents through the provider delete and bulk-insert boundary. [`app/src/main/java/org/zmreborn/AppListFolderStore.java:26-149`](../app/src/main/java/org/zmreborn/AppListFolderStore.java#L26-L149) `AppListFolderProjection` maps installed application components into folder tiles and leaves unassigned applications as ordinary drawer entries. [`app/src/main/java/org/zmreborn/AppListFolderProjection.java:16-49`](../app/src/main/java/org/zmreborn/AppListFolderProjection.java#L16-L49)

Grid and paging both open app-list folders on click and show folder actions on long press. [`app/src/main/java/org/zmreborn/ApplicationsGridView.java:125-165`](../app/src/main/java/org/zmreborn/ApplicationsGridView.java#L125-L165), [`app/src/main/java/org/zmreborn/ApplicationsPagingView.java:281-322`](../app/src/main/java/org/zmreborn/ApplicationsPagingView.java#L281-L322)

## Secondary surfaces

### Preferences

`Preferences.onCreate` inflates the preference XML, applies the slate settings surface, binds restart and loader-reload listeners, clamps default screen when screen count changes, configures application-grid row/column controls, wires dock controls, and exposes restart/reset actions. [`app/src/main/java/org/zmreborn/Preferences.java:23-129`](../app/src/main/java/org/zmreborn/Preferences.java#L23-L129) The XML contains General, Workspace, Applications Grid, Action Bindings, Dock, restart, reset, wallpaper controls, and workspace/application-grid rows and columns. [`app/src/main/res/xml/preferences.xml:3-72`](../app/src/main/res/xml/preferences.xml#L3-L72)

Preference changes persist through default `SharedPreferences`; selected language is persisted before restart, and the launcher reloads orientation/fullscreen, grid state, wallpaper, indicator, dock widths, and dock background on resume. [`app/src/main/java/org/zmreborn/PreferencesUtil.java:10-160`](../app/src/main/java/org/zmreborn/PreferencesUtil.java#L10-L160), [`app/src/main/java/org/zmreborn/Launcher.java:545-588`](../app/src/main/java/org/zmreborn/Launcher.java#L545-L588) Reset deletes the preferences file and restarts the process. [`app/src/main/java/org/zmreborn/Preferences.java:218-242`](../app/src/main/java/org/zmreborn/Preferences.java#L218-L242)

### Wallpaper chooser

The chooser contains a preview image, gallery, and set button, with slate background, `12dp` outer padding, `8dp` gallery/button spacing, and `48dp` minimum touch bounds. [`app/src/main/res/layout/wallpaper_chooser.xml:2-40`](../app/src/main/res/layout/wallpaper_chooser.xml#L2-L40) Selection and resource application are handled by `WallpaperChooser`. [`app/src/main/java/org/zmreborn/WallpaperChooser.java:42-128`](../app/src/main/java/org/zmreborn/WallpaperChooser.java#L42-L128)

### Widgets and widget errors

Widget add results route through `Launcher.onActivityResult`: shortcut, live-folder, app-widget, and application additions use their respective completion paths; canceled widget IDs are deleted. [`app/src/main/java/org/zmreborn/Launcher.java:494-543`](../app/src/main/java/org/zmreborn/Launcher.java#L494-L543) Widget span selection uses `widget_span`, ranges columns and rows against the current desktop grid, persists the widget, creates the host view, and adds it to the workspace; failed placement deletes the allocated widget ID. [`app/src/main/java/org/zmreborn/Launcher.java:931-965`](../app/src/main/java/org/zmreborn/Launcher.java#L931-L965), [`app/src/main/java/org/zmreborn/Launcher.java:2775-2794`](../app/src/main/java/org/zmreborn/Launcher.java#L2775-L2794), [`app/src/main/res/layout/widget_span.xml:2-57`](../app/src/main/res/layout/widget_span.xml#L2-L57) `appwidget_error` is an ember error label with minimum touch dimensions. [`app/src/main/res/layout/appwidget_error.xml:2-15`](../app/src/main/res/layout/appwidget_error.xml#L2-L15)

## Focus and accessibility

Confirmed content-description wiring covers the drawer toggle, delete zone, workspace shortcuts, application-grid workspace items, app-list drawer items, voice search, folder rename/close, and page indicator. [`app/src/main/java/org/zmreborn/Launcher.java:691-700`](../app/src/main/java/org/zmreborn/Launcher.java#L691-L700), [`app/src/main/java/org/zmreborn/Launcher.java:836-855`](../app/src/main/java/org/zmreborn/Launcher.java#L836-L855), [`app/src/main/java/org/zmreborn/Launcher.java:906-915`](../app/src/main/java/org/zmreborn/Launcher.java#L906-L915), [`app/src/main/java/org/zmreborn/ApplicationsAdapter.java:32-50`](../app/src/main/java/org/zmreborn/ApplicationsAdapter.java#L32-L50), [`app/src/main/java/org/zmreborn/Search.java:167-179`](../app/src/main/java/org/zmreborn/Search.java#L167-L179), [`app/src/main/java/org/zmreborn/Folder.java:36-51`](../app/src/main/java/org/zmreborn/Folder.java#L36-L51), [`app/src/main/java/org/zmreborn/ScreenIndicator.java:167-172`](../app/src/main/java/org/zmreborn/ScreenIndicator.java#L167-L172)

Ready drawer state calls `focusFirstApplicationsItem`: grid selects item zero and requests focus; other implementations locate the first actionable child, then fall back to the implementing view. [`app/src/main/java/org/zmreborn/Launcher.java:436-461`](../app/src/main/java/org/zmreborn/Launcher.java#L436-L461) The grid XML declares next-focus links toward the drawer controls. [`app/src/main/res/layout-port/apps_grid_view.xml:2`](../app/src/main/res/layout-port/apps_grid_view.xml#L2) `48dp` minimum touch targets are explicit in the token contract and widget/search/state surfaces. [`app/src/main/res/values/dimens.xml:13-21`](../app/src/main/res/values/dimens.xml#L13-L21), [`app/src/main/res/layout/widget_search.xml:22-45`](../app/src/main/res/layout/widget_search.xml#L22-L45), [`app/src/main/res/layout-port/launcher.xml:20-22`](../app/src/main/res/layout-port/launcher.xml#L20-L22)

Large-font behavior has only resource evidence: labels are expressed in `14sp`, `13sp`, `16sp`, and `17sp`, with the dimensions above. There is no current device proof of large-font rendering. Task **#12 focus/folders polish remains pending**.

## System bars and fullscreen across API 8–35

The compatibility target is explicit: `minSdkVersion 8`, `targetSdkVersion 35`, and `compileSdk 35`. [`app/build.gradle:29-38`](../app/build.gradle#L29-L38) The manifest routes `Launcher` through `@style/LauncherTheme` and preserves the launcher intent contract. [`app/src/main/AndroidManifest.xml:27-36`](../app/src/main/AndroidManifest.xml#L27-L36)

The base `LauncherTheme` inherits `Theme.Wallpaper.NoTitleBar`; the API 35 override sets `android:windowOptOutEdgeToEdgeEnforcement=true`. [`app/src/main/res/values/styles.xml:34-38`](../app/src/main/res/values/styles.xml#L34-L38), [`app/src/main/res/values-v35/styles.xml:3-5`](../app/src/main/res/values-v35/styles.xml#L3-L5) Launcher fullscreen is applied by setting/clearing `FLAG_FULLSCREEN`, saving the preference when requested, and reapplying on window focus. [`app/src/main/java/org/zmreborn/Launcher.java:230-264`](../app/src/main/java/org/zmreborn/Launcher.java#L230-L264), [`app/src/main/java/org/zmreborn/Launcher.java:2611-2615`](../app/src/main/java/org/zmreborn/Launcher.java#L2611-L2615)

`LauncherE2ETest` treats system-bar bounds as an API contract: API `<23` checks the visible display frame; API `>=23` uses root window insets; fullscreen transitions are checked off → on → off with flag and bounds assertions. [`app/src/androidTest/java/org/zmreborn/LauncherE2ETest.java:55-167`](../app/src/androidTest/java/org/zmreborn/LauncherE2ETest.java#L55-L167)

This is a code/resource/test compatibility contract, not current device smoke evidence. API 8 remains untested. Current API 10/API 35 device smoke is absent for this task even though historical API 10/API 35 results are recorded in older [`CHANGELOG.md:79-84`](CHANGELOG.md#L79-L84); the current rebrand entry records device validation as pending. [`CHANGELOG.md:12-22`](CHANGELOG.md#L12-L22) No current screenshots are available.

## Public resources and persistence constraints

- `PublicResourceContractTest` requires stable public resource names, types, IDs, layouts, IDs, colors, dimensions, arrays, strings, and styles. [`app/src/test/java/org/zmreborn/PublicResourceContractTest.java:17-35`](../app/src/test/java/org/zmreborn/PublicResourceContractTest.java#L17-L35), [`app/src/main/res/values/public.xml:3-425`](../app/src/main/res/values/public.xml#L3-L425)
- `LauncherProvider` creates `favorites`, `gestures`, and app-list-folder tables before loading or converting favorites. [`app/src/main/java/org/zmreborn/LauncherProvider.java:155-166`](../app/src/main/java/org/zmreborn/LauncherProvider.java#L155-L166) App-list folder schema is created separately for folder metadata and unique component assignments. [`app/src/main/java/org/zmreborn/LauncherProvider.java:318-323`](../app/src/main/java/org/zmreborn/LauncherProvider.java#L318-L323)
- `LauncherSettings.Favorites` exposes desktop/dock containers and item/widget types; `AppListFolders` exposes folder and folder-item URIs. [`app/src/main/java/org/zmreborn/LauncherSettings.java:27-74`](../app/src/main/java/org/zmreborn/LauncherSettings.java#L27-L74)
- `AppListFolderStore` is the app-list folder persistence boundary. [`app/src/main/java/org/zmreborn/AppListFolderStore.java:26-149`](../app/src/main/java/org/zmreborn/AppListFolderStore.java#L26-L149)
- `LauncherModel` database helpers insert, move, update, delete, and remove folder contents through the provider. [`app/src/main/java/org/zmreborn/LauncherModel.java:790-885`](../app/src/main/java/org/zmreborn/LauncherModel.java#L790-L885)
- Workspace favorites and dock changes persist through those model helpers; locale fingerprint state uses the launcher-private `launcher.preferences` file. [`app/src/main/java/org/zmreborn/Launcher.java:305-348`](../app/src/main/java/org/zmreborn/Launcher.java#L305-L348)
- Saved instance state keeps current screen, pending add geometry, open-folder IDs, drawer-open state during configuration changes, and pending folder rename data. [`app/src/main/java/org/zmreborn/Launcher.java:628-651`](../app/src/main/java/org/zmreborn/Launcher.java#L628-L651), [`app/src/main/java/org/zmreborn/Launcher.java:1080-1111`](../app/src/main/java/org/zmreborn/Launcher.java#L1080-L1111)
- Manifest package visibility includes the `MAIN`/`LAUNCHER` query needed by the drawer. [`app/src/main/AndroidManifest.xml:21-26`](../app/src/main/AndroidManifest.xml#L21-L26)
- README-sourced constraints: `minSdk` 8 is preserved and the app has no third-party runtime dependencies; JUnit 4 is test-only. [`README.md:84-88`](../README.md#L84-L88)

## Automated tests and latest recorded validation

### Focused JVM tests present

| Test | Scope | Current source |
|---|---|---|
| `ApplicationsLoadGenerationTest` | Newest generation wins; stale generation rejected. | [`ApplicationsLoadGenerationTest.java:8-18`](../app/src/test/java/org/zmreborn/ApplicationsLoadGenerationTest.java#L8-L18) |
| `ApplicationsPagePartitionTest` | Empty, exact-capacity, overflow, invalid dimensions, and bounded page ranges. | [`ApplicationsPagePartitionTest.java:7-35`](../app/src/test/java/org/zmreborn/ApplicationsPagePartitionTest.java#L7-L35) |
| `DrawerLayoutMetricsTest` | Positive measured cells and bounded requested grid. | [`DrawerLayoutMetricsTest.java:8-41`](../app/src/test/java/org/zmreborn/DrawerLayoutMetricsTest.java#L8-L41) |
| `FolderLayoutMetricsTest` | Bounded folder panel and empty/invalid geometry. | [`FolderLayoutMetricsTest.java:9-41`](../app/src/test/java/org/zmreborn/FolderLayoutMetricsTest.java#L9-L41) |
| `DockDragTransactionTest` | Reorder insertion, cancellation, rejection, and idempotent completion. | [`DockDragTransactionTest.java:9-80`](../app/src/test/java/org/zmreborn/DockDragTransactionTest.java#L9-L80) |
| `UiTokenContractTest` | Palette, dimensions, touch/rail, and timing contracts. | [`UiTokenContractTest.java:16-165`](../app/src/test/java/org/zmreborn/UiTokenContractTest.java#L16-L165) |
| `SystemBarResourceContractTest` | Manifest theme, base theme, and API 35 opt-out resource contract. | [`SystemBarResourceContractTest.java:13-81`](../app/src/test/java/org/zmreborn/SystemBarResourceContractTest.java#L13-L81) |
| `PublicResourceContractTest` | Stable public resource names/types/IDs. | [`PublicResourceContractTest.java:17-35`](../app/src/test/java/org/zmreborn/PublicResourceContractTest.java#L17-L35) |
| `SettingsResourceContractTest` | Preference keys, ranges, bindings, geometry, wallpaper, and widget contracts. | [`SettingsResourceContractTest.java:21-196`](../app/src/test/java/org/zmreborn/SettingsResourceContractTest.java#L21-L196) |

### Instrumentation tests present

- `ApplicationsDrawerE2ETest` checks drawer inflation, open/close round-trip, and shared state overlay. [`app/src/androidTest/java/org/zmreborn/ApplicationsDrawerE2ETest.java:5-46`](../app/src/androidTest/java/org/zmreborn/ApplicationsDrawerE2ETest.java#L5-L46)
- `LauncherE2ETest` checks launcher views, identity/provider resolution, drawer bounds, system-bar bounds, fullscreen transitions, and preference geometry. [`app/src/androidTest/java/org/zmreborn/LauncherE2ETest.java:12-167`](../app/src/androidTest/java/org/zmreborn/LauncherE2ETest.java#L12-L167)
- `PreferencesE2ETest` checks settings reachability, identity row, screen-range clamping, persisted default-screen correction, and positive row geometry. [`app/src/androidTest/java/org/zmreborn/PreferencesE2ETest.java:10-91`](../app/src/androidTest/java/org/zmreborn/PreferencesE2ETest.java#L10-L91)

### Latest recorded evidence, not this task's validation

- Focused Docker unit tests for drawer generation were recorded as passed; device validation remained pending. [`CHANGELOG.md:3-8`](CHANGELOG.md#L3-L8)
- The recorded ZM Reborn Docker rerun passed `assembleDebug`, `testDebugUnitTest`, `lint`, and `assembleDebugAndroidTest`; 34 JVM tests passed, and the lint baseline decreased from 430 to 324 findings with no issue-ID count increases. [`CHANGELOG.md:12-22`](CHANGELOG.md#L12-L22)
- System-bar static/resource/instrumentation contracts were recorded as passed, while on-device API 35 confirmation remained pending. [`CHANGELOG.md:24-31`](CHANGELOG.md#L24-L31)

This UI-state task did not run Gradle, assemble, APK, adb, or device validation. The user forbids APK builds; only Markdown validation is permitted after this edit.

## Status matrix

| Area | Status | Evidence | Caveat |
|---|---|---|---|
| Shell/resources | Implemented | Qualified launcher shell, dock orientations, palette, type, spacing, timing, and public resources. [`launcher.xml:2-24`](../app/src/main/res/layout-port/launcher.xml#L2-L24), [`colors.xml:3-19`](../app/src/main/res/values/colors.xml#L3-L19), [`dimens.xml:3-32`](../app/src/main/res/values/dimens.xml#L3-L32) | Resource/code confirmation only; no current screenshot. |
| Drawer states and generation | Implemented | Shared loading/ready/empty/error/retry overlay and generation freshness/cancellation. [`Launcher.java:376-461`](../app/src/main/java/org/zmreborn/Launcher.java#L376-L461), [`LauncherModel.java:68-206`](../app/src/main/java/org/zmreborn/LauncherModel.java#L68-L206) | Device population and visual behavior remain un-smoked here. |
| Basic folder interaction/storage | Implemented | Workspace folders, app-list folders, provider URIs, persistence, and projection. [`Folder.java:29-119`](../app/src/main/java/org/zmreborn/Folder.java#L29-L119), [`AppListFolderStore.java:26-149`](../app/src/main/java/org/zmreborn/AppListFolderStore.java#L26-L149) | Task #12 focus/folders polish is pending; do not treat this as complete UX coverage. |
| Preferences and secondary surfaces | Implemented | Preferences, wallpaper chooser, search widget, widget span/error, restart/reset wiring. [`Preferences.java:23-129`](../app/src/main/java/org/zmreborn/Preferences.java#L23-L129), [`preferences.xml:3-72`](../app/src/main/res/xml/preferences.xml#L3-L72) | Widget path still lacks manual exercise. |
| Resource/settings/system-bar contracts | Implemented | JVM contracts and launcher instrumentation assertions are present. [`UiTokenContractTest.java:16-55`](../app/src/test/java/org/zmreborn/UiTokenContractTest.java#L16-L55), [`SystemBarResourceContractTest.java:13-81`](../app/src/test/java/org/zmreborn/SystemBarResourceContractTest.java#L13-L81) | Recorded passes are historical evidence, not this task's run. |
| Dock/DragLayer | In progress | Current `DockDragTransaction` and `DropResultListener` code plus dock/drag routing. [`Dock.java:204-218`](../app/src/main/java/org/zmreborn/Dock.java#L204-L218), [`DragLayer.java:94-166`](../app/src/main/java/org/zmreborn/DragLayer.java#L94-L166) | Task #10 remains active/in progress. |
| Focus/folders polish | Pending | Existing focus and accessibility hooks. [`Launcher.java:436-461`](../app/src/main/java/org/zmreborn/Launcher.java#L436-L461), [`Folder.java:36-51`](../app/src/main/java/org/zmreborn/Folder.java#L36-L51) | Task #12 pending; no large-font device proof. |
| Broader coverage | Pending | Focused JVM and instrumentation tests exist. [`app/src/test/java/org/zmreborn`](../app/src/test/java/org/zmreborn), [`app/src/androidTest/java/org/zmreborn`](../app/src/androidTest/java/org/zmreborn) | Task #14 coverage remains pending. |
| API 10/API 35 smoke and screenshots | Pending | Historical changelog results exist, but current working-tree smoke and captures are absent. [`CHANGELOG.md:79-92`](CHANGELOG.md#L79-L92), [`CHANGELOG.md:151-157`](CHANGELOG.md#L151-L157) | No current device smoke or screenshots; do not fabricate either. |
| API 8 runtime | Pending | `minSdkVersion 8` and compatibility bridges/resources are present. [`app/build.gradle:29-38`](../app/build.gradle#L29-L38) | API 8 remains untested. |
| Lint findings | Pending | Recorded baseline moved from 430 to 324 with no issue-ID increases. [`CHANGELOG.md:12-22`](CHANGELOG.md#L12-L22) | 324 baseline findings remain; remaining full-lint findings are not closed here. |
| Widget manual path | Pending | Add/span/error code and resources exist. [`Launcher.java:931-965`](../app/src/main/java/org/zmreborn/Launcher.java#L931-L965), [`widget_span.xml:2-57`](../app/src/main/res/layout/widget_span.xml#L2-L57) | Manual widget add/bind path has not been exercised. |
| Locale rendering and RTL | Pending | English/pt-BR resources and persisted language selection exist. [`README.md:78-82`](../README.md#L78-L82) | No current locale-rendering device result; supported locales are LTR and RTL remains deferred as applicable. |

## Known gaps

- Task **#10 Dock/DragLayer** is active; current transaction/listener code is not a completion claim.
- Task **#12 focus/folders polish** is pending despite existing focus, descriptions, and folder interaction code.
- Task **#14 coverage** is pending; focused tests do not equal complete runtime coverage.
- No current API 10/API 35 device smoke or screenshots are available for this task.
- API 8 remains untested.
- Full lint still has 324 baseline findings; recorded validation reports no issue-ID increases, not zero findings.
- The widget add/bind path has no current manual exercise.
- Locale rendering has no current device result; RTL support remains deferred as applicable.
- No screenshots are fabricated or implied by this document.

## Event traces

Each trace distinguishes view/controller routing from model/provider persistence.

- **Launch/start:** system HOME/LAUNCHER intent → `LauncherApplication.onCreate` → `Launcher.onCreate` → `setupViews` → `startLoaders` → workspace/application views and model tasks. [`app/src/main/AndroidManifest.xml:29-36`](../app/src/main/AndroidManifest.xml#L29-L36) → [`app/src/main/java/org/zmreborn/LauncherApplication.java:16-24`](../app/src/main/java/org/zmreborn/LauncherApplication.java#L16-L24) → [`app/src/main/java/org/zmreborn/Launcher.java:194-220`](../app/src/main/java/org/zmreborn/Launcher.java#L194-L220) → [`app/src/main/java/org/zmreborn/Launcher.java:654-718`](../app/src/main/java/org/zmreborn/Launcher.java#L654-L718) → [`app/src/main/java/org/zmreborn/Launcher.java:365-374`](../app/src/main/java/org/zmreborn/Launcher.java#L365-L374)
- **Drawer open/tap/app launch:** home/action input → `Launcher.openApplicationsGrid`/`performAction` → selected grid or paging view → item click → `startActivitySafely`; app-list folder click diverts to folder dialog. [`app/src/main/java/org/zmreborn/Launcher.java:2618-2669`](../app/src/main/java/org/zmreborn/Launcher.java#L2618-L2669) → [`app/src/main/java/org/zmreborn/Launcher.java:2710-2773`](../app/src/main/java/org/zmreborn/Launcher.java#L2710-L2773) → [`app/src/main/java/org/zmreborn/ApplicationsGridView.java:125-165`](../app/src/main/java/org/zmreborn/ApplicationsGridView.java#L125-L165) / [`app/src/main/java/org/zmreborn/ApplicationsPagingView.java:296-322`](../app/src/main/java/org/zmreborn/ApplicationsPagingView.java#L296-L322) → [`app/src/main/java/org/zmreborn/Launcher.java:1978-1991`](../app/src/main/java/org/zmreborn/Launcher.java#L1978-L1991)
- **Drawer load/retry:** drawer open → view loading callback → generation start/cancel/query/sort/folder projection → view data delivery → ready/empty/error → retry starts a new generation. [`app/src/main/java/org/zmreborn/Launcher.java:376-427`](../app/src/main/java/org/zmreborn/Launcher.java#L376-L427) → [`app/src/main/java/org/zmreborn/LauncherModel.java:68-206`](../app/src/main/java/org/zmreborn/LauncherModel.java#L68-L206) → [`app/src/main/java/org/zmreborn/ApplicationsLoadGeneration.java:5-13`](../app/src/main/java/org/zmreborn/ApplicationsLoadGeneration.java#L5-L13) → [`app/src/main/java/org/zmreborn/Launcher.java:677-685`](../app/src/main/java/org/zmreborn/Launcher.java#L677-L685)
- **Workspace drag/drop:** long press workspace item → `Launcher.onLongClick` → `Workspace.startDrag` → `DragLayer.startDrag` → workspace drop target → `Workspace.onDrop` → `LauncherModel.moveItemInDatabase` or external add. [`app/src/main/java/org/zmreborn/Launcher.java:2039-2059`](../app/src/main/java/org/zmreborn/Launcher.java#L2039-L2059) → [`app/src/main/java/org/zmreborn/Workspace.java:882-892`](../app/src/main/java/org/zmreborn/Workspace.java#L882-L892) → [`app/src/main/java/org/zmreborn/DragLayer.java:94-166`](../app/src/main/java/org/zmreborn/DragLayer.java#L94-L166) → [`app/src/main/java/org/zmreborn/Workspace.java:924-987`](../app/src/main/java/org/zmreborn/Workspace.java#L924-L987) → provider persistence through [`app/src/main/java/org/zmreborn/LauncherModel.java:790-809`](../app/src/main/java/org/zmreborn/LauncherModel.java#L790-L809)
- **Dock drag/reorder:** long press dock item → `DockDragTransaction` → `DragLayer` motion/drop routing → dock accepts/stages/completes → reorder and update all dock positions in provider. [`app/src/main/java/org/zmreborn/Dock.java:204-218`](../app/src/main/java/org/zmreborn/Dock.java#L204-L218) → [`app/src/main/java/org/zmreborn/DragLayer.java:244-408`](../app/src/main/java/org/zmreborn/DragLayer.java#L244-L408) → [`app/src/main/java/org/zmreborn/Dock.java:63-130`](../app/src/main/java/org/zmreborn/Dock.java#L63-L130) → [`app/src/main/java/org/zmreborn/Dock.java:443-491`](../app/src/main/java/org/zmreborn/Dock.java#L443-L491) → [`app/src/main/java/org/zmreborn/LauncherModel.java:798-809`](../app/src/main/java/org/zmreborn/LauncherModel.java#L798-L809)
- **Folder open/add:** workspace folder tap → `Launcher.handleFolderClick`/`openFolder` → `UserFolder`/`Folder` focus and content → item click launches, long press drags, or `UserFolder.onDrop` adds and persists to the folder container. [`app/src/main/java/org/zmreborn/Launcher.java:1959-2031`](../app/src/main/java/org/zmreborn/Launcher.java#L1959-L2031) → [`app/src/main/java/org/zmreborn/Folder.java:29-119`](../app/src/main/java/org/zmreborn/Folder.java#L29-L119) → [`app/src/main/java/org/zmreborn/UserFolder.java:20-63`](../app/src/main/java/org/zmreborn/UserFolder.java#L20-L63) → [`app/src/main/java/org/zmreborn/LauncherModel.java:790-809`](../app/src/main/java/org/zmreborn/LauncherModel.java#L790-L809)
- **App-list folder open/add:** drawer folder tile tap → `ApplicationsGridView`/`ApplicationsPagingView` → `Launcher.openAppListFolder` or create/selection dialog → `AppListFolderStore` insert/replace → `LauncherModel.loadApplications` → projection back into drawer. [`app/src/main/java/org/zmreborn/ApplicationsGridView.java:125-165`](../app/src/main/java/org/zmreborn/ApplicationsGridView.java#L125-L165) → [`app/src/main/java/org/zmreborn/Launcher.java:1215-1359`](../app/src/main/java/org/zmreborn/Launcher.java#L1215-L1359) → [`app/src/main/java/org/zmreborn/AppListFolderStore.java:45-126`](../app/src/main/java/org/zmreborn/AppListFolderStore.java#L45-L126) → [`app/src/main/java/org/zmreborn/LauncherModel.java:142-180`](../app/src/main/java/org/zmreborn/LauncherModel.java#L142-L180)
- **Preferences change/restart:** settings input → `Preferences` listener → `SharedPreferences` value and restart/reload flag → launcher resume reloads preferences; explicit restart kills the process. [`app/src/main/java/org/zmreborn/Preferences.java:30-85`](../app/src/main/java/org/zmreborn/Preferences.java#L30-L85) → [`app/src/main/java/org/zmreborn/PreferencesUtil.java:10-160`](../app/src/main/java/org/zmreborn/PreferencesUtil.java#L10-L160) → [`app/src/main/java/org/zmreborn/Launcher.java:545-588`](../app/src/main/java/org/zmreborn/Launcher.java#L545-L588) → [`app/src/main/java/org/zmreborn/Preferences.java:224-237`](../app/src/main/java/org/zmreborn/Preferences.java#L224-L237)
- **Wallpaper:** wallpaper menu/chooser input → `WallpaperChooser` resource discovery and preview → `WallpaperManager.setResource` → launcher/workspace wallpaper reload and background selection. [`app/src/main/java/org/zmreborn/WallpaperChooser.java:42-128`](../app/src/main/java/org/zmreborn/WallpaperChooser.java#L42-L128) → [`app/src/main/java/org/zmreborn/Workspace.java:1285-1323`](../app/src/main/java/org/zmreborn/Workspace.java#L1285-L1323) → [`app/src/main/java/org/zmreborn/Launcher.java:2702-2708`](../app/src/main/java/org/zmreborn/Launcher.java#L2702-L2708)
- **Search:** search widget/button or launcher action → `Search` click/key handling → `Launcher.startSearch` → current-screen widget or `SearchManager.startSearch`; cancellation returns through `Launcher.stopSearch`. [`app/src/main/java/org/zmreborn/Search.java:80-180`](../app/src/main/java/org/zmreborn/Search.java#L80-L180) → [`app/src/main/java/org/zmreborn/Launcher.java:1375-1419`](../app/src/main/java/org/zmreborn/Launcher.java#L1375-L1419)
- **Screen indicator:** workspace scroll → `Workspace.computeScroll` progress/current page → `ScreenIndicator.indicate`/`fullIndicate` → dots or `SignalRailView`, auto-hide, content description. [`app/src/main/java/org/zmreborn/Workspace.java:376-403`](../app/src/main/java/org/zmreborn/Workspace.java#L376-L403) → [`app/src/main/java/org/zmreborn/ScreenIndicator.java:47-171`](../app/src/main/java/org/zmreborn/ScreenIndicator.java#L47-L171) → [`app/src/main/java/org/zmreborn/SignalRailView.java:19-108`](../app/src/main/java/org/zmreborn/SignalRailView.java#L19-L108)

## Source reference convention

Use the current working-tree line ranges above when checking behavior. Historical changelog entries can establish recorded validation evidence, but they do not replace implementation references or current device proof. The absence of screenshots, API 8 runtime evidence, current API 10/API 35 smoke, widget manual coverage, and locale-rendering results is intentional.
