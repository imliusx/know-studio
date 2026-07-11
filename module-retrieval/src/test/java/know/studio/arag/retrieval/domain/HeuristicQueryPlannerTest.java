package know.studio.arag.retrieval.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicQueryPlannerTest {

    private final HeuristicQueryPlanner planner = new HeuristicQueryPlanner();

    @Test
    void doesNotDuplicateQuestionWhenOnlyTrailingPunctuationChanges() {
        assertThat(planner.plan("What is RAG?"))
                .containsExactly("What is RAG");
    }

    @Test
    void addsMeaningfulSubQueriesWithoutExceedingLimit() {
        assertThat(planner.plan("向量检索以及关键词检索并且重排"))
                .containsExactly("向量检索以及关键词检索并且重排", "向量检索", "关键词检索");
    }
}
