package know.studio.arag.retrieval.domain;

import java.util.List;

public interface KeywordSearchPort {

    List<SearchCandidate> search(long workspaceId, String query, int limit);
}
