package know.studio.arag.conversation.api;

import know.studio.arag.platform.core.json.JsonLongId;

import java.util.List;

public record ConversationContext(
        @JsonLongId long sessionId,
        String compactSummary,
        String sessionSummary,
        List<ConversationMessage> recentMessages,
        String currentQuestion
) {

    public ConversationContext {
        recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
    }
}
