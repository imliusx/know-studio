package know.studio.arag.platform.ai.provider;

import java.util.Map;

public record ChatRequest(
        String systemPrompt,
        String userPrompt,
        boolean reasoning,
        Map<String, Object> options
) {

    public ChatRequest {
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public static ChatRequest chat(String systemPrompt, String userPrompt) {
        return new ChatRequest(systemPrompt, userPrompt, false, Map.of());
    }
}
