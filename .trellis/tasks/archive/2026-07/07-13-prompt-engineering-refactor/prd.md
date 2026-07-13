# Prompt engineering refactor

## Goal

Restore scenario-specific prompt resources, natural chat behavior, structured knowledge grounding, role-aware conversation context, model quality configuration, prompt tests and evaluation coverage.

## Requirements

- Prompt text must be externalized under module-owned `src/main/resources/prompts/**`; Java services must not contain long scenario prompts.
- Separate ordinary Chat, grounded KnowledgeBase answering, intent classification, query planning, question decomposition, and conversation summarization prompts.
- Ordinary Chat must use a natural Chinese assistant role and must not inherit the KnowledgeBase-only evidence restriction.
- Knowledge answers must remain authorization-safe and evidence-grounded: insufficient evidence refuses deterministically, partial evidence states its limits, and generated text may not exceed the supplied evidence.
- Conversation history must cross the provider boundary as typed SYSTEM/USER/ASSISTANT messages instead of being flattened into one user string.
- Preserve focused extractive handling for explicit rules, citations, evidence grading, KnowledgeBase routing, refusal evaluation, and every existing frontend feature.
- Prompt resources need stable version identifiers that can be attached to bounded model telemetry without recording raw prompts or document content.
- Match the legacy deployment: DashScope `glm-5` is the default chat model, configured only through environment-backed credentials; Ollama remains the embedding provider and optional chat fallback.
- Prompt loading, required variables, message ordering, route-specific prompt selection, refusal safety, and representative answer style require automated regression tests.
- Desktop and mobile browser acceptance must cover a natural ordinary-chat response, a grounded KnowledgeBase answer, and an unrelated-question refusal.

## Acceptance Criteria

- [x] No long prompt constants remain in Agent, retrieval planning, or conversation summary implementations.
- [x] Prompt resources are grouped by scenario and load successfully from every owning Maven module.
- [x] CHAT and KNOWLEDGE requests select different system prompts; the CHAT prompt contains no instruction to rely on absent evidence.
- [x] The provider sends conversation history as real message roles in chronological order, with the current question exactly once.
- [x] Knowledge answers preserve citations and refusal behavior and do not reintroduce unrelated-evidence hallucinations.
- [x] Representative ordinary-chat output is natural, direct, and not written like a system log or audit response.
- [x] Prompt version and generation profile are observable as bounded attributes, while raw prompts and outputs remain excluded.
- [x] Backend tests, frontend checks, Playwright desktop/mobile acceptance, Docker configuration, and diff checks pass.

## Notes

- Confirmed legacy sources: `archive/legacy-backend/src/main/resources/prompts/**`, `AssistantPromptContextBuilder`, `QaChatClientConfiguration`, and `AssistantShortTermMemoryHook`.
- Confirmed current limitation: `AgentOrchestrationService` uses one compact KnowledgeBase-oriented answer prompt for both CHAT and KNOWLEDGE generation.
- Model decision: use the legacy DashScope `glm-5` default; `DASHSCOPE_API_KEY` is already configured locally and must never be committed or logged.
- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
