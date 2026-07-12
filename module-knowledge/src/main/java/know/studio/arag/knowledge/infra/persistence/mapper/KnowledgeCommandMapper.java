package know.studio.arag.knowledge.infra.persistence.mapper;

import know.studio.arag.knowledge.infra.persistence.ChunkEmbeddingRow;
import know.studio.arag.knowledge.infra.persistence.entity.DocumentChunkEntity;
import know.studio.arag.knowledge.infra.persistence.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface KnowledgeCommandMapper {

    int ensureIngestionJob(
            @Param("jobId") long jobId,
            @Param("knowledgeBaseId") long knowledgeBaseId,
            @Param("documentId") long documentId
    );

    int claimDocumentForProcessing(
            @Param("knowledgeBaseId") long knowledgeBaseId,
            @Param("documentId") long documentId
    );

    int insertDocumentChunks(@Param("chunks") List<DocumentChunkEntity> chunks);

    int deleteChunkEmbeddings(
            @Param("knowledgeBaseId") long knowledgeBaseId,
            @Param("documentId") long documentId
    );

    int insertChunkEmbeddings(@Param("embeddings") List<ChunkEmbeddingRow> embeddings);

    int appendNodeLog(@Param("documentId") long documentId, @Param("nodeLogJson") String nodeLogJson);

    List<DocumentEntity> recoverStaleProcessing(
            @Param("updatedBefore") Instant updatedBefore,
            @Param("limit") int limit
    );
}
