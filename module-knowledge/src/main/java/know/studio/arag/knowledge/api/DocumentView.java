package know.studio.arag.knowledge.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record DocumentView(
        @JsonLongId long id,
        @JsonLongId long knowledgeBaseId,
        String fileName,
        String contentType,
        long fileSize,
        String contentHash,
        DocumentStatus status,
        int chunkCount,
        String failureReason,
        String previewText
) {
}
