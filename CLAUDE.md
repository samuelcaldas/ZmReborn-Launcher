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

## Reusable Automation

When a task is procedural and will recur — the same steps, same validation, same command shape, done more than once — package it instead of repeating it ad hoc:

- **Agent** (`.claude/agents/<name>.md`): a bounded, read-only or narrowly-scoped investigation/review run repeatedly (auditing a diff, checking a lifecycle concern, gating a validation step). Follow the frontmatter shape (`name`, `description`, `tools`, optional `model`) used by existing agents (e.g. `dirty-tree-guard.md`, `api-compatibility-auditor.md`). Add a row to `.claude/agents/README.md`'s table (mode, "use when"). New/changed agent definitions need a session restart to take effect.
- **Skill** (`.claude/skills/<name>/SKILL.md`): a guided, invocable workflow with a fixed sequence of steps and an exact expected output (build, run, test-drive, generate). Use a keyword-rich `description` for matching. Document prerequisites, exact invocation, exact success output, gotchas, and a troubleshooting table, mirroring `run-build-apk/SKILL.md`. Bundle a `driver.sh`/`driver.py` when the steps are non-trivial (see `run-emulator-apk-test/`, `run-image-gen/`).
- **Tool/script** (`tools/*.sh`, `tools/*.py`): a deterministic, self-contained command any agent or human can invoke directly, with fixed environment/context resolution and concise fixed-format output (see `tools/build_apk.sh`). Prefer this when the operation has no decision-making or investigation step — just execution.

Choose the narrowest mechanism that fits: a one-shot deterministic command is a tool/script; a guided multi-step procedure with meaningful output is a skill; a repeated judgment/review task is an agent. Do not duplicate an existing agent, skill, or tool — extend it instead. Never bypass an existing wrapper (e.g. `tools/build_apk.sh`) by invoking the underlying command directly.

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

## Incremental Modernization

Apply these checks to **every existing file a task touches**. Do not apply them to untouched files (no unrelated churn). Each applied check is part of the same atomic commit as the triggering change.

### Touch-rule checklist

1. **Docstrings.** Add a concise Javadoc comment to every `public` class, interface, and method in the file that currently lacks one. Cover: purpose, non-obvious parameters, return value, and thrown exceptions. One sentence for simple methods; multi-line for complex ones. Do not add Javadoc for private/package-private implementation details or generated `R` references.

2. **File length.** If the file exceeds 200 lines after modifications, check whether it contains more than one coherent responsibility. When it does, extract the secondary responsibility into a focused companion class in the same package (or the correct subdirectory per rule 3). Move related tests with it. Confirm compilation and run relevant unit tests before committing.

3. **Package placement.** Check whether the class belongs in an existing subdirectory or whether a new one is warranted. When it does, move it in the same commit: update every `import` statement in all source sets (`main`, `test`, `androidTest`), confirm `./gradlew :app:testDebugUnitTest --no-daemon` passes, and run lint. When a move would require touching many callers outside the current task scope, leave an inline `// TODO(move): belongs in <package>` comment instead.

### Suggested subdirectory taxonomy

| Subdirectory | Purpose | Representative classes |
|---|---|---|
| `compat/` *(exists)* | API compatibility bridges (API 24–35) | `AdaptiveIconCompat`, `BackGestureCompat`, `WindowInsetsCompat` |
| `theme/` *(exists)* | Wallpaper palette and color extraction | `WallpaperColorExtractor` |
| `drag/` | Drag-drop controllers and interfaces | `DragController`, `DragLayer`, `DragSource`, `DragScroller`, `DragCancellationState`, `DockDragTransaction`, `DropTarget`, `DropResultListener` |
| `drawer/` | App drawer views, adapters, and layout math | `ApplicationsDrawerView`, `ApplicationsGridView`, `ApplicationsPagingView`, `ApplicationsPageView`, `ApplicationsAdapter`, `ApplicationsView`, `ApplicationsPagePartition`, `DrawerAlphabetIndex`, `DrawerDensityPolicy`, `DrawerFastScrollView`, `DrawerLayoutMetrics`, `DrawerScrollState`, `DrawerSearchFilter` |
| `folder/` | Folder containers, icons, and data stores | `Folder`, `FolderIcon`, `FolderInfo`, `FolderLayoutMetrics`, `AppListFolderInfo`, `AppListFolderProjection`, `AppListFolderRecord`, `AppListFolderStore`, `LiveFolder`, `LiveFolderAdapter`, `LiveFolderIcon`, `LiveFolderInfo`, `UserFolder`, `UserFolderInfo` |
| `widget/` | AppWidget host, views, and info objects | `LauncherAppWidgetHost`, `LauncherAppWidgetHostView`, `LauncherAppWidgetInfo`, `WidgetResizeFrame`, `Widget` |
| `preferences/` | Settings screen and preference widgets | `Preferences`, `PreferencesUtil`, `DialogSeekBarPreference`, `DebouncedIntegerPreference`, `InlineSliderPreference`, `InlineStepperPreference`, `ColourPickerDialog`, `ColourPickerPanelView`, `ColourPickerPreference`, `ColourPickerView`, `SettingsPreference`, `SettingsSummaryBinder` |
| `workspace/` | Desktop workspace and cell layout | `Workspace`, `CellLayout`, `DeleteZone` |
| `indicator/` | Page indicators and signal rail | `ScreenIndicator`, `DotsIndicator`, `SignalRailView` |
| `item/` | Launcher item data models | `ItemInfo`, `ApplicationItemInfo`, `ApplicationsGridItemInfo`, `FolderInfo`, `LiveFolderInfo`, `UserFolderInfo`, `LauncherAppWidgetInfo` |
| `util/` | Stateless utilities and drawables | `Utilities`, `LocaleUtil`, `FastXmlSerializer`, `XmlUtils`, `FastBitmapDrawable`, `AlphaPatternDrawable`, `BubbleTextView`, `SelectorDrawable`, `NumberPicker`, `NumberPickerButton` |
| `paging/` | Horizontal pager and page partitioning | `ViewPager`, `ApplicationsPagePartition` |
| `search/` | Search bar and filter logic | `Search`, `DrawerSearchFilter` |

The taxonomy is a guide. When a class has dependencies that would require a cascade of unrelated moves, defer and leave a `// TODO(move): belongs in <package>` comment instead.

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
