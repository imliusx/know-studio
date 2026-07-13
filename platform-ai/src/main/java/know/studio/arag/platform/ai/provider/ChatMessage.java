package know.studio.arag.platform.ai.provider;

public record ChatMessage(ChatMessageRole role, String content) {

    public ChatMessage {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        content = content == null ? "" : content.trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatMessageRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatMessageRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatMessageRole.ASSISTANT, content);
    }
}
