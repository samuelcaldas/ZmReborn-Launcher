---
name: run-zmreborn
description: build APK from current codebase, deploy to Android emulator, open local noVNC browser interaction, test ZM Reborn manually, install APK, or capture emulator screenshot
---

Builds debug APK, reuses static `zeam-runtime`, installs current build, launches launcher, then opens local noVNC manual controls. Paths relative repo root. Browser URL: `http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=scale`.

Driver: `.claude/skills/run-zmreborn/driver.sh`

## One-time image update

Run after this skill changes emulator image. Rebuild adds Xvfb, x11vnc, noVNC, and websockify:

```bash
env -u DOCKER_HOST docker --context docker-dev build -f tools/Dockerfile.emulator -t zeam-docker-emulator:android35 .
```

Recreate static runtime once. This removes its volatile 9 GB tmpfs AVD state, then creates same fixed container name with loopback-only noVNC mapping:

```bash
bash .claude/skills/run-zmreborn/driver.sh recreate
```

## Run: build, deploy, interact

```bash
bash .claude/skills/run-zmreborn/driver.sh deploy
```

Open `http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=scale` in browser on Docker host. Click/tap Android emulator directly in noVNC. Screenshot from deploy lands at `/tmp/zeam-captures/launched.png`.

## Individual commands

```bash
bash .claude/skills/run-zmreborn/driver.sh build
bash .claude/skills/run-zmreborn/driver.sh start
bash .claude/skills/run-zmreborn/driver.sh install
bash .claude/skills/run-zmreborn/driver.sh launch
bash .claude/skills/run-zmreborn/driver.sh shot my-label
```

## ADB fallback

Use Docker-exec ADB for deterministic screenshots and diagnostics. Host ADB at `localhost:5555` is unreliable in this setup.

```bash
EXEC="env -u DOCKER_HOST docker --context docker-dev exec zeam-runtime"
$EXEC adb shell input tap 540 900
$EXEC adb shell input swipe 540 900 540 900 2000
$EXEC adb shell input keyevent 3
$EXEC adb shell input keyevent 4
$EXEC adb exec-out screencap -p > /tmp/zeam-captures/shot.png
```

## Security and lifecycle

- noVNC binds only to `127.0.0.1:6080`. Never change binding to `0.0.0.0` without authentication and TLS.
- Raw VNC stays container-internal on `localhost:5900`; no host VNC port exists.
- Driver reuses valid `zeam-runtime`; no resource-wasting duplicate containers.
- Docker cannot add a port map to existing container. Driver fails fast when it detects stale mapping/image; rebuild image then run `recreate`.
- `/dev/kvm` is preferred; driver falls back to slow software acceleration when unavailable. Runtime still needs 9 GB RAM for AVD tmpfs and `docker-dev` context.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `lacks local noVNC` or `uses stale emulator image` | Rebuild image, then run `bash .claude/skills/run-zmreborn/driver.sh recreate`. |
| Browser cannot connect | Check `docker logs zeam-runtime`; ensure host opens `127.0.0.1:6080`, not container IP. |
| Container boot timeout | Verify `/dev/kvm` access and 9 GB available RAM for `/root/.android` tmpfs. |
| APK install fails | Driver uses `/tmp/zeam-test.apk` after `adb root`; do not copy directly to `/data/local/tmp`. |
