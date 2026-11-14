# Server API notes

Facts verified against a live OpenCode V2 server (`/openapi.json`) while building
v0.6.0. Useful when adding features without a local copy of the server source.

## Auth

HTTP Basic, user `opencode`, password = the server password.

## Events (`/api/event`, SSE)

Every line is `data: <json>` with a top-level shape:

```json
{ "type": "session.tool.input.started", "created": 1794617113866, "data": { ... } }
```

Status and run lifecycle:

| Event | Data | Use |
|---|---|---|
| `session.status` | `sessionID`, `status.type` = `idle` / `busy` / `retry` | authoritative run state |
| `session.idle` | `sessionID` | turn finished |
| `session.execution.started` | `sessionID` | turn started |
| `session.execution.succeeded` / `failed` / `interrupted` | `sessionID` | terminal states |
| `session.tool.input.started` | `sessionID`, `id`, `name` | current tool name |
| `session.tool.called` | `sessionID`, `id`, `input`, `executed` | tool invoked (no name field) |
| `session.tool.progress` | `sessionID`, `id`, `metadata` | progress metadata |
| `session.tool.success` / `failed` | `sessionID`, `id`, ... | tool finished |
| `session.step.started` / `first-token` / `streamed` / `ended` | step lifecycle | finer-grained progress |
| `session.revert.staged` / `cleared` / `committed` | `revert`, `to` | undo state |
| `session.forked` | `sessionID`, `parentID`, `boundary` | branch created |
| `session.usage.updated` / `recorded` | `cost`, `tokens` | spend |
| `permission.asked` / `replied` | request | permission prompts |
| `form.created` / `replied` / `cancelled` | `form` | agent questions |

Note: `session.status: busy` and `session.execution.started` both mark a run;
only `session.status: idle` and `session.idle` reliably end it.

## Endpoints used by the app

| Endpoint | Notes |
|---|---|
| `GET /api/session?limit=N` | newest first, each item is `Session.Info` |
| `GET /api/session/{id}/message?limit=50&order=desc` | newest first; cursor pages must **not** send `order` |
| `GET /api/session/active` | object keyed by running session IDs |
| `POST /api/session/{id}/prompt` | `{ text, files?, delivery? }` |
| `POST /api/session/{id}/interrupt` | stop the current turn |
| `POST /api/session/{id}/fork` | `{ before: messageID \| null }` → `{ data: Session.Info }` |
| `POST /api/session/{id}/revert/stage` | `{ messageID, files? }` → `{ data: Session.Revert }` |
| `POST /api/session/{id}/revert/commit` | 204; applies the staged boundary |
| `DELETE /api/session/{id}/revert` | 204; clears a staged revert |
| `POST /api/session/{id}/compact` | admits a compaction, returns inbox compaction |
| `GET /api/session/{id}/context` | messages since the last compaction |
| `POST /api/pair` | → `{ code, expires_in }` for a `/auth/connect/:code` link |
| `GET /api/permission/request` | pending permission requests |
| `POST /api/session/{id}/permission/{reqID}/reply` | `{ decision }` |
| `GET/POST /api/session/{id}/form[...]` | agent forms |
| `GET /api/session/{id}/inbox` | queued / steered messages |
| `GET /api/session/{id}/diff` | `FileDiff.Info`: `file, patch, additions, deletions, status` |

Revert semantics: staging marks a boundary; `files: true` also restores file
changes made after it. Commit applies it; delete clears it. A staged revert is
therefore reversible until commit.
