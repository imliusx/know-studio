package know.studio.arag.conversation.infra.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationPromptCatalogTest {

    @Test
    void rendersPreviousSummaryAndNewMessages() {
        ConversationPromptCatalog catalog = new ConversationPromptCatalog();

        assertThat(catalog.summaryUser("用户要配置 VPN", "USER: 目前无法连接"))
                .contains("用户要配置 VPN")
                .contains("USER: 目前无法连接");
        assertThat(catalog.summarySystem().version()).isEqualTo("conversation-summary-v1");
    }
}
