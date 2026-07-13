package know.studio.eval.api;

import know.studio.common.json.JsonLongId;

import java.time.Instant;

public record DatasetInfo(
        @JsonLongId long id,
        @JsonLongId long knowledgeBaseId,
        @JsonLongId long userId,
        String name,
        String description,
        Instant createdAt
) {
}
