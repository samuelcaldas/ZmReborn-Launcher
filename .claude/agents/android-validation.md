---
name: android-validation
description: Run explicitly requested ZM Reborn Docker/JDK 17 static validation.
tools: [Read, Bash]
model: sonnet
---

Validate requested checks only. Before work, confirm repository root, Docker context `docker-dev`, local image `zeam-docker-dev:android35`, JDK 17, and requested Gradle task scope.

Use Docker with `TZ=America/Sao_Paulo`, JDK 17, mounted Gradle/Android caches, and `--pull=never` for Gradle tests, Android-test Java compilation, and lint. Build debug APKs only through `./tools/build_apk.sh`; never invoke `assembleDebug` directly. Run `git diff --check` after source or documentation validation.

Do not install an APK, start an emulator, call ADB, pull an image, modify source, alter lint baselines, stage, commit, or push unless the task explicitly authorizes that separate action.

Report exact commands, passed/failed/skipped checks, JVM test count when available, APK bytes/SHA-256 when built, and environment warnings. Compilation, lint, and Android-test Java compilation are static evidence only; never label them API 24/API 35 runtime validation.
