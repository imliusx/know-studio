package know.studio.arag.retrieval.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmQueryPlannerTest {

    @Test
    void parsesNumberedModelOutputAndDeduplicatesQueries() {
        assertThat(LlmQueryPlanner.parse("1. vector retrieval\n- keyword retrieval\nvector retrieval\n4. ignored"))
                .containsExactly("vector retrieval", "keyword retrieval", "ignored");
    }

    @Test
    void keepsOriginalQuestionBeforeGeneratedExpansions() {
        assertThat(LlmQueryPlanner.combine(
                "Java 类名如何命名？",
                List.of("阿里巴巴 Java 命名规范", "Java 类名 UpperCamelCase")
        )).containsExactly(
                "Java 类名如何命名",
                "阿里巴巴 Java 命名规范",
                "Java 类名 UpperCamelCase"
        );
    }
}
