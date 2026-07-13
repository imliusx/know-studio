package know.studio.ai.routing;

import java.time.Duration;

public record CircuitBreakerConfig(int failureThreshold, Duration openDuration) {

    public CircuitBreakerConfig {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be positive");
        }
        if (openDuration == null || openDuration.isNegative() || openDuration.isZero()) {
            throw new IllegalArgumentException("openDuration must be positive");
        }
    }

    public static CircuitBreakerConfig defaults() {
        return new CircuitBreakerConfig(3, Duration.ofSeconds(30));
    }
}
