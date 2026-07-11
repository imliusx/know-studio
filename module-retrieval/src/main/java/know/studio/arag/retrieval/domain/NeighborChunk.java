package know.studio.arag.retrieval.domain;

public record NeighborChunk(
        long chunkId,
        long documentId,
        int chunkIndex,
        String fileName,
        String text
) {
}
