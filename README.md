# OpenCode Mobile

An Android client for an [OpenCode](https://opencode.ai) server, built with Kotlin,
Jetpack Compose and the OpenCode V2 HTTP API.

It talks directly to your own OpenCode server — over your LAN or through the
OpenCode remote tunnel — and shows the same sessions as the TUI.

## Features

- Session list with live refresh, search, date groups, project filter chips,
  pinning, unread dots, message previews, swipe-to-delete and pull-to-refresh
- Chat with streaming updates (SSE `/api/event` with reconnect, polling fallback)
- Markdown rendering: headings, lists, bold, inline code, fenced code blocks
  with copy, tables, and clickable links (markdown and bare URLs)
- Expandable tool-call cards: full input JSON, output text, and errors
- Collapsible reasoning ("Thought for Ns") blocks
- Agent forms rendered as a sheet (text, number, switch, single/multi-select)
  so the agent never stalls waiting for an answer
- Queued / steered message indicator above the composer
- Image attachments from the photo picker, sent inline (up to 8)
- Permission prompts: allow once / always / reject
- Single morphing send/stop button, plus steer while a turn is running
- Commands & skills sheet: browse server slash commands and skills, insert a
  command or attach skills to the next prompt
- Model and agent picker with per-session cost and token usage
- Context meter with the model's window size and one-tap compaction
- Spend sheet: total, daily history for the last 7 days, breakdown by model,
  and the most expensive sessions
- Per-session notification mute from the row menu
- Diff sheet with per-file statistics; when the project has no VCS diff it
  lists the files the agent edited in the conversation, and any file can be
  opened in a monospace viewer
- Fallback server URL, tried automatically when the main address stops
  answering (LAN at home, tunnel outside)
- Pairing code entry or QR scan: paste `opencode pair`'s code/link or scan it,
  and the app trades it for the password itself
- Per-file revert in the diff sheet: validated reverse-patch, all-or-nothing,
  text files only
- Skills detail view with the skill's full markdown content
- Schedules per session: one-off/interval prompts with pause, resume, delete
- Connections page: MCP servers with connect/disconnect, integration search and
  API-key linking
- Shell runner per session: execute a command and read its output
- Attach any file (up to 2 MB), not just images
- Queued messages can be steered or cancelled; pending prompts support send-now
- A/B comparison highlights added and removed words
- Named server profiles: add the current server, switch, rename, delete
- Finish-only safety net: a periodic check catches turns that ended while the
  watcher was paused
- Pending prompt list in Settings with per-item removal
- Queued/steered messages can be cancelled from the chat queue sheet
- Model A/B: branch before the last prompt and replay it on another model, with
  an original-vs-branch comparison sheet
- Offline queue: a prompt sent while the server is unreachable is delivered by
  the watcher when it reconnects; pending items are listed in Settings
- Spend sheet shows month-to-date and a projected month; daily budget alert
- Widget lists up to three running sessions with a stop action; tile shows count
- Grouped settings with live summaries, sub-pages, segmented controls, theme
  preview and labelled accent swatches
- Voice input (system dictation) and read-aloud for assistant messages
- Biometric lock on launch and optional screenshot / recents blocking
- Home-screen widget and quick-settings tile showing the watcher status
- Saved presets: agent + model + opening prompt, applied in one tap
- Appearance: wallpaper (Material You), classic, or AMOLED black themes with a
  choice of four accent colors
- Running cards show elapsed time and can be stopped from the row menu
- Branch a session, branch from any user message, undo the last turn, and
  compact the context — all from the chat menu
- In-session search, pagination, jump-to-latest, and draft sessions that are
  only created on the first send
- Background watcher (foreground service) driven by server events:
  - two notification modes: "only on finish" (default, no permanent
    notification) and "always" (watcher pinned in the shade)
  - progress notification with the current tool and a live elapsed timer
  - finished notification with inline reply and Continue / Stop / Explain /
    Retry actions
  - permission and agent-form notifications with Allow / Reject actions
  - optional watchdog that stops runs exceeding a chosen time
- Launcher shortcuts for the three most recent sessions
- Share target: send an image or text from any app into a session
- Encrypted credential storage (keystore-backed preferences), session-list cache
- App shortcuts and `opencode://session/<id>` deep links
- Material You dynamic color on Android 12+, light and dark themes, edge-to-edge
- English and Russian localization
- Signed release builds in CI with a stable key
- Architecture: repository + ViewModels with `StateFlow`, typed API errors,
  reconnecting event stream
- Unit tests for message merging and error mapping

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

Personal sideload build, signed release APKs published on GitHub Releases.
Not published to Play.

Known gaps: no artifact gallery (disabled on most servers), no dependency bump
or `targetSdk 36` yet, no widget configuration options, and R8 is disabled
until a shrunk build can be verified on a device.
