package know.studio.agent.domain;

import know.studio.agent.api.IntentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicIntentClassifierTest {

    private final HeuristicIntentClassifier classifier = new HeuristicIntentClassifier();

    @Test
    void routesKnowledgeToolChatAndClarify() {
        assertThat(classifier.classify("介绍一下项目架构", false).intent()).isEqualTo(IntentType.KNOWLEDGE);
        assertThat(classifier.classify("搜索今天的新闻", true).intent()).isEqualTo(IntentType.TOOL);
        assertThat(classifier.classify("你好", false).intent()).isEqualTo(IntentType.CHAT);
        assertThat(classifier.classify("你好，谢谢", true).intent()).isEqualTo(IntentType.CHAT);
        assertThat(classifier.classify("这个", true).intent()).isEqualTo(IntentType.CLARIFY);
    }

    @Test
    void toolModeOffKeepsExternalLookingQuestionInKnowledgeRoute() {
        assertThat(classifier.classify("搜索今天的新闻", false).intent()).isEqualTo(IntentType.KNOWLEDGE);
    }
}
