# Refactor project structure

## Goal

Refactor the Know Studio repository to the approved enterprise-style modular structure documented in `docs/architecture/target-project-structure.md`, while preserving all current behavior and uncommitted user work.

## Requirements

- Rename backend modules to `common`, `auth`, `ai`, `knowledge`, `search`, `chat`, `agent`, `eval`, and `app`.
- Rename `know-studio-ui` to `web`.
- Use `deploy` for deployment resources without introducing `backend/` or `frontend/` wrapper directories.
- Update the root Maven reactor, inter-module artifact IDs, dependencies, Java package names, resource paths, scripts, documentation, Docker configuration, and frontend references consistently.
- Preserve REST routes, SSE contracts, database schemas, environment-variable names, model behavior, permissions, and frontend features.
- Preserve all pre-existing uncommitted changes while moving their owning files.
- Keep `app` as the executable Spring Boot composition module without moving business logic into it.
- Keep RAG as a composed capability across `agent`, `search`, `knowledge`, `chat`, and `ai`, rather than creating a top-level `rag` module.
- Prevent circular Maven dependencies and cross-module implementation leakage.

## Acceptance Criteria

- [x] The repository root contains the approved module names and no obsolete module directories remain.
- [x] Root and module POMs resolve the renamed artifacts with a valid acyclic dependency graph.
- [x] Java packages use `know.studio.common`, `auth`, `ai`, `knowledge`, `search`, `chat`, `agent`, `eval`, and `app` consistently.
- [x] Spring component scanning, MyBatis mapper scanning, resources, prompts, and tests resolve after the move.
- [x] The frontend builds and its scripts, E2E paths, Docker paths, and documentation use `web`.
- [x] Docker Compose and environment configuration reference the renamed application paths correctly.
- [x] Existing uncommitted Agent and frontend E2E changes remain present after migration.
- [x] `mvn clean test` passes.
- [x] `web` lint, typecheck, and build pass.
- [x] `docker compose config -q` and `git diff --check` pass.
- [x] The target architecture document remains accurate after implementation.

## Notes

- Source architecture: `docs/architecture/target-project-structure.md`.
- This is a structural refactor. Public behavior and persistence contracts are out of scope unless a change is strictly required to restore compatibility after moving code.
- The worktree was already dirty when the task started; unrelated user changes must not be discarded or rewritten.
- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
