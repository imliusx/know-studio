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

## Workspace Isolation

- Every core business API takes an explicit workspaceId in its path.
- The current workspace is global application state and the last valid choice may be persisted locally.
- Every workspace-scoped TanStack Query key includes workspaceId. Forbidden examples:

```ts
['documents']
['sessions']
```

Required shape:

```ts
['documents', workspaceId, filters]
['sessions', workspaceId]
```

- Switching workspace aborts active SSE/upload requests and resets transient selections.
- Frontend role checks improve usability only. Backend authorization remains mandatory.

## SSE Events

- Decode `event:` plus all `data:` lines at the API boundary.
- Use a discriminated union for `token`, `thinking`, `tool_call`, `tool_result`, `citation`, `done` and `error`.
- One reducer owns stream state transitions. Rendering components do not implement separate event parsing branches.
- The reducer tolerates duplicate `done`, `error` after partial output, chunks split at arbitrary byte boundaries and a final frame without a blank-line terminator.
- Deduplicate citations by stable chunk/document identity and correlate tool results to the tool call when an identifier is available.
- Every stream owns an AbortController and is aborted on explicit stop, session change, workspace change and component unmount.

## Permissions

- OWNER: all workspace operations.
- ADMIN: member, document, Chat and evaluation operations; no ownership transfer or workspace deletion.
- MEMBER: Chat and read-only access to READY documents/citations.
- Do not show management actions to MEMBER, but still handle backend 403 because UI visibility is not a security boundary.

## React and Query State

- Prefer derived state and event handlers over synchronous `setState` in effects.
- Do not call `Date.now`, `Math.random` or other impure functions during render; initialize through stable factories or refs outside render-sensitive paths.
- Avoid copying query data into component state unless the user can independently edit it. If projection is needed, derive it with stable memoization.
- Mutations invalidate the narrowest workspace-scoped query keys.
- Loading, empty, forbidden, rate-limited and failed states must terminate explicitly; no request may leave an indefinite skeleton/spinner.

## Existing Product Scope

- Ordinary and admin routes are both formal product surfaces.
- User, Q&A, intent, data-channel, task and settings features are preserved while their ARAG backend domains are built later.
- Integration work must not hide or delete unrelated navigation entries to reduce scope.
