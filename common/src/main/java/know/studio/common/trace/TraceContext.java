package know.studio.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文。traceId 存入 SLF4J {@link MDC}，日志模板可直接输出，
 * 并作为一次问答/请求内各 {@link RagTraceNode} 节点的关联键。
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";

    private TraceContext() {
    }

    /** 当前无 traceId 则生成一个并写入 MDC。 */
    public static String startIfAbsent() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = generate();
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return traceId;
    }

    public static String current() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void set(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    private static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
