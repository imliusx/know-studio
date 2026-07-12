# Agent and Conversation Guidelines

## Scenario: Streaming Agent With Durable Conversation Memory

### 1. Scope / Trigger

Use this contract for `module-agent` and `module-conversation` changes that add
messages, context reconstruction, summary compression, tools, MCP calls, or SSE
events.

### 2. Signatures

- `ConversationApi` owns session creation, owner-scoped message append, context
  loading, and summary maintenance.
- Async callers use `appendMessageForOwner(command, ownerUserId)` only after the
  request thread has authenticated the same owner.
- `AgentApi.streamChat(ChatRequest)` returns `Flux<ChatStreamEvent>`.
- SSE event names are `token`, `thinking`, `tool_call`, `tool_result`,
  `citation`, `done`, and `error`.
- MCP endpoints are configured with `MCP_WEB_SEARCH_URL`,
  `MCP_WEB_SEARCH_TOOL`, `MCP_BUSINESS_URL`, `MCP_BUSINESS_TOOL`, and
  `MCP_TIMEOUT`.

### 3. Contracts

- Every session lookup filters `user_id`, `session_id`, and active status explicitly.
- Message and memory queries join `sessions` so owner isolation remains visible in SQL.
- Retrieval computes readable KnowledgeBase IDs on the server. Optional request
  scopes are intersected with that set before vector, keyword, neighbor, rerank,
  citation, or generation steps run.
- Vector SQL and Elasticsearch keyword queries must filter by the same effective
  `knowledge_base_id` set. Evidence and citation payloads carry `knowledgeBaseId`.
- `session_memory.summarized_through_message_id` is the compression cursor.
  Subsequent compression only sends messages after that cursor to the model.
- The request thread captures the authenticated user before returning a Flux.
  Reactor callbacks must not depend on Sa-Token thread-local state.
- Synchronous retrieval and tool failures are created inside `Flux.defer` so
  the stream emits `error` instead of failing before SSE subscription.
- Remote MCP tools have higher priority than the local mock business fallback.

### 4. Validation & Error Matrix

- Session owned by another user -> `NOT_FOUND` without leaking whether the session exists.
- Empty effective KnowledgeBase scope -> `FORBIDDEN`; never fall back to unfiltered retrieval.
- Blank message, negative token count, or missing role -> `BAD_REQUEST`.
- Summary provider failure -> keep the persisted message, log one warning, and
  retry compression on a later append.
- No configured matching tool -> controlled business error, mapped to SSE
  `error` by Agent orchestration.
- MCP `CallToolResult.isError=true` -> controlled business error.
- Intent classifier timeout or malformed output -> deterministic heuristic
  fallback; do not block the chat request indefinitely.

### 5. Good/Base/Bad Cases

- Good: authenticate once, capture `userId`, then persist the assistant response
  with an owner-scoped internal API from the Reactor completion callback.
- Base: use the authenticated `ConversationApi.appendMessage` on a servlet
  request thread.
- Bad: call `IdentityApi.currentUser()` from an arbitrary model callback thread.
- Good: summarize message IDs `(cursor, latest]` and advance the cursor only
  after a nonblank summary is stored.
- Bad: summarize the entire conversation after every message once total count
  exceeds the threshold.
- Good: intersect requested KnowledgeBase IDs with `readableKnowledgeBaseIds()`
  before starting query planning or embeddings.
- Bad: trust client-supplied KnowledgeBase IDs or filter only citation rendering.

### 6. Tests Required

- Unit test all four intent routes and the tool-mode override.
- Unit test duplicate tool calls execute once per `ResultHolder` key.
- Unit test owner isolation and successful threshold-triggered compression.
- Regression test that summary provider failure does not remove the appended
  message.
- Integration test Flyway migrations, session creation/context loading, message JSONB
  round-trip, and representative SSE event ordering.
- Configure a real MCP server and chat/reasoning provider before claiming remote
  MCP or full generated-answer end-to-end verification.

### 7. Wrong vs Correct

#### Wrong

```java
return route(request, context, intent)
        .onErrorResume(this::errorEvent);
```

`route` may execute retrieval before the Flux exists, so its exception bypasses
`onErrorResume`.

#### Correct

```java
return Flux.defer(() -> route(request, context, intent))
        .onErrorResume(this::errorEvent);
```

The synchronous route work now belongs to the stream error boundary.

## Design Decisions

### User-Owned Conversations and KnowledgeBase-Scoped Retrieval

Conversation session rows are owned directly by users and contain no tenant or Team context. Public APIs use
`/api/conversations` and `/api/agent/chat/stream`; ownership is always resolved
from the authenticated user plus session ID. Retrieval accepts an optional set
of KnowledgeBase IDs, but `KnowledgeAccessApi` is the authority and request data
can only narrow its readable set.
