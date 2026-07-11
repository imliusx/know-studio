package know.studio.arag.platform.ai.routing;

import java.time.Clock;
import java.time.Instant;

/** Thread-safe CLOSED/OPEN/HALF_OPEN circuit breaker for one provider. */
public final class ProviderCircuitBreaker {

    private final CircuitBreakerConfig config;
    private final Clock clock;

    private CircuitState state = CircuitState.CLOSED;
    private int failureCount;
    private Instant openedAt;
    private boolean halfOpenProbeInFlight;

    public ProviderCircuitBreaker(CircuitBreakerConfig config) {
        this(config, Clock.systemUTC());
    }

    ProviderCircuitBreaker(CircuitBreakerConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    public synchronized boolean tryAcquirePermission() {
        if (state == CircuitState.OPEN && openDurationElapsed()) {
            state = CircuitState.HALF_OPEN;
            halfOpenProbeInFlight = false;
        }
        if (state == CircuitState.OPEN) {
            return false;
        }
        if (state == CircuitState.HALF_OPEN) {
            if (halfOpenProbeInFlight) {
                return false;
            }
            halfOpenProbeInFlight = true;
        }
        return true;
    }

    public synchronized void onSuccess() {
        state = CircuitState.CLOSED;
        failureCount = 0;
        openedAt = null;
        halfOpenProbeInFlight = false;
    }

    public synchronized void onFailure() {
        if (state == CircuitState.HALF_OPEN) {
            open();
            return;
        }
        failureCount++;
        if (failureCount >= config.failureThreshold()) {
            open();
        }
    }

    public synchronized CircuitState state() {
        if (state == CircuitState.OPEN && openDurationElapsed()) {
            return CircuitState.HALF_OPEN;
        }
        return state;
    }

    private boolean openDurationElapsed() {
        return openedAt != null && !clock.instant().isBefore(openedAt.plus(config.openDuration()));
    }

    private void open() {
        state = CircuitState.OPEN;
        openedAt = clock.instant();
        halfOpenProbeInFlight = false;
    }
}
