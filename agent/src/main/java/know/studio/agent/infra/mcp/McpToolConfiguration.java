package know.studio.agent.infra.mcp;

import know.studio.agent.domain.AgentTool;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties(McpEndpointProperties.class)
public class McpToolConfiguration {

    @Bean(destroyMethod = "close")
    AgentTool webSearchMcpTool(McpEndpointProperties properties) {
        McpEndpointProperties.Endpoint endpoint = properties.getWebSearch();
        return new McpRemoteAgentTool(
                "mcp_web_search",
                endpoint.getUrl(),
                endpoint.getToolName(),
                properties.getTimeout(),
                question -> !isBusinessQuestion(question)
        );
    }

    @Bean(destroyMethod = "close")
    AgentTool businessMcpTool(McpEndpointProperties properties) {
        McpEndpointProperties.Endpoint endpoint = properties.getBusiness();
        return new McpRemoteAgentTool(
                "mcp_business_lookup",
                endpoint.getUrl(),
                endpoint.getToolName(),
                properties.getTimeout(),
                McpToolConfiguration::isBusinessQuestion
        );
    }

    @Bean
    AgentTool mockBusinessTool() {
        return new MockBusinessTool();
    }

    private static boolean isBusinessQuestion(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        return normalized.contains("订单")
                || normalized.contains("客户")
                || normalized.contains("库存")
                || normalized.contains("order");
    }
}
