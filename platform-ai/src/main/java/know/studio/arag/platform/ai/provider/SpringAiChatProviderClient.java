package know.studio.arag.platform.ai.provider;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.StreamingChatModel;
import reactor.core.publisher.Flux;

import java.util.Objects;

/** Adapts any Spring AI streaming chat model to the provider-neutral SPI. */
public final class SpringAiChatProviderClient implements ChatProviderClient {

    private final StreamingChatModel model;

    public SpringAiChatProviderClient(StreamingChatModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        SystemMessage system = new SystemMessage(request.systemPrompt() == null ? "" : request.systemPrompt());
        UserMessage user = new UserMessage(request.userPrompt());
        return model.stream(system, user)
                .filter(text -> text != null && !text.isEmpty())
                .map(request.reasoning() ? ChatChunk::thinking : ChatChunk::token);
    }
}
