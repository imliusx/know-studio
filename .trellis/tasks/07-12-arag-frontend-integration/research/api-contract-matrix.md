# ARAG Core API Contract Matrix

## Baseline

- Frontend API base: `VITE_API_BASE_URL ?? /api`
- Backend response: `{ success, code, data, message }`
- Authentication: Sa-Token value sent as `Authorization: Bearer <token>`
- Workspace isolation: every core business URL contains `{workspaceId}`
- Current frontend typecheck/build pass; lint has 20 errors and 3 warnings before migration.
- Backend `mvn -q clean validate test` passes on the current Java 26 machine while compiling for Java 21.

## Authentication

| Frontend current | ARAG target | Action |
|---|---|---|
| `POST /auth/register` with username/email/displayName/password | `POST /auth/register` with email/displayName/password | Replace request and response types. ARAG returns `AuthSession`. |
| `POST /auth/login` with loginId/password | `POST /auth/login` with email/password | Replace request and map `tokenValue` + `user`. |
| `GET /auth/me` | Same path, different `CurrentIdentity` fields | Replace `userCode`, `mustChangePassword` assumptions. |
| `POST /auth/logout` | Same | Keep and clear local state after completion. |
| `POST /auth/refresh` | Not available | Remove from core auth restoration; validate token through `/auth/me`. |
| `POST /auth/reset-password` | Not available | Preserve non-core page but do not call it from ARAG core flow. |
| `POST /account/change-password` | Not available | Preserve settings page; backend support is a later domain task. |

ARAG `AuthSession`:

```text
user: { userId, email, displayName, systemRole }
tokenName: string
tokenValue: string
```

## Workspaces

| Frontend current | ARAG target | Action |
|---|---|---|
| `GET /groups/my` | `GET /workspaces` | Add backend list endpoint returning workspace + current role. |
| `POST /groups` | `POST /workspaces` | Adapt response from numeric id to `{ workspaceId }`. |
| `GET /groups/{id}/members` | `GET /workspaces/{id}/members` | Add backend query endpoint. |
| invitations/join requests/leave | No ARAG equivalent | Preserve existing formal pages/API module; migrate in a later identity expansion task. |

Required workspace list projection:

```text
workspaceId, name, description, role, ownerId, createdAt
```

## Documents

| Frontend current | ARAG target | Action |
|---|---|---|
| `GET /documents?groupId=...` | `GET /workspaces/{workspaceId}/documents` | Add backend list endpoint and migrate query keys. |
| `POST /documents/upload/init` | `POST /workspaces/{workspaceId}/documents/uploads` | Rename fields: fileHash→contentHash, chunkCount→totalChunks. |
| `POST /documents/upload/chunks` | `PUT /workspaces/{workspaceId}/documents/uploads/{sessionId}/chunks/{chunkIndex}` | Multipart field becomes `file`; hash moves to `X-Chunk-SHA256`. |
| `GET /documents/upload/{uploadId}` | `GET /workspaces/{workspaceId}/documents/uploads/{sessionId}` | Adapt progress type. |
| `POST /documents/upload/{uploadId}/complete` | Same workspace-scoped complete path | Response becomes `{ documentId }`. |
| `GET /documents/{id}/preview` | `GET /workspaces/{workspaceId}/documents/{documentId}` | Use `DocumentView`; preview-specific behavior stays in UI projection. |
| `DELETE /documents/{id}` | Workspace-scoped DELETE | Add backend endpoint. |
| `POST /documents/{id}/retry-ingestion` | Workspace-scoped retry | Add backend endpoint. |

## Conversations and Agent

| Frontend current | ARAG target | Action |
|---|---|---|
| `POST /assistant/sessions` | `POST /workspaces/{workspaceId}/sessions` | Request title/toolMode/deepThinking. |
| `GET /assistant/sessions` | `GET /workspaces/{workspaceId}/sessions` | Add backend endpoint. |
| `GET /assistant/sessions/{id}/context` | `GET /workspaces/{workspaceId}/sessions/{id}/context` | Adapt context and question parameter. |
| PATCH/DELETE assistant session | Workspace-scoped PATCH/DELETE session | Add backend endpoints. |
| `POST /assistant/chat` | No non-stream ARAG endpoint | Remove from core Chat flow. |
| `POST /assistant/chat/stream` | `POST /workspaces/{workspaceId}/agent/chat/stream` | Replace request and event parser. |

SSE events:

```text
token | thinking | tool_call | tool_result | citation | done | error
```

The event name is the discriminant. Payload decoding belongs in the API layer; Chat components consume typed events or reducer state and must not cast raw JSON.

## Evaluation

| Capability | ARAG current | Required addition |
|---|---|---|
| Create dataset | `POST /workspaces/{workspaceId}/evaluations/datasets` | None |
| Add sample | `POST /.../datasets/{datasetId}/samples` | None |
| Run ablation | `POST /.../datasets/{datasetId}/runs/ablation?topK=` | None |
| List datasets | Missing | Add GET |
| List samples | Missing | Add GET |
| List runs | Missing | Add GET |

Evaluation management requires workspace ADMIN or OWNER. MEMBER requests return HTTP 403 / `A0403`.

## Error Handling

| HTTP | Business code | Frontend behavior |
|---:|---|---|
| 400 | `A0400` / `A0500` | Inline validation or action toast; stop loading. |
| 401 | `A0401` | Clear auth state and redirect to sign-in once. |
| 403 | `A0403` | Keep session; show forbidden feedback and disable unauthorized action. |
| 404 | `A0404` | Remove stale selection or show not-found state. |
| 409 | `A0409` | Show conflict and refresh affected query. |
| 429 | `A0429` | Preserve current UI state, show cooldown feedback, allow later retry. |
| 500 | `B0500` | Show recoverable service error; never leave infinite loading. |

## Non-Core APIs

`admin-users.ts`, `qa.ts`, group invitations/join requests, intent, data-channel, task and settings APIs are formal product functionality. They are not deleted in this task. They remain outside the ARAG core migration until matching backend domains are planned.
