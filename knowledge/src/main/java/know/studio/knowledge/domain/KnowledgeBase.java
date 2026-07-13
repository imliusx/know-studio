package know.studio.knowledge.domain;

import know.studio.knowledge.api.KnowledgeBaseVisibility;

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
