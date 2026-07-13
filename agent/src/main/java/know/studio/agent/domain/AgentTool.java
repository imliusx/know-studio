package know.studio.agent.domain;

public interface AgentTool {

    String name();

    int priority();

    boolean available();

    boolean supports(String question);

    ToolResult execute(long userId, String question);
}
