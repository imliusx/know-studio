package know.studio.arag.evaluation.api;

import know.studio.arag.platform.core.json.JsonLongId;

import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        @JsonLongId long datasetId,
        int topK,
        List<EvaluationMetric> metrics,
        Instant completedAt
) {

    public EvaluationReport {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
