package know.studio.arag.conversation.api;

import java.time.Instant;
import java.util.Map;

public record ConversationMessage(
        long id,
        MessageRole role,
        String content,
        int tokens,
        Map<String, Object> metadata,
        Instant createdAt
) {

    public ConversationMessage {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
