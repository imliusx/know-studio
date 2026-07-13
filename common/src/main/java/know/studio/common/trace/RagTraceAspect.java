package know.studio.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 链路追踪切面。环绕 {@link RagTraceNode} 标注的方法，记录 traceId / 节点 / 耗时 / 成败。
 *
 * <p>MVP 以结构化日志落地（可被 Loki/ELK 采集）；后续可在此接入 OpenTelemetry span 导出。
 */
@Aspect
@Slf4j
public class RagTraceAspect {
    private static final int ATTRIBUTE_LIMIT = 2_000;

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    RagTraceAspect(OpenTelemetry openTelemetry, MeterRegistry meterRegistry) {
        this.tracer = openTelemetry.getTracer("know.studio.app.rag");
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(node)")
    public Object around(ProceedingJoinPoint pjp, RagTraceNode node) throws Throwable {
        Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getReturnType();
        if (Mono.class.isAssignableFrom(returnType)) {
            return Mono.defer(() -> Mono.from(tracePublisher(pjp, node)));
        }
        if (Publisher.class.isAssignableFrom(returnType)) {
            return Flux.defer(() -> Flux.from(tracePublisher(pjp, node)));
        }
        return traceSynchronous(pjp, node);
    }

    private Object traceSynchronous(ProceedingJoinPoint pjp, RagTraceNode node) throws Throwable {
        TraceState state = startTrace(pjp, node);
        try (Scope ignored = state.span().makeCurrent()) {
            Object result = pjp.proceed();
            completeSuccess(state, node.captureOutput() ? String.valueOf(result) : null);
            return result;
        } catch (Throwable exception) {
            completeError(state, exception);
            throw exception;
        } finally {
            finish(state);
        }
    }

    private Publisher<?> tracePublisher(ProceedingJoinPoint pjp, RagTraceNode node) {
        TraceState state = startTrace(pjp, node);
        try (Scope ignored = state.span().makeCurrent()) {
            Publisher<?> publisher = (Publisher<?>) pjp.proceed();
            restoreTraceContext(state);
            java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean();
            return Flux.from(publisher)
                    .doOnNext(value -> {
                        if (node.captureOutput()) {
                            state.span().setAttribute("rag.output", truncate(String.valueOf(value)));
                        }
                    })
                    .doOnComplete(() -> {
                        completeSuccess(state, null);
                        completed.set(true);
                    })
                    .doOnError(exception -> {
                        completeError(state, exception);
                        completed.set(true);
                    })
                    .doFinally(ignoredSignal -> {
                        if (!completed.get()) {
                            completeSuccess(state, null);
                        }
                        state.span().end();
                    });
        } catch (Throwable exception) {
            completeError(state, exception);
            restoreTraceContext(state);
            state.span().end();
            return Flux.error(exception);
        }
    }

    private TraceState startTrace(ProceedingJoinPoint pjp, RagTraceNode node) {
        String step = node.value();
        Span span = tracer.spanBuilder(step).startSpan();
        String previousTraceId = TraceContext.current();
        String traceId = resolveTraceId(span, previousTraceId);
        TraceContext.set(traceId);
        span.setAttribute("rag.step", step);
        span.setAttribute("code.function", pjp.getSignature().toShortString());
        if (node.captureInput()) {
            span.setAttribute("rag.input", truncate(java.util.Arrays.deepToString(pjp.getArgs())));
        }
        return new TraceState(step, traceId, previousTraceId, span, System.nanoTime());
    }

    private void completeSuccess(TraceState state, String output) {
        long elapsedMs = elapsedMillis(state.startNanos());
        state.span().setAttribute("rag.elapsed_ms", elapsedMs);
        if (output != null) {
            state.span().setAttribute("rag.output", truncate(output));
        }
        state.span().setStatus(StatusCode.OK);
        meterRegistry.timer("arag.trace.node", "step", state.step(), "status", "success")
                .record(elapsedMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.info("[rag-trace] traceId={} step={} elapsedMs={} status=OK", state.traceId(), state.step(), elapsedMs);
    }

    private void completeError(TraceState state, Throwable exception) {
        long elapsedMs = elapsedMillis(state.startNanos());
        state.span().setAttribute("rag.elapsed_ms", elapsedMs);
        state.span().recordException(exception);
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        state.span().setStatus(StatusCode.ERROR, message);
        meterRegistry.timer("arag.trace.node", "step", state.step(), "status", "error")
                .record(elapsedMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.warn(
                "[rag-trace] traceId={} step={} elapsedMs={} status=ERR err={}",
                state.traceId(),
                state.step(),
                elapsedMs,
                exception.toString()
        );
    }

    private static void finish(TraceState state) {
        state.span().end();
        restoreTraceContext(state);
    }

    private static void restoreTraceContext(TraceState state) {
        if (state.previousTraceId() == null) {
            TraceContext.clear();
        } else {
            TraceContext.set(state.previousTraceId());
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

    private record TraceState(
            String step,
            String traceId,
            String previousTraceId,
            Span span,
            long startNanos
    ) {
    }
}
