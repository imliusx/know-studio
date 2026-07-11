package know.studio.arag.evaluation.api;

import java.time.Instant;
import java.util.List;

public record SampleInfo(
        long id,
        long datasetId,
        String question,
        List<Long> relevantChunkIds,
        String expectedAnswer,
        Instant createdAt
) {

    public SampleInfo {
        relevantChunkIds = relevantChunkIds == null ? List.of() : List.copyOf(relevantChunkIds);
    }
}
