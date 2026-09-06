# Custom VSCode — Mobile (Native Android Kotlin + VS Code Web / Monaco)

> Native Android code app that lets you do whatever you do with projects on desktop — connect to GitHub, browse repos, edit, commit/push — powered by embedded VS Code Web / Monaco.

This repo started from [VSCodium](https://github.com/VSCodium/vscodium) build scripts (desktop Electron builds via `build.sh`, `product.json`, `patches/`). That pipeline has **no reusable mobile backend** — so this project repurposes it into a mobile direction:

- **Old:** `vscode` clone + patches → desktop binaries (Win/macOS/Linux)
- **New:** Native Android shell (Kotlin) + WebView → Monaco / VS Code Web + GitHub API

## Architecture

```
Android App (Kotlin, Jetpack Compose / Views)
 ├─ WebView (Monaco Editor / vscode-web / openvscode-server client)
 ├─ Kotlin bridge (JS Interface: files, git, terminal, settings)
 ├─ GitHub Auth (OAuth Device Flow + PAT fallback)
 ├─ Local: app files, cache, offline edits (Room/DataStore)
 └─ Remote (optional self-host): openvscode-server / code-server for full workspace, extensions, terminal
```

Why this way:
- Electron from VSCodium **does not run on Android**.
- Monaco (`vs/editor`) is the same editor core VS Code uses and runs fine in WebView.
- Full VS Code backend (extensions, terminal, language servers) runs best as `openvscode-server` remotely, phone acts as thin client.

## Features (target)

- [ ] Connect to GitHub (OAuth login, list repos/orgs/starred)
- [ ] Clone / open repo, branch, file tree
- [ ] Edit with Monaco (syntax highlight, find/replace, tabs, minimap off for mobile)
- [ ] Commit / push / pull (via GitHub API + isomorphic-git / server-side git)
- [ ] Offline editing + sync queue
- [ ] Optional: connect to your own `openvscode-server` for terminal + extensions

## Repo layout

```
./                          # legacy VSCodium build scripts (build.sh, product.json, patches/, .github/)
android/                    # NEW: native Kotlin app (coming next)
  app/src/main/...          # MainActivity, WebView + Monaco bridge, GitHub auth
  web/                      # Monaco static bundle loaded into WebView
docs/mobile-plan.md         # detailed mobile plan (TODO)
```

## Quick start (planned)

1. Open `android/` in Android Studio Hedgehog+.
2. Run on emulator (API 29+) — WebView loads local `web/monaco.html`.
3. Tap Connect GitHub → OAuth → pick repo → edit.
4. Optional full backend: `docker run -p 3000:3000 gitpod/openvscode-server` then set Server URL in app settings.

## What from old backend is reused?

- `product.json` (extension gallery config, `open-vsx.org`)
- `patches/00-settings-*` (default settings ideas)
- Branding/icons in `icons/`, `src/stable/resources/`
- Everything else (`build.sh`, Electron packaging) is desktop-only and **not used on mobile**.

## Roadmap

1. Rewrite README (this) — in progress
2. Scaffold `android/` Kotlin + WebView + local Monaco
3. GitHub OAuth + repo browser + file view/edit
4. Commit/push + offline queue
5. Optional server mode for terminal/extensions

## License

[MIT](./LICENSE) — same as upstream VSCodium.
