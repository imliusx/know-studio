package know.studio.chat.domain;

import java.time.Instant;

public record ConversationSession(
        long id,
        long userId,
        String title,
        boolean toolMode,
        boolean deepThinking,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
