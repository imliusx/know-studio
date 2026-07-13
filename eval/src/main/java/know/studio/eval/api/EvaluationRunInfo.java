package know.studio.eval.api;

import know.studio.search.api.RetrievalMode;
import know.studio.common.json.JsonLongId;

import java.time.Instant;

public record EvaluationRunInfo(
        @JsonLongId long id,
        @JsonLongId long datasetId,
        @JsonLongId long userId,
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
