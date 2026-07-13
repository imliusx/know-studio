package know.studio.arag.agent.domain;

import know.studio.arag.agent.prompt.AgentPromptCatalog;
import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.ai.provider.GenerationProfile;
import know.studio.arag.platform.core.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmQuestionDecomposer implements QuestionDecomposer {

    private final ChatModelRouter chatModelRouter;
    private final AgentPromptCatalog promptCatalog;

    @Override
    @RagTraceNode("agent.decompose")
    public List<String> decompose(String question) {
        try {
            String result = chatModelRouter.stream(new ChatRequest(
                            promptCatalog.decomposition().text(),
                            List.of(),
                            question,
                            true,
                            GenerationProfile.PLANNING,
                            promptCatalog.decomposition().version(),
                            java.util.Map.of()
                    ))
                    .timeout(Duration.ofSeconds(10))
                    .map(chunk -> chunk.content())
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .blockOptional()
                    .orElse("");
            List<String> questions = Arrays.stream(result.split("\\R"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .limit(3)
                    .toList();
            return questions.isEmpty() ? List.of(question) : questions;
        } catch (RuntimeException exception) {
            return List.of(question);
        }
    }
}
