package know.studio.ai.provider;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiChatProviderClientTest {

    @Test
    void keepsGenericModelOutputAsAnswerTokensForReasoningRequests() {
        StreamingChatModel model = mock(StreamingChatModel.class);
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(new ChatResponse(
                List.of(new Generation(new AssistantMessage("answer")))
        )));
        SpringAiChatProviderClient client = new SpringAiChatProviderClient(model);

        ChatRequest request = new ChatRequest(
                "system",
                List.of(
                        ChatMessage.system("summary"),
                        ChatMessage.user("earlier question"),
                        ChatMessage.assistant("earlier answer")
                ),
                "question",
                true,
                GenerationProfile.CHAT,
                "chat-v1",
                Map.of()
        );
        StepVerifier.create(client.stream(request))
                .expectNext(ChatChunk.token("answer"))
                .verifyComplete();

        verify(model).stream(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertThat(prompt.getInstructions()).extracting(message -> message.getMessageType()).containsExactly(
                MessageType.SYSTEM,
                MessageType.SYSTEM,
                MessageType.USER,
                MessageType.ASSISTANT,
                MessageType.USER
        );
        assertThat(prompt.getInstructions()).extracting(message -> message.getText()).containsExactly(
                "system",
                "summary",
                "earlier question",
                "earlier answer",
                "question"
        );
        assertThat(prompt.getOptions().getTemperature()).isEqualTo(GenerationProfile.CHAT.temperature());
        assertThat(prompt.getOptions().getMaxTokens()).isEqualTo(GenerationProfile.CHAT.maxTokens());
    }
}
