package know.studio.arag.agent.domain;

public interface AgentTool {

    String name();

    int priority();

    boolean available();

    boolean supports(String question);

    ToolResult execute(long workspaceId, String question);
}
