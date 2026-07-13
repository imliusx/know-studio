package know.studio.eval.domain;

import java.time.Instant;

public record EvaluationDataset(
        long id,
        long knowledgeBaseId,
        long userId,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
