package know.studio.arag.evaluation.api;

import know.studio.arag.platform.core.json.JsonLongId;

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
