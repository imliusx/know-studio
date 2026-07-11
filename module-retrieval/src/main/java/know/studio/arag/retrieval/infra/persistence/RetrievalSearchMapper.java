package know.studio.arag.retrieval.infra.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import know.studio.arag.retrieval.domain.NeighborChunk;

import java.util.List;

@Mapper
public interface RetrievalSearchMapper {

    List<VectorSearchRow> searchByVector(
            @Param("workspaceId") long workspaceId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    List<NeighborChunk> findNeighbors(
            @Param("workspaceId") long workspaceId,
            @Param("seedChunkIds") List<Long> seedChunkIds,
            @Param("radius") int radius
    );
}
