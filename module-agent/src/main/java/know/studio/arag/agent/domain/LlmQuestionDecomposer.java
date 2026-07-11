package know.studio.arag.agent.domain;

import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.core.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmQuestionDecomposer implements QuestionDecomposer {

    private static final String SYSTEM_PROMPT = """
            将复杂问题分解为最多 3 个可独立检索的子问题，每行一个，不要编号和解释。
            如果问题无需分解，只原样输出一行。
            """;

    private final ChatModelRouter chatModelRouter;

    @Override
    @RagTraceNode("agent.decompose")
    public List<String> decompose(String question) {
        try {
            String result = chatModelRouter.stream(new ChatRequest(SYSTEM_PROMPT, question, true, null))
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
