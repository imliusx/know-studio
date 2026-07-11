package know.studio.arag.evaluation.domain;

import know.studio.arag.retrieval.api.RetrievalMode;

import java.time.Instant;

public record EvaluationRun(
        long id,
        long workspaceId,
        long datasetId,
        long userId,
        RetrievalMode mode,
        double recallAtK,
        int sampleCount,
        long averageLatencyMillis,
        int topK,
        Instant createdAt
) {
}
