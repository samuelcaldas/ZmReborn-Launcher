# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch and Provenance

- Work locally on `main`.
- `original_source` is the immutable raw JADX provenance baseline. Preserve reconstructed behavior, provenance, and traceability against it.
- Keep the original APK only at `docs/reference/zeam-launcher-3-1-10-en-android.apk`.
- Do not commit generated APKs or build output.
- Each commit must be atomic: one small coherent logical change; include related tests and documentation when needed; never mix unrelated changes. This rule does not authorize commits without an explicit user request.

## Toolchain and Commands

- Use JDK 17, Gradle Wrapper 8.7, Android Gradle Plugin 8.6.0, SDK 35, Build Tools 34.0.0, and `minSdk` 24.
- Always build local debug APKs with `./tools/build_apk.sh`; never invoke `assembleDebug` directly.
- The wrapper owns Docker context `docker-dev`, resolves image `zeam-docker-dev:android35` to its inspected local image ID, forbids pulls, mounts the Gradle cache, provides the Android SDK, and returns concise build output.

```sh
./tools/build_apk.sh
./gradlew :app:lint --no-daemon
git diff --check
```

Install and launch the debug build:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p org.zmreborn -c android.intent.category.LAUNCHER 1
```

For Docker emulator runtime testing, follow [`README.md#docker-emulator-testing`](README.md#docker-emulator-testing). The API 35 image is built from `tools/Dockerfile.emulator`, uses `--device /dev/kvm`, and exposes ADB on port 5555.

Run unit tests or a single test:

```sh
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:testDebugUnitTest --tests 'org.zmreborn.FastXmlSerializerTest' --no-daemon
```

## Test-Driven Development

- TDD is mandatory for every production change, including Java, resources, manifests, and runtime behavior. Follow red → green → refactor: first add or update the smallest targeted automated test and run it to confirm the intended failure; make the minimum production change; rerun that targeted test until it passes; then refactor only with tests green.
- A defect fix must begin with a regression test that reproduces the reported failure. Confirm its initial failure before implementing the fix.
- After the targeted test is green, run relevant full JVM tests, instrumentation-test compilation, lint, and the debug build. Android-test compilation is static evidence only; it is not device-runtime evidence. Execute relevant instrumentation tests and required API 24/API 35 smoke coverage for runtime behavior.
- Place tests precisely: `app/src/test/java/` is the JVM test source set; within it, use package directory `app/src/test/java/org/zmreborn/` for pure Java, resource/source contracts, and deterministic compatibility guard clauses, and package directory `app/src/test/java/org/zmreborn/compat/` only for JVM-safe compatibility contracts that neither instantiate nor call unmocked Android framework paths. The latter is not a separate source set. Use instrumentation source set `app/src/androidTest/java/`, with package directory `app/src/androidTest/java/org/zmreborn/`, for real Android Views, resources, layouts, input, lifecycle, and API-specific behavior. See [`docs/TESTING.md`](docs/TESTING.md) for workflow and commands.
- Reserve `E2ETest` names for tests that execute and verify user-visible workflows. Name component or integration checks `InstrumentationTest`.
- Tests must verify observable production behavior. Do not claim behavior with tests that only exercise test doubles or only assert an item's existence.
- TDD does not relax the mandatory fail-fast rules in **Constraints** or the atomic-commit rule in **Branch and Provenance**.

## Code Quality

- All new and modified work must meet the mandatory design, failure-handling, risk-analysis, and acceptance-evidence rules in [`docs/CODE_QUALITY.md`](docs/CODE_QUALITY.md). Apply SOLID, Clean Code, and Object Calisthenics without unrelated legacy churn; use patterns only for concrete problems, not speculative abstractions or needless layers.
- Quality gates are required acceptance evidence that reduces defect risk, not a guarantee that no defects exist. Report only validation, coverage, and device evidence actually obtained.

## Architecture and Runtime Flow

- `LauncherApplication` owns the shared `LauncherModel`; `Launcher` coordinates activity lifecycle and launcher views.
- `LauncherModel` asynchronously loads applications and favorites, then persists workspace changes.
- `LauncherProvider` owns the SQLite favorites/workspace database and its initial seeding.
- `Workspace` and `CellLayout` render the desktop; `DragLayer` and drag-drop interfaces route item movement.
- The app drawer uses grid and paging implementations; preferences and receivers trigger model/UI reloads.
- Widget, dynamic-receiver, and wallpaper compatibility bridges preserve behavior from API 24 through API 35.
- Manifest `<queries>` package visibility plus launcher, provider, and receiver registrations are runtime-critical.

## Constraints

- Keep zero third-party app/runtime dependencies. Use Android SDK and Java APIs unless explicitly approved otherwise.
- Use generated `R`; never restore JADX `C0041R` or frozen numeric resource IDs.
- Avoid direct bytecode references to APIs unavailable at `minSdk` 24; use focused compatibility bridges.
- Mandatory: At boundaries, before side effects or heavy work, validate inputs, configuration, platform/runtime prerequisites, widget/grid geometry, and state invariants; reject invalid states with specific descriptive exceptions or explicit failures; never silently swallow, coerce, or defer them; catch only specific exceptions and preserve context when wrapping/rethrowing. See [`docs/CODE_QUALITY.md`](docs/CODE_QUALITY.md) for required boundary, cleanup, and lifecycle practices.

## Validation and Documentation

- `docs/CODE_QUALITY.md` defines mandatory quality gates, risk analysis, changed-path review, and honest acceptance evidence; this section preserves the project-specific commands and runtime checks.
- Run relevant build, lint, and `git diff --check` validation after changes.
- Every new feature must add or update automated unit tests covering its expected behavior.
- Every fixed defect must add or update an automated regression test covering the failure path.
- Runtime-facing changes also require relevant instrumentation tests and API 24/API 35 manual smoke coverage.
- For runtime changes, smoke-test API 24 and API 35: install/launch, app drawer, Preferences, and filtered logcat for fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`.
- Update `docs/CHANGELOG.md` for reconstruction, build, compatibility, or emulator-validation changes; keep README build and compatibility instructions current.
