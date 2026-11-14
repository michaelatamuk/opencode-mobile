# Security

opencode mobile talks directly to a self-hosted OpenCode server. Please report
security issues **privately**, not in a public issue.

## Reporting

Use GitHub’s private vulnerability reporting:
**Security → Advisories → Report a vulnerability** on
`michaelatamuk/opencode-mobile`.

Include:

- Affected app version.
- Steps to reproduce and impact.
- Whether the server was reached over LAN or a tunnel.
- Relevant diagnostics (never include the password).

## Scope

- Credential handling (`SecurePrefs`, profiles, pairing).
- Notification actions and receivers.
- Deep links (`opencode://session/<id>`) and share targets.
- The local watcher service and its exported components.
- File read/write and per-file revert paths.

## Design notes

- Authentication is HTTP Basic `opencode:<password>` to your own server.
- Credentials are stored in keystore-backed encrypted preferences.
- Diagnostics export excludes the password.
- The app has no analytics, telemetry or third-party backend.
- Release builds are signed with a stable key; verify the release page.
