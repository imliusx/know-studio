package know.studio.arag.retrieval.domain;

import java.util.List;

public interface VectorSearchPort {

    List<SearchCandidate> search(long workspaceId, float[] queryEmbedding, int limit);
}
