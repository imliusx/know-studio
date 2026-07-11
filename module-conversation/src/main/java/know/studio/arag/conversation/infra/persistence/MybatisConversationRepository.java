package know.studio.arag.conversation.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import know.studio.arag.conversation.api.ConversationMessage;
import know.studio.arag.conversation.api.MessageRole;
import know.studio.arag.conversation.domain.ConversationMemory;
import know.studio.arag.conversation.domain.ConversationRepository;
import know.studio.arag.conversation.domain.ConversationSession;
import know.studio.arag.conversation.infra.persistence.entity.MessageEntity;
import know.studio.arag.conversation.infra.persistence.entity.SessionEntity;
import know.studio.arag.conversation.infra.persistence.entity.SessionMemoryEntity;
import know.studio.arag.conversation.infra.persistence.mapper.MessageMapper;
import know.studio.arag.conversation.infra.persistence.mapper.SessionMapper;
import know.studio.arag.conversation.infra.persistence.mapper.SessionMemoryMapper;
import know.studio.arag.platform.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisConversationRepository implements ConversationRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() { };

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final SessionMemoryMapper memoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void insertSession(ConversationSession session) {
        sessionMapper.insert(toEntity(session));
    }

    @Override
    public Optional<ConversationSession> findOwnedSession(long workspaceId, long userId, long sessionId) {
        SessionEntity entity = sessionMapper.selectOne(Wrappers.<SessionEntity>lambdaQuery()
                .eq(SessionEntity::getWorkspaceId, workspaceId)
                .eq(SessionEntity::getUserId, userId)
                .eq(SessionEntity::getId, sessionId)
                .eq(SessionEntity::getStatus, "ACTIVE"));
        return Optional.ofNullable(entity).map(MybatisConversationRepository::toDomain);
    }

    @Override
    public List<ConversationSession> findOwnedSessions(long workspaceId, long userId) {
        return sessionMapper.selectList(Wrappers.<SessionEntity>lambdaQuery()
                        .eq(SessionEntity::getWorkspaceId, workspaceId)
                        .eq(SessionEntity::getUserId, userId)
                        .eq(SessionEntity::getStatus, "ACTIVE")
                        .orderByDesc(SessionEntity::getUpdatedAt))
                .stream()
                .map(MybatisConversationRepository::toDomain)
                .toList();
    }

    @Override
    public boolean renameOwnedSession(long workspaceId, long userId, long sessionId, String title) {
        return sessionMapper.update(Wrappers.<SessionEntity>lambdaUpdate()
                .eq(SessionEntity::getWorkspaceId, workspaceId)
                .eq(SessionEntity::getUserId, userId)
                .eq(SessionEntity::getId, sessionId)
                .eq(SessionEntity::getStatus, "ACTIVE")
                .set(SessionEntity::getTitle, title)
                .setSql("updated_at = CURRENT_TIMESTAMP")) == 1;
    }

    @Override
    public boolean deleteOwnedSession(long workspaceId, long userId, long sessionId) {
        return sessionMapper.update(Wrappers.<SessionEntity>lambdaUpdate()
                .eq(SessionEntity::getWorkspaceId, workspaceId)
                .eq(SessionEntity::getUserId, userId)
                .eq(SessionEntity::getId, sessionId)
                .eq(SessionEntity::getStatus, "ACTIVE")
                .set(SessionEntity::getStatus, "DELETED")
                .setSql("updated_at = CURRENT_TIMESTAMP")) == 1;
    }

    @Override
    public void insertMessage(long sessionId, ConversationMessage message) {
        MessageEntity entity = new MessageEntity();
        entity.setId(message.id());
        entity.setSessionId(sessionId);
        entity.setRole(message.role().name());
        entity.setContent(message.content());
        entity.setTokens(message.tokens());
        entity.setMetadata(writeMetadata(message.metadata()));
        entity.setCreatedAt(message.createdAt());
        messageMapper.insertJson(entity);
    }

    @Override
    public List<ConversationMessage> findRecentMessages(long workspaceId, long userId, long sessionId, int limit) {
        List<ConversationMessage> messages = messageMapper.selectRecentOwned(
                        workspaceId,
                        userId,
                        sessionId,
                        limit
                ).stream()
                .map(this::toMessage)
                .toList();
        List<ConversationMessage> ordered = new java.util.ArrayList<>(messages);
        Collections.reverse(ordered);
        return List.copyOf(ordered);
    }

    @Override
    public List<ConversationMessage> findMessagesForSummary(
            long workspaceId,
            long userId,
            long sessionId,
            long afterMessageId
    ) {
        return messageMapper.selectAllOwned(workspaceId, userId, sessionId, afterMessageId).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    public ConversationMemory findMemory(long workspaceId, long userId, long sessionId) {
        SessionMemoryEntity entity = memoryMapper.selectOwned(workspaceId, userId, sessionId);
        if (entity == null) {
            return ConversationMemory.empty();
        }
        return new ConversationMemory(
                entity.getCompactSummary() == null ? "" : entity.getCompactSummary(),
                entity.getSessionSummary() == null ? "" : entity.getSessionSummary(),
                entity.getSummarizedThroughMessageId() == null ? 0L : entity.getSummarizedThroughMessageId()
        );
    }

    @Override
    public int countMessages(long workspaceId, long userId, long sessionId) {
        return messageMapper.countOwned(workspaceId, userId, sessionId);
    }

    @Override
    public long sumTokens(long workspaceId, long userId, long sessionId) {
        return messageMapper.sumTokensOwned(workspaceId, userId, sessionId);
    }

    @Override
    public void upsertMemory(
            long memoryId,
            long sessionId,
            String compactSummary,
            String sessionSummary,
            long summarizedThroughMessageId
    ) {
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setId(memoryId);
        entity.setSessionId(sessionId);
        entity.setCompactSummary(compactSummary);
        entity.setSessionSummary(sessionSummary);
        entity.setSummarizedThroughMessageId(summarizedThroughMessageId);
        memoryMapper.upsert(entity);
    }

    private static SessionEntity toEntity(ConversationSession session) {
        SessionEntity entity = new SessionEntity();
        entity.setId(session.id());
        entity.setWorkspaceId(session.workspaceId());
        entity.setUserId(session.userId());
        entity.setTitle(session.title());
        entity.setToolMode(session.toolMode());
        entity.setDeepThinking(session.deepThinking());
        entity.setStatus(session.status());
        entity.setCreatedAt(session.createdAt());
        entity.setUpdatedAt(session.updatedAt());
        return entity;
    }

    private static ConversationSession toDomain(SessionEntity entity) {
        return new ConversationSession(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getToolMode(),
                entity.getDeepThinking(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ConversationMessage toMessage(MessageEntity entity) {
        return new ConversationMessage(
                entity.getId(),
                MessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                entity.getTokens(),
                readMetadata(entity.getMetadata()),
                entity.getCreatedAt()
        );
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("消息元数据无法序列化");
        }
    }

    private Map<String, Object> readMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("消息元数据无法解析");
        }
    }
}
