package know.studio.ai.rerank;

import know.studio.ai.provider.AiCapability;
import know.studio.ai.provider.AiProvider;
import know.studio.ai.provider.RerankDocument;
import know.studio.ai.provider.RerankResult;
import know.studio.ai.routing.AiRoutingException;
import know.studio.ai.routing.ProviderCircuitBreaker;
import know.studio.ai.routing.ProviderCircuitBreakerRegistry;

import java.util.Comparator;
import java.util.List;

public final class RerankClient {

    private final List<AiProvider> providers;
    private final ProviderCircuitBreakerRegistry breakers;

    public RerankClient(List<AiProvider> providers, ProviderCircuitBreakerRegistry breakers) {
        this.providers = providers.stream()
                .filter(provider -> provider.supports(AiCapability.RERANK))
                .sorted(Comparator.comparingInt(AiProvider::priority))
                .toList();
        this.breakers = breakers;
    }

    public List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        for (AiProvider provider : providers) {
            ProviderCircuitBreaker breaker = breakers.get(provider.id());
            if (!breaker.tryAcquirePermission()) {
                continue;
            }
            try {
                List<RerankResult> result = provider.rerank(query, documents);
                breaker.onSuccess();
                return result;
            } catch (RuntimeException exception) {
                breaker.onFailure();
            }
        }
        throw new AiRoutingException("No healthy rerank provider is available");
    }
}
