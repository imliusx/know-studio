# Observability and Evaluation Guidelines

## Scenario: Production Observability and Retrieval Ablation

### 1. Scope / Trigger

Use this contract when changing rate-limited endpoints, `@RagTraceNode`, model
provider routing, Prometheus/OTLP/Langfuse configuration, retrieval modes, or
evaluation datasets and runs.

### 2. Signatures

- `@RateLimit(key, permits, windowSeconds, scope)` rejects with
  `ErrorCode.TOO_MANY_REQUESTS` and HTTP 429.
- `RetrievalQuery(question, knowledgeBaseIds, topK, mode)` supports
  `VECTOR_ONLY`, `HYBRID`, and `HYBRID_RERANK`; the three-argument constructor
  defaults to `HYBRID_RERANK`.
- `EvaluationApi` creates datasets/samples and returns an `EvaluationReport`
  containing Recall@K, sample count, and average latency for each mode.
- OTLP uses `OTEL_ENABLED`, `OTEL_SERVICE_NAME`,
  `OTEL_EXPORTER_OTLP_ENDPOINT`, and `OTEL_EXPORT_TIMEOUT`.
- Langfuse uses `LANGFUSE_ENABLED`, `LANGFUSE_BASE_URL`,
  `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY`, and `LANGFUSE_TIMEOUT`.

### 3. Contracts

- Reactive trace spans start on subscription and end on publisher termination.
  Restore `TraceContext` immediately after creating the publisher; never keep a
  request ThreadLocal until an asynchronous callback ends on another thread.
- `arag.trace.node`, `arag.rate.limit.*`, and `arag.ai.*` metrics use bounded,
  stable tag values. Never use user IDs, questions, or document names as tags.
- Langfuse delivery is best effort, bounded, timed out, and never includes raw
  prompts or generated text in the built-in payload.
- Evaluation tables and queries explicitly filter `knowledge_base_id`. An ablation
  run must not hold one database transaction across model and retrieval calls.
- Each ablation mode calls the real `RetrievalApi`; do not derive or copy one
  mode's result into another.
- Knife4j 4.5 UI is retained with Springdoc 2.8, but its incompatible ordering
  customizer is replaced by a no-op compatibility bean.

### 4. Validation & Error Matrix

- Rate permit unavailable -> HTTP 429 / `A0429`, rejected counter increments.
- Dataset missing or outside the requested KnowledgeBase -> `NOT_FOUND`.
- Empty dataset, invalid topK, or empty/non-positive relevant chunk IDs ->
  `BAD_REQUEST`.
- Missing static resource -> `NOT_FOUND`, not the unexpected-exception 500 path.
- OTLP backend unavailable -> application continues; exporter retries/drops
  according to the SDK batch processor.
- Langfuse unavailable or queue full -> application response is unaffected.

### 5. Good/Base/Bad Cases

- Good: `Flux.defer` starts a span at subscription and `doFinally` ends it.
- Bad: start and end a span when the controller merely returns a `Flux`.
- Good: record provider ID, outcome, latency, and output character count.
- Bad: send full prompt/output text to metrics or Langfuse by default.
- Good: persist three independent evaluation run rows for the three modes.
- Bad: wrap the complete evaluation loop in a long database transaction.

### 6. Tests Required

- Unit test 429 error code and allowed/rejected metrics.
- Unit test reactive span completion and ThreadLocal restoration while a stream
  remains open.
- Unit test provider observation on success and first-token failover behavior.
- Unit test vector-only mode skips keyword retrieval and reranking.
- Unit test ablation invokes all three modes and computes Recall@K.
- Integration test Flyway V11, absence of legacy ownership columns, JSONB sample IDs/run metadata, real ablation rows,
  `/actuator/prometheus`, `/v3/api-docs`, `/doc.html`, Prometheus target health,
  Tempo trace search, and Grafana datasource provisioning.

### 7. Wrong vs Correct

#### Wrong

```java
@Transactional
public EvaluationReport runAblation(...) {
    retrievalApi.retrieve(...); // remote/model work while DB transaction is open
}
```

#### Correct

```java
public EvaluationReport runAblation(...) {
    EvaluationMetric metric = evaluate(...);
    repository.insertRun(toRun(metric));
}
```

Only short repository writes use database transactions or autocommit.
