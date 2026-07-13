package know.studio.ai.embedding;

import know.studio.ai.provider.AiCapability;
import know.studio.ai.provider.AiProvider;
import know.studio.ai.routing.AiRoutingException;
import know.studio.ai.routing.ProviderCircuitBreaker;
import know.studio.ai.routing.ProviderCircuitBreakerRegistry;

import java.util.Comparator;
import java.util.List;

public final class EmbeddingClient {

    private final List<AiProvider> providers;
    private final ProviderCircuitBreakerRegistry breakers;

    public EmbeddingClient(List<AiProvider> providers, ProviderCircuitBreakerRegistry breakers) {
        this.providers = providers.stream()
                .filter(provider -> provider.supports(AiCapability.EMBEDDING))
                .sorted(Comparator.comparingInt(AiProvider::priority))
                .toList();
        this.breakers = breakers;
    }

    public List<float[]> embed(List<String> texts) {
        for (AiProvider provider : providers) {
            ProviderCircuitBreaker breaker = breakers.get(provider.id());
            if (!breaker.tryAcquirePermission()) {
                continue;
            }
            try {
                List<float[]> result = provider.embed(texts);
                breaker.onSuccess();
                return result;
            } catch (RuntimeException exception) {
                breaker.onFailure();
            }
        }
        throw new AiRoutingException("No healthy embedding provider is available");
    }
}
