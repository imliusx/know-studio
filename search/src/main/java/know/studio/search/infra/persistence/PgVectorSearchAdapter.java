package know.studio.search.infra.persistence;

import know.studio.ai.embedding.EmbeddingVectors;
import know.studio.search.domain.RetrievalSource;
import know.studio.search.domain.SearchCandidate;
import know.studio.search.domain.VectorSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PgVectorSearchAdapter implements VectorSearchPort {

    private final RetrievalSearchMapper mapper;

    @Override
    public List<SearchCandidate> search(Set<Long> knowledgeBaseIds, float[] queryEmbedding, int limit) {
        return mapper.searchByVector(
                        knowledgeBaseIds,
                        EmbeddingVectors.toPgVectorLiteral(queryEmbedding),
                        limit
                ).stream()
                .map(row -> new SearchCandidate(
                        row.getKnowledgeBaseId(),
                        row.getChunkId(),
                        row.getDocumentId(),
                        row.getChunkIndex(),
                        row.getFileName(),
                        row.getChunkText(),
                        row.getScore(),
                        RetrievalSource.VECTOR
                ))
                .toList();
    }
}
