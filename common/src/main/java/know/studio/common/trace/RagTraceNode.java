package know.studio.common.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 链路追踪节点标记。打在方法上，由 {@link RagTraceAspect} 环绕记录
 * traceId / 节点名 / 耗时 / 成败。用于把 RAG 长链路（检索/意图/生成/工具）串成一棵可观测的树。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RagTraceNode {

    /** 节点名，如 "retrieval.hybrid" / "agent.intent" / "qa.generate"。 */
    String value();

    /** Whether method arguments may be recorded. Keep disabled for sensitive inputs. */
    boolean captureInput() default false;

    /** Whether the return value may be recorded. Keep disabled for sensitive outputs. */
    boolean captureOutput() default false;
}
