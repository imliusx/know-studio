package know.studio.arag.retrieval.infra.persistence;

import know.studio.arag.retrieval.domain.ChunkNeighborPort;
import know.studio.arag.retrieval.domain.NeighborChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PgChunkNeighborAdapter implements ChunkNeighborPort {

    private final RetrievalSearchMapper mapper;

    @Override
    public List<NeighborChunk> findNeighbors(long workspaceId, List<Long> seedChunkIds, int radius) {
        if (seedChunkIds.isEmpty()) {
            return List.of();
        }
        return mapper.findNeighbors(workspaceId, seedChunkIds, radius);
    }
}
