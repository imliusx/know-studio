package know.studio.arag.retrieval.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalPromptCatalogTest {

    @Test
    void loadsBoundedQueryPlanningPrompt() {
        RetrievalPromptCatalog catalog = new RetrievalPromptCatalog();

        assertThat(catalog.queryPlanning().text())
                .contains("最多输出 3 行")
                .contains("单一意图")
                .contains("不得扩展为整份文档");
        assertThat(catalog.queryPlanning().version()).isEqualTo("query-planning-v1");
    }
}
