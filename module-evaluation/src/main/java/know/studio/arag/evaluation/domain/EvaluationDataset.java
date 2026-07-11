package know.studio.arag.evaluation.domain;

import java.time.Instant;

public record EvaluationDataset(
        long id,
        long workspaceId,
        long userId,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
