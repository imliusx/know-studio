# Backend Delivery Status

## Completed

- [x] Maven multi-module skeleton and ArchUnit boundaries.
- [x] PostgreSQL, pgvector, Elasticsearch, MinIO, Redis, RabbitMQ and Ollama integration.
- [x] Sa-Token authentication and system roles.
- [x] Team membership and Team Admin authorization.
- [x] KnowledgeBase visibility and cross-Team READ/MANAGE grants.
- [x] Chunked upload, instant upload, ingestion state machine and retry recovery.
- [x] Vector and keyword retrieval, RRF, neighbor expansion, rerank fallback and evidence grading.
- [x] User-owned conversations, summary compression and SSE Agent orchestration.
- [x] MCP tool adapters and deep-thinking events.
- [x] KnowledgeBase-scoped evaluation and three-mode ablation.
- [x] Prometheus, OpenTelemetry and optional Langfuse integration.
- [x] Frontend integration without tenant/space selection context.
- [x] Flyway V11 final ownership schema cleanup.

## Validation Commands

```bash
docker compose up -d postgres redis rabbitmq minio elasticsearch ollama
mvn -q clean validate test

cd web
pnpm lint
pnpm typecheck
pnpm build
```

Migration validation must cover both an existing populated database and an empty
database. After migration, assert that all legacy ownership tables and columns
are absent while Team, KnowledgeBase, document, conversation and evaluation
counts remain correct.

## Local End-to-End Flow

1. Register an administrator and ordinary users.
2. Create two Teams and add a multi-Team member.
3. Create a Team KnowledgeBase and grant the second Team READ.
4. Upload a document and wait for `READY`.
5. Verify the member retrieves authorized evidence and an outsider receives 403.
6. Verify the member cannot mutate the KnowledgeBase or create evaluations.
7. Create a conversation, stream a deep-thinking answer and verify another user receives 404 for its context.
8. Run the three retrieval evaluation modes and verify all run records are persisted.
