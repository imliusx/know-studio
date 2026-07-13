package know.studio.knowledge.infra.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ChunkSearchRepository extends ElasticsearchRepository<ChunkSearchDocument, Long> {

    void deleteByKnowledgeBaseIdAndDocumentId(long knowledgeBaseId, long documentId);
}
