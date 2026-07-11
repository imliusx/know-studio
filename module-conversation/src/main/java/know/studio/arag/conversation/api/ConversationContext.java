package know.studio.arag.conversation.api;

import java.util.List;

public record ConversationContext(
        long sessionId,
        String compactSummary,
        String sessionSummary,
        List<ConversationMessage> recentMessages,
        String currentQuestion
) {

    public ConversationContext {
        recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
    }
}
