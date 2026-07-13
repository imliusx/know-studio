package know.studio.agent.domain;

import java.util.Map;

public record ToolResult(String toolName, String content, Map<String, Object> metadata) {

    public ToolResult {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
