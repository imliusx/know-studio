package know.studio.arag.knowledge.infra.persistence.mapper;

import know.studio.arag.knowledge.infra.persistence.entity.DocumentChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeCommandMapper {

    int ensureIngestionJob(
            @Param("jobId") long jobId,
            @Param("workspaceId") long workspaceId,
            @Param("documentId") long documentId
    );

    int claimDocumentForProcessing(
            @Param("workspaceId") long workspaceId,
            @Param("documentId") long documentId
    );

    int insertDocumentChunks(@Param("chunks") List<DocumentChunkEntity> chunks);

    int appendNodeLog(@Param("documentId") long documentId, @Param("nodeLogJson") String nodeLogJson);
}
