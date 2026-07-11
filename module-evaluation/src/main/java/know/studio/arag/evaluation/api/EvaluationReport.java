package know.studio.arag.evaluation.api;

import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        long datasetId,
        int topK,
        List<EvaluationMetric> metrics,
        Instant completedAt
) {

    public EvaluationReport {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
