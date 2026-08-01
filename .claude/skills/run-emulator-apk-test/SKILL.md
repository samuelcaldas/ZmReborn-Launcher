---
name: run-emulator-apk-test
description: Run the Zeam launcher APK in the Docker Android emulator, navigate every screen, and take screenshots. Use when asked to test, screenshot, or smoke-test the APK in an emulator.
---

# Zeam Android Emulator APK Test

Drives the Zeam APK inside `zeam-docker-emulator:android35` via ADB commands executed
through `docker exec`. Screenshots land in `/tmp/zeam-captures/` by default.

## Prerequisites

Container `zeam-runtime` must be running. Start it if needed:

```bash
env -u DOCKER_HOST docker --context docker-dev run -d --name zeam-runtime \
    --device /dev/kvm -p 5555:5555 -p 127.0.0.1:6080:6080 \
    --tmpfs /root/.android:exec,size=9g \
    -v /home/samuelcaldas/repos/zmreborn/tools/emulator-entrypoint.sh:/entrypoint.sh:ro \
    zeam-docker-emulator:android35
```

Wait for boot (BOOT COMPLETE in logs, ~3–5 min on first run). Open `http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=scale` on the Docker host for manual interaction. noVNC is loopback-only; raw VNC remains container-internal.

## Install APK

```bash
CONTAINER=zeam-runtime bash .claude/skills/run-emulator-apk-test/driver.sh install /path/to/app-debug.apk
```

The script copies the APK into the container then runs `adb install -r`.

## Screenshot all screens (agent path)

```bash
CONTAINER=zeam-runtime bash .claude/skills/run-emulator-apk-test/driver.sh screenshot_all
```

Captures in order: homescreen → app drawer → long-press menu → Add submenu →
wallpaper chooser → preferences root → workspace prefs → dock prefs.

Take a single manual screenshot any time:

```bash
CONTAINER=zeam-runtime bash .claude/skills/run-emulator-apk-test/driver.sh shot my-screen
```

## ADB session inside container

```bash
EXEC="env -u DOCKER_HOST docker --context docker-dev exec zeam-runtime"
$EXEC adb root                                        # enable root ADB (required for am start on non-exported activities)
$EXEC adb shell input tap X Y                         # tap
$EXEC adb shell input swipe X Y X Y 2000              # long-press (hold 2 s)
$EXEC adb shell input keyevent 3                      # HOME
$EXEC adb shell input keyevent 4                      # BACK
$EXEC adb exec-out screencap -p > /tmp/name.png       # screenshot to host
$EXEC adb shell uiautomator dump /data/local/tmp/u.xml && \
  $EXEC adb exec-out cat /data/local/tmp/u.xml        # UI hierarchy dump
```

## Known screen coordinates (pixel_3a 1080×1920, API 35)

| Target | Tap coords |
|---|---|
| App drawer button (dock) | `464 1820` |
| Long-press homescreen | `swipe 540 900 540 900 2000` |
| Preferences root — Workspace row | `540 252` |
| Preferences root — Applications grid | `540 420` |
| Preferences root — Action bindings | `540 588` |
| Preferences root — Dock | `540 756` |
| Long-press menu — Add | `540 792` |
| Long-press menu — Wallpaper | `540 961` |
| Long-press menu — Preferences | `540 1130` |

Preferences activity is **not exported** — launch it with:

```bash
$EXEC adb root
$EXEC adb shell am start -n org.zmreborn/.Preferences
```

## Gotchas

- `--allow-hidden-intents` flag does **not** exist on API 35 `am start`. Use `adb root` instead.
- Host ADB at `localhost:5555` shows `offline`. All ADB must go through `docker exec`.
- The emulator needs ≥7.4 GB for the userdata partition. Use `--tmpfs /root/.android:exec,size=9g` to serve this from RAM (requires ~9 GB free RAM).
- `disk.dataPartition.size` in `config.ini` is ignored — emulator enforces a 7372 MB minimum regardless of that flag.
- `cmd package set-home-activity org.zmreborn/.Launcher` reports success but does not survive a force-stop. Use the UI chooser instead.
- Swipe up from dock area triggers gesture navigation, not the app drawer. Tap the 4-square icon at `464 1820`.
