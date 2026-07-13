package know.studio.chat.infra.ai;

import know.studio.chat.api.ConversationMessage;
import know.studio.chat.domain.ConversationSummaryPort;
import know.studio.ai.chat.ChatModelRouter;
import know.studio.ai.provider.ChatRequest;
import know.studio.ai.provider.GenerationProfile;
import know.studio.common.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmConversationSummaryAdapter implements ConversationSummaryPort {

    private final ChatModelRouter chatModelRouter;
    private final ConversationPromptCatalog promptCatalog;

    @Override
    @RagTraceNode("conversation.summarize")
    public String summarize(String previousSummary, List<ConversationMessage> messages) {
        StringBuilder messageText = new StringBuilder();
        messages.forEach(message -> messageText.append(message.role())
                .append(": ")
                .append(message.content())
                .append('\n'));
        return chatModelRouter.stream(ChatRequest.of(
                        promptCatalog.summarySystem().text(),
                        List.of(),
                        promptCatalog.summaryUser(previousSummary, messageText.toString()),
                        GenerationProfile.SUMMARY,
                        promptCatalog.summarySystem().version()
                ))
                .map(chunk -> chunk.content())
                .collectList()
                .map(parts -> String.join("", parts))
                .blockOptional()
                .orElse("");
    }
}
