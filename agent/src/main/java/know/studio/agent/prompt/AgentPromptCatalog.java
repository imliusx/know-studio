package know.studio.agent.prompt;

import know.studio.ai.prompt.PromptResource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentPromptCatalog {

    private final PromptResource chat = PromptResource.classpath(
            "prompts/agent/chat-system.st",
            "chat-v1"
    );
    private final PromptResource knowledge = PromptResource.classpath(
            "prompts/agent/knowledge-system.st",
            "knowledge-v3"
    );
    private final PromptResource knowledgeUser = PromptResource.classpath(
            "prompts/agent/knowledge-user.st",
            "knowledge-user-v1"
    );
    private final PromptResource intent = PromptResource.classpath(
            "prompts/agent/intent-system.st",
            "intent-v1"
    );
    private final PromptResource decomposition = PromptResource.classpath(
            "prompts/agent/decompose-system.st",
            "decompose-v1"
    );

    public PromptResource chat() {
        return chat;
    }

    public PromptResource knowledge() {
        return knowledge;
    }

    public String knowledgeUser(
            String question,
            String evidenceLevel,
            String evidenceGuidance,
            String evidence
    ) {
        return knowledgeUser.render(Map.of(
                "question", question,
                "evidenceLevel", evidenceLevel,
                "evidenceGuidance", evidenceGuidance == null ? "" : evidenceGuidance,
                "evidence", evidence
        ));
    }

    public PromptResource intent() {
        return intent;
    }

    public PromptResource decomposition() {
        return decomposition;
    }
}
