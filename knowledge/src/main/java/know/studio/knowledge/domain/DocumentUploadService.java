package know.studio.knowledge.domain;

import know.studio.auth.api.CurrentIdentity;
import know.studio.auth.api.IdentityApi;
import know.studio.knowledge.api.KnowledgeAccessApi;
import know.studio.knowledge.api.DocumentStatus;
import know.studio.common.exception.BusinessException;
import know.studio.common.exception.ErrorCode;
import know.studio.common.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadService {

    private static final Pattern SHA256_PATTERN = Pattern.compile("[a-f0-9]{64}");
    private static final long MIN_COMPOSE_PART_SIZE = 5L * 1024 * 1024;
    private static final int MAX_CHUNKS = 10_000;

    private final KnowledgeRepository repository;
    private final ObjectStoragePort storage;
    private final IdentityApi identityApi;
    private final KnowledgeAccessApi knowledgeAccessApi;
    private final SnowflakeIdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final UploadPolicy uploadPolicy;

    @Transactional
    public UploadInitResult initiate(
            long knowledgeBaseId,
            String fileName,
            String contentType,
            long fileSize,
            String contentHash,
            int totalChunks
    ) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        CurrentIdentity current = identityApi.currentUser();
        String normalizedHash = normalizeHash(contentHash);
        validateInitiation(fileName, fileSize, totalChunks);

        DocumentRecord ready = repository.findReadyDocumentByHash(knowledgeBaseId, normalizedHash).orElse(null);
        if (ready != null) {
            return new UploadInitResult(true, ready.id(), null, List.of());
        }

        UploadSession active = repository.findActiveUploadSessionByHash(knowledgeBaseId, normalizedHash).orElse(null);
        if (active != null) {
            return new UploadInitResult(
                    false,
                    null,
                    active.id(),
                    repository.findUploadChunks(active.id()).stream().map(UploadChunk::chunkIndex).toList()
            );
        }

        long sessionId = idGenerator.nextId();
        repository.insertUploadSession(new UploadSession(
                sessionId,
                knowledgeBaseId,
                sanitizeFileName(fileName),
                contentType,
                fileSize,
                normalizedHash,
                totalChunks,
                UploadStatus.UPLOADING,
                current.userId(),
                null,
                Instant.now().plus(uploadPolicy.sessionExpiry())
        ));
        return new UploadInitResult(false, null, sessionId, List.of());
    }

    @Transactional
    public UploadProgress uploadChunk(
            long knowledgeBaseId,
            long sessionId,
            int chunkIndex,
            long chunkSize,
            String contentType,
            String expectedHash,
            InputStreamProvider content
    ) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        UploadSession session = requireActiveSession(knowledgeBaseId, sessionId);
        validateChunk(session, chunkIndex, chunkSize);
        String normalizedHash = normalizeHash(expectedHash);

        UploadChunk existing = repository.findUploadChunks(sessionId).stream()
                .filter(chunk -> chunk.chunkIndex() == chunkIndex)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (existing.chunkHash().equals(normalizedHash) && existing.chunkSize() == chunkSize) {
                return progress(session);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "该分片序号已上传且内容不同");
        }

        String actualHash;
        try (InputStream input = content.open()) {
            actualHash = Hashing.sha256(input);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取上传分片");
        }
        if (!actualHash.equals(normalizedHash)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分片 SHA-256 校验失败");
        }

        String objectKey = chunkObjectKey(knowledgeBaseId, sessionId, chunkIndex);
        try (InputStream input = content.open()) {
            storage.put(objectKey, input, chunkSize, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to reopen upload chunk", exception);
        }

        try {
            repository.insertUploadChunk(new UploadChunk(
                    idGenerator.nextId(),
                    sessionId,
                    chunkIndex,
                    objectKey,
                    chunkSize,
                    normalizedHash
            ));
        } catch (DuplicateKeyException exception) {
            UploadChunk concurrent = repository.findUploadChunks(sessionId).stream()
                    .filter(chunk -> chunk.chunkIndex() == chunkIndex)
                    .findFirst()
                    .orElseThrow(() -> exception);
            if (!concurrent.chunkHash().equals(normalizedHash)) {
                throw new BusinessException(ErrorCode.CONFLICT, "该分片被并发写入不同内容");
            }
        }
        return progress(session);
    }

    public UploadProgress progress(long knowledgeBaseId, long sessionId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        UploadSession session = repository.findUploadSession(knowledgeBaseId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传会话不存在"));
        return progress(session);
    }

    @Transactional
    public long complete(long knowledgeBaseId, long sessionId) {
        knowledgeAccessApi.requireManageable(knowledgeBaseId);
        UploadSession session = repository.findUploadSession(knowledgeBaseId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传会话不存在"));
        if (session.status() == UploadStatus.COMPLETED && session.documentId() != null) {
            return session.documentId();
        }
        requireActiveSession(knowledgeBaseId, sessionId);

        List<UploadChunk> chunks = repository.findUploadChunks(sessionId);
        validateCompleteChunks(session, chunks);
        long documentId = idGenerator.nextId();
        String targetKey = documentObjectKey(knowledgeBaseId, documentId, session.fileName());
        storage.compose(targetKey, chunks.stream().map(UploadChunk::objectKey).toList());

        try (InputStream combined = storage.open(targetKey)) {
            String actualHash = Hashing.sha256(combined);
            if (!actualHash.equals(session.contentHash())) {
                storage.delete(List.of(targetKey));
                throw new BusinessException(ErrorCode.BAD_REQUEST, "完整文件 SHA-256 校验失败");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to verify composed object", exception);
        }

        DocumentRecord document = new DocumentRecord(
                documentId,
                knowledgeBaseId,
                session.fileName(),
                targetKey,
                session.contentType(),
                session.fileSize(),
                session.contentHash(),
                DocumentStatus.PENDING,
                null,
                null,
                0,
                session.createdBy()
        );
        try {
            repository.insertDocument(document);
        } catch (DuplicateKeyException exception) {
            DocumentRecord concurrent = repository.findDocumentByHash(knowledgeBaseId, session.contentHash())
                    .orElseThrow(() -> exception);
            storage.delete(List.of(targetKey));
            repository.completeUploadSession(sessionId, concurrent.id());
            return concurrent.id();
        }

        repository.completeUploadSession(sessionId, documentId);
        eventPublisher.publishEvent(new DocumentUploadCompletedEvent(knowledgeBaseId, documentId));
        try {
            storage.delete(chunks.stream().map(UploadChunk::objectKey).toList());
        } catch (RuntimeException exception) {
            log.warn("Failed to clean upload chunks sessionId={}", sessionId, exception);
        }
        return documentId;
    }

    private UploadSession requireActiveSession(long knowledgeBaseId, long sessionId) {
        UploadSession session = repository.findUploadSession(knowledgeBaseId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传会话不存在"));
        if (session.status() != UploadStatus.UPLOADING) {
            throw new BusinessException(ErrorCode.CONFLICT, "上传会话已结束");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "上传会话已过期");
        }
        return session;
    }

    private UploadProgress progress(UploadSession session) {
        List<Integer> uploaded = repository.findUploadChunks(session.id()).stream()
                .map(UploadChunk::chunkIndex)
                .toList();
        return new UploadProgress(session.id(), session.totalChunks(), uploaded, session.status(), session.documentId());
    }

    private static void validateInitiation(String fileName, long fileSize, int totalChunks) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        if (fileSize <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小必须大于 0");
        }
        if (totalChunks < 1 || totalChunks > MAX_CHUNKS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分片数量超出允许范围");
        }
    }

    private static void validateChunk(UploadSession session, int chunkIndex, long chunkSize) {
        if (chunkIndex < 0 || chunkIndex >= session.totalChunks()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分片序号超出范围");
        }
        if (chunkSize <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分片大小必须大于 0");
        }
        if (chunkIndex < session.totalChunks() - 1 && chunkSize < MIN_COMPOSE_PART_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非末尾分片不能小于 5 MiB");
        }
    }

    private static void validateCompleteChunks(UploadSession session, List<UploadChunk> chunks) {
        if (chunks.size() != session.totalChunks()) {
            throw new BusinessException(ErrorCode.CONFLICT, "上传分片尚未完成");
        }
        for (int index = 0; index < chunks.size(); index++) {
            if (chunks.get(index).chunkIndex() != index) {
                throw new BusinessException(ErrorCode.CONFLICT, "上传分片不连续");
            }
        }
        long uploadedSize = chunks.stream().mapToLong(UploadChunk::chunkSize).sum();
        if (uploadedSize != session.fileSize()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传分片总大小与文件大小不一致");
        }
    }

    private static String normalizeHash(String hash) {
        String normalized = hash == null ? "" : hash.trim().toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SHA-256 格式不正确");
        }
        return normalized;
    }

    private static String sanitizeFileName(String fileName) {
        String sanitized = fileName.replace('\\', '_').replace('/', '_').trim();
        if (sanitized.length() > 255) {
            return sanitized.substring(sanitized.length() - 255);
        }
        return sanitized;
    }

    private static String chunkObjectKey(long knowledgeBaseId, long sessionId, int chunkIndex) {
        return "uploads/" + knowledgeBaseId + "/" + sessionId + "/" + String.format("%05d", chunkIndex);
    }

    private static String documentObjectKey(long knowledgeBaseId, long documentId, String fileName) {
        return "documents/" + knowledgeBaseId + "/" + documentId + "/" + sanitizeFileName(fileName);
    }
}
