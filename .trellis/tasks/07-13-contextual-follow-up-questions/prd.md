# Resolve contextual follow-up questions

## Goal

Resolve short contextual follow-ups such as `那常量呢` against the previous user
question so the system continues the Knowledge conversation instead of asking
for unnecessary clarification.

## Requirements

- Preserve the user's original follow-up message in conversation history.
- Before intent classification and retrieval, resolve a short `那<命名对象>呢`
  follow-up by replacing the previous naming subject with the new subject.
- `如何命名 Java 的类名，给出示例代码` followed by `那常量呢` must resolve to
  `如何命名 Java 的常量，给出示例代码`.
- Only resolve recognized naming subjects and only when the previous user
  question is a naming question; unrelated short questions remain eligible for
  clarification.
- Use the resolved question for intent classification, retrieval, evidence
  focusing, and model generation without duplicating the original current
  message in model history.
- REST, SSE, persistence, authorization, and citation contracts remain unchanged.

## Acceptance Criteria

- [x] Unit tests cover contextual resolution, non-naming fallback, current-message
  history deduplication, and Knowledge retrieval with the resolved question.
- [x] Browser acceptance asks a class-naming question followed by `那常量呢` and
  receives the constant naming rule without a clarification response.
- [x] Backend tests, frontend checks, desktop/mobile Playwright, Docker config,
  and diff checks pass.

## Out Of Scope

- General-purpose pronoun resolution for arbitrary domains.
- Adding another model call solely to rewrite follow-up questions.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
