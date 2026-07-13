# Normalize naming question subjects

## Goal

Normalize possessive particles in naming questions so equivalent wording consistently selects the exact naming-rule evidence.

## Requirements

- Equivalent naming questions must resolve to the same normalized naming
  subject regardless of possessive or location particles, including `索引如何命名`,
  `Java 的索引如何命名`, `Java 中的索引如何命名`, and `数据库索引怎么命名`.
- Subject normalization must remove leading context particles without changing
  the actual domain noun. For the examples above, the subject is `索引`.
- Correct naming-rule evidence containing `索引名为` must outrank SQL
  performance or index-usage evidence that only repeats `索引`.
- The final answer must continue through DashScope `glm-5`, preserve one direct
  citation, and remain evidence-grounded.
- Do not copy the brighter comparison answer's unsupported examples or unrelated
  table/boolean-field rules unless the selected evidence directly supports them.
- REST, SSE, authorization, retrieval, and frontend contracts remain unchanged.

## Acceptance Criteria

- [x] Unit tests cover possessive/location variants and prove they normalize to
  the same subject.
- [x] Grounding tests prove `Java 的索引如何命名` selects the `pk_`/`uk_`/`idx_`
  naming rule over repeated index-performance content.
- [x] Playwright uses the exact reported wording and receives a completed answer
  containing `pk_`, `uk_`, and `idx_` without the false refusal.
- [x] A real API request records successful `dashscope-chat` Knowledge generation
  with one citation.
- [x] Backend tests, frontend checks, desktop/mobile E2E, Docker config, and diff
  checks pass.

## Out of Scope

- Generating examples not present in the selected evidence.
- Broad retrieval or embedding changes.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
