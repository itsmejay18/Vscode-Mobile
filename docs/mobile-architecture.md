# Mobile architecture — reuse analysis + plan

## What exists in this repo (desktop)

- `build.sh`, `prepare_vscode.sh`, `build_cli.sh`, `version.sh` — clone `microsoft/vscode`, run `gulp vscode-*-min`, package Electron. Desktop-only, **cannot run on Android** (no Electron on arm64 Android, no Node desktop binaries).
- `product.json` — **reusable**: extension gallery (`open-vsx.org`), proposal list. Copied as reference for mobile defaults.
- `patches/00-settings-*`, `00-ui-*` — **reusable as ideas**: default settings, gallery URL. Not applied as patches on mobile (no vscode source build on-device).
- `src/stable/src/vs/workbench/...` SVGs, `icons/` — **reusable**: branding/empty-state art for mobile workbench.
- `src/stable/resources/{linux,darwin,win32,server}` — server REH build proves `vscode-reh-web` exists upstream; mobile does NOT run REH on-device in v1 (too heavy). Instead: local static workbench + native runtime.
- No backend server, no API, no mobile code existed before `android/`.

## What Android reuses vs replaces

| Desktop layer | Mobile decision |
|---|---|
| Electron shell | Replaced by Native Kotlin Activity + WebView (`webview/WorkbenchWebView.kt`) |
| Node extension host | Replaced by `runtime/RuntimeManager` + localhost processes |
| `vs/editor` (Monaco) | Vendored as static assets under `app/src/main/assets/editor/` + `monaco/` (TODO: copy min build in; fallback editor ships so APK builds offline) |
| Workbench UI | Reimplemented lightweight local `workbench.html` (Explorer/Tabs/Terminal/Problems/Preview) talking to explicit bridges |
| product.json gallery | Reused for future extension support |
| Build pipeline | Isolated: desktop scripts untouched; mobile builds via `android/gradlew assembleDebug`, abi arm64-v8a, minSdk 29 |

## Module map (spec §24, adapted)

```
android/app/src/main/java/com/customvscode/mobile/
  MainActivity.kt (workbench host), IdeApplication.kt
  storage/Workspace.kt
  bridge/FileBridge.kt, TerminalBridge.kt, ProjectBridge.kt, RuntimeBridge.kt, BridgeSecurity.kt
  terminal/TerminalSession.kt
  runtime/RuntimeManager.kt, LinuxEnvironment.kt, ShellManager.kt, ProcessManager.kt, RuntimeInstaller.kt, RuntimeRegistry.kt, RuntimeModels.kt
  projects/ProjectManager.kt
  settings/SettingsStore.kt
  preview/PreviewManager.kt, PreviewBridge.kt
  webview/WorkbenchWebView.kt
android/app/src/main/assets/editor/workbench.html (local, no CDN)
android/app/src/main/assets/runtime-manifests/*.json
android/runtime-manifests/*.json (source of truth, copied into assets at build)
```

## Stage status

- Stage 1 (APK builds): in progress — verify with Gradle 8.7 + AGP 8.5.2 below.
- Stage 2 (local Monaco): workbench shell done; `monaco/min/vs` vendoring TODO, fallback editor labeled honestly.
- Stages 3-4 (explorer/bridges): File/Project/Terminal/Runtime bridges done with traversal checks.
- Stage 5 (shell/PTY): real `ProcessBuilder` sessions; full PTY (cols/rows, ANSI) TODO via native pty.
- Stage 6+ (runtimes): manifests + registry + installer skeleton done; per-ABI binaries pending — UI shows NOT_INSTALLED, never fakes.
