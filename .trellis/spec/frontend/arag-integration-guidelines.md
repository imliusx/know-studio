# ARAG Frontend Integration Guidelines

## API Ownership

- `src/api` owns wire-level request/response types, `unknown` payload decoding and endpoint paths.
- Components consume typed domain projections; they must not cast raw `ApiResponse` or SSE payload fields.
- Use the shared Axios instance for request/response APIs and one shared fetch-based SSE helper for streamed POST responses.
- The ARAG response envelope is `{ success, code, data, message }`. Never infer success from HTTP 200 alone.
- Do not maintain duplicate long-lived clients for old and ARAG core endpoints. Preserve non-core clients only while their formal features still depend on them.

## Authentication

- Use backend `AuthSession.tokenValue` as the access token and `AuthSession.user` as the current identity.
- Send the token in the backend-supported Authorization header for Axios and SSE.
- Restore a persisted session by calling `/api/auth/me`; the current backend has no refresh-token endpoint.
- A 401 clears local authentication once and redirects to sign-in. A 403 must not clear the session.
- Never store provider keys or service credentials in frontend state or Vite environment variables exposed to the browser.

## Team and KnowledgeBase Access

- Ordinary users do not select a Workspace or Team before Chat. The backend derives readable KnowledgeBases from the authenticated user.
- Document and evaluation administration takes an explicit knowledgeBaseId.
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
