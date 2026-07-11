package know.studio.arag.conversation.domain;

import know.studio.arag.conversation.api.ConversationMessage;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    void insertSession(ConversationSession session);

    Optional<ConversationSession> findOwnedSession(long workspaceId, long userId, long sessionId);

    List<ConversationSession> findOwnedSessions(long workspaceId, long userId);

    boolean renameOwnedSession(long workspaceId, long userId, long sessionId, String title);

    boolean deleteOwnedSession(long workspaceId, long userId, long sessionId);

    void insertMessage(long sessionId, ConversationMessage message);

    List<ConversationMessage> findRecentMessages(long workspaceId, long userId, long sessionId, int limit);

    List<ConversationMessage> findMessagesForSummary(
            long workspaceId,
            long userId,
            long sessionId,
            long afterMessageId
    );

    ConversationMemory findMemory(long workspaceId, long userId, long sessionId);

    int countMessages(long workspaceId, long userId, long sessionId);

    long sumTokens(long workspaceId, long userId, long sessionId);

    void upsertMemory(
            long memoryId,
            long sessionId,
            String compactSummary,
            String sessionSummary,
            long summarizedThroughMessageId
    );
}
