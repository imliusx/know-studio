package know.studio.arag.platform.core.trace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 链路追踪切面。环绕 {@link RagTraceNode} 标注的方法，记录 traceId / 节点 / 耗时 / 成败。
 *
 * <p>MVP 以结构化日志落地（可被 Loki/ELK 采集）；后续可在此接入 OpenTelemetry span 导出。
 */
@Aspect
@Component
public class RagTraceAspect {

    private static final Logger log = LoggerFactory.getLogger(RagTraceAspect.class);

    @Around("@annotation(node)")
    public Object around(ProceedingJoinPoint pjp, RagTraceNode node) throws Throwable {
        String traceId = TraceContext.startIfAbsent();
        String step = node.value();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[rag-trace] traceId={} step={} elapsedMs={} status=OK", traceId, step, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("[rag-trace] traceId={} step={} elapsedMs={} status=ERR err={}", traceId, step, System.currentTimeMillis() - start, ex.toString());
            throw ex;
        }
    }
}
