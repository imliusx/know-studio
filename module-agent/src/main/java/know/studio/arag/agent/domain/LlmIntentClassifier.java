package know.studio.arag.agent.domain;

import know.studio.arag.agent.api.IntentResult;
import know.studio.arag.agent.api.IntentType;
import know.studio.arag.agent.prompt.AgentPromptCatalog;
import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.ai.provider.GenerationProfile;
import know.studio.arag.platform.core.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LlmIntentClassifier implements IntentClassifier {

    private static final Duration CLASSIFICATION_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern INTENT_PATTERN = Pattern.compile(
            "(KNOWLEDGE|TOOL|CHAT|CLARIFY)\\s*[,|:]\\s*(0(?:\\.\\d+)?|1(?:\\.0+)?)",
            Pattern.CASE_INSENSITIVE
    );
    private final ChatModelRouter chatModelRouter;
    private final AgentPromptCatalog promptCatalog;
    private final HeuristicIntentClassifier fallback = new HeuristicIntentClassifier();

    @Override
    @RagTraceNode("agent.intent")
    public IntentResult classify(String message, boolean toolMode) {
        IntentResult heuristic = fallback.classify(message, toolMode);
        if (heuristic.intent() == IntentType.CHAT) {
            return heuristic;
        }
        try {
            String response = chatModelRouter.stream(ChatRequest.of(
                            promptCatalog.intent().text(),
                            java.util.List.of(),
                            message,
                            GenerationProfile.CLASSIFICATION,
                            promptCatalog.intent().version()
                    ))
                    .timeout(CLASSIFICATION_TIMEOUT)
                    .map(chunk -> chunk.content())
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .blockOptional()
                    .orElse("");
            Matcher matcher = INTENT_PATTERN.matcher(response.trim());
            if (!matcher.find()) {
                return fallback.classify(message, toolMode);
            }
            IntentType intent = IntentType.valueOf(matcher.group(1).toUpperCase(Locale.ROOT));
            if (!toolMode && intent == IntentType.TOOL) {
                intent = IntentType.KNOWLEDGE;
            }
            double confidence = Double.parseDouble(matcher.group(2));
            if (confidence < 0.55) {
                return heuristic;
            }
            return new IntentResult(intent, confidence, "");
        } catch (RuntimeException exception) {
            return heuristic;
        }
    }
}
