package know.studio.arag.platform.ai.provider;

import reactor.core.publisher.Flux;

import java.util.Set;

public final class DeepSeekProvider implements AiProvider {

    private final ChatProviderClient client;
    private final int priority;

    public DeepSeekProvider(ChatProviderClient client, int priority) {
        this.client = client;
        this.priority = priority;
    }

    @Override
    public String id() {
        return "deepseek";
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Set<AiCapability> capabilities() {
        return Set.of(AiCapability.CHAT, AiCapability.REASONING);
    }

    @Override
    public Flux<ChatChunk> streamChat(ChatRequest request) {
        return client.stream(request);
    }
}
