# Reusable repository agents

| Agent | Mode | Use when |
| --- | --- | --- |
| `dirty-tree-guard` | Read-only | Before reviews or validation on a non-clean tree. |
| `android-validation` | Stateful, explicit request only | Run requested Docker/JDK 17 static checks or debug APK build. |
| `api-compatibility-auditor` | Read-only | Review API, manifest, platform-resource, inset, widget, or back-navigation changes. |
| `resource-contract-auditor` | Read-only | Review resources, localization, preferences, manifest, or public resource schema changes. |
| `launcher-lifecycle-reviewer` | Read-only | Review Launcher, drawer, workspace, folders, adapters, widgets, or wallpaper lifecycle changes. |
| `runtime-evidence-steward` | Report-first | Plan or record explicitly authorized API 24/API 35 runtime validation. |

Recommended order: `dirty-tree-guard`, relevant specialized reviewers in parallel, then `android-validation`. Invoke `runtime-evidence-steward` only when device/emulator evidence is explicitly requested.

Agents preserve the dirty tree, do not commit generated artifacts, and never present static checks as runtime validation.

Restart Claude Code or open a new session after adding/changing definitions so the local agent registry reloads them.
