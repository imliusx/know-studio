package know.studio.ai.routing;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCircuitBreakerTest {

    @Test
    void transitionsFromClosedToOpenToHalfOpenAndBack() {
        MutableClock clock = new MutableClock();
        ProviderCircuitBreaker breaker = new ProviderCircuitBreaker(
                new CircuitBreakerConfig(2, Duration.ofSeconds(10)),
                clock
        );

        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.onFailure();
        assertThat(breaker.state()).isEqualTo(CircuitState.CLOSED);

        breaker.onFailure();
        assertThat(breaker.state()).isEqualTo(CircuitState.OPEN);
        assertThat(breaker.tryAcquirePermission()).isFalse();

        clock.advance(Duration.ofSeconds(10));
        assertThat(breaker.state()).isEqualTo(CircuitState.HALF_OPEN);
        assertThat(breaker.tryAcquirePermission()).isTrue();
        assertThat(breaker.tryAcquirePermission()).isFalse();

        breaker.onSuccess();
        assertThat(breaker.state()).isEqualTo(CircuitState.CLOSED);
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
