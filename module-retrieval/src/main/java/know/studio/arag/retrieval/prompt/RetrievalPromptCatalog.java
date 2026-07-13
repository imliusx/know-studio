package know.studio.arag.retrieval.prompt;

import know.studio.arag.platform.ai.prompt.PromptResource;
import org.springframework.stereotype.Component;

@Component
public class RetrievalPromptCatalog {

    private final PromptResource queryPlanning = PromptResource.classpath(
            "prompts/retrieval/query-planning-system.st",
            "query-planning-v1"
    );

    public PromptResource queryPlanning() {
        return queryPlanning;
    }
}
