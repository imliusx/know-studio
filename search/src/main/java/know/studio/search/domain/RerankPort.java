package know.studio.search.domain;

import java.util.List;

public interface RerankPort {

    List<FusedCandidate> rerank(String query, List<FusedCandidate> candidates);
}
