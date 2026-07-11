package know.studio.arag.platform.ai.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("arag.observability.langfuse")
public class LangfuseProperties {

    private boolean enabled;
    private String baseUrl = "https://cloud.langfuse.com";
    private String publicKey = "";
    private String secretKey = "";
    private String environment = "dev";
    private Duration timeout = Duration.ofSeconds(5);

    public boolean configured() {
        return enabled && !publicKey.isBlank() && !secretKey.isBlank();
    }
}
