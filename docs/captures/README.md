# ZM Reborn UI captures

This directory stores device captures used to track launcher UI progress.

## Naming

Use:

```text
<date>-api<level>-<orientation>-<surface>.png
```

Examples:

```text
2026-07-19-api35-portrait-homescreen.png
2026-07-19-api35-portrait-app-drawer-grid.png
2026-07-19-api35-landscape-app-drawer-paging.png
```

Keep captures tied to the source revision recorded in the matching progress note. Do not commit APKs, emulator snapshots, or build output here.

## Capture checklist

Capture each surface in portrait and landscape where applicable:

- Homescreen with dock and workspace indicator.
- Vertical app drawer.
- Horizontal paged app drawer.
- Workspace folder open, including long title and action controls.
- App-list folder open, including long title and action controls.
- Preferences root and each nested settings screen.
- Drawer open with large system font scale.
- Drawer after rotation and after changing row/column settings.

For each capture, verify no visible icon, label, action, folder control, indicator, or close control is clipped or outside the device bounds.

## Capture command

After installing a debug APK on a connected device:

```sh
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > docs/captures/<date>-api<level>-<orientation>-<surface>.png
```

Record device/API, orientation, font scale, drawer mode, relevant preferences, and source revision in a progress note.
