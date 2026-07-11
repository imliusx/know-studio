# Logging Guidelines

## Scenario: SLF4J and Logback

### 1. Scope / Trigger

Applies whenever backend code emits operational, error, audit, MQ, or RAG trace logs.

### 2. Signatures

- Logger declaration: Lombok `@Slf4j`.
- Development output: console pattern from `bootstrap/src/main/resources/logback-spring.xml`.
- Production output: asynchronous rolling JSON file using `logstash-logback-encoder`.
- Correlation field: MDC key `traceId`.

### 3. Contracts

- Log arguments use `{}` placeholders.
- Exceptions are passed as the final logger argument when stack traces are required.
- Never log passwords, tokens, raw authorization headers, or complete document contents.
- Production JSON files roll at 100 MB, retain 30 days, and cap total storage at 10 GB.

### 4. Validation & Error Matrix

- Missing trace ID -> output `traceId=`; request interceptors and async executors must propagate MDC.
- Sensitive payload requested for trace capture -> keep `captureInput/captureOutput` disabled.
- Unexpected exception -> log once at the global boundary; do not repeatedly log and rethrow at every layer.

### 5. Good/Base/Bad Cases

- Good: structured identifiers and elapsed time.
- Base: concise lifecycle information at INFO.
- Bad: token values, full prompt/document text, or duplicate stack traces.

### 6. Tests Required

- Application startup parses `logback-spring.xml` without warnings.
- Development output includes application, thread, logger, and traceId.
- Production profile creates valid JSON and rolling files.

### 7. Wrong vs Correct

#### Wrong

```java
log.info("token=" + token);
exception.printStackTrace();
```

#### Correct

```java
log.warn("provider call failed providerId={}", providerId, exception);
```
