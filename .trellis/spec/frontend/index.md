# Frontend Development Guidelines

## Guidelines Index

| Guide | Description | Status |
|---|---|---|
| [ARAG Integration Guidelines](./arag-integration-guidelines.md) | API boundaries, workspace isolation, permissions and SSE state | Active |

## Pre-Development Checklist

- Read `arag-integration-guidelines.md` before changing core auth, workspace, document, Chat or evaluation code.
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

For interactive changes, verify desktop and mobile routes in the browser. Streaming and workspace changes require explicit abort, stale-state and permission tests.
