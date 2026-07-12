package know.studio.arag.retrieval.infra.persistence;

import know.studio.arag.retrieval.domain.ChunkNeighborPort;
import know.studio.arag.retrieval.domain.NeighborChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PgChunkNeighborAdapter implements ChunkNeighborPort {

    private final RetrievalSearchMapper mapper;

    @Override
    public List<NeighborChunk> findNeighbors(Set<Long> knowledgeBaseIds, List<Long> seedChunkIds, int radius) {
        if (seedChunkIds.isEmpty()) {
            return List.of();
        }
        return mapper.findNeighbors(knowledgeBaseIds, seedChunkIds, radius);
    }
}
