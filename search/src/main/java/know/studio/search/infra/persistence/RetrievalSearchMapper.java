package know.studio.search.infra.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import know.studio.search.domain.NeighborChunk;

import java.util.List;
import java.util.Set;

@Mapper
public interface RetrievalSearchMapper {

    List<VectorSearchRow> searchByVector(
            @Param("knowledgeBaseIds") Set<Long> knowledgeBaseIds,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    List<NeighborChunk> findNeighbors(
            @Param("knowledgeBaseIds") Set<Long> knowledgeBaseIds,
            @Param("seedChunkIds") List<Long> seedChunkIds,
            @Param("radius") int radius
    );
}
