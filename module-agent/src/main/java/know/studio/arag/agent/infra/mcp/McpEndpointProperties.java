package know.studio.arag.agent.infra.mcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("arag.agent.mcp")
public class McpEndpointProperties {

    private Duration timeout = Duration.ofSeconds(15);
    private Endpoint webSearch = new Endpoint();
    private Endpoint business = new Endpoint();

    @Getter
    @Setter
    public static class Endpoint {

        private String url = "";
        private String toolName = "";
    }
}
