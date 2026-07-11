package know.studio.arag.retrieval.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmQueryPlannerTest {

    @Test
    void parsesNumberedModelOutputAndDeduplicatesQueries() {
        assertThat(LlmQueryPlanner.parse("1. vector retrieval\n- keyword retrieval\nvector retrieval\n4. ignored"))
                .containsExactly("vector retrieval", "keyword retrieval", "ignored");
    }
}
