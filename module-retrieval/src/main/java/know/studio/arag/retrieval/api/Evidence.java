package know.studio.arag.retrieval.api;

import know.studio.arag.platform.core.json.JsonLongId;

import java.util.Set;

public record Evidence(
        @JsonLongId long knowledgeBaseId,
        @JsonLongId long documentId,
        @JsonLongId long chunkId,
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
