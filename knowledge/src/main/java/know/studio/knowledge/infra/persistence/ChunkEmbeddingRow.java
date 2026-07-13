package know.studio.knowledge.infra.persistence;

public record ChunkEmbeddingRow(
        long chunkId,
        long knowledgeBaseId,
        long documentId,
        String embedding,
        String metadata
) {
}
