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
mvn -pl bootstrap -am spring-boot:run
```

The backend listens on port `8080` by default. Supporting services are defined in the root `docker-compose.yml`.

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
