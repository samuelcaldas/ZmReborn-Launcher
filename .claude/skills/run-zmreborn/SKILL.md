---
name: run-zmreborn
description: build APK from current codebase, deploy to Android emulator, run ZM Reborn launcher for manual testing; use when asked to run the app, test on emulator, deploy APK, build and install, or take a screenshot of the running launcher
---

Builds the debug APK via Docker (`tools/build_apk.sh`), starts the `zeam-runtime` emulator container if needed, installs the APK, and launches the launcher for manual interaction. All paths relative to repo root.

Driver: `.claude/skills/run-zmreborn/driver.sh`

## Prerequisites

- Docker context `docker-dev` available
- Image `zeam-docker-dev:android35` present (build)
- Image `zeam-docker-emulator:android35` present (emulator)
- `/dev/kvm` accessible on host (emulator needs KVM)

## Run: deploy (agent path — full pipeline)

```bash
bash .claude/skills/run-zmreborn/driver.sh deploy
```

Runs: build → start container → install → launch → screenshot.
Screenshot lands at `/tmp/zeam-captures/launched.png`.
Takes ~3 min on cold container start (boot), ~30 s if already running.

## Run: individual steps

```bash
bash .claude/skills/run-zmreborn/driver.sh build            # build APK only
bash .claude/skills/run-zmreborn/driver.sh start            # start/wait for emulator
bash .claude/skills/run-zmreborn/driver.sh install          # install built APK
bash .claude/skills/run-zmreborn/driver.sh install /path/to/other.apk
bash .claude/skills/run-zmreborn/driver.sh launch           # force-stop + start Launcher
bash .claude/skills/run-zmreborn/driver.sh shot my-label    # screenshot to /tmp/zeam-captures/my-label.png
```

## Manual interaction after deploy

```bash
EXEC="env -u DOCKER_HOST docker --context docker-dev exec zeam-runtime"

$EXEC adb shell input tap X Y                        # tap
$EXEC adb shell input swipe X Y X Y 2000             # long-press (2 s)
$EXEC adb shell input keyevent 3                     # HOME
$EXEC adb shell input keyevent 4                     # BACK
$EXEC adb exec-out screencap -p > /tmp/shot.png      # screenshot to host
$EXEC adb shell am start -n org.zmreborn/.Preferences  # open Preferences (requires adb root first)
```

See `run-emulator-apk-test` for known screen coordinates and crash recovery.

## Gotchas

- **`docker cp` to `/data/local/tmp/` fails** — the driver copies the APK to container `/tmp/` first, then `adb install -r /tmp/zeam-test.apk`. Do not bypass this.
- **Black homescreen on first launch** — wallpaper access throws `RemoteException` before workspace data loads. Force-stop + restart via `launch` fixes it. The driver does this automatically.
- **`adb root` required before `install`** — without it, `/data/local/tmp` is not writable and streaming install silently fails.
- **Static container name `zeam-runtime`** — driver creates/reuses this name. If a container from a previous session is stopped, it restarts it rather than creating a duplicate. Run `docker --context docker-dev rm zeam-runtime` to reset state.
- **KVM required** — emulator refuses to start without `/dev/kvm`. Pass `--device /dev/kvm` as the driver does.
- **`localhost:5555` ADB shows offline** — all ADB must go through `docker exec zeam-runtime`. Host ADB cannot connect.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Docker image unavailable` | Build or load `zeam-docker-emulator:android35` into `docker-dev` context |
| `container name already in use` | `env -u DOCKER_HOST docker --context docker-dev rm zeam-runtime` then redeploy |
| Boot timeout after 5 min | Check `docker logs zeam-runtime` for emulator crash; KVM may not be available |
| `adb: failed to stat /tmp/zeam-test.apk` | Run `adb root` inside the container before install |
| Launcher shows black screen | Run `bash .claude/skills/run-zmreborn/driver.sh launch` to force-restart |
