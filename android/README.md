# Android app — Custom VSCode Mobile

Native Kotlin + WebView + Monaco. Zero extra dependencies (no OkHttp/Retrofit) so it builds offline in Android Studio.

## Open

1. Android Studio Hedgehog+ → Open → select `android/` folder.
2. Let Gradle sync (AGP 8.5.2, Kotlin 1.9.24, minSdk 29).
3. Run `app` on emulator (API 29+) or USB device.

## Use

- Paste GitHub token (PAT `ghp_…` for now, OAuth next) + `owner/repo` + file path → **Open**.
- Edit in Monaco (CDN `monaco-editor@0.52.2`), auto-saves locally to `last_edit.txt`.
- **Push** commits via GitHub Contents API.

## Optional full VS Code backend

```bash
docker run -it --rm -p 3000:3000 gitpod/openvscode-server --without-connection-token
```

Then in `MainActivity` change `loadUrl` to `file:///android_asset/monaco.html?server=http://10.0.2.2:3000`
and use the in-page **Server** button to toggle to the iframe.

## Next

- [ ] OAuth Device Flow UI (no manual PAT paste)
- [ ] Repo browser + file tree + branches
- [ ] Offline queue (Room) + diff before push
- [ ] Bundle Monaco locally instead of CDN
