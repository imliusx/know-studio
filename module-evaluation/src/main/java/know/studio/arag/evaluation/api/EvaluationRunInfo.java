package know.studio.arag.evaluation.api;

import know.studio.arag.retrieval.api.RetrievalMode;

import java.time.Instant;

public record EvaluationRunInfo(
        long id,
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
