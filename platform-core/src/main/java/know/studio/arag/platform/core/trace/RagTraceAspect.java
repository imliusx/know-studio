package know.studio.arag.platform.core.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 链路追踪切面。环绕 {@link RagTraceNode} 标注的方法，记录 traceId / 节点 / 耗时 / 成败。
 *
 * <p>MVP 以结构化日志落地（可被 Loki/ELK 采集）；后续可在此接入 OpenTelemetry span 导出。
 */
@Aspect
@Component
@Slf4j
public class RagTraceAspect {
    private static final int ATTRIBUTE_LIMIT = 2_000;

    private final Tracer tracer;

    public RagTraceAspect() {
        this(GlobalOpenTelemetry.get());
    }

    RagTraceAspect(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("know.studio.arag.rag");
    }

    @Around("@annotation(node)")
    public Object around(ProceedingJoinPoint pjp, RagTraceNode node) throws Throwable {
        String step = node.value();
        Span span = tracer.spanBuilder(step).startSpan();
        String previousTraceId = TraceContext.current();
        String traceId = resolveTraceId(span, previousTraceId);
        long start = System.nanoTime();

        try (Scope ignored = span.makeCurrent()) {
            TraceContext.set(traceId);
            span.setAttribute("rag.step", step);
            span.setAttribute("code.function", pjp.getSignature().toShortString());
            if (node.captureInput()) {
                span.setAttribute("rag.input", truncate(java.util.Arrays.deepToString(pjp.getArgs())));
            }

            Object result = pjp.proceed();
            long elapsedMs = elapsedMillis(start);
            span.setAttribute("rag.elapsed_ms", elapsedMs);
            if (node.captureOutput()) {
                span.setAttribute("rag.output", truncate(String.valueOf(result)));
            }
            span.setStatus(StatusCode.OK);
            log.info("[rag-trace] traceId={} step={} elapsedMs={} status=OK", traceId, step, elapsedMs);
            return result;
        } catch (Throwable ex) {
            long elapsedMs = elapsedMillis(start);
            span.setAttribute("rag.elapsed_ms", elapsedMs);
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            log.warn("[rag-trace] traceId={} step={} elapsedMs={} status=ERR err={}", traceId, step, elapsedMs, ex.toString());
            throw ex;
        } finally {
            span.end();
            if (previousTraceId == null) {
                TraceContext.clear();
            } else {
                TraceContext.set(previousTraceId);
            }
        }
    }

    private static String resolveTraceId(Span span, String previousTraceId) {
        if (previousTraceId != null && !previousTraceId.isBlank()) {
            return previousTraceId;
        }
        if (span.getSpanContext().isValid()) {
            return span.getSpanContext().getTraceId();
        }
        return TraceContext.startIfAbsent();
    }

    private static long elapsedMillis(long start) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private static String truncate(String value) {
        if (value.length() <= ATTRIBUTE_LIMIT) {
            return value;
        }
        return value.substring(0, ATTRIBUTE_LIMIT);
    }
}
