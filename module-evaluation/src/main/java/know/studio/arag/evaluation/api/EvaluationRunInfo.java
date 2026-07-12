package know.studio.arag.evaluation.api;

import know.studio.arag.retrieval.api.RetrievalMode;
import know.studio.arag.platform.core.json.JsonLongId;

import java.time.Instant;

public record EvaluationRunInfo(
        @JsonLongId long id,
        @JsonLongId long datasetId,
        @JsonLongId long userId,
        RetrievalMode mode,
        double recallAtK,
        int sampleCount,
        long averageLatencyMillis,
        int topK,
        Instant createdAt
) {
}
