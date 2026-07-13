package know.studio.chat.domain;

import know.studio.chat.api.AppendMessageCommand;
import know.studio.chat.api.ConversationMessage;
import know.studio.chat.api.MessageRole;
import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.auth.api.SystemRole;
import know.studio.common.exception.BusinessException;
import know.studio.common.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationServiceTest {

    private InMemoryRepository repository;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        IdentityApi identityApi = new StubIdentityApi();
        service = new ConversationService(
                repository,
                (previous, messages) -> "compressed:" + messages.size(),
                identityApi,
                new SnowflakeIdGenerator(0, 0)
        );
        repository.session = new ConversationSession(
                100L,
                20L,
                "test",
                false,
                false,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void appendTriggersSummaryAfterMessageThreshold() {
        for (int index = 0; index < 21; index++) {
            service.appendMessage(new AppendMessageCommand(
                    100L,
                    MessageRole.USER,
                    "message-" + index,
                    10,
                    Map.of()
            ));
        }

        assertThat(repository.memory.compactSummary()).isEqualTo("compressed:21");
        assertThat(repository.memory.summarizedThroughMessageId()).isEqualTo(messagesLastId(repository.messages));
    }

    @Test
    void anotherUsersSessionIsNotVisible() {
        repository.session = new ConversationSession(
                100L,
                99L,
                "other",
                false,
                false,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        assertThatThrownBy(() -> service.loadContext(100L, "question"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("会话不存在");
    }

    @Test
    void summaryFailureDoesNotRejectPersistedMessage() {
        ConversationService failingSummaryService = new ConversationService(
                repository,
                (previous, messages) -> {
                    throw new IllegalStateException("provider offline");
                },
                new StubIdentityApi(),
                new SnowflakeIdGenerator(0, 0)
        );

        for (int index = 0; index < 21; index++) {
            failingSummaryService.appendMessage(new AppendMessageCommand(
                    100L,
                    MessageRole.USER,
                    "message-" + index,
                    10,
                    Map.of()
            ));
        }

        assertThat(repository.messages).hasSize(21);
        assertThat(repository.memory).isEqualTo(ConversationMemory.empty());
    }

    @Test
    void renamesAndSoftDeletesOwnedSession() {
        assertThat(service.renameSession(100L, " Renamed ").title()).isEqualTo("Renamed");
        assertThat(service.listSessions()).singleElement().extracting("title").isEqualTo("Renamed");

        service.deleteSession(100L);

        assertThat(service.listSessions()).isEmpty();
        assertThatThrownBy(() -> service.loadContext(100L, "question"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("会话不存在");
    }

    private static final class StubIdentityApi implements IdentityApi {

        @Override
        public CurrentIdentity currentUser() {
            return new CurrentIdentity(20L, "user@example.com", "User", SystemRole.USER);
        }
    }

    private static final class InMemoryRepository implements ConversationRepository {

        private ConversationSession session;
        private final List<ConversationMessage> messages = new ArrayList<>();
        private ConversationMemory memory = ConversationMemory.empty();

        @Override
        public void insertSession(ConversationSession value) {
            session = value;
        }

        @Override
        public Optional<ConversationSession> findOwnedSession(long userId, long sessionId) {
            if (session != null
                    && session.userId() == userId
                    && session.id() == sessionId
                    && "ACTIVE".equals(session.status())) {
                return Optional.of(session);
            }
            return Optional.empty();
        }

        @Override
        public List<ConversationSession> findOwnedSessions(long userId) {
            return findOwnedSession(userId, session == null ? -1L : session.id())
                    .stream()
                    .toList();
        }

        @Override
        public boolean renameOwnedSession(long userId, long sessionId, String title) {
            if (findOwnedSession(userId, sessionId).isEmpty()) {
                return false;
            }
            session = new ConversationSession(
                    session.id(), session.userId(), title,
                    session.toolMode(), session.deepThinking(), session.status(),
                    session.createdAt(), Instant.now()
            );
            return true;
        }

        @Override
        public boolean deleteOwnedSession(long userId, long sessionId) {
            if (findOwnedSession(userId, sessionId).isEmpty()) {
                return false;
            }
            session = new ConversationSession(
                    session.id(), session.userId(), session.title(),
                    session.toolMode(), session.deepThinking(), "DELETED",
                    session.createdAt(), Instant.now()
            );
            return true;
        }

        @Override
        public void insertMessage(long sessionId, ConversationMessage message) {
            messages.add(message);
        }

        @Override
        public List<ConversationMessage> findRecentMessages(
                long userId,
                long sessionId,
                int limit
        ) {
            return messages.stream().skip(Math.max(0, messages.size() - limit)).toList();
        }

        @Override
        public List<ConversationMessage> findMessagesForSummary(
                long userId,
                long sessionId,
                long afterMessageId
        ) {
            return messages.stream().filter(message -> message.id() > afterMessageId).toList();
        }

        @Override
        public ConversationMemory findMemory(long userId, long sessionId) {
            return memory;
        }

        @Override
        public int countMessages(long userId, long sessionId) {
            return messages.size();
        }

        @Override
        public long sumTokens(long userId, long sessionId) {
            return messages.stream().mapToLong(ConversationMessage::tokens).sum();
        }

        @Override
        public void upsertMemory(
                long memoryId,
                long sessionId,
                String compactSummary,
                String sessionSummary,
                long summarizedThroughMessageId
        ) {
            memory = new ConversationMemory(compactSummary, sessionSummary, summarizedThroughMessageId);
        }
    }

    private static long messagesLastId(List<ConversationMessage> messages) {
        return messages.getLast().id();
    }
}
