# Prompt Engineering Refactor Plan

- [x] Add a platform AI prompt resource loader, typed message contract, generation profiles, and provider message-order tests.
- [x] Add module-owned prompt resource catalogs and migrate ordinary Chat plus grounded Knowledge answering off Java text blocks.
- [x] Pass conversation summaries and recent history as real message roles; remove flattened `USER:`/`ASSISTANT:` context serialization.
- [x] Externalize intent, decomposition, retrieval planning, and conversation summary prompts while preserving fallbacks and timeouts.
- [x] Add bounded prompt-version/profile observability and tests that reject raw prompt or content telemetry.
- [x] Restore DashScope `glm-5` as the primary Chat provider, keep Ollama as embedding and lower-priority Chat fallback, and add profile-specific generation options without exposing credentials.
- [x] Expand backend regression tests for route-specific prompts, natural answer style, evidence refusal, citations, and explicit-rule extraction.
- [x] Run real model API checks and Playwright desktop/mobile acceptance for Chat, grounded Knowledge, and refusal behavior.
- [x] Run `mvn clean test`, frontend lint/typecheck/build/E2E, `docker compose config -q`, and `git diff --check`.
- [x] Update backend prompt-engineering specs, commit the work, archive the task, and record the session.

## Rollback Points

- Keep REST/SSE contracts unchanged so prompt/message refactoring can be reverted independently.
- Do not add a database migration.
- Do not delete legacy prompt resources from `archive/`; they remain comparison evidence.
