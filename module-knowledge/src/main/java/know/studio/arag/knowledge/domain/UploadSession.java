package know.studio.arag.knowledge.domain;

import java.time.Instant;

public record UploadSession(
        long id,
        long knowledgeBaseId,
        String fileName,
        String contentType,
        long fileSize,
        String contentHash,
        int totalChunks,
        UploadStatus status,
        long createdBy,
        Long documentId,
        Instant expiresAt
) {
}
