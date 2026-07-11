package know.studio.arag.conversation.api;

import java.time.Instant;

public record SessionInfo(
        long id,
        long workspaceId,
        long userId,
        String title,
        boolean toolMode,
        boolean deepThinking,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
