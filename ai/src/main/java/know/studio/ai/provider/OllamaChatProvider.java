package know.studio.ai.provider;

import reactor.core.publisher.Flux;

import java.util.Set;

public final class OllamaChatProvider implements AiProvider {

    private final ChatProviderClient client;
    private final int priority;

    public OllamaChatProvider(ChatProviderClient client, int priority) {
        this.client = client;
        this.priority = priority;
    }

    @Override
    public String id() {
        return "ollama-chat";
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Set<AiCapability> capabilities() {
        return Set.of(AiCapability.CHAT);
    }

    @Override
    public Flux<ChatChunk> streamChat(ChatRequest request) {
        return client.stream(request);
    }
}
