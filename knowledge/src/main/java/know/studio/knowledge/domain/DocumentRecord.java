package know.studio.knowledge.domain;

import know.studio.knowledge.api.DocumentStatus;

public record DocumentRecord(
        long id,
        long knowledgeBaseId,
        String fileName,
        String objectKey,
        String contentType,
        long fileSize,
        String contentHash,
        DocumentStatus status,
        String previewText,
        String failureReason,
        int chunkCount,
        long createdBy
) {
}
