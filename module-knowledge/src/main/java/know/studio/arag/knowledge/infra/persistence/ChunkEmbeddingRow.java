package know.studio.arag.knowledge.infra.persistence;

public record ChunkEmbeddingRow(
        long chunkId,
        long knowledgeBaseId,
        long documentId,
        String embedding,
        String metadata
) {
}
