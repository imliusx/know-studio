package know.studio.ai.provider;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

/**
 * Provider SPI used by all model routing. Lower priority values are preferred.
 */
public interface AiProvider {

    String id();

    int priority();

    Set<AiCapability> capabilities();

    default Flux<ChatChunk> streamChat(ChatRequest request) {
        return Flux.error(unsupported(AiCapability.CHAT));
    }

    default List<float[]> embed(List<String> texts) {
        throw unsupported(AiCapability.EMBEDDING);
    }

    default List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        throw unsupported(AiCapability.RERANK);
    }

    default boolean supports(AiCapability capability) {
        return capabilities().contains(capability);
    }

    private UnsupportedOperationException unsupported(AiCapability capability) {
        return new UnsupportedOperationException(id() + " does not support " + capability);
    }
}
