package know.studio.arag.retrieval.domain;

public record SearchCandidate(
        long chunkId,
        long documentId,
        int chunkIndex,
        String fileName,
        String text,
        double score,
        RetrievalSource source
) {
}
