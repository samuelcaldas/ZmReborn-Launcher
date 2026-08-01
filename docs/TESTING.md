# Testing

## Mandatory test-driven development

Test-driven development (TDD) is mandatory for every production change, including Java, resources, manifests, and runtime-facing behavior. Follow this order without skipping steps:

1. **Red:** add or update the smallest targeted automated test that describes the intended behavior. Run that targeted test first and confirm it fails for the intended reason. Stop and correct the test or setup if it passes unexpectedly or fails for an unrelated reason.
2. **Green:** make the minimum production change that satisfies the failing test. Rerun the same targeted test and confirm it passes.
3. **Refactor:** improve test or production design only while the targeted test remains green. Rerun it after each refactor.
4. **Validate:** run the applicable full JVM suite, Android-test compilation, lint, and debug build. For runtime-facing behavior, execute relevant instrumentation tests and perform required API 24/API 35 device smoke coverage.

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
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.WidgetResizeInstrumentationTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.zmreborn.LauncherMenuInstrumentationTest
```

`WidgetPickerInstrumentationTest` covers Search-first ordering, provider metadata, accessible preview cards, asynchronous loading, zero host-ID allocation before selection, bind-cancellation cleanup, and picker state restoration. `WidgetResizeInstrumentationTest` covers resize handles and body-drag callback separation; it is component evidence, not proof of real-provider movement or DeleteZone persistence. `LauncherMenuInstrumentationTest` protects direct home-menu order and retained options Add categories.

API 24/API 35 device smoke must still select real providers, exercise bind approval and provider configuration, rotate during pending flows, move and delete a resizable widget, relaunch to verify persistence, and inspect host IDs/logcat for abandoned allocations or platform failures.

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

On primary failure, driver captures bounded logcat, window/activity/process/package state, UI hierarchy,
and screenshot before emulator teardown, while preserving primary exit status. Run state and command
output are written under `e2e-diagnostics/`; workflow uploads that directory as `e2e-diagnostics` even
when tests fail. `bash tools/test_ci_workflow_contract.sh` protects single-process execution,
timeout/result checks, pre-teardown diagnostics, and artifact wiring.

Hosted API 24 results are runtime compatibility evidence. API 35 local instrumentation is separate
runtime evidence; Android-test assembly only proves source compilation and packaging.

## Required validation sequence

Run commands from the repository root after the targeted test passes. Apply the Android steps when the change has instrumentation or runtime scope.

```sh
# Red and green: replace with changed test class or method.
./gradlew :app:testDebugUnitTest --tests 'org.zmreborn.ChangedTest' --no-daemon

# Full JVM suite.
./gradlew :app:testDebugUnitTest --no-daemon

# Static instrumentation-source compilation and APK assembly; not runtime evidence.
./gradlew :app:assembleDebugAndroidTest --no-daemon

# Execute relevant Android tests on a connected device or emulator.
./gradlew :app:connectedDebugAndroidTest --no-daemon

# Static checks and required debug build.
./gradlew :app:lint --no-daemon
./tools/build_apk.sh

git diff --check
```

For runtime-facing changes, also smoke-test API 24 and API 35: install and launch the debug APK, open the app drawer and Preferences, then inspect filtered logcat for fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`. Record only validation actually run; Android-test compilation does not replace connected instrumentation execution or device smoke evidence.
