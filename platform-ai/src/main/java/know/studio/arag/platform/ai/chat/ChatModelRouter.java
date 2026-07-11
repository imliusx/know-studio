package know.studio.arag.platform.ai.chat;

import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.ai.routing.AiRoutingException;
import know.studio.arag.platform.ai.routing.ProviderCircuitBreaker;
import know.studio.arag.platform.ai.routing.ProviderCircuitBreakerRegistry;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/** Routes streaming chat with circuit breaking and first-chunk failover. */
public final class ChatModelRouter {

    private final List<AiProvider> providers;
    private final ProviderCircuitBreakerRegistry breakers;

    public ChatModelRouter(List<AiProvider> providers, ProviderCircuitBreakerRegistry breakers) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(AiProvider::priority))
                .toList();
        this.breakers = breakers;
    }

    public Flux<ChatChunk> stream(ChatRequest request) {
        List<AiProvider> candidates = providers.stream()
                .filter(provider -> supportsRequest(provider, request))
                .toList();
        return route(candidates, 0, request);
    }

    private static boolean supportsRequest(AiProvider provider, ChatRequest request) {
        if (!request.reasoning()) {
            return provider.supports(AiCapability.CHAT);
        }
        return provider.supports(AiCapability.REASONING) || provider.supports(AiCapability.CHAT);
    }

    private Flux<ChatChunk> route(List<AiProvider> candidates, int index, ChatRequest request) {
        if (index >= candidates.size()) {
            return Flux.error(new AiRoutingException("No healthy AI provider is available"));
        }

        AiProvider provider = candidates.get(index);
        ProviderCircuitBreaker breaker = breakers.get(provider.id());
        if (!breaker.tryAcquirePermission()) {
            return route(candidates, index + 1, request);
        }

        return Flux.defer(() -> provider.streamChat(request))
                .switchOnFirst((signal, stream) -> {
                    if (signal.hasValue()) {
                        breaker.onSuccess();
                        return stream.doOnError(ignored -> breaker.onFailure());
                    }
                    breaker.onFailure();
                    return route(candidates, index + 1, request);
                });
    }
}
