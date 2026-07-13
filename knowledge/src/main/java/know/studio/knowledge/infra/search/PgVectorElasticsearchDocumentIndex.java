package know.studio.knowledge.infra.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import know.studio.knowledge.domain.DocumentChunk;
import know.studio.knowledge.domain.DocumentIndexPort;
import know.studio.knowledge.domain.DocumentRecord;
import know.studio.knowledge.domain.KnowledgeRepository;
import know.studio.knowledge.infra.persistence.ChunkEmbeddingRow;
import know.studio.knowledge.infra.persistence.mapper.KnowledgeCommandMapper;
import know.studio.ai.embedding.EmbeddingVectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PgVectorElasticsearchDocumentIndex implements DocumentIndexPort {

    private static final int EMBEDDING_DIMENSIONS = 1_024;

    private final KnowledgeRepository repository;
    private final KnowledgeCommandMapper commandMapper;
    private final ChunkSearchRepository searchRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void replace(
            long knowledgeBaseId,
            DocumentRecord document,
            List<DocumentChunk> chunks,
            List<float[]> embeddings
    ) {
        List<ChunkEmbeddingRow> rows = embeddingRows(knowledgeBaseId, document.id(), chunks, embeddings);
        commandMapper.deleteChunkEmbeddings(knowledgeBaseId, document.id());
        repository.replaceDocumentChunks(knowledgeBaseId, document.id(), chunks);
        commandMapper.insertChunkEmbeddings(rows);

        searchRepository.deleteByKnowledgeBaseIdAndDocumentId(knowledgeBaseId, document.id());
        searchRepository.saveAll(searchDocuments(document, chunks));
    }

    @Override
    @Transactional
    public void delete(long knowledgeBaseId, long documentId) {
        commandMapper.deleteChunkEmbeddings(knowledgeBaseId, documentId);
        searchRepository.deleteByKnowledgeBaseIdAndDocumentId(knowledgeBaseId, documentId);
    }

    private List<ChunkEmbeddingRow> embeddingRows(
            long knowledgeBaseId,
            long documentId,
            List<DocumentChunk> chunks,
            List<float[]> embeddings
    ) {
        List<ChunkEmbeddingRow> rows = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            float[] embedding = embeddings.get(index);
            if (embedding.length != EMBEDDING_DIMENSIONS) {
                throw new IllegalStateException("Expected 1024-dimensional BGE-M3 embedding");
            }
            DocumentChunk chunk = chunks.get(index);
            rows.add(new ChunkEmbeddingRow(
                    chunk.id(),
                    knowledgeBaseId,
                    documentId,
                    EmbeddingVectors.toPgVectorLiteral(embedding),
                    metadata(chunk)
            ));
        }
        return rows;
    }

    private List<ChunkSearchDocument> searchDocuments(DocumentRecord document, List<DocumentChunk> chunks) {
        return chunks.stream().map(chunk -> {
            ChunkSearchDocument target = new ChunkSearchDocument();
            target.setId(chunk.id());
            target.setKnowledgeBaseId(chunk.knowledgeBaseId());
            target.setDocumentId(chunk.documentId());
            target.setChunkIndex(chunk.chunkIndex());
            target.setFileName(document.fileName());
            target.setChunkText(chunk.text());
            target.setStatus("READY");
            target.setDeleted(false);
            return target;
        }).toList();
    }

    private String metadata(DocumentChunk chunk) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "chunkIndex", chunk.chunkIndex(),
                    "sectionPath", chunk.sectionPath() == null ? "" : chunk.sectionPath()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chunk metadata", exception);
        }
    }

}
