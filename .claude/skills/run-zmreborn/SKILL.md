---
name: run-zmreborn
description: build, deploy, manually inspect, or run KVM-backed API 35/API 36 Android instrumentation for ZM Reborn in Docker
---

Uses `.claude/skills/run-zmreborn/driver.sh` for current APK deployment, noVNC interaction, screenshots, and full Docker instrumentation. Paths are repository-relative. API 35 remains default; `API_LEVEL=36` selects isolated Android 16 names, ports, and output paths.

## Build emulator images

API 35 default:

```bash
env -u DOCKER_HOST docker --context docker-dev build \
  -f tools/Dockerfile.emulator \
  -t zeam-docker-emulator:android35 \
  .
```

Android 16/API 36 runtime image:

```bash
env -u DOCKER_HOST docker --context docker-dev build \
  -f tools/Dockerfile.emulator \
  --build-arg API_LEVEL=36 \
  --build-arg SYSTEM_IMAGE='system-images;android-36;google_apis;x86_64' \
  -t zeam-docker-emulator:android36 \
  .
```

Both builds use local `zeam-docker-dev:android35` by default. Override `BASE_IMAGE` only when compatible SDK command-line tools and emulator binaries are already available. Image build fails when requested system image is unavailable.

## Full instrumentation

Automated tests require usable `/dev/kvm`; software acceleration is manual-only evidence.

```bash
KVM_DEVICE=/dev/kvm \
  bash .claude/skills/run-zmreborn/driver.sh test

API_LEVEL=36 KVM_DEVICE=/dev/kvm \
  bash .claude/skills/run-zmreborn/driver.sh test
```

`test` validates Docker plus daemon-host KVM exposure before building, runs `./tools/build_apk.sh --with-android-test`, boots emulator, verifies exact runtime API, installs both APKs, executes full instrumentation, runs Launcher focus/workspace smoke, and applies filtered logcat gate.

Verified baseline (2026-08-03): native `docker-dev` KVM runs on API 35 and exact Android 16/API 36 each passed the selector-unset 133-test suite, Launcher focus/workspace smoke, and filtered logcat gate. Local runtime evidence only; not hosted API 36 or `targetSdk` 36 evidence.

API 36 defaults:

- image: `zeam-docker-emulator:android36`
- container: `zeam-runtime-api36`
- AVD: `zeam_avd_api36`
- host ports: ADB `5556`, noVNC `6081`
- output: `.android-emulator-artifacts/api36/e2e-diagnostics-api36`

Focused diagnosis does not replace full-suite evidence:

```bash
API_LEVEL=36 KVM_DEVICE=/dev/kvm \
INSTRUMENTATION_TEST_CLASS='org.zmreborn.WidgetInsertionE2ETest' \
  bash .claude/skills/run-zmreborn/driver.sh test
```

## Manual deploy and noVNC

```bash
bash .claude/skills/run-zmreborn/driver.sh deploy
API_LEVEL=36 KVM_DEVICE=/dev/kvm \
  bash .claude/skills/run-zmreborn/driver.sh deploy
```

Driver verifies noVNC inside container. Because ports bind only on remote Docker daemon loopback, first run `ssh -N -L 6080:127.0.0.1:6080 docker-dev` for API 35 or `ssh -N -L 6081:127.0.0.1:6081 docker-dev` for API 36, then open matching local URL printed by driver. Existing manual commands remain:

```bash
bash .claude/skills/run-zmreborn/driver.sh build
bash .claude/skills/run-zmreborn/driver.sh start
bash .claude/skills/run-zmreborn/driver.sh recreate
bash .claude/skills/run-zmreborn/driver.sh install
bash .claude/skills/run-zmreborn/driver.sh launch
bash .claude/skills/run-zmreborn/driver.sh shot my-label
```

Driver mounts repository read-only at `/workspace` and selected output directory read-write at `/artifacts`. Use Docker-exec ADB; host ADB forwarding may be offline.

## Configuration

Validated overrides: `DOCKER_CONTEXT`, `API_LEVEL` (`35` or `36`), `SYSTEM_IMAGE`, `EMULATOR_IMAGE`, `CONTAINER`, `AVD_NAME`, `ADB_HOST_PORT`, `NOVNC_HOST_PORT`, `OUT_DIR`, `KVM_DEVICE`, and `INSTRUMENTATION_TEST_CLASS`. `OUT_DIR` must resolve beneath repository `.android-emulator-artifacts/`; ADB and noVNC host ports must be canonical decimal values from 1 to 65535 and must differ.

## Security and lifecycle

- ADB and noVNC bind only to Docker daemon-host loopback; raw VNC remains container-internal. Remote browser access requires explicit SSH tunnel.
- Runtime container label binds API, image, AVD, ports, source mount, and output mount. Mismatch fails instead of reusing stale runtime state.
- Manual start without `KVM_DEVICE` retains slow software fallback. `test` always rejects missing/unusable KVM.
- Source mount is read-only. Only selected output directory is writable from emulator container.
- Recreate removes volatile AVD tmpfs state. For KVM test-compatible recreation, use `KVM_DEVICE=/dev/kvm ... recreate`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Docker image unavailable` | Build matching `android35` or `android36` image first. |
| `system image` build failure | Confirm base SDK manager lists exact API 36 Google APIs x86_64 package. |
| `Docker daemon cannot expose usable KVM device` | Expose readable/writable `/dev/kvm` on Docker daemon host, not only Docker client host; do not claim software run as automation evidence. |
| `incompatible API, ports, or mounts` | Run printed API-qualified recreate command, then retry. |
| Browser cannot connect | Keep driver running, establish printed SSH tunnel to `docker-dev`, then open printed local URL. |
| Boot timeout | Verify KVM, 9 GB tmpfs memory allowance, and image/AVD API match. |
| Test failure | Inspect API-qualified `e2e-diagnostics-api<level>` under selected output directory. |
