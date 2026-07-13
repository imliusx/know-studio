package know.studio.ai.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "arag.ai.rerank")
@Getter
@Setter
public class HttpRerankProperties {

    private boolean enabled;
    private URI baseUrl = URI.create("http://localhost:8081");
    private Duration timeout = Duration.ofSeconds(10);
    private int priority = 10;
}
