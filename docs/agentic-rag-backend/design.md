# Know Studio Backend Architecture

## Module Layout

The backend is a Java 21 modular monolith. IntelliJ IDEA should import the root
`pom.xml`; there is no extra directory layer around the modules.

```text
platform-core       response, errors, trace, rate limit, SSE, context, IDs
platform-ai         model providers, routing, circuit breaking, embedding, rerank
module-identity     users, Teams, memberships and authentication
module-knowledge    KnowledgeBases, grants, documents and ingestion
module-retrieval    vector/keyword retrieval, fusion, rerank and evidence
module-agent        intent routing, tools, MCP and streamed orchestration
module-conversation user-owned sessions, messages and memory
module-evaluation   KnowledgeBase-scoped datasets, samples and runs
bootstrap           application entry point, configuration and Flyway
```

Business modules expose contracts through their `api` packages. Cross-module
imports of another module's `domain`, `infra` or `rest` packages are rejected by
ArchUnit.

## Ownership Model

```text
User -> TeamMember -> Team
Team -> KnowledgeBaseTeamGrant -> KnowledgeBase
KnowledgeBase -> Document -> Chunk -> Embedding
User -> Conversation -> Message / Memory
KnowledgeBase -> Evaluation Dataset -> Sample / Run
```

Content, ingestion and evaluation rows use `knowledge_base_id`. Conversation
queries use `user_id` plus `session_id`. Team membership is an authorization
relationship, not a request-selected tenant context.

## Authorization Flow

1. Authenticate the current user.
2. Load Team IDs and Team roles.
3. Add company-visible KnowledgeBases.
4. Treat every Team grant as READ for members.
5. Allow the grant's MANAGE permission only for `TEAM_ADMIN` users.
6. Add creator ownership and system-admin access.
7. Intersect optional client scope and intent-selected scope with this set.
8. Carry the effective IDs through retrieval, evidence and citation queries.

## Storage and Retrieval

- PostgreSQL + MyBatis-Plus for standard CRUD.
- Mapper XML for pgvector, JSONB, atomic state transitions and batch SQL.
- MinIO stores source files and upload chunks.
- RabbitMQ drives retryable ingestion jobs with manual acknowledgement.
- BGE-M3 creates 1024-dimensional embeddings stored in pgvector.
- Elasticsearch stores KnowledgeBase metadata for BM25 keyword search.
- Hybrid retrieval combines vector and keyword candidates with RRF, optional
  neighbor expansion and reranking.

## Chat and Evaluation

Chat APIs do not require Team or KnowledgeBase selection. The server computes
the readable set, routes intent, retrieves evidence and persists the actual
KnowledgeBase IDs and citations used by the answer. SSE events are `token`,
`thinking`, `tool_call`, `tool_result`, `citation`, `done` and `error`.

Evaluation datasets belong to one KnowledgeBase and require effective MANAGE
permission. Ablation runs execute independent `VECTOR_ONLY`, `HYBRID` and
`HYBRID_RERANK` retrieval calls and persist Recall@K and latency metrics.

## Migration

Flyway V6-V10 introduced and activated Team, KnowledgeBase, user-owned
conversation and evaluation ownership. V11 removed the legacy ownership tables,
indexes and columns. Applied migrations are immutable; all later changes must be
new versioned scripts.
