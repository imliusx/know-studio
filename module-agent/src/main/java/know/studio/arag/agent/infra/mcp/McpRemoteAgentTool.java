package know.studio.arag.agent.infra.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import know.studio.arag.agent.domain.AgentTool;
import know.studio.arag.agent.domain.ToolResult;
import know.studio.arag.platform.core.exception.BusinessException;

import java.time.Duration;
import java.net.URI;
import java.util.Map;

public final class McpRemoteAgentTool implements AgentTool, AutoCloseable {

    private final String name;
    private final String url;
    private final String remoteToolName;
    private final Duration timeout;
    private final java.util.function.Predicate<String> matcher;
    private volatile McpSyncClient client;

    public McpRemoteAgentTool(
            String name,
            String url,
            String remoteToolName,
            Duration timeout,
            java.util.function.Predicate<String> matcher
    ) {
        this.name = name;
        this.url = url == null ? "" : url.trim();
        this.remoteToolName = remoteToolName == null ? "" : remoteToolName.trim();
        this.timeout = timeout;
        this.matcher = matcher;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean available() {
        return !url.isBlank() && !remoteToolName.isBlank();
    }

    @Override
    public boolean supports(String question) {
        return matcher.test(question);
    }

    @Override
    public ToolResult execute(long workspaceId, String question) {
        if (!available()) {
            throw new BusinessException("MCP 工具未配置");
        }
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest(
                remoteToolName,
                Map.of("query", question, "workspaceId", workspaceId)
        ));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new BusinessException("MCP 工具执行失败");
        }
        String content = result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(java.util.stream.Collectors.joining("\n"));
        return new ToolResult(name, content, Map.of("protocol", "MCP", "remoteTool", remoteToolName));
    }

    private McpSyncClient client() {
        McpSyncClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                URI endpointUri = URI.create(url);
                String baseUri = endpointUri.getScheme() + "://" + endpointUri.getAuthority();
                String endpoint = endpointUri.getRawPath();
                if (endpointUri.getRawQuery() != null) {
                    endpoint += '?' + endpointUri.getRawQuery();
                }
                HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(baseUri)
                        .endpoint(endpoint.isBlank() ? "/mcp" : endpoint)
                        .build();
                client = McpClient.sync(transport)
                        .requestTimeout(timeout)
                        .initializationTimeout(timeout)
                        .build();
                client.initialize();
            }
            return client;
        }
    }

    @Override
    public void close() {
        McpSyncClient current = client;
        if (current != null) {
            current.closeGracefully();
        }
    }
}
