package know.studio.arag.knowledge.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import know.studio.arag.knowledge.api.DocumentStatus;
import know.studio.arag.knowledge.domain.ChunkStatus;
import know.studio.arag.knowledge.domain.DocumentChunk;
import know.studio.arag.knowledge.domain.DocumentRecord;
import know.studio.arag.knowledge.domain.IngestionStatus;
import know.studio.arag.knowledge.domain.KnowledgeRepository;
import know.studio.arag.knowledge.domain.UploadChunk;
import know.studio.arag.knowledge.domain.UploadSession;
import know.studio.arag.knowledge.domain.UploadStatus;
import know.studio.arag.knowledge.infra.persistence.entity.DocumentChunkEntity;
import know.studio.arag.knowledge.infra.persistence.entity.DocumentEntity;
import know.studio.arag.knowledge.infra.persistence.entity.IngestionJobEntity;
import know.studio.arag.knowledge.infra.persistence.entity.UploadChunkEntity;
import know.studio.arag.knowledge.infra.persistence.entity.UploadSessionEntity;
import know.studio.arag.knowledge.infra.persistence.mapper.DocumentChunkMapper;
import know.studio.arag.knowledge.infra.persistence.mapper.DocumentMapper;
import know.studio.arag.knowledge.infra.persistence.mapper.IngestionJobMapper;
import know.studio.arag.knowledge.infra.persistence.mapper.KnowledgeCommandMapper;
import know.studio.arag.knowledge.infra.persistence.mapper.UploadChunkMapper;
import know.studio.arag.knowledge.infra.persistence.mapper.UploadSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisKnowledgeRepository implements KnowledgeRepository {

    private final DocumentMapper documentMapper;
    private final UploadSessionMapper uploadSessionMapper;
    private final UploadChunkMapper uploadChunkMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final IngestionJobMapper ingestionJobMapper;
    private final KnowledgeCommandMapper commandMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<DocumentRecord> findDocument(long knowledgeBaseId, long documentId) {
        DocumentEntity entity = documentMapper.selectOne(Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId));
        return Optional.ofNullable(entity).map(MybatisKnowledgeRepository::toDomain);
    }

    @Override
    public List<DocumentRecord> findDocuments(long knowledgeBaseId, DocumentStatus status, String fileName) {
        var query = Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .ne(DocumentEntity::getStatus, DocumentStatus.DELETED.name())
                .orderByDesc(DocumentEntity::getUpdatedAt);
        if (status != null && status != DocumentStatus.DELETED) {
            query.eq(DocumentEntity::getStatus, status.name());
        }
        if (fileName != null) {
            query.like(DocumentEntity::getFileName, fileName);
        }
        return documentMapper.selectList(query).stream()
                .map(MybatisKnowledgeRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<DocumentRecord> findReadyDocumentByHash(long knowledgeBaseId, String contentHash) {
        DocumentEntity entity = documentMapper.selectOne(Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getContentHash, contentHash)
                .eq(DocumentEntity::getStatus, DocumentStatus.READY.name()));
        return Optional.ofNullable(entity).map(MybatisKnowledgeRepository::toDomain);
    }

    @Override
    public Optional<DocumentRecord> findDocumentByHash(long knowledgeBaseId, String contentHash) {
        DocumentEntity entity = documentMapper.selectOne(Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getContentHash, contentHash));
        return Optional.ofNullable(entity).map(MybatisKnowledgeRepository::toDomain);
    }

    @Override
    public Optional<UploadSession> findActiveUploadSessionByHash(long knowledgeBaseId, String contentHash) {
        UploadSessionEntity entity = uploadSessionMapper.selectOne(Wrappers.<UploadSessionEntity>lambdaQuery()
                .eq(UploadSessionEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(UploadSessionEntity::getContentHash, contentHash)
                .eq(UploadSessionEntity::getStatus, UploadStatus.UPLOADING.name())
                .gt(UploadSessionEntity::getExpiresAt, Instant.now())
                .orderByDesc(UploadSessionEntity::getId)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(MybatisKnowledgeRepository::toDomain);
    }

    @Override
    public void insertDocument(DocumentRecord document) {
        documentMapper.insert(toEntity(document));
    }

    @Override
    public void insertUploadSession(UploadSession session) {
        uploadSessionMapper.insert(toEntity(session));
    }

    @Override
    public Optional<UploadSession> findUploadSession(long knowledgeBaseId, long sessionId) {
        UploadSessionEntity entity = uploadSessionMapper.selectOne(Wrappers.<UploadSessionEntity>lambdaQuery()
                .eq(UploadSessionEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(UploadSessionEntity::getId, sessionId));
        return Optional.ofNullable(entity).map(MybatisKnowledgeRepository::toDomain);
    }

    @Override
    public List<UploadChunk> findUploadChunks(long sessionId) {
        return uploadChunkMapper.selectList(Wrappers.<UploadChunkEntity>lambdaQuery()
                        .eq(UploadChunkEntity::getUploadSessionId, sessionId)
                        .orderByAsc(UploadChunkEntity::getChunkIndex))
                .stream()
                .map(MybatisKnowledgeRepository::toDomain)
                .toList();
    }

    @Override
    public void insertUploadChunk(UploadChunk chunk) {
        uploadChunkMapper.insert(toEntity(chunk));
    }

    @Override
    public void completeUploadSession(long sessionId, long documentId) {
        uploadSessionMapper.update(Wrappers.<UploadSessionEntity>lambdaUpdate()
                .eq(UploadSessionEntity::getId, sessionId)
                .set(UploadSessionEntity::getStatus, UploadStatus.COMPLETED.name())
                .set(UploadSessionEntity::getDocumentId, documentId)
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void ensureIngestionJob(long jobId, long knowledgeBaseId, long documentId) {
        commandMapper.ensureIngestionJob(jobId, knowledgeBaseId, documentId);
    }

    @Override
    public boolean claimDocumentForProcessing(long knowledgeBaseId, long documentId) {
        if (commandMapper.claimDocumentForProcessing(knowledgeBaseId, documentId) == 0) {
            return false;
        }
        ingestionJobMapper.update(Wrappers.<IngestionJobEntity>lambdaUpdate()
                .eq(IngestionJobEntity::getDocumentId, documentId)
                .set(IngestionJobEntity::getStatus, IngestionStatus.PROCESSING.name())
                .set(IngestionJobEntity::getError, null)
                .setSql("started_at = COALESCE(started_at, CURRENT_TIMESTAMP)")
                .setSql("updated_at = CURRENT_TIMESTAMP"));
        return true;
    }

    @Override
    public void replaceDocumentChunks(long knowledgeBaseId, long documentId, List<DocumentChunk> chunks) {
        documentChunkMapper.delete(Wrappers.<DocumentChunkEntity>lambdaQuery()
                .eq(DocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentChunkEntity::getDocumentId, documentId));
        if (!chunks.isEmpty()) {
            commandMapper.insertDocumentChunks(chunks.stream().map(MybatisKnowledgeRepository::toEntity).toList());
        }
    }

    @Override
    public void markDocumentReady(long knowledgeBaseId, long documentId, String previewText, int chunkCount) {
        documentMapper.update(Wrappers.<DocumentEntity>lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId)
                .set(DocumentEntity::getStatus, DocumentStatus.READY.name())
                .set(DocumentEntity::getPreviewText, previewText)
                .set(DocumentEntity::getChunkCount, chunkCount)
                .set(DocumentEntity::getFailureReason, null)
                .setSql("processed_at = CURRENT_TIMESTAMP")
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void markDocumentFailed(long knowledgeBaseId, long documentId, String reason) {
        documentMapper.update(Wrappers.<DocumentEntity>lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId)
                .set(DocumentEntity::getStatus, DocumentStatus.FAILED.name())
                .set(DocumentEntity::getFailureReason, truncate(reason))
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void markIngestionCompleted(long documentId) {
        ingestionJobMapper.update(Wrappers.<IngestionJobEntity>lambdaUpdate()
                .eq(IngestionJobEntity::getDocumentId, documentId)
                .set(IngestionJobEntity::getStatus, IngestionStatus.COMPLETED.name())
                .setSql("finished_at = CURRENT_TIMESTAMP")
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void markIngestionFailed(long documentId, String reason) {
        ingestionJobMapper.update(Wrappers.<IngestionJobEntity>lambdaUpdate()
                .eq(IngestionJobEntity::getDocumentId, documentId)
                .set(IngestionJobEntity::getStatus, IngestionStatus.FAILED.name())
                .set(IngestionJobEntity::getError, truncate(reason))
                .setSql("finished_at = CURRENT_TIMESTAMP")
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void appendNodeLog(long documentId, String node, String status, long elapsedMillis, String detail) {
        commandMapper.appendNodeLog(documentId, toJson(List.of(Map.of(
                "node", node,
                "status", status,
                "elapsedMs", elapsedMillis,
                "detail", detail == null ? "" : truncate(detail),
                "at", Instant.now().toString()
        ))));
    }

    @Override
    public List<DocumentRecord> findStaleProcessing(Instant updatedBefore, int limit) {
        return documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery()
                        .eq(DocumentEntity::getStatus, DocumentStatus.PROCESSING.name())
                        .lt(DocumentEntity::getUpdatedAt, updatedBefore)
                        .orderByAsc(DocumentEntity::getUpdatedAt)
                        .last("LIMIT " + Math.max(1, limit)))
                .stream()
                .map(MybatisKnowledgeRepository::toDomain)
                .toList();
    }

    @Override
    public List<DocumentRecord> recoverStaleProcessing(Instant updatedBefore, int limit) {
        List<DocumentRecord> recovered = commandMapper.recoverStaleProcessing(
                        updatedBefore,
                        Math.max(1, limit)
                ).stream()
                .map(MybatisKnowledgeRepository::toDomain)
                .toList();
        recovered.forEach(document -> markIngestionFailed(
                document.id(),
                "Recovered stale ingestion task"
        ));
        return recovered;
    }

    @Override
    public void deferRecoveredDocument(long knowledgeBaseId, long documentId, String reason) {
        documentMapper.update(Wrappers.<DocumentEntity>lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId)
                .eq(DocumentEntity::getStatus, DocumentStatus.FAILED.name())
                .set(DocumentEntity::getStatus, DocumentStatus.PROCESSING.name())
                .set(DocumentEntity::getFailureReason, truncate(reason))
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public boolean markDocumentDeleted(long knowledgeBaseId, long documentId) {
        return documentMapper.update(Wrappers.<DocumentEntity>lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId)
                .in(DocumentEntity::getStatus, DocumentStatus.PENDING.name(), DocumentStatus.READY.name(),
                        DocumentStatus.FAILED.name())
                .set(DocumentEntity::getStatus, DocumentStatus.DELETED.name())
                .setSql("updated_at = CURRENT_TIMESTAMP")) == 1;
    }

    @Override
    public boolean resetFailedDocument(long knowledgeBaseId, long documentId) {
        return documentMapper.update(Wrappers.<DocumentEntity>lambdaUpdate()
                .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentEntity::getId, documentId)
                .eq(DocumentEntity::getStatus, DocumentStatus.FAILED.name())
                .set(DocumentEntity::getStatus, DocumentStatus.PENDING.name())
                .set(DocumentEntity::getFailureReason, null)
                .setSql("updated_at = CURRENT_TIMESTAMP")) == 1;
    }

    @Override
    public void markDocumentChunksDeleted(long knowledgeBaseId, long documentId) {
        documentChunkMapper.update(Wrappers.<DocumentChunkEntity>lambdaUpdate()
                .eq(DocumentChunkEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(DocumentChunkEntity::getDocumentId, documentId)
                .set(DocumentChunkEntity::getDeleted, true));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize ingestion node log", exception);
        }
    }

    private static DocumentEntity toEntity(DocumentRecord document) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.id());
        entity.setKnowledgeBaseId(document.knowledgeBaseId());
        entity.setFileName(document.fileName());
        entity.setObjectKey(document.objectKey());
        entity.setContentType(document.contentType());
        entity.setFileSize(document.fileSize());
        entity.setContentHash(document.contentHash());
        entity.setStatus(document.status().name());
        entity.setPreviewText(document.previewText());
        entity.setFailureReason(document.failureReason());
        entity.setChunkCount(document.chunkCount());
        entity.setCreatedBy(document.createdBy());
        return entity;
    }

    private static UploadSessionEntity toEntity(UploadSession session) {
        UploadSessionEntity entity = new UploadSessionEntity();
        entity.setId(session.id());
        entity.setKnowledgeBaseId(session.knowledgeBaseId());
        entity.setFileName(session.fileName());
        entity.setContentType(session.contentType());
        entity.setFileSize(session.fileSize());
        entity.setContentHash(session.contentHash());
        entity.setTotalChunks(session.totalChunks());
        entity.setStatus(session.status().name());
        entity.setCreatedBy(session.createdBy());
        entity.setDocumentId(session.documentId());
        entity.setExpiresAt(session.expiresAt());
        return entity;
    }

    private static UploadChunkEntity toEntity(UploadChunk chunk) {
        UploadChunkEntity entity = new UploadChunkEntity();
        entity.setId(chunk.id());
        entity.setUploadSessionId(chunk.uploadSessionId());
        entity.setChunkIndex(chunk.chunkIndex());
        entity.setObjectKey(chunk.objectKey());
        entity.setChunkSize(chunk.chunkSize());
        entity.setChunkHash(chunk.chunkHash());
        return entity;
    }

    private static DocumentChunkEntity toEntity(DocumentChunk chunk) {
        DocumentChunkEntity entity = new DocumentChunkEntity();
        entity.setId(chunk.id());
        entity.setKnowledgeBaseId(chunk.knowledgeBaseId());
        entity.setDocumentId(chunk.documentId());
        entity.setChunkIndex(chunk.chunkIndex());
        entity.setChunkText(chunk.text());
        entity.setCharStart(chunk.charStart());
        entity.setCharEnd(chunk.charEnd());
        entity.setSectionPath(chunk.sectionPath());
        entity.setStatus(ChunkStatus.READY.name());
        entity.setDeleted(false);
        return entity;
    }

    private static DocumentRecord toDomain(DocumentEntity entity) {
        return new DocumentRecord(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getFileName(),
                entity.getObjectKey(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getContentHash(),
                DocumentStatus.valueOf(entity.getStatus()),
                entity.getPreviewText(),
                entity.getFailureReason(),
                entity.getChunkCount(),
                entity.getCreatedBy()
        );
    }

    private static UploadSession toDomain(UploadSessionEntity entity) {
        return new UploadSession(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getContentHash(),
                entity.getTotalChunks(),
                UploadStatus.valueOf(entity.getStatus()),
                entity.getCreatedBy(),
                entity.getDocumentId(),
                entity.getExpiresAt()
        );
    }

    private static UploadChunk toDomain(UploadChunkEntity entity) {
        return new UploadChunk(
                entity.getId(),
                entity.getUploadSessionId(),
                entity.getChunkIndex(),
                entity.getObjectKey(),
                entity.getChunkSize(),
                entity.getChunkHash()
        );
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 2_000) {
            return value;
        }
        return value.substring(0, 2_000);
    }
}
