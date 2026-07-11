package know.studio.arag.evaluation.api;

import java.time.Instant;

public record DatasetInfo(
        long id,
        long workspaceId,
        long userId,
        String name,
        String description,
        Instant createdAt
) {
}
