package know.studio.arag.conversation.api;

public interface ConversationApi {

    SessionInfo createSession(CreateSessionCommand command);

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
