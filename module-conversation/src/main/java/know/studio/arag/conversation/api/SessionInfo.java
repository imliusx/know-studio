package know.studio.arag.conversation.api;

import know.studio.arag.platform.core.json.JsonLongId;

import java.time.Instant;

public record SessionInfo(
        @JsonLongId long id,
        @JsonLongId long userId,
        String title,
        boolean toolMode,
        boolean deepThinking,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
