package know.studio.search.domain;

public record NeighborChunk(
        long knowledgeBaseId,
        long chunkId,
        long documentId,
        int chunkIndex,
        String fileName,
        String text
) {
}
