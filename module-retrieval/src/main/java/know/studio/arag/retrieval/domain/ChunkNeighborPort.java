package know.studio.arag.retrieval.domain;

import java.util.List;
import java.util.Set;

public interface ChunkNeighborPort {

    List<NeighborChunk> findNeighbors(Set<Long> knowledgeBaseIds, List<Long> seedChunkIds, int radius);
}
