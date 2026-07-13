package know.studio.knowledge.api;

import know.studio.common.json.JsonLongId;

public record KnowledgeBaseInfo(
        @JsonLongId long knowledgeBaseId,
        String name,
        String description,
        KnowledgeBaseVisibility visibility,
        @JsonLongId Long ownerTeamId,
        KnowledgeBasePermission permission
) {
}
