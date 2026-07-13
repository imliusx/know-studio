# Backend Directory Structure

## Scenario: Maven Module Organization and Boundaries

### 1. Scope / Trigger

Use this contract when adding, moving, renaming, or depending on a backend module or package. The repository is a Maven modular monolith with business-capability modules at the repository root.

### 2. Signatures

The backend reactor modules are:

```text
common  auth  ai  knowledge  search  chat  agent  eval  app
```

The executable application entry point is:

```java
know.studio.app.KnowStudioApplication
```

Cross-module Java contracts live under:

```text
know.studio.<module>.api
```

### 3. Contracts

- `common` owns minimal shared response, error, context, trace, ID, SSE, and MQ foundations.
- `auth` owns users, login, Teams, roles, and resource permissions.
- `ai` owns provider-neutral Chat, embedding, rerank, routing, fallback, and bounded AI telemetry.
- `knowledge` owns KnowledgeBases, documents, parsing, chunking, embedding ingestion, index writing, and document state.
- `search` owns query planning, vector/keyword retrieval, fusion, neighbor expansion, rerank, and evidence grading.
- `chat` owns sessions, messages, summaries, memory, and model-ready conversation context.
- `agent` owns intent routing, orchestration, tools, MCP, and streamed answers.
- `eval` owns datasets, runs, retrieval metrics, refusal metrics, and Agent evaluation.
- `app` owns startup, global configuration, security integration, migrations, health checks, and final assembly; it contains no business workflow.
- The React application is `web`; deployment resources use `deploy`.
- RAG is composed from `agent`, `search`, `knowledge`, `chat`, and `ai`; there is no top-level `rag` Maven module.
- Public REST paths, SSE events, database tables, configuration keys, and environment variables do not change merely because a module or package is renamed.

Allowed internal dependencies:

| Module | Allowed dependencies |
|---|---|
| `common` | none |
| `auth` | `common` |
| `ai` | `common` |
| `knowledge` | `common`, `auth`, `ai` |
| `search` | `common`, `knowledge`, `ai`; use `auth` only when authorization cannot be supplied through a public scope contract |
| `chat` | `common`, `auth`, `ai` |
| `agent` | `common`, `auth`, `ai`, `chat`, `search` |
| `eval` | `common`, `ai`, `search`, `agent`, plus public APIs needed for evaluation ownership |
| `app` | all backend modules |

### 4. Validation & Error Matrix

- Circular Maven dependency -> reject the change and redesign the call direction.
- Cross-module import outside the target module's `api` package -> ArchUnit failure or review rejection.
- `app` business Service, Mapper, Prompt, parser, search algorithm, or Agent runtime -> move it to the owning module.
- Sa-Token usage outside `auth` or `app` security integration -> replace it with an `auth.api` contract.
- Provider SDK usage outside `ai` -> add or use a provider-neutral `ai.api` contract.
- Search returning final prose instead of evidence -> move generation to `agent`.
- Agent querying another module's tables -> call the owning module API.
- Module/package rename leaves old path, namespace, XML Mapper namespace, Docker path, frontend path, or documentation reference -> migration is incomplete.

### 5. Good/Base/Bad Cases

- Good: `agent` calls `search.api.RetrievalApi` and receives an `EvidenceBundle`.
- Base: a module uses its own Repository and Mapper internally.
- Bad: `agent` imports `search.infra.persistence.RetrievalSearchMapper`.
- Good: `app` uses `@SpringBootApplication(scanBasePackages = "know.studio")` to assemble sibling modules.
- Bad: place the entry point in `know.studio.app` without an explicit base scan, causing sibling modules not to load.
- Good: a module rename updates directory paths, artifact IDs, package declarations, imports, tests, XML namespaces, scripts, Docker paths, and docs in one reviewed migration.
- Bad: rely on an incremental Maven build after a package move; stale `target` classes can hide missing source changes.

### 6. Tests Required

- Run `mvn clean test`; incremental compilation is not sufficient after package or module moves.
- Run the ArchUnit module-boundary tests from `app`.
- Run `cd web && pnpm lint && pnpm typecheck && pnpm build` after renaming or moving frontend paths.
- Run `docker compose config -q` after moving deployment resources.
- Run `git diff --check` and search for all obsolete module names and package prefixes.
- Package the executable application and assert `app/target/know-studio.jar` contains `know.studio.app.KnowStudioApplication`.

### 7. Wrong vs Correct

#### Wrong

```java
package know.studio.app;

@SpringBootApplication
public class KnowStudioApplication {
}
```

This scans only `know.studio.app` and misses sibling module packages such as `know.studio.agent` and `know.studio.auth`.

#### Correct

```java
package know.studio.app;

@SpringBootApplication(scanBasePackages = "know.studio")
public class KnowStudioApplication {
}
```

## Naming Conventions

- Repository directories and Maven artifact IDs use short lowercase names: `common`, `auth`, `ai`, `knowledge`, `search`, `chat`, `agent`, `eval`, and `app`.
- Java packages mirror the module: `know.studio.<module>`.
- The executable artifact is `know-studio.jar`.
- Use precise domain names inside modules even when the module directory is short; for example, `RetrievalApi` may remain a precise public type inside `search`.
- Avoid adding wrapper directories such as `backend/` or `frontend/`.

## Current Reference

The detailed repository tree, technology ownership, data ownership, and migration mapping are documented in `docs/architecture/target-project-structure.md`.
