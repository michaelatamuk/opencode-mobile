# Contributing

Thanks for wanting to improve **opencode mobile**. This is an independent,
unofficial Android client for the OpenCode HTTP API.

## Before you start

- Read [`README.md`](README.md) — especially **Invariants** and **Limitations**.
- The API contract notes live in [`docs/server-api-notes.md`](docs/server-api-notes.md).
- Check existing issues before opening a new one.

## Issues

A useful report includes:

- App version (Settings → About) and Android version/device.
- OpenCode server version.
- The exact screen and steps.
- What you expected vs what happened.
- Diagnostics: Settings → About → **Export diagnostics** (contains no password).

## Pull requests

- Keep each PR focused; describe the user-visible change.
- Match the surrounding Compose style and keep strings localized (EN/RU).
- Run `./gradlew testReleaseUnitTest` before opening.
- Do not add a second composer action, an ongoing notification in
  *Only on finish* mode, or a cursor query with `order` (see invariants).
- Do not commit credentials, server URLs from your own machine, keystores or
  `local.properties`.
- Do not add OpenCode brand assets to new places; compatibility is stated as
  “Works with OpenCode”, not as official affiliation.

## License

By contributing you agree that your changes are licensed under the project’s
[MIT license](LICENSE).
