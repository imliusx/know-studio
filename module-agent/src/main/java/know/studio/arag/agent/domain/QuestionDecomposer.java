package know.studio.arag.agent.domain;

import java.util.List;

public interface QuestionDecomposer {

    List<String> decompose(String question);
}
