package know.studio.arag.platform.ai.provider;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.StreamingChatModel;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiChatProviderClientTest {

    @Test
    void keepsGenericModelOutputAsAnswerTokensForReasoningRequests() {
        StreamingChatModel model = mock(StreamingChatModel.class);
        when(model.stream(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(Flux.just("answer"));
        SpringAiChatProviderClient client = new SpringAiChatProviderClient(model);

        StepVerifier.create(client.stream(new ChatRequest("system", "question", true, Map.of())))
                .expectNext(ChatChunk.token("answer"))
                .verifyComplete();
    }
}
