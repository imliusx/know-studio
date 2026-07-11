package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.DocumentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository {

    Optional<DocumentRecord> findDocument(long workspaceId, long documentId);

    Optional<DocumentRecord> findReadyDocumentByHash(long workspaceId, String contentHash);

    Optional<DocumentRecord> findDocumentByHash(long workspaceId, String contentHash);

    Optional<UploadSession> findActiveUploadSessionByHash(long workspaceId, String contentHash);

    void insertDocument(DocumentRecord document);

    void insertUploadSession(UploadSession session);

    Optional<UploadSession> findUploadSession(long workspaceId, long sessionId);

    List<UploadChunk> findUploadChunks(long sessionId);

    void insertUploadChunk(UploadChunk chunk);

    void completeUploadSession(long sessionId, long documentId);

    void ensureIngestionJob(long jobId, long workspaceId, long documentId);

    boolean claimDocumentForProcessing(long workspaceId, long documentId);

    void replaceDocumentChunks(long workspaceId, long documentId, List<DocumentChunk> chunks);

    void markDocumentReady(long workspaceId, long documentId, String previewText, int chunkCount);

    void markDocumentFailed(long workspaceId, long documentId, String reason);

    void markIngestionCompleted(long documentId);

    void markIngestionFailed(long documentId, String reason);

    void appendNodeLog(long documentId, String node, String status, long elapsedMillis, String detail);

    List<DocumentRecord> findStaleProcessing(Instant updatedBefore, int limit);

    List<DocumentRecord> recoverStaleProcessing(Instant updatedBefore, int limit);

    void deferRecoveredDocument(long workspaceId, long documentId, String reason);

    default DocumentStatus status(long workspaceId, long documentId) {
        return findDocument(workspaceId, documentId)
                .map(DocumentRecord::status)
                .orElse(null);
    }
}
