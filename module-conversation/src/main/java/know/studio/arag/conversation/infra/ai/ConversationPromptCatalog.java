package know.studio.arag.conversation.infra.ai;

import know.studio.arag.platform.ai.prompt.PromptResource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConversationPromptCatalog {

    private final PromptResource summarySystem = PromptResource.classpath(
            "prompts/conversation/summary-system.st",
            "conversation-summary-v1"
    );
    private final PromptResource summaryUser = PromptResource.classpath(
            "prompts/conversation/summary-user.st",
            "conversation-summary-user-v1"
    );

    public PromptResource summarySystem() {
        return summarySystem;
    }

    public String summaryUser(String previousSummary, String messages) {
        return summaryUser.render(Map.of(
                "previousSummary", previousSummary == null ? "" : previousSummary,
                "messages", messages
        ));
    }
}
