package know.studio.search.domain;

import java.util.List;

public interface QueryPlanner {

    List<String> plan(String question);
}
