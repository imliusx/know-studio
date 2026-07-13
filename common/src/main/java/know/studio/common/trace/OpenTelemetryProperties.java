package know.studio.common.trace;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("arag.observability.otel")
public class OpenTelemetryProperties {

    private boolean enabled;
    private String serviceName = "arag";
    private String endpoint = "http://localhost:4317";
    private Duration exportTimeout = Duration.ofSeconds(10);
}
