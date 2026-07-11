package know.studio.arag.retrieval.domain;

import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.AiCapability;
import know.studio.arag.platform.ai.provider.AiProvider;
import know.studio.arag.platform.ai.provider.ChatChunk;
import know.studio.arag.platform.ai.provider.ChatRequest;
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
    private static final String SYSTEM_PROMPT = """
            You plan retrieval queries for a knowledge base. Return at most three concise search queries,
            one query per line, without numbering or explanation. Preserve important entities and terms.
            """;

    private final ChatModelRouter chatModelRouter;
    private final List<AiProvider> providers;
    private final HeuristicQueryPlanner fallback;

    @Override
    public List<String> plan(String question) {
        if (providers.stream().noneMatch(provider -> provider.supports(AiCapability.CHAT))) {
            return fallback.plan(question);
        }
        try {
            String output = chatModelRouter.stream(ChatRequest.chat(SYSTEM_PROMPT, question))
                    .filter(chunk -> chunk.type() == ChatChunk.Type.TOKEN)
                    .map(ChatChunk::content)
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .block(PLAN_TIMEOUT);
            List<String> queries = parse(output);
            return queries.isEmpty() ? fallback.plan(question) : queries;
        } catch (RuntimeException exception) {
            log.warn("LLM query planning failed, using deterministic fallback", exception);
            return fallback.plan(question);
        }
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
