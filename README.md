# Know Studio

Know Studio is organized as a Maven multi-module backend with a separate frontend application.

## Project layout

- `pom.xml`: Maven reactor root. Open or import this file in IntelliJ IDEA to load all backend modules.
- `common`: shared response, error, tracing, context and infrastructure helpers.
- `auth`, `knowledge`, `search`, `chat`, `agent`, `eval`: business capability modules.
- `ai`: provider-neutral Chat, embedding, rerank and model-routing infrastructure.
- `app`: executable Spring Boot application and module assembly.
- `web`: React frontend application.
- `docs/agentic-rag-backend`: backend requirements, design, and implementation notes.
- `archive/legacy-backend`: archived copy of the previous single-module backend and deployment files.

## Backend

Requirements: JDK 21 and Maven 3.9+.

```bash
mvn clean test
mvn -DskipTests install
mvn -f app/pom.xml spring-boot:run
```

The backend listens on port `8080` by default. Supporting services are defined in the root `docker-compose.yml`.

Start the local dependencies before the backend:

```bash
cp .env.example .env
docker compose up -d postgres redis rabbitmq minio elasticsearch ollama
docker compose up ollama-model-init
```

The default AI setup matches the legacy backend: DashScope `glm-5` handles Chat
and Ollama `bge-m3` provides 1024-dimensional embeddings. Configure
`DASHSCOPE_API_KEY` in `.env`; never commit the real key. Override
`DASHSCOPE_CHAT_MODEL` when using another compatible DashScope model.

Ollama `qwen2.5:1.5b` remains a lower-priority Chat fallback. Set
`OLLAMA_CHAT_FALLBACK_ENABLED=false` to disable it, or change
`OLLAMA_CHAT_MODEL` to use another local fallback model.

API documentation is available at `/doc.html` (Knife4j) and `/v3/api-docs`.
Prometheus metrics are exposed at `/actuator/prometheus`.

Start the optional observability stack with:

```bash
docker compose --profile observability up -d prometheus tempo grafana
OTEL_ENABLED=true mvn -pl app spring-boot:run
```

Grafana listens on `3000`, Prometheus on `9090`, and Tempo accepts OTLP on
`4317`/`4318`. Langfuse export is optional and uses `LANGFUSE_ENABLED`,
`LANGFUSE_BASE_URL`, `LANGFUSE_PUBLIC_KEY`, and `LANGFUSE_SECRET_KEY`; raw
prompts and model output are not sent by the built-in observation payload.

## Frontend

```bash
cd web
pnpm install
pnpm dev
```

The frontend listens on `5174` and proxies `/api` to `http://localhost:8080` by
default. Set `VITE_DEV_PROXY_TARGET` before starting Vite to use another backend.

## Local demo flow

1. Register or sign in. Ordinary users can open Chat immediately without selecting an organization context.
2. A system administrator creates Teams and a KnowledgeBase, then adds Team members and grants `READ` or `MANAGE` access.
3. A system administrator or authorized Team Admin uploads a text, PDF, Markdown, or Office document and waits for `READY`.
4. A Team member opens Chat and asks a question. The backend derives all readable KnowledgeBases and returns only authorized citations.
5. Enable deep thinking to inspect `thinking`, citation, and answer events.
6. An administrator opens **检索评测**, creates a dataset, adds relevant Chunk IDs, and runs the three-mode ablation comparison.

The product is a single-company internal assistant. System `ADMIN` users manage
all Teams and KnowledgeBases. A Team `MEMBER` receives read access from Team
grants; only `TEAM_ADMIN` users can exercise a `MANAGE` grant. Conversations are
owned directly by users, and content/evaluation data is isolated by
`knowledge_base_id` throughout storage and retrieval.

## Core API boundaries

- Identity and administration: `/api/auth`, `/api/teams`, `/api/knowledge-bases`
- Documents: `/api/knowledge-bases/{knowledgeBaseId}/documents/**`
- Conversations and Chat: `/api/conversations`, `/api/agent/chat/stream`
- Retrieval: `/api/retrieval/search`
- Evaluation: `/api/knowledge-bases/{knowledgeBaseId}/evaluations/**`
