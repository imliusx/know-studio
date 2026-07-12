package know.studio.arag.retrieval.api;

import java.util.Set;

public record Evidence(
        long knowledgeBaseId,
        long documentId,
        long chunkId,
        int chunkIndex,
        String fileName,
        String text,
        double score,
        Set<String> sources
) {

    public Evidence {
        sources = sources == null ? Set.of() : Set.copyOf(sources);
    }
}
