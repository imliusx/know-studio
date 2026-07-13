package know.studio.chat.api;

import know.studio.common.json.JsonLongId;

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
