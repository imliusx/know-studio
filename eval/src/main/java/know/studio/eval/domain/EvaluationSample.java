package know.studio.eval.domain;

import java.time.Instant;
import java.util.List;

public record EvaluationSample(
        long id,
        long knowledgeBaseId,
        long datasetId,
        String question,
        List<Long> relevantChunkIds,
        String expectedAnswer,
        boolean expectRefusal,
        Instant createdAt
) {

    public EvaluationSample {
        relevantChunkIds = List.copyOf(relevantChunkIds);
    }
}
