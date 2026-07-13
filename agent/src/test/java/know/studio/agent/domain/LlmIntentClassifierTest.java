package know.studio.agent.domain;

import know.studio.agent.api.IntentResult;
import know.studio.agent.api.IntentType;
import know.studio.agent.prompt.AgentPromptCatalog;
import know.studio.ai.chat.ChatModelRouter;
import know.studio.ai.provider.ChatChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmIntentClassifierTest {

    @Mock
    private ChatModelRouter chatModelRouter;

    @Test
    void routesShortGreetingToChatWithoutCallingModel() {
        IntentResult result = classifier().classify("你好", false);

        assertThat(result.intent()).isEqualTo(IntentType.CHAT);
        verify(chatModelRouter, never()).stream(any());
    }

    @Test
    void routesAssistantIntroductionToChatWithoutCallingModel() {
        IntentResult result = classifier().classify("请介绍一下你自己，你能做什么？", false);

        assertThat(result.intent()).isEqualTo(IntentType.CHAT);
        verify(chatModelRouter, never()).stream(any());
    }

    @Test
    void fallsBackToHeuristicWhenModelConfidenceIsLow() {
        when(chatModelRouter.stream(any())).thenReturn(Flux.just(ChatChunk.token("KNOWLEDGE,0.40")));

        IntentResult result = classifier().classify("介绍一下项目架构", false);

        assertThat(result.intent()).isEqualTo(IntentType.KNOWLEDGE);
        assertThat(result.confidence()).isEqualTo(0.72);
    }

    private LlmIntentClassifier classifier() {
        return new LlmIntentClassifier(chatModelRouter, new AgentPromptCatalog());
    }
}
