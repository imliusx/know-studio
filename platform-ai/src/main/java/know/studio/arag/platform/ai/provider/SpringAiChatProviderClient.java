package know.studio.arag.platform.ai.provider;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Adapts any Spring AI streaming chat model to the provider-neutral SPI. */
public final class SpringAiChatProviderClient implements ChatProviderClient {

    private final StreamingChatModel model;

    public SpringAiChatProviderClient(StreamingChatModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        request.history().stream().map(SpringAiChatProviderClient::toSpringMessage).forEach(messages::add);
        messages.add(new UserMessage(request.userPrompt()));

        DefaultChatOptions options = new DefaultChatOptions();
        options.setTemperature(request.profile().temperature());
        options.setMaxTokens(request.profile().maxTokens());
        Prompt prompt = new Prompt(messages, options);
        return model.stream(prompt)
                .mapNotNull(response -> response.getResult() == null ? null : response.getResult().getOutput())
                .mapNotNull(output -> output.getText())
                .filter(text -> !text.isEmpty())
                .map(ChatChunk::token);
    }

    private static Message toSpringMessage(ChatMessage message) {
        return switch (message.role()) {
            case SYSTEM -> new SystemMessage(message.content());
            case USER -> new UserMessage(message.content());
            case ASSISTANT -> new AssistantMessage(message.content());
        };
    }
}
