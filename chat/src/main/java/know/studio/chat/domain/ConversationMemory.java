package know.studio.chat.domain;

public record ConversationMemory(
        String compactSummary,
        String sessionSummary,
        long summarizedThroughMessageId
) {

    public static ConversationMemory empty() {
        return new ConversationMemory("", "", 0L);
    }
}
