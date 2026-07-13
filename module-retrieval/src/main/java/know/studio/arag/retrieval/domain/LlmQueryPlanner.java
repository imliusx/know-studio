package know.studio.arag.retrieval.domain;

import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.ai.provider.GenerationProfile;
import know.studio.arag.retrieval.prompt.RetrievalPromptCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class LlmQueryPlanner implements QueryPlanner {

    private static final int MAX_QUERIES = 3;
    private static final Duration PLAN_TIMEOUT = Duration.ofSeconds(8);
    private final ChatModelRouter chatModelRouter;
    private final List<AiProvider> providers;
    private final HeuristicQueryPlanner fallback;
    private final RetrievalPromptCatalog promptCatalog;

    @Override
    public List<String> plan(String question) {
        List<String> deterministicQueries = fallback.plan(question);
        if (deterministicQueries.size() <= 1) {
            return deterministicQueries;
        }
        if (providers.stream().noneMatch(provider -> provider.supports(AiCapability.CHAT))) {
            return deterministicQueries;
        }
        try {
            String output = chatModelRouter.stream(ChatRequest.of(
                            promptCatalog.queryPlanning().text(),
                            List.of(),
                            question,
                            GenerationProfile.PLANNING,
                            promptCatalog.queryPlanning().version()
                    ))
                    .filter(chunk -> chunk.type() == ChatChunk.Type.TOKEN)
                    .map(ChatChunk::content)
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .block(PLAN_TIMEOUT);
            return combine(question, parse(output));
        } catch (RuntimeException exception) {
            log.warn("LLM query planning failed, using deterministic fallback", exception);
            return deterministicQueries;
        }
    }

    static List<String> combine(String question, List<String> generatedQueries) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String normalizedQuestion = question == null
                ? ""
                : question.replaceAll("[?？!！]+$", "").trim();
        if (!normalizedQuestion.isBlank()) {
            queries.add(normalizedQuestion);
        }
        queries.addAll(generatedQueries);
        return queries.stream().filter(query -> !query.isBlank()).limit(MAX_QUERIES).toList();
    }

    static List<String> parse(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        for (String line : output.split("\\R")) {
            String query = line.replaceFirst("^\\s*(?:[-*]|\\d+[.)])\\s*", "")
                    .replaceAll("^[\"']|[\"']$", "")
                    .trim();
            if (!query.isBlank()) {
                queries.add(query);
            }
            if (queries.size() >= MAX_QUERIES) {
                break;
            }
        }
        return List.copyOf(queries);
    }
}
