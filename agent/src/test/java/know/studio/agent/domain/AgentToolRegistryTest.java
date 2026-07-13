package know.studio.agent.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolRegistryTest {

    @Test
    void returnsEmptyWhenNoAvailableToolSupportsTheQuestion() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool(false, true)));

        assertThat(registry.find("knowledge question")).isEmpty();
    }

    private static AgentTool tool(boolean available, boolean supports) {
        return new AgentTool() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public int priority() {
                return 1;
            }

            @Override
            public boolean available() {
                return available;
            }

            @Override
            public boolean supports(String question) {
                return supports;
            }

            @Override
            public ToolResult execute(long userId, String question) {
                return new ToolResult(name(), "result", null);
            }
        };
    }
}
