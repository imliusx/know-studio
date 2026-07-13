package know.studio.arag.agent.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptCatalogTest {

    private final AgentPromptCatalog catalog = new AgentPromptCatalog();

    @Test
    void separatesNaturalChatFromGroundedKnowledgeInstructions() {
        assertThat(catalog.chat().text())
                .contains("自然交流")
                .doesNotContain("只能使用与问题直接相关的证据");
        assertThat(catalog.knowledge().text())
                .contains("只能使用与问题直接相关的证据")
                .contains("保持自然、直接、专业")
                .contains("不要机械照抄")
                .contains("精确标识符")
                .contains("CommonMark/GFM")
                .contains("- 主键索引")
                .contains("禁止写成 `-主键索引`")
                .contains("每个列表项必须独占一行")
                .contains("fenced code block");
        assertThat(catalog.chat().version()).isEqualTo("chat-v1");
        assertThat(catalog.knowledge().version()).isEqualTo("knowledge-v3");
    }

    @Test
    void rendersKnowledgeQuestionAndEvidence() {
        assertThat(catalog.knowledgeUser("Java 类名如何命名？", "SUFFICIENT", "正常回答", "UpperCamelCase"))
                .contains("Java 类名如何命名？")
                .contains("SUFFICIENT")
                .contains("UpperCamelCase");
    }
}
