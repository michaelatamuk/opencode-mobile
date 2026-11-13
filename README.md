# OpenCode Mobile

An Android client for an [OpenCode](https://opencode.ai) server, built with Kotlin,
Jetpack Compose and the OpenCode V2 HTTP API.

It talks directly to your own OpenCode server — over your LAN or through the
OpenCode remote tunnel — and shows the same sessions as the TUI.

## Features

- Session list with live refresh, cost and relative timestamps
- Chat with streaming updates (SSE `/api/event`, polling fallback)
- Markdown rendering: headings, lists, bold, inline code, fenced code blocks
  with copy, and tables
- Expandable tool-call cards: full input JSON, output text, and errors
- Collapsible reasoning ("Thought for Ns") blocks
- Image attachments from the photo picker, sent inline
- Permission prompts: allow once / always / reject
- Send while a turn is running (`delivery: "steer"`) and a separate stop button
- Background notifications when a session finishes, via a foreground service
  watching `/api/session/active`
- High-priority permission notifications with Allow / Reject actions
- Share target: send an image or text from any app into a session
- Paginated history (loads older messages while scrolling up)
- Light and dark themes, edge-to-edge, Material 3

## Requirements

- Android 8.0+ (minSdk 26), targetSdk 35
- JDK 17 and the Android SDK (compileSdk 35)
- An OpenCode server with a password. Username is always `opencode`.

## Build

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

`local.properties` with `sdk.dir=...` is required locally and is git-ignored.

## Configure

On first launch enter the server URL and password:

- LAN: `http://<your-ip>:28894`
- Remote: the OpenTunnel URL printed by `opencode pair --remote`

Authentication is HTTP Basic, `opencode:<password>`.

## Status

Personal sideload build, debug-signed. Not published to Play.

Known gaps: message pagination (the API returns pages of ~50, newest first),
no model/agent picker, no slash commands, no diff view, credentials stored in
plain `SharedPreferences`.
