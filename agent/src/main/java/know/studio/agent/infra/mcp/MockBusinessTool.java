package know.studio.agent.infra.mcp;

import know.studio.agent.domain.AgentTool;
import know.studio.agent.domain.ToolResult;
import know.studio.common.trace.RagTraceNode;

import java.util.Locale;
import java.util.Map;

public class MockBusinessTool implements AgentTool {

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
    @RagTraceNode("agent.tool.mock-business")
    public ToolResult execute(long userId, String question) {
        String content = "模拟业务系统返回：user=" + userId
                + "，查询条件=" + question + "，状态=正常";
        return new ToolResult(name(), content, Map.of("mock", true, "userId", userId));
    }
}
