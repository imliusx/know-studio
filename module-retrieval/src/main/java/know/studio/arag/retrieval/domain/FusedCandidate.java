package know.studio.arag.retrieval.domain;

import java.util.Set;

public record FusedCandidate(
        long chunkId,
        long documentId,
        int chunkIndex,
        String fileName,
        String text,
        double rrfScore,
        Double rerankScore,
        Set<RetrievalSource> sources,
        int supportCount
) {

    public FusedCandidate {
        sources = Set.copyOf(sources);
        supportCount = Math.max(1, supportCount);
    }

    public double finalScore() {
        return rerankScore == null ? rrfScore : rerankScore;
    }

    public FusedCandidate withRerankScore(double score) {
        return new FusedCandidate(
                chunkId,
                documentId,
                chunkIndex,
                fileName,
                text,
                rrfScore,
                score,
                sources,
                supportCount
        );
    }
}
