# Frontend Development Guidelines

## Guidelines Index

| Guide | Description | Status |
|---|---|---|
| [ARAG Integration Guidelines](./arag-integration-guidelines.md) | API boundaries, KnowledgeBase access, permissions and SSE state | Active |

## Pre-Development Checklist

- Read `arag-integration-guidelines.md` before changing core auth, Team, KnowledgeBase, document, Chat or evaluation code.
- Search existing `src/api`, stores, query keys and UI components before adding a new helper or abstraction.
- Preserve existing formal routes and unrelated features unless the active task explicitly changes them.
- Snapshot uncommitted frontend changes and do not overwrite work from another session.

## Quality Check

```bash
cd know-studio-ui
pnpm lint
pnpm typecheck
pnpm build
```

For interactive knowledge and evaluation changes, start the backend on `127.0.0.1:8080` and the frontend on `127.0.0.1:5174`, then run the standalone Chromium acceptance suite:

```bash
cd know-studio-ui
pnpm test:e2e
```

Set `PLAYWRIGHT_BASE_URL`, `PLAYWRIGHT_EMAIL` and `PLAYWRIGHT_PASSWORD` when the local defaults do not apply. Verify both desktop and mobile projects; streaming and KnowledgeBase changes also require explicit abort, stale-state and permission tests.
