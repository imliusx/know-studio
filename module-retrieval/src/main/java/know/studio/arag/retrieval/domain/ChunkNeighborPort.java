package know.studio.arag.retrieval.domain;

import java.util.List;

public interface ChunkNeighborPort {

    List<NeighborChunk> findNeighbors(long workspaceId, List<Long> seedChunkIds, int radius);
}
