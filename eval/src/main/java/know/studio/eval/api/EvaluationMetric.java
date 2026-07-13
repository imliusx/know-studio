package know.studio.eval.api;

import know.studio.search.api.RetrievalMode;

public record EvaluationMetric(
        RetrievalMode mode,
        double recallAtK,
        double refusalAccuracy,
        int sampleCount,
        int positiveSampleCount,
        int refusalSampleCount,
        long averageLatencyMillis
) {
}
