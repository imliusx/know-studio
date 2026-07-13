package know.studio.eval.api;

import know.studio.common.json.JsonLongId;
import know.studio.common.json.JsonLongIds;

import java.time.Instant;
import java.util.List;

public record SampleInfo(
        @JsonLongId long id,
        @JsonLongId long datasetId,
        String question,
        @JsonLongIds List<Long> relevantChunkIds,
        String expectedAnswer,
        boolean expectRefusal,
        Instant createdAt
) {

    public SampleInfo {
        relevantChunkIds = relevantChunkIds == null ? List.of() : List.copyOf(relevantChunkIds);
    }
}
