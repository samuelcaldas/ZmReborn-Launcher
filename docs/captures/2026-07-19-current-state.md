# UI capture progress — 2026-07-19

## Source

- Revision: `cb2c24b` (`main`, geometry fix pending commit)
- Scope: app-list drawer rendering, paging bounds, label accessibility, workspace/app-list folder surfaces, settings continuity, and system-bar-safe drawer bounds.

## Device

- Runtime: Docker container `zeam-runtime`
- Emulator: `emulator-5554`
- API: 35 / Android 15
- Portrait display: 320 × 640 px
- Navigation bar: y=616..640
- Status bar: y=0..24
- Font scale: default

## Capture status

| Capture | Orientation | State | Status |
|---|---|---|---|
| Homescreen with dock and indicator | Portrait | Empty workspace | `2026-07-19-api35-portrait-homescreen.png` — captured |
| Homescreen with dock and indicator | Landscape | Normal workspace | Pending |
| App drawer | Portrait | Horizontal paging, before safe-area fix | `2026-07-19-api35-portrait-app-drawer.png` — defect evidence |
| App drawer | Portrait | Horizontal paging, after safe-area fix | `2026-07-19-api35-portrait-app-drawer-safe-area.png` — verified |
| App drawer | Landscape | Horizontal paging | Pending |
| App drawer | Portrait | Vertical grid | Pending preference selection |
| App drawer | Landscape | Vertical grid | Pending preference selection |
| Workspace folder | Portrait | Open, long title, action controls | Pending |
| App-list folder | Portrait | Creation/membership dialog | `2026-07-19-api35-portrait-app-folder-membership.png` — captured |
| App-list folder | Portrait | Tile with long title | `2026-07-19-api35-portrait-app-folder-tile.png` — captured |
| App-list folder | Portrait | Open contents dialog | `2026-07-19-api35-portrait-app-folder-open.png` — verified with Calendar |
| Preferences | Portrait | Root settings screen | `2026-07-19-api35-portrait-preferences-root.png` — verified |
| App drawer | Portrait | Large font scale | Pending |
| App drawer | Landscape | Rotation and changed rows/columns | Pending |
| New app-folder dialog | Portrait | Folder creation dialog | `2026-07-19-api35-portrait-new-app-folder-dialog.png` — captured |

The file `2026-07-19-api35-portrait-settings-invalid-launch-failed.png` is retained only as failed-attempt evidence. It is not a valid Preferences capture: direct external activity launch was denied because `org.zeam/.Preferences` is not exported, and the subsequent screenshot showed another surface. App-owned menu navigation now opens Preferences successfully; the valid root capture is recorded above.

## Safe-area verification

The initial drawer capture exposed two geometry defects under API 35 edge-to-edge rendering:

- First app row began at y=0 and rendered behind the status bar.
- Close control bounds were `[133,595][187,640]`, overlapping the navigation bar.

After applying visible-window safe-area insets to both drawer renderers and the close control, UI Automator reported:

- `view_pager`: `[0,24][320,576]`
- `Calendar`: `[3,24][81,162]`
- `Camera`: `[81,24][159,162]`
- `home_button`: `[133,571][187,616]`

All reported drawer descendants now remain above the status bar and navigation bar on this API 35 portrait device. The verified result is `2026-07-19-api35-portrait-app-drawer-safe-area.png`.

## Automated validation

- Docker API 35 instrumentation suite passed: 9 tests, including drawer inflation/open-close, launcher flows, and Preferences flows.
- Docker unit tests, lint, and debug build passed after safe-area and Preferences fixes.

## Remaining validation

- Capture landscape and vertical-grid variants.
- Reach Preferences through an app-owned navigation path and capture root/nested screens.
- Complete workspace-folder and app-list-folder open/edit/close captures.
- Repeat bounds checks after large-font, rotation, and row/column preference changes.
- Run API 10 smoke coverage after the geometry change where the Docker image remains available.
