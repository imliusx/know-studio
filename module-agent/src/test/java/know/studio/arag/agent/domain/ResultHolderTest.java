package know.studio.arag.agent.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ResultHolderTest {

    @Test
    void computesSameToolCallOnlyOnce() {
        ResultHolder holder = new ResultHolder();
        AtomicInteger calls = new AtomicInteger();

        ToolResult first = holder.getOrCompute("tool:args", () -> result(calls));
        ToolResult second = holder.getOrCompute("tool:args", () -> result(calls));

        assertThat(first).isSameAs(second);
        assertThat(calls).hasValue(1);
    }

    private static ToolResult result(AtomicInteger calls) {
        calls.incrementAndGet();
        return new ToolResult("tool", "result", Map.of());
    }
}
