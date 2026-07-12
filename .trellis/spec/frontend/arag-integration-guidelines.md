# ARAG Frontend Integration Guidelines

## API Ownership

- `src/api` owns wire-level request/response types, `unknown` payload decoding and endpoint paths.
- Components consume typed domain projections; they must not cast raw `ApiResponse` or SSE payload fields.
- Use the shared Axios instance for request/response APIs and one shared fetch-based SSE helper for streamed POST responses.
- The ARAG response envelope is `{ success, code, data, message }`. Never infer success from HTTP 200 alone.
- Do not maintain duplicate long-lived clients for old and ARAG core endpoints. Preserve non-core clients only while their formal features still depend on them.

## Scenario: Snowflake ID Browser Contract

### 1. Scope / Trigger

- Applies to every ARAG API or SSE payload that exposes a Snowflake `BIGINT` identifier to browser code.

### 2. Signatures

- Backend response ID: `@JsonLongId long id` or `@JsonLongIds List<Long> ids`.
- Frontend entity ID: `EntityId = string` from `src/api/id.ts`.
- Backend request path/body may continue accepting `long`; Jackson and Spring MVC parse decimal strings without converting them in JavaScript.

### 3. Contracts

- Entity IDs such as `userId`, `teamId`, `knowledgeBaseId`, `documentId`, `chunkId`, `sessionId`, `datasetId` and upload session IDs are decimal JSON strings.
- File sizes, chunk indexes, counts, scores, latency and pagination values remain JSON numbers.
- API boundary code must preserve IDs as strings through query keys, stores, route params, request bodies and SSE citation decoding.

### 4. Validation & Error Matrix

- Missing or non-decimal response ID -> reject/ignore the malformed projection at the API boundary.
- Numeric legacy ID -> accept only when it is a positive JavaScript safe integer; never call `Number(...)` on a Snowflake ID string.
- Session ID rounded by JavaScript -> backend returns `会话不存在`; treat this as an ID contract defect, not an authorization workaround.

### 5. Good/Base/Bad Cases

- Good: request body contains `{ "sessionId": "351744060462989312" }`.
- Base: local-only row indexes and animation/message counters use `number` because they never cross the API boundary.
- Bad: `sessionId: Number(response.data.id)` or a store typed as `number | null` for a KnowledgeBase ID.

### 6. Tests Required

- Backend serialization test asserts annotated IDs are strings while ordinary `long` values remain numbers.
- Frontend typecheck must compile `src` with `tsc -b`; a root `tsc --noEmit` invocation is insufficient for this referenced project.
- E2E creates a Snowflake-ID session, verifies JSON type `string`, sends the same string ID to SSE and receives `done` without `会话不存在`.
- Historical citation metadata test converts numeric KnowledgeBase/document/chunk IDs to strings before response serialization.

### 7. Wrong vs Correct

```ts
// Wrong
const sessionId = Number(payload.id)

// Correct
const sessionId: EntityId = payload.id
```

## Authentication

- Use backend `AuthSession.tokenValue` as the access token and `AuthSession.user` as the current identity.
- Send the token in the backend-supported Authorization header for Axios and SSE.
- Restore a persisted session by calling `/api/auth/me`; the current backend has no refresh-token endpoint.
- A 401 clears local authentication once and redirects to sign-in. A 403 must not clear the session.
- Never store provider keys or service credentials in frontend state or Vite environment variables exposed to the browser.

## Team and KnowledgeBase Access

- Ordinary users do not select a tenant or Team context before Chat. The backend derives readable KnowledgeBases from the authenticated user.
- Document and evaluation administration takes an explicit knowledgeBaseId.
- The document administration overview loads documents for every manageable
  KnowledgeBase. A KnowledgeBase detail route loads its route `knowledgeBaseId`;
  neither view may depend on the unrelated globally selected KnowledgeBase.
- Every knowledge-scoped TanStack Query key includes knowledgeBaseId. Forbidden examples:

```ts
['documents']
['evaluation-runs']
```

Required shape:

```ts
['documents', knowledgeBaseId, filters]
['evaluation-runs', knowledgeBaseId]
```

- Switching the selected admin KnowledgeBase aborts uploads and resets document/evaluation selections.
- Chat sessions are user-owned and do not reset when a Team filter changes.
- Frontend role and grant checks improve usability only. Backend authorization remains mandatory.

## SSE Events

- Decode `event:` plus all `data:` lines at the API boundary.
- Use a discriminated union for `token`, `thinking`, `tool_call`, `tool_result`, `citation`, `done` and `error`.
- One reducer owns stream state transitions. Rendering components do not implement separate event parsing branches.
- The reducer tolerates duplicate `done`, `error` after partial output, chunks split at arbitrary byte boundaries and a final frame without a blank-line terminator.
- Deduplicate citations by stable chunk/document identity and correlate tool results to the tool call when an identifier is available.
- Every stream owns an AbortController and is aborted on explicit stop, session change and component unmount.

## Permissions

- System ADMIN: all Team, KnowledgeBase, document, Chat and evaluation operations.
- TEAM_ADMIN: Team membership plus KnowledgeBases with MANAGE grants.
- USER/MEMBER: Chat and citations from COMPANY or granted KnowledgeBases.
- Do not show management actions without MANAGE permission, but still handle backend 403 because UI visibility is not a security boundary.
- System ADMIN sees the complete administration navigation. Team Admin sees Team
  administration, and users with effective MANAGE permission see document and
  evaluation administration. Ordinary members see none of these entries.
- Citation downloads use the authenticated KnowledgeBase document-content API;
  never render a direct object-storage URL.

## React and Query State

- Prefer derived state and event handlers over synchronous `setState` in effects.
- Do not call `Date.now`, `Math.random` or other impure functions during render; initialize through stable factories or refs outside render-sensitive paths.
- Avoid copying query data into component state unless the user can independently edit it. If projection is needed, derive it with stable memoization.
- Mutations invalidate the narrowest knowledgeBase-scoped query keys.
- Loading, empty, forbidden, rate-limited and failed states must terminate explicitly; no request may leave an indefinite skeleton/spinner.

## Existing Product Scope

- Ordinary and admin routes are both formal product surfaces.
- User, Q&A, intent, data-channel, task and settings features are preserved while their ARAG backend domains are built later.
- Integration work must not hide or delete unrelated navigation entries to reduce scope.
