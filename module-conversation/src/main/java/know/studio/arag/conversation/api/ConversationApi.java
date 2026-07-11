package know.studio.arag.conversation.api;

public interface ConversationApi {

    SessionInfo createSession(CreateSessionCommand command);

    java.util.List<SessionInfo> listSessions(long workspaceId);

    SessionInfo renameSession(long workspaceId, long sessionId, String title);

    void deleteSession(long workspaceId, long sessionId);

    ConversationMessage appendMessage(AppendMessageCommand command);

    ConversationMessage appendMessageForOwner(AppendMessageCommand command, long ownerUserId);

    ConversationContext loadContext(long workspaceId, long sessionId, String currentQuestion);

    ConversationContext loadContextForOwner(
            long workspaceId,
            long ownerUserId,
            long sessionId,
            String currentQuestion
    );

    void summarizeIfNeeded(long workspaceId, long sessionId);
}
