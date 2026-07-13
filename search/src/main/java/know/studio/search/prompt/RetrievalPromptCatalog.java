package know.studio.search.prompt;

import know.studio.ai.prompt.PromptResource;
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
