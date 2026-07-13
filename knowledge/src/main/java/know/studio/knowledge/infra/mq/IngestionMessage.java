package know.studio.knowledge.infra.mq;

public record IngestionMessage(long knowledgeBaseId, long documentId) {
}
