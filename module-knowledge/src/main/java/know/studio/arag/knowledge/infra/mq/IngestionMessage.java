package know.studio.arag.knowledge.infra.mq;

public record IngestionMessage(long workspaceId, long documentId) {
}
