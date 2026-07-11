package know.studio.arag.evaluation.api;

import know.studio.arag.retrieval.api.RetrievalMode;

public record EvaluationMetric(
        RetrievalMode mode,
        double recallAtK,
        int sampleCount,
        long averageLatencyMillis
) {
}
