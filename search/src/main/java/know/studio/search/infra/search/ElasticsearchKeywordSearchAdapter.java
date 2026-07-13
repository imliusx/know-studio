package know.studio.search.infra.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import know.studio.search.domain.KeywordSearchPort;
import know.studio.search.domain.RetrievalSource;
import know.studio.search.domain.SearchCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ElasticsearchKeywordSearchAdapter implements KeywordSearchPort {

    private static final String INDEX_NAME = "arag_document_chunks";

    private final ElasticsearchClient client;

    @Override
    public List<SearchCandidate> search(Set<Long> knowledgeBaseIds, String query, int limit) {
        try {
            List<FieldValue> allowedIds = knowledgeBaseIds.stream()
                    .map(FieldValue::of)
                    .toList();
            SearchResponse<KeywordChunkDocument> response = client.search(search -> search
                            .index(INDEX_NAME)
                            .size(limit)
                            .query(root -> root.bool(bool -> bool
                                    .filter(filter -> filter.terms(terms -> terms
                                            .field("knowledgeBaseId")
                                            .terms(values -> values.value(allowedIds))))
                                    .filter(filter -> filter.term(term -> term
                                            .field("status")
                                            .value("READY")))
                                    .filter(filter -> filter.term(term -> term
                                            .field("deleted")
                                            .value(false)))
                                    .must(must -> must.match(match -> match
                                            .field("chunkText")
                                            .query(query)
                                            .minimumShouldMatch("60%"))))),
                    KeywordChunkDocument.class
            );
            return response.hits().hits().stream()
                    .map(ElasticsearchKeywordSearchAdapter::toCandidate)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch keyword search failed", exception);
        }
    }

    private static SearchCandidate toCandidate(Hit<KeywordChunkDocument> hit) {
        KeywordChunkDocument source = hit.source();
        if (source == null || source.getId() == null) {
            return null;
        }
        return new SearchCandidate(
                source.getKnowledgeBaseId(),
                source.getId(),
                source.getDocumentId(),
                source.getChunkIndex(),
                source.getFileName(),
                source.getChunkText(),
                hit.score() == null ? 0.0 : hit.score(),
                RetrievalSource.KEYWORD
        );
    }
}
