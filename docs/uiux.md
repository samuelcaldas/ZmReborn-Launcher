# Android Launcher Visual Refactoring — Historical UI Audit and Solution Proposal

> Historical baseline. All observations in section 1 describe audit-time behavior, not current
> implementation state. Implemented status and outstanding validation are tracked in
> [`UI_STATE.md`](UI_STATE.md) and [`CHANGELOG.md`](CHANGELOG.md).

## 1. Detailed baseline description of the interface

At audit time, launcher combined several generations of Android UI patterns without a unified design system. Modern application icons and a contemporary device resolution appeared inside an interface that still used visual conventions from Android 2.x–4.x, including Holo-style dialogs, square checkboxes, beveled buttons, fixed-size popups, legacy sliders, hard borders, and manually calculated grids.

This created both visual inconsistency and functional problems.

### Home screen

The home screen does not currently behave like a modern Android launcher:

* The persistent message instructing the user to long-press to add shortcuts and widgets occupies the workspace whenever it is empty. An empty home screen should be a valid user choice, not an error or incomplete state.
* The dock uses a dark opaque container, glossy icons, reflections, and an old app-drawer button. These elements visually conflict with modern Android and Material 3.
* The wallpaper is partially obscured by a large dark translucent rectangular region in one screenshot. This appears to be either a stale widget container, an edit-state overlay, or an incorrectly calculated widget boundary.
* A widget appears compressed into the upper-left portion of its allocated area instead of filling the selected grid span.
* Widget dimensions are selected through a modal that asks for a number of columns and rows. This exposes internal grid implementation details and is much less intuitive than resizing the widget directly.
* The home-page indicator is positioned near the top of the screen, using small circles and an orange capsule that do not match the rest of the UI.
* The bottom dock and home-screen content do not appear to account consistently for navigation-bar insets, gesture navigation, orientation, or varying screen proportions.
* The wallpaper crop and home-screen offset appear inconsistent between states.
* There is no stock-style gesture flow in which swiping up opens the app drawer.
* The launcher still depends on a dedicated app-drawer button, although the intended interaction is gesture-first.

### App drawer

The application grid shows several layout and rendering problems:

* Icon cells have excessive and inconsistent horizontal and vertical spacing.
* One screenshot displays only four rows with unusually large empty spaces between them, while another displays a much denser continuous list. This indicates that grid preferences or screen metrics are being applied inconsistently.
* The fixed four-column configuration does not use the available screen width efficiently.
* Icon size, label size, cell height, and row spacing are not derived from the same layout system.
* Labels wrap inconsistently. Some use two or three lines, while others are truncated with an ellipsis.
* Long labels such as “Carteira de Trabalho Dig...” do not follow the same wrapping policy as the other applications.
* The Chrome label is visible while its icon is missing, suggesting an icon-loading, recycling, caching, or asynchronous rendering defect.
* Some icons have circular masks, others use rounded rectangles, others remain square, and some contain their own background. The launcher is not respecting the device’s adaptive-icon mask.
* The current icon normalization produces a visually uneven grid because foreground artwork appears at different scales.
* The grid does not provide a modern search surface, alphabetical navigation, or a clearly defined initial scroll state.
* When the vertical application grid is scrolled completely to the top, the applications disappear. This strongly suggests a stale translation, alpha, clipping, overscroll, recycling, or layout-state bug.
* The drawer is visually indistinguishable from a basic scrolling activity rather than an integrated launcher surface.

### Long-press and edit menus

The long-press interface is visibly inconsistent:

* “Add”, “Wallpaper”, and “Preferences” are presented as equivalent actions, but “Add” uses a different icon treatment and visual hierarchy.
* The popup uses a white legacy rectangular surface over a dark launcher, without Material 3 elevation, shape, color, spacing, or motion.
* Icons are taken from different visual families.
* The menu uses fixed dimensions instead of adapting to the screen width.
* Dividers, padding, typography, and touch-target dimensions do not match the settings interface or the home screen.
* The interaction does not communicate whether the launcher is entering edit mode, opening a separate activity, or performing an immediate action.

### Settings interface

The settings screens contain the most obvious legacy UI elements:

* Holo-style square checkboxes with green checkmarks.
* Android 2.x–4.x-style sliders with yellow fill, grey tracks, and metallic thumbs.
* Thick rectangular borders around every preference row.
* Orange section headings that are disconnected from the launcher wallpaper and theme.
* Low-contrast secondary text.
* Fixed-height rows that do not adapt to text scaling.
* Titles and preference rows partially outside the viewport.
* Content rendered under or beyond screen boundaries.
* Dialogs with hard square corners, thick outlines, old spinner arrows, and desktop-like “OK” and “Cancel” buttons.
* Numeric grid configuration is exposed through rows and columns instead of understandable visual density options.
* Boolean settings use checkboxes even when switches would better represent persistent on/off behavior.
* Some values are shown as summaries but are not actually persisted.
* Changed settings are not restored after navigation, process recreation, or launcher restart.
* The settings screen appears to use fixed pixel dimensions or assumptions based on much older display sizes.
* Portrait and landscape settings are mixed together without explaining when they apply.
* Several controls are redundant. For example, a continuously scrolling vertical drawer should not require a fixed “number of rows” setting.

### Theme and visual identity

The launcher has no coherent appearance model:

* The launcher surface is dark, but dialogs and menus switch abruptly between light and dark legacy themes.
* There is no complete light theme.
* There is no complete dark theme.
* There is no wallpaper-derived dynamic color mode.
* Colors are hard-coded rather than derived from semantic design tokens.
* Icon labels, surfaces, borders, sliders, dialogs, and dock elements use unrelated color systems.
* Wallpaper contrast is not considered when rendering labels, indicators, or the dock.
* The launcher does not appear to respond to the system’s light/dark preference.

Material 3 supports structured color schemes, typography, shapes, component states, and wallpaper-based dynamic color. Dynamic color is available on Android 12 and later and should have a custom fallback palette for earlier Android versions. ([Android Developers][1])

---

# 2. Proposed solution

## 2.1 Refactoring strategy

Do not rewrite the entire launcher engine solely to modernize its appearance. Preserve stable components such as application discovery, activity launching, widget hosting, and workspace persistence where possible.

Refactor the project into clearly separated layers:

```text
Launcher domain
├── Installed applications
├── Workspace pages and items
├── Dock items
├── Widgets
├── Gesture mappings
└── Launcher settings

Data layer
├── Settings repository
├── Workspace repository
├── Icon repository and cache
└── Wallpaper repository

UI layer
├── Home workspace
├── Dock
├── App drawer
├── Edit mode
├── Widget resize overlay
└── Settings
```

The UI should observe immutable launcher state rather than directly modifying views and static variables.

For a legacy View-based project, screen-by-screen migration is preferable:

* Use Material Components or Compose Material 3 for settings, dialogs, bottom sheets, and onboarding.
* Preserve existing `AppWidgetHostView` integration.
* Keep the workspace in Views initially if its drag-and-drop implementation is stable.
* Replace custom app-drawer scrolling with a standard `RecyclerView` grid or `LazyVerticalGrid`.
* Remove legacy Holo resources, custom bevels, gradient buttons, old selectors, and fixed-pixel layouts.

## 2.2 Reliable settings persistence

Simple launcher settings should be stored through a single settings repository backed by Preferences DataStore. Structured workspace data should use Room or another transactional store rather than a large collection of unrelated preference keys.

DataStore is the modern Android storage mechanism for persistent key-value preferences and supports asynchronous, consistent updates. ([Android Developers][2])

A suggested model is:

```kotlin
data class LauncherSettings(
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val appDrawerLayout: AppDrawerLayout,
    val appDrawerColumns: GridColumnMode,
    val showAppLabels: Boolean,
    val showDock: Boolean,
    val showPageIndicator: Boolean,
    val wallpaperMode: WallpaperMode,
    val swipeUpAction: GestureAction,
    val swipeDownAction: GestureAction,
    val doubleTapAction: GestureAction,
    val enableElasticOverscroll: Boolean
)
```

Every setting must:

1. Load from persistent storage on startup.
2. Update immediately when changed.
3. Be exposed as a `Flow` or `StateFlow`.
4. Survive activity recreation, process termination, launcher restart, and device reboot.
5. Have a defined default value.
6. Support migration from the legacy preference format.

The UI must never keep a separate unsaved copy of a checkbox, slider, or numeric value.

## 2.3 Material 3 design system

Create a centralized launcher theme with semantic tokens instead of hard-coded visual values:

* Primary, secondary, tertiary and error colors.
* Surface, surface container, outline and inverse colors.
* Material 3 typography roles.
* Consistent corner-radius tokens.
* Standard elevation and state layers.
* Shared spacing values.
* Standard icon sizes and touch targets.

Theme configuration should use two independent concepts:

### Brightness mode

* Follow system
* Light
* Dark

### Color source

* Dynamic wallpaper colors
* Launcher fallback palette

This structure is preferable to treating “Dynamic” as a third brightness mode because dynamic color can operate in both light and dark appearances.

On Android 12 or newer, use the device’s dynamic color scheme. On earlier versions, use a carefully designed fallback scheme. ([Android Developers][1])

## 2.4 Home-screen redesign

The home screen should use the following hierarchy:

```text
System status-bar area
Workspace pages
Optional page indicator
Dock
System gesture/navigation area
```

Recommended changes:

* Remove the permanent empty-workspace message.
* Show an onboarding hint only once, such as a dismissible snackbar or temporary coach mark.
* Store a `hasSeenWorkspaceHint` value.
* An intentionally empty workspace must remain visually empty.
* Long-pressing empty space should enter edit mode with haptic feedback.
* Move page indicators near the dock.
* Hide page indicators when only one page exists.
* Fade indicators after page navigation.
* Remove icon reflections, glossy effects, bevels, and heavy dock shadows.
* Use either a transparent dock or a subtle tonal surface with a wallpaper-aware scrim.
* Remove the app-drawer button by default.
* Provide an optional accessibility setting to restore the app-drawer button.
* Ensure that stale edit overlays or widget selection surfaces are always removed when edit mode ends.
* Preserve workspace state across orientation and process recreation.

The default first-run dock should contain resolved system handlers rather than hard-coded package names:

* Phone
* Messages
* Browser
* Camera
* Contacts

The launcher should resolve the user’s installed default application for each function. These defaults should be inserted only during initial setup. If the user removes one, the launcher must not silently recreate it.

## 2.5 Adaptive icon rendering

The launcher should load and preserve `AdaptiveIconDrawable` instances rather than flattening every icon into a custom square or circle.

Adaptive icons are specifically designed to use device-dependent masks and can respond to theming capabilities. Their displayed shape may vary between Samsung One UI, Pixel Launcher, tablets, foldables, and other Android environments. ([Android Developers][3])

Required behavior:

* Respect the system or device adaptive-icon mask.
* Preserve foreground and background layers.
* Support monochrome/themed icons when available.
* Do not add an additional launcher-specific border around adaptive icons.
* Normalize only legacy bitmap icons.
* Scale legacy icons conservatively so their artwork visually matches adaptive icons.
* Use a theme-aware backing plate only when a legacy icon requires one.
* Cache icons using the application component and user/profile as the key.
* Invalidate the cache when packages, themes, density, or icon packs change.
* Never display the label without either the icon or a deterministic fallback icon.

## 2.6 App drawer redesign

The default app drawer should be a vertically scrolling surface opened by swiping upward.

Recommended structure:

```text
Search field
Optional suggested/recent applications
Alphabetical application grid
Optional alphabetical fast-scroll indicator
```

### Grid behavior

The number of columns must be derived from the available width rather than fixed for every device.

Suggested defaults:

* Compact phone width: 4 or 5 columns.
* Medium width or unfolded device: 5 or 6 columns.
* Tablet or expanded window: 6 to 8 columns, with centered maximum content width.

Use adaptive window-size information rather than device-name checks. Android window-size classes categorize the available viewport and are intended for responsive layouts across phones, tablets, foldables, multi-window environments, and external displays. ([Android Developers][4])

For a vertical drawer:

* Remove the “number of rows” setting.
* Calculate rows naturally through scrolling.
* Allow `Automatic`, `Comfortable`, `Default`, and `Compact` density presets.
* Provide an advanced column override only when necessary.
* Use consistent cell height.
* Center icons horizontally.
* Keep label spacing independent from icon artwork.
* Use a maximum of two label lines.
* Apply the same ellipsis policy to every application.
* Use stable keys based on component name and user profile.

### Fix for applications disappearing at the top

Remove custom scroll translation and overscroll logic temporarily. Reimplement the drawer using one authoritative scroll container.

Specifically:

* Clamp scroll offset to valid bounds.
* Reset `translationY`, scale and alpha when a gesture is cancelled or completed.
* Never persist an overscroll translation as drawer state.
* Avoid nesting multiple vertically scrolling containers.
* Use immutable app lists.
* Use stable item IDs.
* Disable change animations while diagnosing icon disappearance.
* Restore scroll position only after the grid has received its application list.
* Ensure that the top scrim and search bar do not clip grid children.
* Test scrolling rapidly to the top after package installation, removal, rotation, search clearing and theme changes.

## 2.7 Widget placement and resizing

The numeric “columns and rows” widget-size dialog should be removed.

Android widgets are expected to support direct resize interaction using drag handles or corners when their provider allows resizing. ([Android Developers][5])

The new interaction should be:

1. Long-press a widget.
2. Display a subtle selection outline.
3. Show resize handles only on supported axes.
4. Drag an edge or corner.
5. Snap the widget to valid workspace cells.
6. Show its current span as temporary feedback, such as `2 × 3`.
7. Commit the new size when released.
8. Offer undo through a snackbar.

The launcher must read:

* Minimum widget width and height.
* Minimum resize width and height.
* Resize mode.
* Target cell dimensions when available.
* Provider-supported portrait and landscape behavior.

After resizing or changing orientation, recalculate the actual widget bounds and update its options using dimensions derived from the measured workspace. Do not calculate widget size before the workspace has a valid width, height, density and inset configuration.

The launcher should also:

* Prevent overlap unless free-form overlap is explicitly supported.
* Show invalid placement using a clear error state.
* Automatically move neighboring items only when the user has enabled reflow.
* Preserve the widget ID and placement through process recreation.
* Delete abandoned widget IDs when configuration is cancelled.

## 2.8 Gestures

Default gesture mappings should approximate stock Android launcher behavior:

| Gesture                     | Default action                    |
| --------------------------- | --------------------------------- |
| Swipe up on home            | Open app drawer                   |
| Swipe down on home          | Open notifications                |
| Long-press empty space      | Enter edit mode                   |
| Swipe horizontally          | Change home page                  |
| Tap Home while already home | Return to primary page            |
| Double-tap                  | Disabled by default; configurable |
| Pinch                       | Optional overview/edit mode       |

Gesture actions should be configurable from settings.

The gesture recognizer must distinguish between:

* Workspace page navigation.
* Opening the app drawer.
* Opening notifications.
* Scrolling inside widgets.
* Dragging icons or widgets.
* Resizing widgets.
* Interacting with an app widget.

Use distance and velocity thresholds and cancel launcher gestures when an interactive widget has consumed the touch sequence.

Opening the notification shade is subject to Android platform restrictions. A privileged/system launcher can use system capabilities, while an ordinary third-party launcher may require an explicitly enabled accessibility service and `GLOBAL_ACTION_NOTIFICATIONS`. The launcher must not rely on hidden or reflection-based status-bar APIs. ([Android Developers][6])

## 2.9 Wallpaper management

The system wallpaper should be the default source.

The launcher should:

* Render the current system wallpaper behind the workspace.
* Open the system wallpaper picker for wallpaper changes.
* Support static and live wallpapers.
* Listen for wallpaper color changes.
* Recalculate dynamic colors when the wallpaper changes.
* Respect system wallpaper scrolling behavior.
* Avoid maintaining a duplicate internal wallpaper unless the user explicitly enables fallback mode.

`WallpaperManager` provides the platform APIs and intents for reading, changing, and selecting system wallpapers. ([Android Developers][7])

Suggested configuration:

```text
Wallpaper source
● System wallpaper — default
○ Launcher-managed fallback
```

The launcher-managed mode should remain available for devices where system wallpaper hosting is unsupported or unreliable.

## 2.10 Settings redesign

Replace the current bordered preference screen with a Material 3 settings hierarchy.

### Recommended structure

```text
Appearance
  Theme
  Dynamic color
  Icon labels
  App icon size
  Wallpaper source

Home screen
  Grid density
  Dock
  Page indicator
  Home-screen rotation
  Empty-space behavior

App drawer
  Vertical or paged layout
  Grid density
  Search field
  Application labels
  Sorting

Gestures
  Swipe up
  Swipe down
  Double-tap
  Pinch
  Long-press

Widgets
  Automatic placement
  Reflow items
  Widget padding

Advanced
  Reset layout
  Export configuration
  Import configuration
  Launcher-managed wallpaper
```

Control selection should follow component semantics:

* Use switches for immediate boolean states.
* Use radio-button dialogs or selection bottom sheets for mutually exclusive values.
* Use sliders only for genuinely continuous values such as icon scale.
* Use segmented buttons for a small number of visible choices.
* Use density presets rather than raw row and column counts.
* Show the active value as secondary text.
* Save changes immediately.
* Use confirmation only for destructive operations such as resetting the workspace.

AndroidX Preference can provide consistent settings behavior in View-based applications, while Compose Material 3 can provide a fully customized modern settings surface. ([Android Developers][8])

## 2.11 Long-press edit interface

Replace the legacy popup with a Material 3 modal bottom sheet on phones.

Use equal visual treatment for the available actions:

```text
Widgets
Wallpaper and style
Home settings
```

Each action should use:

* A consistent Material icon.
* The same icon container.
* The same row height.
* The same typography.
* A minimum 48 dp touch target.
* A concise supporting description when needed.

On tablets and large screens, use a centered dialog or side sheet rather than stretching a phone bottom sheet across the full width.

## 2.12 Viewport, insets and large-screen behavior

Remove all fixed screen-width, screen-height and raw-pixel assumptions.

Layouts must account for:

* Status bar.
* Gesture navigation.
* Three-button navigation.
* Display cutouts.
* Rounded corners.
* Landscape orientation.
* Split-screen mode.
* Tablets.
* Foldables.
* Font scaling.
* Display scaling.
* Keyboard and search input.

Modern Android layouts are expected to operate edge-to-edge while correctly applying system-bar and safe-area insets to interactive content. ([Android Developers][9])

All dialogs should use:

* Responsive maximum width.
* Scrollable content when required.
* Insets around system bars.
* No fixed vertical height.
* Correct behavior at increased font scale.

## 2.13 Accessibility and usability

The refactored launcher should include:

* Minimum 48 dp interactive touch areas.
* TalkBack descriptions for icons, gestures and resize handles.
* Keyboard and switch-access navigation.
* Visible focus states.
* Support for large fonts without clipping.
* Sufficient contrast over bright and dark wallpapers.
* Haptic feedback for successful placement and grid snapping.
* Reduced-motion behavior when system animation scale is disabled.
* No information communicated by color alone.
* Predictable back behavior from the app drawer, settings and edit mode.

## 2.14 Validation criteria

The refactoring should not be considered complete until the following conditions pass:

### Persistence

* Every setting remains correct after closing and reopening settings.
* Settings survive process termination.
* Workspace state survives reboot.
* Widget positions and sizes survive orientation changes.
* Legacy settings migrate once without being overwritten.

### Home screen

* An empty home screen shows no permanent instructional text.
* Swipe up opens the app drawer.
* The app-drawer button is absent by default.
* The dock contains resolved default applications on first run.
* Page indicators appear only when relevant.
* No stale dark overlay remains after leaving edit mode.

### Application drawer

* No icon disappears during rapid scrolling.
* Every application always has an icon or fallback.
* Labels follow one wrapping policy.
* Column count adapts to viewport width.
* Grid spacing remains consistent in portrait and landscape.
* Search and clearing search preserve a valid scroll state.

### Widgets

* Widgets initially occupy the correct span.
* Resizable widgets expose edge or corner handles.
* Non-resizable widgets expose no invalid handles.
* Resizing updates provider options.
* Widgets never render outside the workspace viewport.
* Cancelled widget placement releases its widget ID.

### Visual system

* Light, dark and dynamic themes are complete.
* No Holo sliders, checkboxes, spinner dialogs or beveled buttons remain.
* Preference rows do not have unnecessary borders.
* All surfaces use the same shape, spacing and color token system.
* System insets are respected on phones, tablets and foldables.

### Test matrix

At minimum, validate:

* A compact 360 dp-wide phone.
* A modern Samsung-sized phone.
* Landscape phone orientation.
* A 600 dp tablet.
* An expanded tablet or foldable window.
* Gesture navigation.
* Three-button navigation.
* Light theme.
* Dark theme.
* Dynamic wallpaper colors.
* Font scales of 100%, 130% and 200%.
* Empty, partially populated and heavily populated workspaces.

The final result should feel like a native contemporary Android launcher rather than a legacy launcher with modern colors applied on top. The priority is not only visual modernization, but also predictable state, responsive geometry, correct widget hosting, adaptive icon rendering and stock-like launcher interaction.

[1]: https://developer.android.com/develop/ui/views/theming/dynamic-colors?utm_source=chatgpt.com "Enable users to personalize their color experience in your ..."
[2]: https://developer.android.com/topic/libraries/architecture/datastore?utm_source=chatgpt.com "App Architecture: Data Layer - DataStore"
[3]: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive?utm_source=chatgpt.com "Adaptive icons | Jetpack Compose"
[4]: https://developer.android.com/develop/ui/views/layout/use-window-size-classes?utm_source=chatgpt.com "Use window size classes | Views"
[5]: https://developer.android.com/develop/ui/views/appwidgets/overview?utm_source=chatgpt.com "App widgets overview  |  Views  |  Android Developers"
[6]: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService?utm_source=chatgpt.com "AccessibilityService | API reference"
[7]: https://developer.android.com/reference/android/app/WallpaperManager.html?utm_source=chatgpt.com "WallpaperManager  |  API reference  |  Android Developers"
[8]: https://developer.android.com/develop/ui/views/components/settings/use-saved-values?utm_source=chatgpt.com "Use saved Preference values | Views"
[9]: https://developer.android.com/design/ui/mobile/guides/foundations/system-bars?utm_source=chatgpt.com "Android system bars  |  Mobile  |  Android Developers"
