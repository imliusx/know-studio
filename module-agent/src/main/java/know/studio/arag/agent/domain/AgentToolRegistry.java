package know.studio.arag.agent.domain;

import know.studio.arag.platform.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentToolRegistry {

    private final List<AgentTool> tools;

    public AgentTool select(String question) {
        return tools.stream()
                .filter(AgentTool::available)
                .filter(tool -> tool.supports(question))
                .min(java.util.Comparator.comparingInt(AgentTool::priority))
                .orElseThrow(() -> new BusinessException("没有可用于当前问题的工具"));
    }
}
