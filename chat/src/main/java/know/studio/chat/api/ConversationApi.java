package know.studio.chat.api;

public interface ConversationApi {

    SessionInfo createSession(CreateSessionCommand command);

    java.util.List<SessionInfo> listSessions();

    SessionInfo renameSession(long sessionId, String title);

    void deleteSession(long sessionId);

    ConversationMessage appendMessage(AppendMessageCommand command);

    ConversationMessage appendMessageForOwner(AppendMessageCommand command, long ownerUserId);

    ConversationContext loadContext(long sessionId, String currentQuestion);

    ConversationContext loadContextForOwner(
            long ownerUserId,
            long sessionId,
            String currentQuestion
    );

    void summarizeIfNeeded(long sessionId);
}
