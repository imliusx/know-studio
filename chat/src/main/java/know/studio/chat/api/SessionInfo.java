package know.studio.chat.api;

import know.studio.common.json.JsonLongId;

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
