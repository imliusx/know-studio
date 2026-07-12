package know.studio.arag.evaluation.api;

import java.time.Instant;

public record DatasetInfo(
        long id,
        long knowledgeBaseId,
        long userId,
        String name,
        String description,
        Instant createdAt
) {
}
