package know.studio.search.domain;

public record SearchCandidate(
        long knowledgeBaseId,
        long chunkId,
        long documentId,
        int chunkIndex,
        String fileName,
        String text,
        double score,
        RetrievalSource source
) {
}
