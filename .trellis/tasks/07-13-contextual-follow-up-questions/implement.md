# Contextual follow-up implementation

1. Load conversation context with the original current question.
2. Add recognized naming follow-up resolution in agent orchestration.
3. Route a derived request through classifier, retrieval, and generation while
   persisting the original request.
4. Deduplicate model history using `ConversationContext.currentQuestion`.
5. Add unit and desktop/mobile browser regression coverage.
6. Update agent conversation and prompt-engineering specs.
7. Run backend, frontend, Playwright, Docker, and diff quality gates.
