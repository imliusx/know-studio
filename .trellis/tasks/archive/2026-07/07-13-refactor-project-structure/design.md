# Project Structure Refactor Design

## Target Modules

The backend remains a Maven modular monolith with the following modules: `common`, `auth`, `ai`, `knowledge`, `search`, `chat`, `agent`, `eval`, and executable `app`. The React frontend becomes `web`. Deployment resources are grouped under `deploy` only where existing files can be moved without changing runtime behavior.

## Mapping

| Current | Target |
|---|---|
| `platform-core` | `common` |
| `module-identity` | `auth` |
| `platform-ai` | `ai` |
| `module-knowledge` | `knowledge` |
| `module-retrieval` | `search` |
| `module-conversation` | `chat` |
| `module-agent` | `agent` |
| `module-evaluation` | `eval` |
| `bootstrap` | `app` |
| `know-studio-ui` | `web` |

## Package Mapping

The existing base `know.studio.arag` is simplified to `know.studio`. Module segments map to the target module names. Public HTTP paths, SQL tables, serialized fields, configuration properties, and environment variables do not change.

## Dependency Direction

`common` has no internal dependency. `auth` and `ai` depend on `common`. `knowledge` depends on `common`, `auth`, and `ai`. `search` depends on `common`, `auth`, `knowledge`, and `ai`. `chat` depends on `common`, `auth`, and `ai`. `agent` depends on `common`, `auth`, `ai`, `chat`, and `search`. `eval` depends on `common`, `ai`, `search`, and `agent`. `app` assembles all modules.

If the existing implementation has a dependency that conflicts with this ideal graph, the first migration preserves a minimal acyclic graph and records remaining boundary cleanup rather than changing runtime behavior speculatively.

## Compatibility

- Preserve REST and SSE endpoints.
- Preserve database tables and MyBatis SQL.
- Preserve application property keys and environment variables.
- Preserve prompt text and classpath loading behavior.
- Preserve existing tests and user changes through file moves.

## Migration Strategy

Use `git mv` for directories, then perform mechanical artifact and package rewrites. Validate compilation after backend moves before changing secondary documentation and deployment paths. Do not combine behavioral redesign with structural migration.

## Rollback

The work is isolated on `codex/refactor-project-structure`. Each migration stage remains reviewable through Git moves and text changes. No database migration or external-state mutation is required.
