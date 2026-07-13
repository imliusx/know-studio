package know.studio.search.domain;

import java.util.List;
import java.util.Set;

public interface VectorSearchPort {

    List<SearchCandidate> search(Set<Long> knowledgeBaseIds, float[] queryEmbedding, int limit);
}
