package know.studio.arag.conversation.domain;

import know.studio.arag.conversation.api.AppendMessageCommand;
import know.studio.arag.conversation.api.ConversationApi;
import know.studio.arag.conversation.api.ConversationContext;
import know.studio.arag.conversation.api.ConversationMessage;
import know.studio.arag.conversation.api.CreateSessionCommand;
import know.studio.arag.conversation.api.SessionInfo;
import know.studio.arag.identity.api.CurrentIdentity;
import know.studio.arag.identity.api.IdentityApi;
import know.studio.arag.platform.core.exception.BusinessException;
import know.studio.arag.platform.core.exception.ErrorCode;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService implements ConversationApi {

    static final int RECENT_MESSAGE_LIMIT = 12;
    static final int SUMMARY_MESSAGE_THRESHOLD = 20;
    static final long SUMMARY_TOKEN_THRESHOLD = 8_000;
    static final int INCREMENTAL_SUMMARY_MESSAGE_THRESHOLD = 10;
    static final long INCREMENTAL_SUMMARY_TOKEN_THRESHOLD = 4_000;

    private final ConversationRepository repository;
    private final ConversationSummaryPort summaryPort;
    private final IdentityApi identityApi;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public SessionInfo createSession(CreateSessionCommand command) {
        identityApi.requireWorkspaceReadable(command.workspaceId());
        CurrentIdentity identity = identityApi.currentUser();
        String title = normalizeTitle(command.title());
        Instant now = Instant.now();
        ConversationSession session = new ConversationSession(
                idGenerator.nextId(),
                command.workspaceId(),
                identity.userId(),
                title,
                command.toolMode(),
                command.deepThinking(),
                "ACTIVE",
                now,
                now
        );
        repository.insertSession(session);
        return toInfo(session);
    }

    @Override
    @Transactional
    public ConversationMessage appendMessage(AppendMessageCommand command) {
        CurrentIdentity identity = requireOwnedSession(command.workspaceId(), command.sessionId());
        return appendMessageForOwner(command, identity.userId());
    }

    @Override
    @Transactional
    public ConversationMessage appendMessageForOwner(AppendMessageCommand command, long ownerUserId) {
        requireOwnedSession(command.workspaceId(), ownerUserId, command.sessionId());
        if (command.content() == null || command.content().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (command.role() == null || command.tokens() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息角色或 token 数不合法");
        }
        ConversationMessage message = new ConversationMessage(
                idGenerator.nextId(),
                command.role(),
                command.content().trim(),
                command.tokens(),
                command.metadata(),
                Instant.now()
        );
        repository.insertMessage(command.sessionId(), message);
        summarizeIfNeeded(command.workspaceId(), ownerUserId, command.sessionId());
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationContext loadContext(long workspaceId, long sessionId, String currentQuestion) {
        CurrentIdentity identity = requireOwnedSession(workspaceId, sessionId);
        return loadContextForOwner(workspaceId, identity.userId(), sessionId, currentQuestion);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationContext loadContextForOwner(
            long workspaceId,
            long ownerUserId,
            long sessionId,
            String currentQuestion
    ) {
        requireOwnedSession(workspaceId, ownerUserId, sessionId);
        ConversationMemory memory = repository.findMemory(workspaceId, ownerUserId, sessionId);
        List<ConversationMessage> recent = repository.findRecentMessages(
                workspaceId,
                ownerUserId,
                sessionId,
                RECENT_MESSAGE_LIMIT
        );
        return new ConversationContext(
                sessionId,
                memory.compactSummary(),
                memory.sessionSummary(),
                recent,
                currentQuestion == null ? "" : currentQuestion.trim()
        );
    }

    @Override
    @Transactional
    public void summarizeIfNeeded(long workspaceId, long sessionId) {
        CurrentIdentity identity = requireOwnedSession(workspaceId, sessionId);
        summarizeIfNeeded(workspaceId, identity.userId(), sessionId);
    }

    private void summarizeIfNeeded(long workspaceId, long userId, long sessionId) {
        int messageCount = repository.countMessages(workspaceId, userId, sessionId);
        long tokenCount = repository.sumTokens(workspaceId, userId, sessionId);
        ConversationMemory current = repository.findMemory(workspaceId, userId, sessionId);
        if (current.summarizedThroughMessageId() == 0
                && messageCount <= SUMMARY_MESSAGE_THRESHOLD
                && tokenCount <= SUMMARY_TOKEN_THRESHOLD) {
            return;
        }
        List<ConversationMessage> messages = repository.findMessagesForSummary(
                workspaceId,
                userId,
                sessionId,
                current.summarizedThroughMessageId()
        );
        long incrementalTokens = messages.stream().mapToLong(ConversationMessage::tokens).sum();
        if (current.summarizedThroughMessageId() > 0
                && messages.size() < INCREMENTAL_SUMMARY_MESSAGE_THRESHOLD
                && incrementalTokens < INCREMENTAL_SUMMARY_TOKEN_THRESHOLD) {
            return;
        }
        if (messages.isEmpty()) {
            return;
        }
        String summary;
        try {
            summary = summaryPort.summarize(current.compactSummary(), messages);
        } catch (RuntimeException exception) {
            log.warn("Conversation summary failed sessionId={} messageCount={}", sessionId, messages.size());
            return;
        }
        if (summary == null || summary.isBlank()) {
            return;
        }
        long summarizedThrough = messages.getLast().id();
        repository.upsertMemory(
                idGenerator.nextId(),
                sessionId,
                summary.trim(),
                summary.trim(),
                summarizedThrough
        );
    }

    private CurrentIdentity requireOwnedSession(long workspaceId, long sessionId) {
        identityApi.requireWorkspaceReadable(workspaceId);
        CurrentIdentity identity = identityApi.currentUser();
        repository.findOwnedSession(workspaceId, identity.userId(), sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        return identity;
    }

    private void requireOwnedSession(long workspaceId, long ownerUserId, long sessionId) {
        repository.findOwnedSession(workspaceId, ownerUserId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新会话";
        }
        String normalized = title.trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }

    private static SessionInfo toInfo(ConversationSession session) {
        return new SessionInfo(
                session.id(),
                session.workspaceId(),
                session.userId(),
                session.title(),
                session.toolMode(),
                session.deepThinking(),
                session.status(),
                session.createdAt(),
                session.updatedAt()
        );
    }
}
