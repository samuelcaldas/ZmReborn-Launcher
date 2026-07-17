# CLAUDE.md

## Branch Scope

- Work only on `dev`.
- Never read, modify, compare, merge, or use `main` or `master`; those branches contain an outdated, broken reconstruction attempt.

## Project Context

- Zeam Launcher 3.1.10 is reconstructed from JADX-decompiled source.
- Preserve original behavior, source provenance, and traceability.
- Keep the original APK only at `docs/reference/zeam-launcher-3-1-10-en-android.apk`.
- Do not commit generated APKs or build output.

## Build Configuration

- JDK: 17
- Gradle Wrapper: 8.7
- Android Gradle Plugin: 8.5.2
- `compileSdk` / `targetSdk`: 35
- `minSdk`: 8

Set `ANDROID_SDK_ROOT`, then run:

```sh
./gradlew assembleDebug --no-daemon
```

Use Docker for development/builds and KVM-backed Android emulators for runtime validation.

## Dependency Policy

- Keep zero third-party app/runtime dependencies.
- Use Android SDK and Java APIs only unless the user explicitly approves another dependency.
- Android Gradle Plugin remains the only build dependency.

## Compatibility Rules

- Avoid direct bytecode references to APIs unavailable at `minSdk 8`.
- Use small, focused compatibility bridges when old and current Android APIs differ.
- Use generated `R`; never restore JADX's `C0041R` class or frozen numeric resource IDs.
- Preserve fail-fast behavior and specific exception handling at system boundaries.

## Validation

For affected runtime behavior, test both API 10 and API 35. Minimum smoke coverage:

1. Install and launch.
2. Open the app drawer.
3. Open Preferences.
4. Review filtered logcat for fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`.

Run `git diff --check` and relevant Gradle tasks after changes.

## Documentation

- Update `docs/CHANGELOG.md` for reconstruction, build, compatibility, or emulator-validation changes.
- Keep `README.md` build and compatibility instructions current.
