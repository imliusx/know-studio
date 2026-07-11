package know.studio.arag.platform.ai.provider;

public record ChatChunk(Type type, String content) {

    public enum Type {
        TOKEN,
        THINKING
    }

    public ChatChunk {
        content = content == null ? "" : content;
    }

    public static ChatChunk token(String content) {
        return new ChatChunk(Type.TOKEN, content);
    }

    public static ChatChunk thinking(String content) {
        return new ChatChunk(Type.THINKING, content);
    }
}
