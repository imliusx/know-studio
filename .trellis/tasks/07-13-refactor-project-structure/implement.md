# Project Structure Refactor Plan

- [x] Snapshot current Git status and identify all references to current module names and `know.studio.arag` packages.
- [x] Move backend module directories and `know-studio-ui` using Git-aware renames.
- [x] Update the root reactor, artifact IDs, inter-module dependencies, build output, and application entry point.
- [x] Rewrite Java package declarations/imports and move source directories to the new package paths.
- [x] Update MyBatis XML namespaces, Spring configuration, tests, prompt/resource references, and architecture tests.
- [x] Update frontend paths, workspace files, Playwright configuration, Docker Compose, scripts, and documentation.
- [x] Reconcile actual dependencies with the approved module graph without introducing behavioral changes.
- [x] Run targeted Maven compilation/tests and frontend lint/typecheck/build during migration.
- [x] Run the full backend, frontend, Docker Compose, and diff quality gates.
- [x] Update the target architecture document if implementation evidence requires a correction.

## Validation Commands

```bash
mvn clean test
cd web && pnpm lint && pnpm typecheck && pnpm build
docker compose config -q
git diff --check
```

## Risk and Rollback Points

- Existing uncommitted files must remain byte-for-byte represented after their directory moves except for required package/path rewrites.
- Package rewrites can break reflection, MyBatis namespaces, component scanning, and test resources; search every old namespace before declaring completion.
- Frontend directory renaming can break Docker, scripts, CI, screenshots, and Playwright paths; search every old path.
- Do not change database schemas, API paths, environment keys, or prompt behavior as part of this refactor.
