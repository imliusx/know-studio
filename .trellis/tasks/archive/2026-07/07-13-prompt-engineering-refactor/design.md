# Prompt Engineering Refactor Design

## Architecture

Prompt ownership follows module ownership:

- `module-agent`: ordinary Chat, grounded Knowledge answering, intent classification, and question decomposition prompts.
- `module-retrieval`: retrieval query planning prompt.
- `module-conversation`: durable summary prompt.
- `platform-ai`: typed model-message contract, prompt resource loader, generation profile, and provider adaptation.

Each module exposes a small prompt catalog to its domain service. Catalogs load UTF-8 classpath resources during startup, validate required variables, render immutable prompt values, and expose a stable version such as `chat-v1` or `knowledge-v1`. Missing resources or variables fail application startup or the owning unit test rather than silently falling back to an empty prompt.

## Model Request Contract

Extend the platform AI request with:

- one system prompt;
- zero or more typed historical messages (`SYSTEM`, `USER`, `ASSISTANT`);
- exactly one current user prompt;
- a bounded generation profile (`CHAT`, `KNOWLEDGE`, `CLASSIFICATION`, `PLANNING`, `SUMMARY`);
- prompt version metadata.

The Spring AI provider builds one ordered `Prompt`: system message, optional summary system message, chronological history, then the current user message. Existing convenience constructors remain available while call sites migrate.

## Scenario Behavior

### Ordinary Chat

Use the useful parts of the legacy assistant instruction: direct answer, natural professional Chinese, conclusion before explanation, limited clarification, correct Markdown, and no internal reasoning. It does not mention retrieval evidence.

### Knowledge Answering

Keep deterministic retrieval, evidence grading, routing, focused evidence, citations, and refusal. The model receives an evidence-aware system prompt and a structured user template containing the current question, evidence level/guidance, and bounded evidence. `NONE` still bypasses the model. Explicit-rule extraction remains deterministic to protect factual precision, but its formatter should follow the same natural response conventions.

### Classification And Planning

Externalize existing prompts and tighten their output contracts. Keep heuristic fallbacks and timeouts. Query planning always preserves the normalized original question and never broadens a single-intent query into a document-summary request.

### Conversation Memory

Externalize the summary prompt. Inject the resulting summary as a system memory message and recent messages as their original roles. The current user message must not be duplicated.

## Model Configuration

Restore the legacy provider split: DashScope `glm-5` is the primary Chat provider and Ollama `bge-m3` remains the embedding provider. `DASHSCOPE_API_KEY` is required through the environment and is never persisted or logged. Ollama Chat remains a lower-priority fallback controlled by `OLLAMA_CHAT_MODEL`. Generation settings are profile-owned and bounded; low temperature for classification/knowledge, moderate temperature for ordinary Chat.

## Observability

Add only low-cardinality attributes: prompt version and generation profile. Do not record prompt text, user questions, evidence, generated answers, document names, or IDs as metric tags.

## Compatibility And Rollback

- REST and SSE contracts do not change.
- Existing provider routing and circuit breakers remain in place.
- Existing four-argument AI request construction remains source-compatible during migration.
- Rollback is limited to reverting prompt catalogs/message-role support; database migrations are not required.

## Validation

- Unit tests capture provider messages and assert exact role ordering.
- Prompt catalog tests load every resource and render all required variables.
- Agent tests assert CHAT/KNOWLEDGE prompt separation, deterministic refusal, citations, and current-question deduplication.
- Real local-model checks compare ordinary Chat, grounded Java-rule answering, and unrelated expense refusal.
- Playwright verifies the same workflows on desktop and mobile.
