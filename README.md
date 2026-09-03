# VK Music TV

**VK Music TV** is a Kotlin Android TV WebView shell for VK's mobile site. It is designed for a television remote rather than touch input: directional-pad events move a large virtual cursor, and **OK** sends a regular click to the webpage. Authentication and the actual audio catalogue remain in VK's own web experience.

> This is an independent WebView client. VK policies, availability, subscription requirements, and site markup can change independently of the application. Use it only with an account and content you are authorized to access.

## What is implemented

| Area | Implementation |
|---|---|
| Sign-in and session | Opens `https://m.vk.com/audio` in a persistent WebView. Cookies, DOM storage, and WebView cache are enabled and cookies are flushed after navigation. |
| D-pad operation | A clean pointer moves on `DPAD_*` with a short, precise step; **OK/Enter** dispatches a complete touch gesture at the pointer's location. **Back** follows the WebView history. **Menu/Search** focuses a likely site-search input. |
| TV text entry | When a VK input or textarea receives focus, a large on-screen keyboard opens. It supports digits, English/Russian letters, space, backspace, language switching, and Done/close actions. |
| Playback controls | A platform `MediaSession` receives standard headset/remote play, pause, next and previous actions, and uses best-effort semantic selectors to pass them to VK's HTML player. |
| TV presentation | Landscape-only full-screen activity, dark base surface, large first-run hint, visible cursor, launcher and Leanback launcher entries. |
| Compatibility | Kotlin, `minSdk 21`, `targetSdk 35`, no external runtime UI dependencies. |

## Opening in Android Studio

1. Open this directory in a current Android Studio version (Koala/2024.1 or later).
2. Let Gradle download the Android Gradle Plugin and the Android SDK Platform 35 if prompted.
3. Run the **app** configuration on an Android TV emulator or a physical Android TV device.
4. Sign in only through the WebView's VK page. The app neither asks for nor stores the password itself.

## Remote control map

| Remote key | Result |
|---|---|
| Arrow keys | Moves the virtual cursor. Hold a key for faster movement. |
| OK / Enter | Clicks at the virtual cursor location. |
| Back | Goes to the previous webpage; exits at the start page. |
| Menu / Search | Focuses a search field where VK exposes one. |
| On-screen keyboard | Use arrows to select a key, **OK** to enter it, **Рус/Eng** to switch alphabet, and **Готово** to close the keyboard. |
| Headset/media keys | Passes play, pause, next or previous commands to the website on a best-effort basis. |

## Build from a terminal

```bash
./gradlew :app:assembleDebug
```

The debug output is `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture notes

The site owns login, search, playlists, favourites, lyrics, content restrictions, and HTML5 audio playback. Keeping that logic on the provider's authenticated website avoids collecting user credentials or duplicating provider APIs. The native shell provides only device integration: session persistence, a TV-sized interaction method, media-key routing, and visual framing.

Native `MediaSession` routing cannot make a webpage more permissive than the underlying operating system or provider. For best background audio behavior, leave the app's battery optimization disabled on the TV and start playback with a normal remote click; the page then has a user gesture and may begin its own HTML5 media session.

## Verification checklist

- [ ] App appears in the Android TV launcher.
- [ ] VK login survives a process restart.
- [ ] Arrow keys show and move the cursor.
- [ ] OK opens a selected website element.
- [ ] Audio begins after selecting a track in VK.
- [ ] Back navigates web history.
- [ ] Media remote keys are tested against the current VK DOM on the target device.
