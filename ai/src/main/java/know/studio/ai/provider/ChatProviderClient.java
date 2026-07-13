package know.studio.ai.provider;

import reactor.core.publisher.Flux;

@FunctionalInterface
public interface ChatProviderClient {

    Flux<ChatChunk> stream(ChatRequest request);
}
