package know.studio.chat.domain;

import know.studio.chat.api.ConversationMessage;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    void insertSession(ConversationSession session);

    Optional<ConversationSession> findOwnedSession(long userId, long sessionId);

    List<ConversationSession> findOwnedSessions(long userId);

    boolean renameOwnedSession(long userId, long sessionId, String title);

    boolean deleteOwnedSession(long userId, long sessionId);

    void insertMessage(long sessionId, ConversationMessage message);

    List<ConversationMessage> findRecentMessages(long userId, long sessionId, int limit);

    List<ConversationMessage> findMessagesForSummary(
            long userId,
            long sessionId,
            long afterMessageId
    );

    ConversationMemory findMemory(long userId, long sessionId);

    int countMessages(long userId, long sessionId);

    long sumTokens(long userId, long sessionId);

    void upsertMemory(
            long memoryId,
            long sessionId,
            String compactSummary,
            String sessionSummary,
            long summarizedThroughMessageId
    );
}
