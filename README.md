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

## Frontend

```bash
cd know-studio-ui
pnpm install
pnpm dev
```
