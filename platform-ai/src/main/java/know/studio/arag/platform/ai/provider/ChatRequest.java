package know.studio.arag.platform.ai.provider;

import java.util.List;
import java.util.Map;

public record ChatRequest(
        String systemPrompt,
        List<ChatMessage> history,
        String userPrompt,
        boolean reasoning,
        GenerationProfile profile,
        String promptVersion,
        Map<String, Object> options
) {

    public ChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
        profile = profile == null ? GenerationProfile.KNOWLEDGE : profile;
        promptVersion = promptVersion == null || promptVersion.isBlank() ? "unknown" : promptVersion;
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public ChatRequest(String systemPrompt, String userPrompt, boolean reasoning, Map<String, Object> options) {
        this(
                systemPrompt,
                List.of(),
                userPrompt,
                reasoning,
                GenerationProfile.KNOWLEDGE,
                "legacy-inline",
                options
        );
    }

    public static ChatRequest chat(String systemPrompt, String userPrompt) {
        return of(systemPrompt, List.of(), userPrompt, GenerationProfile.CHAT, "inline-chat");
    }

    public static ChatRequest of(
            String systemPrompt,
            List<ChatMessage> history,
            String userPrompt,
            GenerationProfile profile,
            String promptVersion
    ) {
        return new ChatRequest(
                systemPrompt,
                history,
                userPrompt,
                false,
                profile,
                promptVersion,
                Map.of()
        );
    }
}
