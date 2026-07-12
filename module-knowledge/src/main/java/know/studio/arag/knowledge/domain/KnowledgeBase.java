package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.KnowledgeBaseVisibility;

public record KnowledgeBase(
        long id,
        String name,
        String description,
        KnowledgeBaseVisibility visibility,
        Long ownerTeamId,
        long createdBy,
        String status
) {
}
