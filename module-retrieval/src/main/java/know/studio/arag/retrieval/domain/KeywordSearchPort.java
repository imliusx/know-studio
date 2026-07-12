package know.studio.arag.retrieval.domain;

import java.util.List;
import java.util.Set;

public interface KeywordSearchPort {

    List<SearchCandidate> search(Set<Long> knowledgeBaseIds, String query, int limit);
}
