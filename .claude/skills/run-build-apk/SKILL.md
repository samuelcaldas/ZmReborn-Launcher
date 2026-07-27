---
name: run-build-apk
description: build APK, build debug APK, assemble APK, compile Android app, run build_apk.sh, docker build APK, gradle assembleDebug
---

Build the Zeam debug APK deterministically inside Docker. The driver is `tools/build_apk.sh` — a self-contained wrapper that resolves the local image ID, mounts the Gradle cache, runs `assembleDebug`, and on success prints only APK path, byte size, and SHA-256.

## Prerequisites

- Docker installed and `docker-dev` context available
- Local image `zeam-docker-dev:android35` present (no network pull)
- Gradle cache volume `zeam-gradle-cache` (created automatically on first run)

Verify both are ready:

```sh
docker --context docker-dev image inspect zeam-docker-dev:android35 --format '{{.Id}}'
docker --context docker-dev volume ls --filter name=zeam-gradle-cache
```

## Build (agent path)

Run from repo root:

```sh
./tools/build_apk.sh
```

Success output (exact format):

```
APK: app/build/outputs/apk/debug/app-debug.apk
Bytes: 746768
SHA-256: e9c7c4c8b0f390203cc60171694ebebef72566849a1226858e1e52eb65bc1f96
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Environment overrides

```sh
DOCKER_CONTEXT=docker-dev \
DOCKER_IMAGE=zeam-docker-dev:android35 \
GRADLE_CACHE_VOLUME=zeam-gradle-cache \
./tools/build_apk.sh
```

## Install and smoke-test on device/emulator

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
```

## Gotchas

- **Never invoke `./gradlew :app:assembleDebug` directly.** The wrapper owns Docker context resolution, image ID pinning (no pull), and Gradle cache mounting. Bypassing it breaks reproducibility.
- **Do not commit the APK.** `app/build/` is gitignored; keep it that way.
- **`DOCKER_HOST` is unset intentionally.** `docker_cli` in the script calls `env -u DOCKER_HOST docker` to prevent ambient socket interference.
- **Image must be local.** `--pull=never` is set; if the image is absent the script fails with `Docker image unavailable: zeam-docker-dev:android35`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Docker context unavailable: docker-dev` | `docker context create docker-dev --docker "host=unix:///var/run/docker.sock"` |
| `Docker image unavailable: zeam-docker-dev:android35` | Build or pull the image into the `docker-dev` context first |
| `Gradle wrapper is not executable` | `chmod +x gradlew` |
| `app/build.gradle not found` | Run from repo root, not a subdirectory |
| Build exits 1 with no output | `cat /tmp/tmp.*/` — the script writes diagnostics to the temp log printed on failure |
