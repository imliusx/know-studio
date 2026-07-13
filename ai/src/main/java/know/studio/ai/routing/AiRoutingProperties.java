package know.studio.ai.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "arag.ai.routing")
public class AiRoutingProperties {

    private int failureThreshold = 3;
    private Duration openDuration = Duration.ofSeconds(30);

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Duration getOpenDuration() {
        return openDuration;
    }

    public void setOpenDuration(Duration openDuration) {
        this.openDuration = openDuration;
    }

    public CircuitBreakerConfig toConfig() {
        return new CircuitBreakerConfig(failureThreshold, openDuration);
    }
}
