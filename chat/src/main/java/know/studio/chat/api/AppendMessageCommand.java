package know.studio.chat.api;

import java.util.Map;

public record AppendMessageCommand(
        long sessionId,
        MessageRole role,
        String content,
        int tokens,
        Map<String, Object> metadata
) {

    public AppendMessageCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
