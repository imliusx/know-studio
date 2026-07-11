package know.studio.arag.agent.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ResultHolder {

    private final Map<String, ToolResult> results = new ConcurrentHashMap<>();

    public ToolResult getOrCompute(String key, Supplier<ToolResult> supplier) {
        return results.computeIfAbsent(key, ignored -> supplier.get());
    }
}
