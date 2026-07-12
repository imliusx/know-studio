package know.studio.arag.evaluation.api;

import know.studio.arag.platform.core.json.JsonLongId;
import know.studio.arag.platform.core.json.JsonLongIds;

import java.time.Instant;
import java.util.List;

public record SampleInfo(
        @JsonLongId long id,
        @JsonLongId long datasetId,
        String question,
        @JsonLongIds List<Long> relevantChunkIds,
        String expectedAnswer,
        Instant createdAt
) {

    public SampleInfo {
        relevantChunkIds = relevantChunkIds == null ? List.of() : List.copyOf(relevantChunkIds);
    }
}
