package know.studio.arag.agent.infra.mcp;

import know.studio.arag.agent.domain.AgentTool;
import know.studio.arag.agent.domain.ToolResult;

import java.util.Locale;
import java.util.Map;

public final class MockBusinessTool implements AgentTool {

    @Override
    public String name() {
        return "mock_business_lookup";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean supports(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        return normalized.contains("订单")
                || normalized.contains("客户")
                || normalized.contains("库存")
                || normalized.contains("order");
    }

    @Override
    public ToolResult execute(long workspaceId, String question) {
        String content = "模拟业务系统返回：workspace=" + workspaceId
                + "，查询条件=" + question + "，状态=正常";
        return new ToolResult(name(), content, Map.of("mock", true, "workspaceId", workspaceId));
    }
}
