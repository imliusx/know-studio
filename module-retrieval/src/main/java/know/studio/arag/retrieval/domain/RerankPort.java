package know.studio.arag.retrieval.domain;

import java.util.List;

public interface RerankPort {

    List<FusedCandidate> rerank(String query, List<FusedCandidate> candidates);
}
