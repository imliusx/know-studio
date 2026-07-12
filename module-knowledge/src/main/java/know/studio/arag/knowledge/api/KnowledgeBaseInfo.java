package know.studio.arag.knowledge.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record KnowledgeBaseInfo(
        @JsonLongId long knowledgeBaseId,
        String name,
        String description,
        KnowledgeBaseVisibility visibility,
        @JsonLongId Long ownerTeamId,
        KnowledgeBasePermission permission
) {
}
