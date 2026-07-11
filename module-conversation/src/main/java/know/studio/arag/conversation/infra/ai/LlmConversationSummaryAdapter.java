package know.studio.arag.conversation.infra.ai;

import know.studio.arag.conversation.api.ConversationMessage;
import know.studio.arag.conversation.domain.ConversationSummaryPort;
import know.studio.arag.platform.ai.chat.ChatModelRouter;
import know.studio.arag.platform.ai.provider.ChatRequest;
import know.studio.arag.platform.core.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmConversationSummaryAdapter implements ConversationSummaryPort {

    private static final String SYSTEM_PROMPT = """
            你是会话记忆压缩器。保留用户目标、已确认事实、关键约束、工具结果和未解决问题。
            删除寒暄、重复内容和无关细节。只输出紧凑摘要，不要解释过程。
            """;

    private final ChatModelRouter chatModelRouter;

    @Override
    @RagTraceNode("conversation.summarize")
    public String summarize(String previousSummary, List<ConversationMessage> messages) {
        StringBuilder prompt = new StringBuilder();
        if (previousSummary != null && !previousSummary.isBlank()) {
            prompt.append("已有摘要：\n").append(previousSummary).append("\n\n");
        }
        prompt.append("新增会话：\n");
        messages.forEach(message -> prompt.append(message.role())
                .append(": ")
                .append(message.content())
                .append('\n'));
        return chatModelRouter.stream(ChatRequest.chat(SYSTEM_PROMPT, prompt.toString()))
                .map(chunk -> chunk.content())
                .collectList()
                .map(parts -> String.join("", parts))
                .blockOptional()
                .orElse("");
    }
}
