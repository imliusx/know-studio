# Prompt Engineering Guidelines

## Scenario: Scenario-Owned Prompts and Role-Aware Model Requests

### 1. Scope / Trigger

Use this contract when adding or changing model prompts, provider routing,
conversation context, generation settings, evidence grounding, or AI telemetry.
It applies to `platform-ai`, `module-agent`, `module-retrieval`, and
`module-conversation`.

### 2. Signatures

- `PromptResource.classpath(path, version)` loads a UTF-8 classpath prompt and
  exposes `text()`, `render(variables)`, and a stable `version()`.
- `ChatRequest(systemPrompt, history, userPrompt, reasoning, profile,
  promptVersion, options)` is the provider-neutral model request.
- `ChatMessage` carries one `SYSTEM`, `USER`, or `ASSISTANT` role and its content.
- `GenerationProfile` owns bounded temperature and maximum-token settings for
  `CHAT`, `KNOWLEDGE`, `CLASSIFICATION`, `PLANNING`, and `SUMMARY`.
- `ChatModelRouter.stream(request)` selects the lowest-priority-number healthy
  provider. `dashscope-chat` has priority 10 and Ollama Chat fallback has
  priority 20.

### 3. Contracts

- Prompt files belong to the module that owns the scenario:
  `prompts/agent/**`, `prompts/retrieval/**`, or `prompts/conversation/**`.
  Java services must not contain long scenario prompt constants.
- Ordinary Chat and Knowledge answering use different system prompts. Chat is
  natural Chinese assistance without an evidence-only restriction. Knowledge
  answers use only supplied evidence and deterministically refuse when the
  evidence level is `NONE`.
- Provider message order is: primary system prompt, optional system memory,
  chronological typed history, then exactly one current user message.
- Conversation summaries are system memory. Recent user and assistant messages
  retain their original roles; the current user message is removed from history
  before the request is sent.
- Prompt templates declare stable low-cardinality versions such as `chat-v1` or
  `knowledge-v3`. Increment the version when behavior changes materially.
- `DASHSCOPE_API_KEY` is environment-only. `DASHSCOPE_CHAT_MODEL` defaults to
  `glm-5`; `OLLAMA_EMBEDDING_MODEL` defaults to `bge-m3`; optional Chat fallback
  is controlled by `OLLAMA_CHAT_FALLBACK_ENABLED` and `OLLAMA_CHAT_MODEL`.
- Metrics and Langfuse metadata may contain provider ID, generation profile,
  prompt version, outcome, latency, and output character count. They must not
  contain raw prompts, questions, evidence, answers, document names, IDs, or
  credentials.
- Citation events and persisted citation metadata must use the same selected
  grounding evidence. Rank evidence by semantic question-term coverage before
  repeated-term frequency so generic large chunks do not displace the direct
  source.
- Before weighting explicit naming rules, normalize equivalent naming-question
  subjects by removing possessive or location context and resolving the known
  domain-noun suffix. For example, `Java 的索引如何命名`, `Java 中的索引如何命名`,
  and `数据库索引怎么命名` must all resolve to the subject `索引`.
- Deterministic rule matching may select and focus grounding evidence, but it
  must not emit raw document lines as the final answer. Every non-refusal
  Knowledge answer goes through the configured Chat model.
- Knowledge generation preserves exact identifiers and casing while removing
  source numbering, page markers, broken PDF lines, duplicate fragments, and
  stiff fixed preambles.
- Knowledge answers use valid CommonMark/GFM. List markers, ordered-list numbers,
  and heading markers require a following space; list items and fenced-code
  delimiters occupy their own lines. Known code-block languages are declared.

### 4. Validation & Error Matrix

- Missing prompt resource -> fail component construction with
  `IllegalStateException`; never continue with an empty prompt.
- Blank prompt path or version -> `IllegalArgumentException`.
- Missing template variable -> prompt rendering fails; add a catalog test that
  renders every required variable.
- Blank `DASHSCOPE_API_KEY` or unavailable DashScope -> router may use the
  enabled healthy Ollama Chat fallback; if none is healthy, return the existing
  controlled AI routing error.
- Intent output malformed or model call exceeds eight seconds -> deterministic
  heuristic classification fallback.
- Knowledge evidence level `NONE` -> deterministic refusal without model call
  and without citations.
- Partial evidence -> answer only the supported portion and state the limit.
- Malformed pseudo-Markdown such as `-主键索引` -> prevent through the prompt
  contract; the frontend may narrowly repair historical line-start hyphen lists.
- Provider failure after output has started -> do not switch providers mid-answer.

### 5. Good/Base/Bad Cases

- Good: a self-introduction uses `chat-system.st`, profile `CHAT`, and typed
  conversation history, producing a direct natural response.
- Base: a factual Knowledge question uses `knowledge-system.st` plus the rendered
  evidence user prompt and emits citations from the selected evidence.
- Bad: a general chat request inherits the Knowledge evidence-only prompt and
  responds with an unrelated-document refusal.
- Good: a Service/DAO naming question selects the chunk containing the complete
  `get` and `list` rule and emits one citation.
- Bad: a long chunk that repeats `Service`, `DAO`, and `getter/setter` outranks
  the direct rule only because it repeats generic terms more often.
- Good: `Java 索引如何命名` selects the `pk_`/`uk_`/`idx_` naming rule, then the
  model rewrites it as a concise list while preserving those identifiers.
- Good: adding `的` or `中的` to the same naming question selects the same rule
  and citation as the shorter wording.
- Good: multiple naming rules are emitted as separate `- ` list items with
  identifiers such as `pk_字段名` formatted as inline code.
- Bad: return `3. 【强制】...` directly from a PDF chunk or mistake a varchar
  index-length rule for an index-naming rule because both contain `索引`.
- Bad: parse `Java 的索引如何命名` as `的索引`, then let repeated generic `索引`
  text outrank the direct `索引名为` rule.
- Bad: emit `-主键索引` or place multiple list items on one line and rely on the
  browser to guess the intended structure.

### 6. Tests Required

- Catalog tests load every prompt resource and render every required variable.
- Provider tests assert exact SYSTEM/USER/ASSISTANT order, current-question
  deduplication, and profile-specific generation options.
- Agent tests assert Chat/Knowledge prompt separation, natural Chat style,
  deterministic refusal, partial-evidence limits, explicit-rule evidence
  selection followed by model generation, citation consistency, and
  semantic-coverage evidence ranking.
- Naming-rule regression tests assert possessive and location variants normalize
  to the same domain subject and select the same direct evidence.
- Prompt catalog tests assert the concrete CommonMark/GFM rules and the current
  Knowledge prompt version. Browser acceptance asserts semantic list elements,
  not only visible hyphen-prefixed text.
- Observability tests assert bounded provider/profile/version fields and the
  absence of raw prompt, question, evidence, answer, and credential content.
- Real API acceptance verifies DashScope `glm-5` natural Chat, one grounded
  Knowledge citation, and unrelated-question refusal.
- Playwright desktop and mobile acceptance covers the same three user-visible
  behaviors without horizontal overflow.

### 7. Wrong vs Correct

#### Wrong

```java
String prompt = "USER: " + oldQuestion + "\nASSISTANT: " + oldAnswer
        + "\nUSER: " + currentQuestion;
return router.stream(new ChatRequest(KNOWLEDGE_PROMPT, prompt, false, Map.of()));
```

This flattens roles, duplicates the current question easily, and applies the
Knowledge-only behavior to every scenario.

#### Correct

```java
return router.stream(ChatRequest.of(
        promptCatalog.chat().text(),
        List.of(ChatMessage.user(oldQuestion), ChatMessage.assistant(oldAnswer)),
        currentQuestion,
        GenerationProfile.CHAT,
        promptCatalog.chat().version()
));
```

The provider receives typed chronological messages and one scenario-specific
current user request.
