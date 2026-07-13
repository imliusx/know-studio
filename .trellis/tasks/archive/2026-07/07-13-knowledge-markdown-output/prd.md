# Normalize knowledge Markdown output

## Goal

Ensure Knowledge answers use valid, consistently rendered Markdown instead of
displaying malformed list markers such as `-主键索引` as plain text.

## Requirements

- Restore the legacy backend's explicit Markdown-output rules in the current
  Knowledge prompt while preserving evidence grounding and natural Chinese.
- Unordered list items must use a marker followed by one space, each item on its
  own line, with a blank line between the preceding paragraph and the list.
- Ordered lists, headings, inline code, fenced code blocks, and paragraphs must
  follow CommonMark/GFM syntax. Code fences must be on their own lines and carry
  a language when known.
- Increment the Knowledge prompt version because the generation contract changes.
- The frontend Markdown renderer must safely repair legacy or occasional model
  output where a line-start hyphen list marker is missing its required space.
- Frontend repair must not alter fenced code blocks or Markdown horizontal rules.
- REST, SSE, citation, retrieval, authorization, and persisted message contracts
  remain unchanged.

## Acceptance Criteria

- [x] Prompt catalog tests prove the explicit Markdown contract and new prompt
  version are loaded.
- [x] Markdown rendering converts `-主键索引名为 \`pk_字段名\`` into a real list
  item rather than visible raw hyphen text.
- [x] Existing valid lists, fenced code, and horizontal rules remain unchanged.
- [x] The exact Knowledge question renders the three index naming rules as list
  items on desktop and mobile.
- [x] Backend tests, frontend lint/typecheck/build, Playwright, Docker config, and
  diff checks pass.

## Out Of Scope

- Adding unsupported naming examples or rules not present in retrieval evidence.
- Replacing the Markdown renderer or changing the SSE protocol.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
