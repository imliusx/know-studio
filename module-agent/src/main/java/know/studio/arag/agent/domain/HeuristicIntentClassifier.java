package know.studio.arag.agent.domain;

import know.studio.arag.agent.api.IntentResult;
import know.studio.arag.agent.api.IntentType;

import java.util.Locale;
import java.util.Set;

public final class HeuristicIntentClassifier implements IntentClassifier {

    private static final Set<String> TOOL_TERMS = Set.of(
            "搜索", "联网", "天气", "新闻", "订单", "客户", "库存", "search", "weather", "order"
    );
    private static final Set<String> CHAT_TERMS = Set.of(
            "你好", "谢谢", "再见", "你是谁", "介绍一下你自己", "能做什么", "你的能力",
            "聊聊天", "讲个笑话", "hello", "thanks", "who are you", "what can you do"
    );

    @Override
    public IntentResult classify(String message, boolean toolMode) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (CHAT_TERMS.stream().anyMatch(normalized::contains)) {
            return new IntentResult(IntentType.CHAT, 0.9, "");
        }
        if (message.length() < 3 || normalized.equals("这个") || normalized.equals("帮我看看")) {
            return new IntentResult(IntentType.CLARIFY, 0.35, "请补充你希望查询的对象或具体问题。");
        }
        if (toolMode && TOOL_TERMS.stream().anyMatch(normalized::contains)) {
            return new IntentResult(IntentType.TOOL, 0.85, "");
        }
        return new IntentResult(IntentType.KNOWLEDGE, 0.72, "");
    }
}
