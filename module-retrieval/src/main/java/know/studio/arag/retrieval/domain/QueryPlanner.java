package know.studio.arag.retrieval.domain;

import java.util.List;

public interface QueryPlanner {

    List<String> plan(String question);
}
