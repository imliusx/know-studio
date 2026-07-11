# Know Studio

Know Studio is organized as a Maven multi-module backend with a separate frontend application.

## Project layout

- `pom.xml`: Maven reactor root. Open or import this file in IntelliJ IDEA to load all backend modules.
- `platform-core`, `platform-ai`: shared platform modules.
- `module-*`: business modules.
- `bootstrap`: Spring Boot application entry point.
- `know-studio-ui`: frontend application.
- `docs/agentic-rag-backend`: backend requirements, design, and implementation notes.
- `archive/legacy-backend`: archived copy of the previous single-module backend and deployment files.

## Backend

Requirements: JDK 21 and Maven 3.9+.

```bash
mvn clean test
mvn -DskipTests install
mvn -f bootstrap/pom.xml spring-boot:run
```

The backend listens on port `8080` by default. Supporting services are defined in the root `docker-compose.yml`.

Start the local dependencies before the backend:

```bash
cp .env.example .env
docker compose up -d postgres redis rabbitmq minio elasticsearch ollama
docker compose up ollama-model-init
```

The default local AI setup uses `bge-m3` for 1024-dimensional embeddings and
`qwen2.5:1.5b` for Chat. Override `OLLAMA_EMBEDDING_MODEL` or
`OLLAMA_CHAT_MODEL` in `.env` when using another compatible Ollama model.

API documentation is available at `/doc.html` (Knife4j) and `/v3/api-docs`.
Prometheus metrics are exposed at `/actuator/prometheus`.

Start the optional observability stack with:

```bash
docker compose --profile observability up -d prometheus tempo grafana
OTEL_ENABLED=true mvn -pl bootstrap spring-boot:run
```

Grafana listens on `3000`, Prometheus on `9090`, and Tempo accepts OTLP on
`4317`/`4318`. Langfuse export is optional and uses `LANGFUSE_ENABLED`,
`LANGFUSE_BASE_URL`, `LANGFUSE_PUBLIC_KEY`, and `LANGFUSE_SECRET_KEY`; raw
prompts and model output are not sent by the built-in observation payload.

## Frontend

```bash
cd know-studio-ui
pnpm install
pnpm dev
```

The frontend listens on `5174` and proxies `/api` to `http://localhost:8080` by
default. Set `VITE_DEV_PROXY_TARGET` before starting Vite to use another backend.

## Local demo flow

1. Register or sign in, then create/select a workspace.
2. Upload a text, PDF, Markdown, or Office document and wait for `READY`.
3. Open Chat, select local knowledge, and ask a question that should cite the document.
4. Enable deep thinking to inspect `thinking`, citation, and answer events.
5. Open **检索评测**, create a dataset, add relevant Chunk IDs, and run the
   three-mode ablation comparison.

Workspace permissions are enforced by the backend: OWNER has all operations,
ADMIN can manage members/documents/Chat/evaluation, and MEMBER can use Chat and
read READY documents/citations.
