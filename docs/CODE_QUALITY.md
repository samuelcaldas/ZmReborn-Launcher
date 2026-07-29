# Code Quality

## Scope and intent

Apply these rules to all new and modified production code, tests, resources, manifests, and build/runtime configuration. Improve touched code when needed for correctness or clarity, but do not create unrelated legacy churn. Preserve JADX provenance, reconstructed behavior, platform compatibility, and atomic commits.

Quality gates reduce defect risk and provide required acceptance evidence; they cannot honestly guarantee that no defect remains. Record only validation and device evidence actually obtained; never fabricate coverage, execution, or results.

## Design

- Apply SOLID, Clean Code, and Object Calisthenics pragmatically: prefer composition, small focused responsibilities, descriptive names, guard clauses over unnecessary `else`, shallow nesting, short methods/classes, minimal state, and no deep chains.
- Introduce first-class domain types or collections only when they make invariants or behavior clearer.
- Apply a design pattern only for a concrete variation, creation, or coordination problem. Do not overengineer: ban speculative abstractions, needless layers, and pattern-for-pattern's-sake.
- Keep existing code stable unless a touched path requires a focused correction. Do not refactor unrelated legacy code solely to meet a style preference.

## Fail fast and recover safely

- At every boundary and before side effects or heavy work, validate null, empty, whitespace-only, malformed, out-of-range, configuration, platform/runtime, geometry, and state inputs. Reject invalid state with specific descriptive exceptions or explicit failures; never silently coerce, defer, or swallow errors.
- Limit `try`/`catch` to fallible I/O, framework, concurrency, parsing, or persistence operations. Catch narrowly; add operation and input context when wrapping and rethrowing; do not catch-and-ignore.
- Clean up resources and lifecycle registrations on every exit path. Make cancellation, rollback, and partial-failure behavior safe and explicit.

## Risk analysis and tests

Before implementation, identify relevant failure paths. Add behavior-focused coverage for applicable cases:

- null, empty, and whitespace values; zero, one, and many items; bounds and overflow; invalid configuration or state;
- orientation, density, system bars/insets, accessibility, lifecycle transitions, and process recreation;
- API 24/API 35 verifier safety and compatibility bridges; avoid direct unavailable-API bytecode references;
- asynchronous ordering, concurrency, persistence, cancellation/rollback, and partial failures.

Coverage must demonstrate meaningful observable behavior, not a vanity percentage or test-double-only assertion.

## Required acceptance evidence

1. Use TDD for every production change: red (small targeted test fails), green (minimum change passes), refactor (tests stay green). A defect fix starts with a regression test reproducing its failure.
2. Run the targeted test and relevant full test suite. Compile Android tests when relevant, but treat compilation as static evidence only; execute relevant instrumentation tests on a device or emulator for runtime evidence.
3. Run lint, `./tools/build_apk.sh` for every local debug APK build, and `git diff --check`. Never invoke `assembleDebug` directly.
4. For runtime-facing changes, obtain API 24 and API 35 install/launch, app-drawer, Preferences, and filtered-logcat evidence, including fatal exceptions, verifier failures, missing methods, and `UnsupportedOperationException`.
5. Review changed paths for behavior, error handling, compatibility, tests, accessibility, and documentation. Keep documentation live in the same change.
6. Keep zero third-party app/runtime dependencies unless explicitly approved, and use generated `R` only.

Follow [`TESTING.md`](TESTING.md) for source-set placement and commands, and repository rules in [`../CLAUDE.md`](../CLAUDE.md) for provenance, toolchain, fail-fast, and atomic-commit requirements.
