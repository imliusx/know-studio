package know.studio.arag.knowledge.domain;

import know.studio.arag.knowledge.api.DocumentStatus;

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
