# Natural knowledge answer generation

## Goal

Remove the raw extractive answer bypass, restore model-generated natural knowledge answers, and prevent naming questions from matching semantically different rules.

## Requirements

- Every Knowledge answer with usable evidence must pass through the configured
  Chat model so the final wording is natural, concise, and adapted to the
  question. Do not return raw extracted PDF lines as the final answer.
- Keep deterministic `EvidenceLevel.NONE` refusal without a model call.
- Preserve the focused-evidence safeguard that prevents the model from mixing
  unrelated rules, but use it to select and trim grounding evidence rather than
  to bypass generation.
- Naming questions must prefer evidence that expresses an actual naming rule
  (`名为`, `命名`, `使用 ... 风格`, prefix/suffix rules) over evidence that only
  mentions the same subject in another operation.
- For `Java 索引如何命名`, the selected evidence and answer must cover primary
  key `pk_字段名`, unique index `uk_字段名`, and normal index `idx_字段名`; it must
  not answer with the varchar index-length rule.
- Knowledge prompts must instruct the model to preserve exact identifiers while
  removing source numbering, page artifacts, duplicated fragments, and stiff
  preambles from the final prose.
- Citations must still come from exactly the evidence supplied to generation.
- REST, SSE, persistence, authorization, and frontend feature contracts remain
  unchanged.

## Acceptance Criteria

- [x] The explicit-rule shortcut no longer emits a final token without calling
  `ChatModelRouter`.
- [x] A regression test proves a naming-rule chunk outranks a repeated but
  semantically different index rule.
- [x] A regression test proves a grounded naming answer invokes the Knowledge
  generation profile and emits only the selected citation.
- [x] The Knowledge prompt version is incremented and explicitly requests
  natural paraphrasing without changing exact identifiers.
- [x] A real API request for `Java 索引如何命名` returns `pk_`, `uk_`, and `idx_`,
  does not return the varchar index-length rule, and records a successful
  DashScope Knowledge generation metric.
- [x] Backend tests, frontend checks, Playwright desktop/mobile acceptance,
  Docker config, and diff checks pass.

## Out of Scope

- Replacing hybrid retrieval, embeddings, or the current provider router.
- Allowing the model to answer beyond supplied evidence.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
