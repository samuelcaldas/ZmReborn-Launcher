# Testing

## Mandatory test-driven development

Test-driven development (TDD) is mandatory for every production change, including Java, resources, manifests, and runtime-facing behavior. Follow this order without skipping steps:

1. **Red:** add or update the smallest targeted automated test that describes the intended behavior. Run that targeted test first and confirm it fails for the intended reason. Stop and correct the test or setup if it passes unexpectedly or fails for an unrelated reason.
2. **Green:** make the minimum production change that satisfies the failing test. Rerun the same targeted test and confirm it passes.
3. **Refactor:** improve test or production design only while the targeted test remains green. Rerun it after each refactor.
4. **Validate:** run the applicable full JVM suite, Android-test compilation, lint, and debug build. For runtime-facing behavior, execute relevant instrumentation tests and perform required API 24/API 35/API 36 device smoke coverage.

A defect fix starts with a regression test that reproduces the failure. Confirm that test initially fails before implementing the fix. A test added only after the fix is not sufficient regression evidence.

TDD complements, rather than changes, mandatory fail-fast behavior: validate boundaries and invariants before side effects or heavy work, and fail explicitly with specific context. Keep every commit atomic: one coherent change with its tests and necessary documentation; never mix unrelated changes.

## Test source-set and package-directory placement

Choose the source set from behavior under test, not implementation convenience. `app/src/test/java/` is one JVM test source set; `app/src/test/java/org/zmreborn/` and `app/src/test/java/org/zmreborn/compat/` are package directories within it, not separate source sets.

| Source set | Package directory | Put these tests here | Do not put these tests here |
| --- | --- | --- | --- |
| JVM: `app/src/test/java/` | `app/src/test/java/org/zmreborn/` | Pure Java behavior; resource or source contracts; deterministic compatibility guard clauses that need no Android runtime. | Tests requiring Android Views, resources, layouts, input dispatch, lifecycle, or platform API behavior. |
| JVM: `app/src/test/java/` | `app/src/test/java/org/zmreborn/compat/` | JVM-safe contracts for compatibility code only when the test neither instantiates nor calls unmocked Android framework paths. | Framework-touching compatibility behavior, even when the production class is under `org.zmreborn.compat`. Move it to `androidTest`. |
| Instrumentation: `app/src/androidTest/java/` | `app/src/androidTest/java/org/zmreborn/` | Real Android View, resource, layout, input, lifecycle, and API-specific behavior; framework-touching compatibility paths; component and integration checks; user-visible workflow tests. | Pure Java/resource/source contract checks that can run deterministically in the JVM suite. |

Android-test compilation proves only that the instrumentation source set compiles and packages. It is static evidence, not device-runtime evidence. Device execution is required to establish real framework, lifecycle, rendering, input, or API behavior.

## Test naming and validity

Use `*E2ETest` only for a test that executes and verifies a user-visible workflow from interaction to observable outcome. Use `*InstrumentationTest` for Android component or integration checks that do not verify a complete user-visible workflow.

Each test must prove observable production behavior. A test may use a fake, stub, or mock to isolate a boundary, but it must drive real production logic and assert a meaningful outcome. Do not claim behavior with tests that only exercise test doubles, only assert a class/resource/view exists, or only assert implementation wiring without a behavioral result.

### Focused widget scenarios

Run these instrumentation classes when changing widget selection, placement, editing, or home-menu routing:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.widget.WidgetPickerInstrumentationTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.WidgetInsertionE2ETest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.WidgetInsertionCleanupInstrumentationTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.WidgetResizeInstrumentationTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.LauncherMenuInstrumentationTest
```

`WidgetPickerInstrumentationTest` covers Search-first ordering, provider metadata, accessible preview cards, asynchronous loading, real pointer-event selection, zero host-ID allocation before selection, bind-cancellation cleanup, denied configuration-activity cleanup, failed bind-authority verification rollback, and picker state restoration. `WidgetInsertionE2ETest` uses instrumentation-APK-only external provider/configuration components to dispatch pointer input through the visible production Add dialog and provider card, require one newly allocated exact-provider ID, verify a durable configuration-only `RemoteViews` marker, host-view insertion, placement, persistence, pending-state cleanup, and test cleanup. It probes bind authority with a disposable host ID and revokes only authority granted by the test, including failed post-grant verification. `WidgetInsertionCleanupInstrumentationTest` proves cleanup cancels pending placement and removes its layout listener before deleting a newly allocated host ID. `WidgetResizeInstrumentationTest` covers resize handles and body-drag callback separation; it is component evidence, not proof of real-provider movement or DeleteZone persistence. `LauncherMenuInstrumentationTest` protects direct home-menu order and retained options Add categories.

The deterministic test provider proves launcher-owned insertion without shipping fixture components in the production APK. API 24/API 35/API 36 hands-on smoke must still select third-party providers, exercise user-facing bind approval, rotate during pending flows, move and delete a resizable widget, relaunch to verify persistence, and inspect host IDs/logcat for abandoned allocations or platform failures.

## Hosted API 24 CI execution

GitHub Actions invokes `./tools/run_ci_emulator_tests.sh` as one command inside
`reactivecircus/android-emulator-runner`. Keep shell control flow in that checked-in Bash process;
the action executes separate `script:` lines in separate shells, so multiline YAML scripts lose strict
mode, variables, and compound-command state.

Driver requires built debug and Android-test APKs plus an active ADB target. It validates inputs before
installation, bounds every ADB command with `timeout --foreground`, and requires both a successful ADB
status and `INSTRUMENTATION_CODE: -1`. Crash and failure markers still fail execution when
`am instrument` exits zero. To restrict a diagnostic run to one class or method, set a selector before
invocation:

```sh
INSTRUMENTATION_TEST_CLASS='org.zmreborn.DrawerFastScrollE2ETest' \
  ./tools/run_ci_emulator_tests.sh

INSTRUMENTATION_TEST_CLASS='org.zmreborn.DrawerFastScrollE2ETest#testVerticalDrawerFastScrollNavigatesAlphabeticalSections' \
  ./tools/run_ci_emulator_tests.sh
```

After instrumentation, driver launches Launcher, clears a focused foreign platform ANR dialog through
its focused action, relaunches Launcher, waits for exact current-window focus, and polls UI hierarchy for
the workspace. A focused `org.zmreborn` ANR remains a hard failure; driver never dismisses it.

On primary failure, driver captures bounded logcat, window/activity/process/package state, UI hierarchy,
and screenshot before emulator teardown, while preserving primary exit status. Run state and command
output are written under `e2e-diagnostics/`; workflow uploads that directory as `e2e-diagnostics` even
when tests fail. `bash tools/test_ci_workflow_contract.sh` protects single-process execution,
timeout/result checks, foreign-ANR handling, pre-teardown diagnostics, and artifact wiring.

Hosted API 24 results are runtime compatibility evidence. API 35 and API 36 local Docker instrumentation are separate runtime evidence; Android-test assembly and emulator-image availability only prove static packaging and environment preparation.

## Local Docker API 35 and API 36 execution

One parameterized emulator image definition supports API 35 and Android 16/API 36. Keep `minSdk 24`, `compileSdk 35`, and `targetSdk 35`; API 36 execution validates current target-35 APK runtime compatibility and does not establish target-SDK-36 or forced-edge-to-edge migration.

Build API-specific emulator tags using commands in [`README.md#docker-emulator-testing`](../README.md#docker-emulator-testing). Run full instrumentation through one API-aware driver:

```sh
KVM_DEVICE=/dev/kvm bash .claude/skills/run-zmreborn/driver.sh test
API_LEVEL=36 KVM_DEVICE=/dev/kvm bash .claude/skills/run-zmreborn/driver.sh test
```

`test` probes `/dev/kvm` on Docker daemon host before building APKs, verifies daemon sees exact current runner/entrypoint plus writable artifact mount, and binds reusable runtime identity to the requested AVD before using `./tools/build_apk.sh --with-android-test`. Source is mounted read-only at `/workspace`; mount probes and diagnostics write only through `/artifacts` into `.android-emulator-artifacts/api<level>/`. Port validation rejects non-canonical, overflowing, equal, or out-of-range values before Docker startup. ADB and noVNC bind only to daemon-host loopback; use Docker-exec ADB and explicit SSH tunnel for browser access. Shared runner requires exact `ro.build.version.sdk`, full instrumentation success including `INSTRUMENTATION_CODE: -1`, Launcher focus, workspace hierarchy, and clean bounded filtered logcat.

`INSTRUMENTATION_TEST_CLASS` may select one class or method for diagnosis. Selector runs do not replace selector-unset full-suite acceptance evidence. Software acceleration remains available for manual noVNC use but cannot count as automated API 35/API 36 evidence. Missing daemon-side KVM must fail before APK build or emulator boot and be reported as blocker, not converted into pass.

Hosted CI remains API 24 only. On 2026-08-03, selector-unset native Docker/KVM full-suite runs passed on API 35 and exact Android 16/API 36: each completed 133 instrumentation tests with `INSTRUMENTATION_CODE: -1`, exact Launcher focus/workspace smoke, and clean bounded filtered logcat. These runs include external-widget insertion E2E after its bind fixture began resolving a numeric user and pairing grant cleanup with that same captured user-bound operation. These local runtime results do not claim hosted API 36 Docker CI, target-SDK-36 behavior, or forced-edge-to-edge migration.

## Required validation sequence

Run commands from the repository root after the targeted test passes. Apply the Android steps when the change has instrumentation or runtime scope.

```sh
# Red and green: replace with changed test class or method.
./gradlew :app:testDebugUnitTest --tests 'org.zmreborn.ChangedTest' --no-daemon

# Full JVM suite.
./gradlew :app:testDebugUnitTest --no-daemon

# Static app and instrumentation APK assembly through required wrapper; not runtime evidence.
./tools/build_apk.sh --with-android-test

# Execute relevant Android tests on a connected device or emulator.
./gradlew :app:connectedDebugAndroidTest --no-daemon

# Static checks and required debug build.
./gradlew :app:lint --no-daemon
./tools/build_apk.sh

git diff --check
```

For runtime-facing changes, also smoke-test API 24, API 35, and API 36: install and launch the debug APK, open the app drawer and Preferences, then inspect filtered logcat for fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`. Record only validation actually run; Android-test compilation and emulator-image builds do not replace connected instrumentation execution or device smoke evidence.
