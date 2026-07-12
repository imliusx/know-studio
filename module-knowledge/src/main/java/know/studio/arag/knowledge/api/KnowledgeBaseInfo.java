package know.studio.arag.knowledge.api;

public record KnowledgeBaseInfo(
        long knowledgeBaseId,
        String name,
        String description,
        KnowledgeBaseVisibility visibility,
        Long ownerTeamId,
        KnowledgeBasePermission permission
) {
}
