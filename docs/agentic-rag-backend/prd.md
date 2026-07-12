# Know Studio Agentic RAG Backend

## Product Goal

Know Studio is a single-company internal knowledge assistant. Administrators
maintain Teams, KnowledgeBases, documents, access grants and evaluations.
Ordinary users sign in and use Q&A plus their own conversation history.

## Access Model

- System roles: `ADMIN`, `USER`.
- Team roles: `TEAM_ADMIN`, `MEMBER`.
- KnowledgeBase visibility: `COMPANY`, `TEAM`, `PRIVATE`.
- Team grant permissions: `READ`, `MANAGE`.
- All Team members can read a granted KnowledgeBase.
- Only Team Admins can exercise a `MANAGE` grant.
- Company-visible KnowledgeBases are readable by every authenticated user.
- Conversations and messages are owned directly by the current user.

## Core Capabilities

- Sa-Token registration, login and authorization.
- Team and membership administration.
- KnowledgeBase CRUD and cross-Team grants.
- Chunked/resumable document upload, MinIO storage and RabbitMQ ingestion.
- Structured parsing, chunking, BGE-M3 embeddings, pgvector and Elasticsearch indexing.
- Vector/BM25 hybrid retrieval, RRF fusion, neighbor expansion and optional reranking.
- Agent intent routing, MCP tools, deep-thinking events, citations and SSE streaming.
- User-owned conversation memory and summary compression.
- KnowledgeBase-scoped evaluation datasets and three-mode retrieval ablation.
- Prometheus, OpenTelemetry and optional Langfuse observability.

## Security Requirements

- Client-provided KnowledgeBase IDs can only narrow the server-authorized set.
- Vector, keyword, neighbor, evidence, citation and document reads all enforce the same KnowledgeBase boundary.
- Another user cannot list, open, rename or delete a conversation they do not own.
- Ordinary Team members cannot mutate documents, grants or evaluations.
- No business table or public API depends on a tenant/space selection context.

## Acceptance

- Flyway applies from both an existing migrated database and an empty database.
- Document upload reaches `READY` and is retrievable only by authorized users.
- Multi-Team and cross-Team grants work without switching context.
- Unauthorized content and evaluation operations return 403; foreign conversations return 404.
- Agent SSE emits terminal events and citations remain KnowledgeBase-scoped.
- Maven tests and frontend lint, typecheck and production build pass.
