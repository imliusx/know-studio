package know.studio.knowledge.domain;

import know.studio.knowledge.api.DocumentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository {

    Optional<DocumentRecord> findDocument(long knowledgeBaseId, long documentId);

    List<DocumentRecord> findDocuments(long knowledgeBaseId, DocumentStatus status, String fileName);

    Optional<DocumentRecord> findReadyDocumentByHash(long knowledgeBaseId, String contentHash);

    Optional<DocumentRecord> findDocumentByHash(long knowledgeBaseId, String contentHash);

    Optional<UploadSession> findActiveUploadSessionByHash(long knowledgeBaseId, String contentHash);

    void insertDocument(DocumentRecord document);

    void insertUploadSession(UploadSession session);

    Optional<UploadSession> findUploadSession(long knowledgeBaseId, long sessionId);

    List<UploadChunk> findUploadChunks(long sessionId);

    void insertUploadChunk(UploadChunk chunk);

    void completeUploadSession(long sessionId, long documentId);

    void ensureIngestionJob(long jobId, long knowledgeBaseId, long documentId);

    boolean claimDocumentForProcessing(long knowledgeBaseId, long documentId);

    void replaceDocumentChunks(long knowledgeBaseId, long documentId, List<DocumentChunk> chunks);

    void markDocumentReady(long knowledgeBaseId, long documentId, String previewText, int chunkCount);

    void markDocumentFailed(long knowledgeBaseId, long documentId, String reason);

    void markIngestionCompleted(long documentId);

    void markIngestionFailed(long documentId, String reason);

    void appendNodeLog(long documentId, String node, String status, long elapsedMillis, String detail);

    List<DocumentRecord> findStaleProcessing(Instant updatedBefore, int limit);

    List<DocumentRecord> recoverStaleProcessing(Instant updatedBefore, int limit);

    void deferRecoveredDocument(long knowledgeBaseId, long documentId, String reason);

    boolean markDocumentDeleted(long knowledgeBaseId, long documentId);

    boolean resetFailedDocument(long knowledgeBaseId, long documentId);

    void markDocumentChunksDeleted(long knowledgeBaseId, long documentId);

    default DocumentStatus status(long knowledgeBaseId, long documentId) {
        return findDocument(knowledgeBaseId, documentId)
                .map(DocumentRecord::status)
                .orElse(null);
    }
}
