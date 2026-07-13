package know.studio.eval.domain;

import know.studio.search.api.RetrievalMode;

import java.time.Instant;

public record EvaluationRun(
        long id,
        long knowledgeBaseId,
        long datasetId,
        long userId,
        RetrievalMode mode,
        double recallAtK,
        double refusalAccuracy,
        int sampleCount,
        int positiveSampleCount,
        int refusalSampleCount,
        long averageLatencyMillis,
        int topK,
        Instant createdAt
) {
}
