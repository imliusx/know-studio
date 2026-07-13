package know.studio.ai.routing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProviderCircuitBreakerRegistry {

    private final CircuitBreakerConfig config;
    private final ConcurrentMap<String, ProviderCircuitBreaker> breakers = new ConcurrentHashMap<>();

    public ProviderCircuitBreakerRegistry(CircuitBreakerConfig config) {
        this.config = config;
    }

    public ProviderCircuitBreaker get(String providerId) {
        return breakers.computeIfAbsent(providerId, ignored -> new ProviderCircuitBreaker(config));
    }
}
