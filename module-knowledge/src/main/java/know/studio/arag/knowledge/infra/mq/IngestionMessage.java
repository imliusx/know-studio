package know.studio.arag.knowledge.infra.mq;

public record IngestionMessage(long knowledgeBaseId, long documentId) {
}
